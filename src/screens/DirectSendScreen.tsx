import { NativeStackScreenProps } from "@react-navigation/native-stack";
import React, { useEffect, useRef, useState } from "react";
import { ActivityIndicator, SafeAreaView, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import QRCode from "react-native-qrcode-svg";
import { RootStackParamList } from "../navigation";
import { randomPairingCode } from "../services/webrtc/signaling";
import { sendFileDirect, TransferState } from "../services/webrtc/transfer";

type Props = NativeStackScreenProps<RootStackParamList, "DirectSend">;

const STATE_LABEL: Record<TransferState, string> = {
  waiting: "Waiting for the other device to scan this code…",
  connecting: "Found it — connecting…",
  transferring: "Sending…",
  done: "Sent!",
  error: "Something went wrong",
};

export default function DirectSendScreen({ route, navigation }: Props) {
  const { file } = route.params;
  const [code] = useState(randomPairingCode);
  const [state, setState] = useState<TransferState>("waiting");
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const cancelRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    const { promise, cancel } = sendFileDirect(code, file, {
      onStateChange: setState,
      onProgress: setProgress,
    });
    cancelRef.current = cancel;
    promise.catch((err: Error) => setError(err.message));
    return () => cancel();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.heading}>Send directly</Text>
      <Text style={styles.caption}>
        Open FileDrop on the other device, tap "Receive", and scan this code — the
        file goes straight there, no internet upload needed once connected.
      </Text>

      <View style={styles.qrBox}>
        <QRCode value={`filedrop:${code}`} size={220} />
      </View>
      <Text style={styles.code}>{code}</Text>

      {state === "done" ? (
        <>
          <Text style={styles.status}>✓ {file.fileName} sent</Text>
          <TouchableOpacity
            style={styles.doneButton}
            onPress={() => navigation.reset({ index: 0, routes: [{ name: "Home" }] })}
          >
            <Text style={styles.doneButtonText}>Done</Text>
          </TouchableOpacity>
        </>
      ) : error ? (
        <Text style={styles.error}>{error}</Text>
      ) : (
        <View style={styles.statusRow}>
          <ActivityIndicator />
          <Text style={styles.status}>{STATE_LABEL[state]}</Text>
          {state === "transferring" && (
            <Text style={styles.progress}>{Math.round(progress * 100)}%</Text>
          )}
        </View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, backgroundColor: "#fff", alignItems: "center" },
  heading: { fontSize: 22, fontWeight: "700", marginTop: 12 },
  caption: { color: "#666", marginTop: 8, textAlign: "center", lineHeight: 20 },
  qrBox: { marginTop: 28, padding: 16, backgroundColor: "#fff", borderRadius: 16, borderWidth: 1, borderColor: "#eee" },
  code: { marginTop: 14, fontSize: 22, fontWeight: "700", letterSpacing: 4, color: "#333" },
  statusRow: { marginTop: 32, alignItems: "center" },
  status: { marginTop: 10, color: "#444", textAlign: "center" },
  progress: { marginTop: 4, color: "#1a73e8", fontWeight: "700", fontSize: 18 },
  error: { marginTop: 24, color: "#c0392b", textAlign: "center" },
  doneButton: {
    marginTop: 24,
    backgroundColor: "#1a73e8",
    paddingVertical: 14,
    paddingHorizontal: 40,
    borderRadius: 12,
  },
  doneButtonText: { color: "#fff", fontSize: 16, fontWeight: "700" },
});
