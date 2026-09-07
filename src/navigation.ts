import { CloudProvider, SharedFile, UploadResult } from "./types";

export type RootStackParamList = {
  Home: undefined;
  Connect: undefined;
  ShareTarget: { files: SharedFile[] };
  LinkResult: {
    results: UploadResult[];
    /** Files that were queued but failed to upload — shown so nothing silently vanishes. */
    failed?: { fileName: string; message: string }[];
  };
  DirectSend: { file: SharedFile };
  DirectReceive: undefined;
};

export type { CloudProvider, SharedFile, UploadResult };
