import { ensureSession, supabase } from "../../lib/supabase";

/**
 * The two devices never talk to any server of ours for the file itself —
 * only for this tiny handshake (WebRTC's SDP offer/answer + ICE candidates,
 * a few KB of text). Supabase Realtime's broadcast feature is just a
 * message relay keyed by the pairing code; once the peer connection opens,
 * this channel is torn down and every byte of the actual file goes device
 * to device.
 */

export type SignalMessage =
  | { kind: "offer"; sdp: string }
  | { kind: "answer"; sdp: string }
  | { kind: "ice"; candidate: string; sdpMid: string | null; sdpMLineIndex: number | null }
  | { kind: "bye" };

export function randomPairingCode(): string {
  // 6 chars, digits + uppercase, excludes visually-confusable characters
  // (0/O, 1/I/L) since this is sometimes typed by hand instead of scanned.
  const alphabet = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
  let code = "";
  for (let i = 0; i < 6; i++) {
    code += alphabet[Math.floor(Math.random() * alphabet.length)];
  }
  return code;
}

export interface SignalingChannel {
  send: (message: SignalMessage) => void;
  onMessage: (handler: (message: SignalMessage) => void) => void;
  close: () => void;
}

export function openSignalingChannel(pairingCode: string): SignalingChannel {
  if (!supabase) {
    throw new Error(
      "Direct transfer needs Supabase configured (EXPO_PUBLIC_SUPABASE_URL / _ANON_KEY) — it's only used to help the two phones find each other, see README.md."
    );
  }

  const channel = supabase.channel(`filedrop-pair-${pairingCode}`, {
    config: { broadcast: { self: false } },
  });

  let handler: ((message: SignalMessage) => void) | null = null;
  let ready = false;
  const pending: SignalMessage[] = [];

  channel.on("broadcast", { event: "signal" }, (payload) => {
    handler?.(payload.payload as SignalMessage);
  });

  // Realtime's broadcast needs a signed-in client to authorize the channel
  // on projects with Realtime Authorization turned on — the same anonymous
  // session used for share history (see lib/supabase.ts). Messages sent
  // before the channel finishes subscribing are queued rather than lost.
  (async () => {
    await ensureSession();
    channel.subscribe((status) => {
      if (status === "SUBSCRIBED") {
        ready = true;
        for (const message of pending.splice(0)) {
          channel.send({ type: "broadcast", event: "signal", payload: message });
        }
      }
    });
  })();

  return {
    send: (message) => {
      if (ready) {
        channel.send({ type: "broadcast", event: "signal", payload: message });
      } else {
        pending.push(message);
      }
    },
    onMessage: (h) => {
      handler = h;
    },
    close: () => {
      supabase!.removeChannel(channel);
    },
  };
}
