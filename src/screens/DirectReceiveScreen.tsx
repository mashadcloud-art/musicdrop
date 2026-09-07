import { NativeStackScreenProps } from "@react-navigation/native-stack";
import { CameraView, useCameraPermissions } from "expo-camera";
import * as Sharing from "expo-sharing";
import React, { useEffect, useRef, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  SafeAreaView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";
import { RootStackParamList } from "../navigation";
import { receiveFileDirect, ReceivedFile, TransferState } from "../services/webrtc/transfer";

type Props = NativeStackScreenProps<RootStackParamList, "DirectReceive">;

const STATE_LABEL: Record<TransferState, string> = {
  waiting: "Waiting to connect…",
  connecting: "Found the other device — connecting…",
  transferring: "Receiving…",
  done: "Received!",
  error: "Something went wrong",
};

function normalizeCode(input: string): string | null {
  // Accept either the raw 6-char code or a scanned "filedrop:XXXXXX" QR value.
  const raw = input.trim().toUpperCase();
  const match = raw.match(/^(?:FILEDROP:)?([A-Z0-9]{6})$/);
  return match ? match[1] : null;
}

export default function DirectReceiveScreen({ navigation }: Props) {
  const [permission, requestPermission] = useCameraPermissions();
  const [manualCode, setManualCode] = useState("");
  const [activeCode, setActiveCode] = useState<string | null>(null);
  const [state, setState] = useState<TransferState>("waiting");
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ReceivedFile | null>(null);
  const cancelRef = useRef<(() => void) | null>(null);
  const scannedRef = useRef(false);

  useEffect(() => {
    if (!activeCode) return;
    setError(null);
    setResult(null);
    scannedRef.current = true;
    const { promise, cancel } = receiveFileDirect(activeCode, {
      onStateChange: setState,
      onProgress: setProgress,
    });
    cancelRef.current = cancel;
    promise.then(setResult).catch((err: Error) => setError(err.message));
    return () => cancel();
  }, [activeCode]);

  const handleScan = ({ data }: { data: string }) => {
    if (scannedRef.current) return;
    const code = normalizeCode(data);
    if (code) setActiveCode(code);
  };

  const submitManualCode = () => {
    const code = normalizeCode(manualCode);
    if (!code) {
      Alert.alert("Not a valid code", "Codes are 6 letters/numbers — check the other device's screen.");
      return;
    }
    setActiveCode(code);
  };

  const shareReceived = async () => {
    if (!result) return;
    if (await Sharing.isAvailableAsync()) {
      await Sharing.shareAsync(result.uri, { mimeType: result.mimeType });
    }
  };

  if (result) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.checkCircle}>
          <Text style={{ fontSize: 32 }}>✓</Text>
        </View>
        <Text style={styles.heading}>Received</Text>
        <Text style={styles.caption}>{result.fileName}</Text>
        <TouchableOpacity style={styles.primaryButton} onPress={shareReceived}>
          <Text style={styles.primaryButtonText}>Save / share file</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.doneButton}
          onPress={() => navigation.reset({ index: 0, routes: [{ name: "Home" }] })}
        >
          <Text style={styles.doneButtonText}>Done</Text>
        </TouchableOpacity>
      </SafeAreaView>
    );
  }

  if (activeCode) {
    return (
      <SafeAreaView style={styles.container}>
        <Text style={styles.heading}>Receiving</Text>
        {error ? (
          <>
            <Text style={styles.error}>{error}</Text>
            <TouchableOpacity
              style={styles.doneButton}
              onPress={() => {
                scannedRef.current = false;
                setActiveCode(null);
              }}
            >
              <Text style={styles.doneButtonText}>Try again</Text>
            </TouchableOpacity>
          </>
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

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.heading}>Scan to receive</Text>
      <Text style={styles.caption}>
        Point your camera at the code shown on the sending device.
      </Text>

      <View style={styles.cameraBox}>
        {!permission ? (
          <ActivityIndicator />
        ) : !permission.granted ? (
          <TouchableOpacity style={styles.primaryButton} onPress={requestPermission}>
            <Text style={styles.primaryButtonText}>Allow camera access</Text>
          </TouchableOpacity>
        ) : (
          <CameraView
            style={styles.camera}
            facing="back"
            barcodeScannerSettings={{ barcodeTypes: ["qr"] }}
            onBarcodeScanned={handleScan}
          />
        )}
      </View>

      <Text style={styles.orLabel}>or type the code</Text>
      <View style={styles.manualRow}>
        <TextInput
          style={styles.input}
          value={manualCode}
          onChangeText={setManualCode}
          autoCapitalize="characters"
          maxLength={6}
          placeholder="ABC123"
          placeholderTextColor="#bbb"
        />
        <TouchableOpacity style={styles.goButton} onPress={submitManualCode}>
          <Text style={styles.goButtonText}>Go</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, backgroundColor: "#fff", alignItems: "center" },
  heading: { fontSize: 22, fontWeight: "700", marginTop: 12 },
  caption: { color: "#666", marginTop: 8, textAlign: "center", lineHeight: 20 },
  cameraBox: {
    marginTop: 20,
    width: 260,
    height: 260,
    borderRadius: 16,
    overflow: "hidden",
    backgroundColor: "#f1f1f1",
    alignItems: "center",
    justifyContent: "center",
  },
  camera: { width: "100%", height: "100%" },
  orLabel: { color: "#999", marginTop: 24, marginBottom: 10 },
  manualRow: { flexDirection: "row", gap: 10 },
  input: {
    borderWidth: 1,
    borderColor: "#ddd",
    borderRadius: 10,
    paddingHorizontal: 16,
    paddingVertical: 10,
    fontSize: 18,
    letterSpacing: 3,
    width: 160,
    textAlign: "center",
  },
  goButton: { backgroundColor: "#1a73e8", borderRadius: 10, paddingHorizontal: 20, justifyContent: "center" },
  goButtonText: { color: "#fff", fontWeight: "700", fontSize: 16 },
  statusRow: { marginTop: 32, alignItems: "center" },
  status: { marginTop: 10, color: "#444", textAlign: "center" },
  progress: { marginTop: 4, color: "#1a73e8", fontWeight: "700", fontSize: 18 },
  error: { marginTop: 24, color: "#c0392b", textAlign: "center" },
  checkCircle: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: "#e6f4ea",
    alignItems: "center",
    justifyContent: "center",
    marginTop: 20,
  },
  primaryButton: {
    marginTop: 24,
    backgroundColor: "#1a73e8",
    paddingVertical: 14,
    paddingHorizontal: 32,
    borderRadius: 12,
  },
  primaryButtonText: { color: "#fff", fontSize: 16, fontWeight: "700" },
  doneButton: { marginTop: 16, paddingVertical: 10 },
  doneButtonText: { color: "#888" },
});
