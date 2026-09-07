import { NativeStackScreenProps } from "@react-navigation/native-stack";
import React, { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Image,
  SafeAreaView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { RootStackParamList } from "../navigation";
import { connectedProviders, QueuedUploadProgress, uploadFiles } from "../services/upload";
import { CloudProvider } from "../types";

type Props = NativeStackScreenProps<RootStackParamList, "ShareTarget">;

function formatBytes(bytes?: number): string {
  if (!bytes) return "";
  const units = ["B", "KB", "MB", "GB"];
  let n = bytes;
  let i = 0;
  while (n >= 1024 && i < units.length - 1) {
    n /= 1024;
    i++;
  }
  return `${n.toFixed(n < 10 && i > 0 ? 1 : 0)} ${units[i]}`;
}

export default function ShareTargetScreen({ route, navigation }: Props) {
  const { files } = route.params;
  const [available, setAvailable] = useState<CloudProvider[]>([]);
  const [provider, setProvider] = useState<CloudProvider | null>(null);
  const [uploading, setUploading] = useState(false);
  const [queueProgress, setQueueProgress] = useState<QueuedUploadProgress | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    connectedProviders().then((list) => {
      setAvailable(list);
      if (list.length === 1) setProvider(list[0]);
    });
  }, []);

  const send = async () => {
    if (!provider || files.length === 0) return;
    setUploading(true);
    setError(null);
    setQueueProgress({ index: 0, total: files.length, fileName: files[0].fileName, fraction: 0 });

    // Every file gets its own attempt — one failing doesn't stop the rest
    // of the queue, and anything that failed comes back to LinkResult
    // alongside the successes instead of just vanishing.
    const { results, failed } = await uploadFiles(provider, files, setQueueProgress);

    setUploading(false);

    if (results.length === 0) {
      setError(failed[0]?.message ?? "Upload failed — check your connection and try again.");
      return;
    }

    navigation.replace("LinkResult", {
      results,
      failed: failed.length
        ? failed.map((f) => ({ fileName: f.file.fileName, message: f.message }))
        : undefined,
    });
  };

  if (available.length === 0) {
    return (
      <SafeAreaView style={styles.container}>
        <Text style={styles.heading}>Connect a cloud account first</Text>
        <Text style={styles.caption}>
          FileDrop uploads straight into your own Google Drive or Dropbox — connect
          one to send this file, or send it directly to a nearby device instead.
        </Text>
        <TouchableOpacity style={styles.primaryButton} onPress={() => navigation.navigate("Connect")}>
          <Text style={styles.primaryButtonText}>Connect an account</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.secondaryButton}
          onPress={() => navigation.navigate("DirectSend", { file: files[0] })}
        >
          <Text style={styles.secondaryButtonText}>Send directly to a nearby device</Text>
        </TouchableOpacity>
      </SafeAreaView>
    );
  }

  const file = files[0];

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.heading}>Send this file</Text>

      <View style={styles.filePreview}>
        {file.mimeType?.startsWith("image/") ? (
          <Image source={{ uri: file.path }} style={styles.thumb} />
        ) : (
          <View style={[styles.thumb, styles.thumbPlaceholder]}>
            <Text style={{ fontSize: 28 }}>📄</Text>
          </View>
        )}
        <View style={{ flex: 1, marginLeft: 12 }}>
          <Text numberOfLines={2} style={styles.fileName}>
            {file.fileName}
          </Text>
          <Text style={styles.fileMeta}>
            {formatBytes(file.size)}
            {files.length > 1 ? ` · +${files.length - 1} more` : ""}
          </Text>
        </View>
      </View>

      {files.length > 1 && (
        <Text style={styles.caption}>
          {files.length} files shared — they'll upload one after another and you'll get a
          link for each.
        </Text>
      )}

      <Text style={styles.sectionLabel}>Send to</Text>
      <View style={styles.providerRow}>
        {available.map((p) => (
          <TouchableOpacity
            key={p}
            style={[styles.chip, provider === p && styles.chipActive]}
            onPress={() => setProvider(p)}
          >
            <Text style={[styles.chipText, provider === p && styles.chipTextActive]}>
              {p === "google" ? "Google Drive" : "Dropbox"}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {error && <Text style={styles.error}>{error}</Text>}

      {uploading ? (
        <View style={styles.uploadingBox}>
          <ActivityIndicator />
          <Text style={styles.caption}>
            {files.length > 1
              ? `Uploading ${(queueProgress?.index ?? 0) + 1} of ${files.length} — ${
                  queueProgress?.fileName ?? ""
                } (${Math.round((queueProgress?.fraction ?? 0) * 100)}%)`
              : `${Math.round((queueProgress?.fraction ?? 0) * 100)}% uploaded`}
          </Text>
        </View>
      ) : (
        <>
          <TouchableOpacity
            style={[styles.primaryButton, !provider && styles.primaryButtonDisabled]}
            disabled={!provider}
            onPress={send}
          >
            <Text style={styles.primaryButtonText}>Upload & get link</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.secondaryButton}
            onPress={() => navigation.navigate("DirectSend", { file: files[0] })}
          >
            <Text style={styles.secondaryButtonText}>Send directly to a nearby device instead</Text>
          </TouchableOpacity>
        </>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: "#fff" },
  heading: { fontSize: 22, fontWeight: "700", marginTop: 12, marginBottom: 16 },
  caption: { color: "#666", marginTop: 8, lineHeight: 20 },
  filePreview: { flexDirection: "row", alignItems: "center", marginBottom: 20 },
  thumb: { width: 64, height: 64, borderRadius: 10, backgroundColor: "#f1f1f1" },
  thumbPlaceholder: { alignItems: "center", justifyContent: "center" },
  fileName: { fontSize: 16, fontWeight: "600" },
  fileMeta: { color: "#888", marginTop: 4 },
  sectionLabel: { fontSize: 13, color: "#888", marginBottom: 8, textTransform: "uppercase" },
  providerRow: { flexDirection: "row", gap: 10 },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: "#ddd",
  },
  chipActive: { backgroundColor: "#1a73e8", borderColor: "#1a73e8" },
  chipText: { color: "#333", fontWeight: "600" },
  chipTextActive: { color: "#fff" },
  error: { color: "#c0392b", marginTop: 16 },
  uploadingBox: { marginTop: 32, alignItems: "center" },
  primaryButton: {
    marginTop: 32,
    backgroundColor: "#1a73e8",
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: "center",
  },
  primaryButtonDisabled: { opacity: 0.4 },
  primaryButtonText: { color: "#fff", fontSize: 16, fontWeight: "700" },
  secondaryButton: { marginTop: 14, paddingVertical: 10, alignItems: "center" },
  secondaryButtonText: { color: "#1a73e8", fontWeight: "600" },
});
