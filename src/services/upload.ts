import { isConnected } from "../lib/secureStore";
import { recordShare } from "../lib/supabase";
import { uploadToDropbox } from "./dropbox";
import { uploadToGoogleDrive } from "./googleDrive";
import { CloudProvider, SharedFile, UploadResult } from "../types";

export async function connectedProviders(): Promise<CloudProvider[]> {
  const providers: CloudProvider[] = ["google", "dropbox"];
  const flags = await Promise.all(providers.map((p) => isConnected(p)));
  return providers.filter((_, i) => flags[i]);
}

export async function uploadFile(
  provider: CloudProvider,
  file: SharedFile,
  onProgress?: (fraction: number) => void
): Promise<UploadResult> {
  const result =
    provider === "google"
      ? await uploadToGoogleDrive(file, onProgress)
      : await uploadToDropbox(file, onProgress);

  await recordShare(result);
  return result;
}

export interface QueuedUploadProgress {
  /** 0-based index of the file currently uploading. */
  index: number;
  total: number;
  fileName: string;
  /** 0-1 progress of just the current file. */
  fraction: number;
}

export interface QueuedUploadFailure {
  file: SharedFile;
  message: string;
}

/**
 * Uploads every file one after another (cloud APIs here are single-stream,
 * so parallel uploads would just fight each other for bandwidth). One
 * file failing doesn't stop the rest of the queue — every file gets its
 * own shot, and failures come back alongside the successes so the caller
 * can show both rather than losing the failed ones silently.
 */
export async function uploadFiles(
  provider: CloudProvider,
  files: SharedFile[],
  onProgress?: (progress: QueuedUploadProgress) => void
): Promise<{ results: UploadResult[]; failed: QueuedUploadFailure[] }> {
  const results: UploadResult[] = [];
  const failed: QueuedUploadFailure[] = [];

  for (let index = 0; index < files.length; index++) {
    const file = files[index];
    try {
      const result = await uploadFile(provider, file, (fraction) =>
        onProgress?.({ index, total: files.length, fileName: file.fileName, fraction })
      );
      results.push(result);
    } catch (err: any) {
      failed.push({ file, message: err?.message ?? "Upload failed" });
    }
  }

  return { results, failed };
}
