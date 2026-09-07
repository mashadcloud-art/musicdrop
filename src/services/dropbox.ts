import * as AuthSession from "expo-auth-session";
import * as WebBrowser from "expo-web-browser";
import { File, FileMode, UploadType } from "expo-file-system";
import { getTokens, saveTokens, clearTokens } from "../lib/secureStore";
import { SharedFile, StoredTokens, UploadResult } from "../types";

WebBrowser.maybeCompleteAuthSession();

// From the Dropbox App Console (App key). Dropbox's PKCE flow needs no
// client secret for a mobile app — see README.md.
const DROPBOX_CLIENT_ID = process.env.EXPO_PUBLIC_DROPBOX_CLIENT_ID ?? "";

const DISCOVERY = {
  authorizationEndpoint: "https://www.dropbox.com/oauth2/authorize",
  tokenEndpoint: "https://api.dropboxapi.com/oauth2/token",
};

// Dropbox's single-shot /files/upload endpoint hard-caps at 150MB — past
// that we have to switch to session-based chunked upload. 8MB chunks keep
// memory flat while still moving large videos fast over a good connection.
const SINGLE_SHOT_LIMIT = 150 * 1024 * 1024;
const CHUNK_SIZE = 8 * 1024 * 1024;

function nowMs() {
  return Date.now();
}

export async function connectDropbox(): Promise<void> {
  if (!DROPBOX_CLIENT_ID) {
    throw new Error(
      "Missing EXPO_PUBLIC_DROPBOX_CLIENT_ID — see README.md to create a Dropbox app."
    );
  }

  const redirectUri = AuthSession.makeRedirectUri({ scheme: "filedrop" });

  const request = new AuthSession.AuthRequest({
    clientId: DROPBOX_CLIENT_ID,
    scopes: ["files.content.write", "files.content.read", "sharing.write", "account_info.read"],
    redirectUri,
    responseType: AuthSession.ResponseType.Code,
    usePKCE: true,
    extraParams: { token_access_type: "offline" },
  });

  const result = await request.promptAsync(DISCOVERY);
  if (result.type !== "success" || !result.params.code) {
    throw new Error(
      result.type === "cancel" ? "Sign-in cancelled." : "Dropbox sign-in failed."
    );
  }

  const tokenResponse = await AuthSession.exchangeCodeAsync(
    {
      clientId: DROPBOX_CLIENT_ID,
      code: result.params.code,
      redirectUri,
      extraParams: { code_verifier: request.codeVerifier ?? "" },
    },
    DISCOVERY
  );

  const tokens: StoredTokens = {
    accessToken: tokenResponse.accessToken,
    refreshToken: tokenResponse.refreshToken,
    expiresAt: nowMs() + (tokenResponse.expiresIn ?? 14400) * 1000,
  };

  const account = await fetch("https://api.dropboxapi.com/2/users/get_current_account", {
    method: "POST",
    headers: { Authorization: `Bearer ${tokens.accessToken}` },
  })
    .then((r) => (r.ok ? r.json() : null))
    .catch(() => null);
  if (account?.email) tokens.accountLabel = account.email;

  await saveTokens("dropbox", tokens);
}

export async function disconnectDropbox(): Promise<void> {
  const tokens = await getTokens("dropbox");
  if (tokens?.accessToken) {
    await fetch("https://api.dropboxapi.com/2/auth/token/revoke", {
      method: "POST",
      headers: { Authorization: `Bearer ${tokens.accessToken}` },
    }).catch(() => {});
  }
  await clearTokens("dropbox");
}

async function getValidAccessToken(): Promise<string> {
  const tokens = await getTokens("dropbox");
  if (!tokens) throw new Error("Dropbox is not connected.");

  if (tokens.expiresAt - nowMs() > 60_000) {
    return tokens.accessToken;
  }
  if (!tokens.refreshToken) {
    throw new Error("Dropbox session expired — please reconnect.");
  }

  const refreshed = await AuthSession.refreshAsync(
    { clientId: DROPBOX_CLIENT_ID, refreshToken: tokens.refreshToken },
    DISCOVERY
  );

  const next: StoredTokens = {
    accessToken: refreshed.accessToken,
    refreshToken: refreshed.refreshToken ?? tokens.refreshToken,
    expiresAt: nowMs() + (refreshed.expiresIn ?? 14400) * 1000,
    accountLabel: tokens.accountLabel,
  };
  await saveTokens("dropbox", next);
  return next.accessToken;
}

function dropboxApiArg(value: object): string {
  // Dropbox content-endpoints take their JSON args in a header, and it must
  // be ASCII-safe -- non-ASCII characters (e.g. an emoji in a filename) get
  // \u-escaped rather than sent raw, per Dropbox's own documented rule.
  return JSON.stringify(value).replace(/[^\x00-\x7F]/g, (c) => {
    return "\\u" + c.charCodeAt(0).toString(16).padStart(4, "0");
  });
}

