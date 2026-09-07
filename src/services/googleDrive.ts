import * as AuthSession from "expo-auth-session";
import * as WebBrowser from "expo-web-browser";
import { File, UploadType } from "expo-file-system";
import { getTokens, saveTokens, clearTokens } from "../lib/secureStore";
import { SharedFile, StoredTokens, UploadResult } from "../types";

WebBrowser.maybeCompleteAuthSession();

// Fill these in from a Google Cloud Console OAuth client (type: Android / iOS,
// or "Web application" if you route through an Expo AuthSession proxy).
// See README.md for the exact console steps.
const GOOGLE_CLIENT_ID = process.env.EXPO_PUBLIC_GOOGLE_CLIENT_ID ?? "";

const DISCOVERY = {
  authorizationEndpoint: "https://accounts.google.com/o/oauth2/v2/auth",
  tokenEndpoint: "https://oauth2.googleapis.com/token",
  revocationEndpoint: "https://oauth2.googleapis.com/revoke",
};

// drive.file (not full "drive") = the app can only see/manage files IT
// created or that the user explicitly opened with it. Deliberately narrow:
// it's enough for "upload and get a link", it keeps the user's other Drive
// files completely out of reach, and — importantly — it avoids Google's
// "restricted scope" security review that the broad `drive` scope requires
// before you can ship to real users.
const SCOPES = [
  "https://www.googleapis.com/auth/drive.file",
  "email",
];

function nowMs() {
  return Date.now();
}

/** Kicks off the Google sign-in flow and stores the resulting tokens. */
export async function connectGoogleDrive(): Promise<void> {
  if (!GOOGLE_CLIENT_ID) {
    throw new Error(
      "Missing EXPO_PUBLIC_GOOGLE_CLIENT_ID — see README.md to set up a Google Cloud OAuth client."
    );
  }

  const redirectUri = AuthSession.makeRedirectUri({ scheme: "filedrop" });

  const request = new AuthSession.AuthRequest({
    clientId: GOOGLE_CLIENT_ID,
    scopes: SCOPES,
    redirectUri,
    responseType: AuthSession.ResponseType.Code,
    usePKCE: true,
    extraParams: { access_type: "offline", prompt: "consent" },
  });

  const result = await request.promptAsync(DISCOVERY);
  if (result.type !== "success" || !result.params.code) {
    throw new Error(
      result.type === "cancel" ? "Sign-in cancelled." : "Google sign-in failed."
    );
  }

  const tokenResponse = await AuthSession.exchangeCodeAsync(
    {
      clientId: GOOGLE_CLIENT_ID,
      code: result.params.code,
      redirectUri,
      extraParams: { code_verifier: request.codeVerifier ?? "" },
    },
    DISCOVERY
  );

  const tokens: StoredTokens = {
    accessToken: tokenResponse.accessToken,
    refreshToken: tokenResponse.refreshToken,
    expiresAt: nowMs() + (tokenResponse.expiresIn ?? 3600) * 1000,
  };

  const profile = await fetch("https://www.googleapis.com/oauth2/v3/userinfo", {
    headers: { Authorization: `Bearer ${tokens.accessToken}` },
  })
    .then((r) => (r.ok ? r.json() : null))
    .catch(() => null);
  if (profile?.email) tokens.accountLabel = profile.email;

  await saveTokens("google", tokens);
}

export async function disconnectGoogleDrive(): Promise<void> {
  const tokens = await getTokens("google");
  if (tokens?.accessToken) {
    await fetch(
      `${DISCOVERY.revocationEndpoint}?token=${encodeURIComponent(tokens.accessToken)}`,
      { method: "POST" }
    ).catch(() => {});
  }
  await clearTokens("google");
}

async function getValidAccessToken(): Promise<string> {
  const tokens = await getTokens("google");
  if (!tokens) throw new Error("Google Drive is not connected.");

  if (tokens.expiresAt - nowMs() > 60_000) {
    return tokens.accessToken;
  }
  if (!tokens.refreshToken) {
    throw new Error("Google Drive session expired — please reconnect.");
  }

  const refreshed = await AuthSession.refreshAsync(
    { clientId: GOOGLE_CLIENT_ID, refreshToken: tokens.refreshToken },
    DISCOVERY
  );

  const next: StoredTokens = {
    accessToken: refreshed.accessToken,
    refreshToken: refreshed.refreshToken ?? tokens.refreshToken,
    expiresAt: nowMs() + (refreshed.expiresIn ?? 3600) * 1000,
    accountLabel: tokens.accountLabel,
  };
  await saveTokens("google", next);
  return next.accessToken;
}

/**
 * Uploads a file straight from the device to the user's own Google Drive
 * using Drive's resumable-upload protocol, then flips sharing on and
 * returns a "anyone with the link can view" URL.
 *
 * The file bytes are streamed natively from disk to Google (never buffered
 * into JS memory, never touching any server of ours) — that's what keeps
 * this fast and byte-for-byte lossless regardless of file size.
 */
export async function uploadToGoogleDrive(
  file: SharedFile,
  onProgress?: (fraction: number) => void
): Promise<UploadResult> {
  const accessToken = await getValidAccessToken();

  // Step 1: initiate the resumable session — Drive hands back a one-time
  // upload URL in the Location header.
  const initRes = await fetch(
    "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&fields=id,webViewLink",
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json; charset=UTF-8",
        "X-Upload-Content-Type": file.mimeType || "application/octet-stream",
      },
      body: JSON.stringify({ name: file.fileName }),
    }
  );
  if (!initRes.ok) {
    throw new Error(`Could not start Drive upload (${initRes.status}).`);
  }
  const uploadUrl = initRes.headers.get("Location");
  if (!uploadUrl) throw new Error("Drive did not return an upload URL.");

  // Step 2: stream the whole file to that URL in one native request.
  const uploadTask = new File(file.path).createUploadTask(uploadUrl, {
    httpMethod: "PUT",
    uploadType: UploadType.BINARY_CONTENT,
    headers: { "Content-Type": file.mimeType || "application/octet-stream" },
    onProgress: (data) => {
      if (data.totalBytes > 0) onProgress?.(data.bytesSent / data.totalBytes);
    },
  });

  const uploadResult = await uploadTask.uploadAsync();
  if (!uploadResult || uploadResult.status < 200 || uploadResult.status >= 300) {
    throw new Error(`Drive upload failed (${uploadResult?.status ?? "network error"}).`);
  }
  const uploaded = JSON.parse(uploadResult.body) as { id: string; webViewLink?: string };

  // Step 3: make it link-shareable (anyone with the link can view).
  await fetch(
    `https://www.googleapis.com/drive/v3/files/${uploaded.id}/permissions`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ role: "reader", type: "anyone" }),
    }
  );

  return {
    provider: "google",
    fileName: file.fileName,
    fileId: uploaded.id,
    link: uploaded.webViewLink ?? `https://drive.google.com/file/d/${uploaded.id}/view`,
    createdAt: new Date().toISOString(),
  };
}
