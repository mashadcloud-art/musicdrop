export type CloudProvider = "google" | "dropbox";

export interface StoredTokens {
  accessToken: string;
  refreshToken?: string;
  /** Epoch ms when accessToken expires. */
  expiresAt: number;
  /** Provider account email/username, for display only. */
  accountLabel?: string;
}

export interface SharedFile {
  /** Local file:// URI on the device. */
  path: string;
  fileName: string;
  mimeType: string;
  size?: number;
}

export type UploadStatus =
  | "idle"
  | "starting"
  | "uploading"
  | "finalizing"
  | "done"
  | "error";

export interface UploadResult {
  provider: CloudProvider;
  fileName: string;
  /** Direct "anyone with the link can view" share link. */
  link: string;
  fileId: string;
  createdAt: string;
}