async function shareLink(accessToken: string, path: string): Promise<string> {
  const create = await fetch(
    "https://api.dropboxapi.com/2/sharing/create_shared_link_with_settings",
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ path }),
    }
  );
  if (create.ok) {
    const json = await create.json();
    return json.url;
  }

  // Link already exists for this file — look it up instead of failing.
  const existing = await fetch("https://api.dropboxapi.com/2/sharing/list_shared_links", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ path, direct_only: true }),
  });
  const json = await existing.json();
  const url = json?.links?.[0]?.url;
  if (!url) throw new Error("Could not create a Dropbox share link.");
  return url;
}

async function uploadSingleShot(
  accessToken: string,
  file: SharedFile,
  dropboxPath: string,
  onProgress?: (fraction: number) => void
): Promise<void> {
  const task = new File(file.path).createUploadTask(
    "https://content.dropboxapi.com/2/files/upload",
    {
      httpMethod: "POST",
      uploadType: UploadType.BINARY_CONTENT,
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/octet-stream",
        "Dropbox-API-Arg": dropboxApiArg({
          path: dropboxPath,
          mode: "add",
          autorename: true,
          mute: true,
        }),
      },
      onProgress: (data) => {
        if (data.totalBytes > 0) onProgress?.(data.bytesSent / data.totalBytes);
      },
    }
  );
  const result = await task.uploadAsync();
  if (!result || result.status < 200 || result.status >= 300) {
    throw new Error(`Dropbox upload failed (${result?.status ?? "network error"}).`);
  }
}

async function uploadChunked(
  accessToken: string,
  file: SharedFile,
  dropboxPath: string,
  totalSize: number,
  onProgress?: (fraction: number) => void
): Promise<void> {
  let offset = 0;
  let sessionId: string | null = null;
  const handle = new File(file.path).open(FileMode.ReadOnly);

  try {
  while (offset < totalSize) {
    const length = Math.min(CHUNK_SIZE, totalSize - offset);
    handle.offset = offset;
    const chunk = handle.readBytes(length); // Uint8Array, read straight off disk
    // fetch's body type wants a plain ArrayBuffer, not a view over one —
    // slice out exactly this chunk's bytes.
    const binary = chunk.buffer.slice(chunk.byteOffset, chunk.byteOffset + chunk.byteLength);
    const isLast = offset + length >= totalSize;

    if (sessionId === null) {
      const startRes = await fetch("https://content.dropboxapi.com/2/files/upload_session/start", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/octet-stream",
          "Dropbox-API-Arg": dropboxApiArg({ close: false }),
        },
        body: binary,
      });
      if (!startRes.ok) throw new Error("Could not start Dropbox upload session.");
      sessionId = (await startRes.json()).session_id;
    } else if (!isLast) {
      const appendRes = await fetch("https://content.dropboxapi.com/2/files/upload_session/append_v2", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/octet-stream",
          "Dropbox-API-Arg": dropboxApiArg({
            cursor: { session_id: sessionId, offset },
            close: false,
          }),
        },
        body: binary,
      });
      if (!appendRes.ok) throw new Error("Dropbox upload chunk failed.");
    } else {
      const finishRes = await fetch("https://content.dropboxapi.com/2/files/upload_session/finish", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/octet-stream",
          "Dropbox-API-Arg": dropboxApiArg({
            cursor: { session_id: sessionId, offset },
            commit: { path: dropboxPath, mode: "add", autorename: true, mute: true },
          }),
        },
        body: binary,
      });
      if (!finishRes.ok) throw new Error("Could not finish Dropbox upload.");
    }

    offset += length;
    onProgress?.(offset / totalSize);
  }
  } finally {
    handle.close();
  }
}

/**
 * Uploads a file straight from the device to the user's own Dropbox, then
 * returns a public share link. Small/medium files go up in a single
 * natively-streamed request; anything over Dropbox's 150MB single-request
 * cap is split into chunked upload-session calls instead.
 */
export async function uploadToDropbox(
  file: SharedFile,
  onProgress?: (fraction: number) => void
): Promise<UploadResult> {
  const accessToken = await getValidAccessToken();
  const dropboxPath = `/FileDrop/${file.fileName}`;

  const diskFile = new File(file.path);
  const size = file.size ?? (diskFile.exists ? diskFile.size : 0);

  if (size > 0 && size <= SINGLE_SHOT_LIMIT) {
    await uploadSingleShot(accessToken, file, dropboxPath, onProgress);
  } else {
    await uploadChunked(accessToken, file, dropboxPath, size, onProgress);
  }

  const link = await shareLink(accessToken, dropboxPath);

  return {
    provider: "dropbox",
    fileName: file.fileName,
    fileId: dropboxPath,
    link,
    createdAt: new Date().toISOString(),
  };
}
