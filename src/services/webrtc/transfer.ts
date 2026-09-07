import { File, FileMode, Paths } from "expo-file-system";
import { RTCIceCandidate, RTCPeerConnection, RTCSessionDescription } from "react-native-webrtc";
import { openSignalingChannel, SignalingChannel, SignalMessage } from "./signaling";
import { SharedFile } from "../../types";

// Public STUN only (no TURN) — this connects two phones directly whenever
// their networks allow it (same Wi-Fi, most home/mobile NAT setups), which
// covers the common case. Some carrier-grade or symmetric-NAT networks
// won't be able to punch through without a TURN relay; if that turns out
// to matter for real users, a TURN server is the next thing to add here —
// deliberately left out for v1 to avoid standing up (and paying for) relay
// infrastructure before knowing it's needed.
const ICE_SERVERS = [
  { urls: "stun:stun.l.google.com:19302" },
  { urls: "stun:stun1.l.google.com:19302" },
];

const CHUNK_SIZE = 16 * 1024; // 16KB — safe under every DataChannel implementation's per-message limit
const BUFFER_HIGH_WATERMARK = 1 * 1024 * 1024; // pause sending past 1MB unflushed
const BUFFER_LOW_WATERMARK = 256 * 1024; // resume once it drains below this

export type TransferState =
  | "waiting" // waiting for the other device to connect
  | "connecting" // peer found, negotiating the direct connection
  | "transferring"
  | "done"
  | "error";

export interface ReceivedFile {
  uri: string;
  fileName: string;
  mimeType: string;
  size: number;
}

type MetaMessage = { type: "meta"; fileName: string; mimeType: string; size: number };
type DoneMessage = { type: "done" };

function sanitizeFileName(name: string): string {
  return name.replace(/[/\\:*?"<>|]/g, "_").slice(0, 200) || "shared-file";
}

async function waitForLowBuffer(dc: any): Promise<void> {
  if (dc.bufferedAmount < BUFFER_HIGH_WATERMARK) return;
  await new Promise<void>((resolve) => {
    dc.bufferedAmountLowThreshold = BUFFER_LOW_WATERMARK;
    const handler = () => {
      dc.removeEventListener("bufferedamountlow", handler);
      resolve();
    };
    dc.addEventListener("bufferedamountlow", handler);
  });
}

function wireIceExchange(pc: RTCPeerConnection, signaling: SignalingChannel) {
  pc.onicecandidate = (event: any) => {
    if (!event.candidate) return;
    signaling.send({
      kind: "ice",
      candidate: event.candidate.candidate,
      sdpMid: event.candidate.sdpMid,
      sdpMLineIndex: event.candidate.sdpMLineIndex,
    });
  };
}

async function streamFileOverChannel(
  dc: any,
  file: SharedFile,
  onProgress?: (fraction: number) => void
): Promise<void> {
  const diskFile = new File(file.path);
  const size = file.size ?? diskFile.size;

  const meta: MetaMessage = {
    type: "meta",
    fileName: file.fileName,
    mimeType: file.mimeType || "application/octet-stream",
    size,
  };
  dc.send(JSON.stringify(meta));

  const handle = diskFile.open(FileMode.ReadOnly);
  try {
    let offset = 0;
    while (offset < size) {
      const length = Math.min(CHUNK_SIZE, size - offset);
      handle.offset = offset;
      const bytes = handle.readBytes(length);
      await waitForLowBuffer(dc);
      dc.send(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength));
      offset += length;
      onProgress?.(offset / size);
    }
  } finally {
    handle.close();
  }

  const done: DoneMessage = { type: "done" };
  dc.send(JSON.stringify(done));
}

/** Sends one file directly to whichever device joins with the same pairing code. */
export function sendFileDirect(
  pairingCode: string,
  file: SharedFile,
  callbacks: { onStateChange?: (s: TransferState) => void; onProgress?: (f: number) => void }
): { promise: Promise<void>; cancel: () => void } {
  const signaling = openSignalingChannel(pairingCode);
  const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS });
  let settled = false;

  const cleanup = () => {
    signaling.close();
    pc.close();
  };

  const promise = new Promise<void>((resolve, reject) => {
    const fail = (err: Error) => {
      if (settled) return;
      settled = true;
      callbacks.onStateChange?.("error");
      cleanup();
      reject(err);
    };

    wireIceExchange(pc, signaling);

    (pc as any).onconnectionstatechange = () => {
      if (pc.connectionState === "failed" || pc.connectionState === "closed") {
        fail(new Error("Connection to the other device was lost."));
      }
    };

    const dc = pc.createDataChannel("filedrop", { ordered: true });
    dc.binaryType = "arraybuffer";

    dc.onopen = async () => {
      try {
        callbacks.onStateChange?.("transferring");
        await streamFileOverChannel(dc, file, callbacks.onProgress);
        settled = true;
        callbacks.onStateChange?.("done");
        cleanup();
        resolve();
      } catch (err) {
        fail(err as Error);
      }
    };

    signaling.onMessage(async (message: SignalMessage) => {
      try {
        if (message.kind === "answer") {
          await pc.setRemoteDescription(new RTCSessionDescription({ type: "answer", sdp: message.sdp }));
          callbacks.onStateChange?.("connecting");
        } else if (message.kind === "ice") {
          await pc.addIceCandidate(
            new RTCIceCandidate({
              candidate: message.candidate,
              sdpMid: message.sdpMid ?? undefined,
              sdpMLineIndex: message.sdpMLineIndex ?? undefined,
            })
          );
        }
      } catch (err) {
        fail(err as Error);
      }
    });

    (async () => {
      try {
        const offer = await pc.createOffer({});
        await pc.setLocalDescription(offer);
        signaling.send({ kind: "offer", sdp: offer.sdp! });
        callbacks.onStateChange?.("waiting");
      } catch (err) {
        fail(err as Error);
      }
    })();
  });

  return { promise, cancel: cleanup };
}

/** Waits for a sender using the same pairing code, then receives their file. */
export function receiveFileDirect(
  pairingCode: string,
  callbacks: { onStateChange?: (s: TransferState) => void; onProgress?: (f: number) => void }
): { promise: Promise<ReceivedFile>; cancel: () => void } {
  const signaling = openSignalingChannel(pairingCode);
  const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS });
  let settled = false;

  const cleanup = () => {
    signaling.close();
    pc.close();
  };

  const promise = new Promise<ReceivedFile>((resolve, reject) => {
    const fail = (err: Error) => {
      if (settled) return;
      settled = true;
      callbacks.onStateChange?.("error");
      cleanup();
      reject(err);
    };

    wireIceExchange(pc, signaling);
    callbacks.onStateChange?.("waiting");

    (pc as any).onconnectionstatechange = () => {
      if (pc.connectionState === "failed" || pc.connectionState === "closed") {
        fail(new Error("Connection to the other device was lost."));
      }
    };

    (pc as any).ondatachannel = (event: any) => {
      const dc = event.channel;
      let meta: MetaMessage | null = null;
      let destFile: File | null = null;
      let handle: ReturnType<File["open"]> | null = null;
      let receivedBytes = 0;

      callbacks.onStateChange?.("transferring");

      dc.onmessage = (msgEvent: any) => {
        try {
          if (typeof msgEvent.data === "string") {
            const parsed = JSON.parse(msgEvent.data) as MetaMessage | DoneMessage;
            if (parsed.type === "meta") {
              meta = parsed;
              destFile = new File(Paths.document, `filedrop-${Date.now()}-${sanitizeFileName(parsed.fileName)}`);
              if (destFile.exists) destFile.delete();
              destFile.create();
              handle = destFile.open(FileMode.ReadWrite);
            } else if (parsed.type === "done") {
              handle?.close();
              settled = true;
              callbacks.onStateChange?.("done");
              const result: ReceivedFile = {
                uri: destFile!.uri,
                fileName: meta!.fileName,
                mimeType: meta!.mimeType,
                size: meta!.size,
              };
              cleanup();
              resolve(result);
            }
          } else if (handle) {
            const bytes = new Uint8Array(msgEvent.data as ArrayBuffer);
            handle.writeBytes(bytes);
            receivedBytes += bytes.byteLength;
            if (meta) callbacks.onProgress?.(receivedBytes / meta.size);
          }
        } catch (err) {
          fail(err as Error);
        }
      };
    };

    signaling.onMessage(async (message: SignalMessage) => {
      try {
        if (message.kind === "offer") {
          callbacks.onStateChange?.("connecting");
          await pc.setRemoteDescription(new RTCSessionDescription({ type: "offer", sdp: message.sdp }));
          const answer = await pc.createAnswer();
          await pc.setLocalDescription(answer);
          signaling.send({ kind: "answer", sdp: answer.sdp! });
        } else if (message.kind === "ice") {
          await pc.addIceCandidate(
            new RTCIceCandidate({
              candidate: message.candidate,
              sdpMid: message.sdpMid ?? undefined,
              sdpMLineIndex: message.sdpMLineIndex ?? undefined,
            })
          );
        }
      } catch (err) {
        fail(err as Error);
      }
    });
  });

  return { promise, cancel: cleanup };
}
