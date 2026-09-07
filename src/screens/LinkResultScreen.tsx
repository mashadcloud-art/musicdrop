import { NativeStackScreenProps } from "@react-navigation/native-stack";
import * as Clipboard from "expo-clipboard";
import React, { useState } from "react";
import { FlatList, SafeAreaView, Share, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { RootStackParamList } from "../navigation";
import { UploadResult } from "../types";

type Props = NativeStackScreenProps<RootStackParamList, "LinkResult">;

function providerLabel(result: UploadResult): string {
  return result.provider === "google" ? "Google Drive" : "Dropbox";
}

/** One row's own copy state, so copying row 3 doesn't flash "Copied!" on every row. */
function ResultRow({ result }: { result: UploadResult }) {
  const [copied, setCopied] = useState(false);

  const copy = async () => {
    await Clipboard.setStringAsync(result.link);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  return (
    <View style={styles.row}>
      <View style={{ flex: 1, marginRight: 12 }}>
        <Text numberOfLines={1} style={styles.rowName}>
          {result.fileName}
        </Text>
        <Text style={styles.rowProvider}>{providerLabel(result)}</Text>
      </View>
      <TouchableOpacity onPress={() => Share.share({ message: result.link, url: result.link })}>
        <Text style={styles.rowAction}>Share</Text>
      </TouchableOpacity>
      <TouchableOpacity onPress={copy} style={{ marginLeft: 16 }}>
        <Text style={styles.rowAction}>{copied ? "Copied!" : "Copy"}</Text>
      </TouchableOpacity>
    </View>
  );
}

export default function LinkResultScreen({ route, navigation }: Props) {
  const { results, failed } = route.params;
  const single = results.length === 1 ? results[0] : null;
  const [copiedAll, setCopiedAll] = useState(false);

  const copyAll = async () => {
    await Clipboard.setStringAsync(results.map((r) => `${r.fileName}: ${r.link}`).join("\n"));
    setCopiedAll(true);
    setTimeout(() => setCopiedAll(false), 1500);
  };

  const shareAll = () => {
    Share.share({ message: results.map((r) => `${r.fileName}: ${r.link}`).join("\n") });
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.checkCircle}>
        <Text style={{ fontSize: 32 }}>✓</Text>
      </View>
      <Text style={styles.heading}>
        {results.length > 1 ? `${results.length} files uploaded` : "Uploaded"}
      </Text>

      {single ? (
        <>
          <Text style={styles.caption}>
            {single.fileName} is on your {providerLabel(single)}
          </Text>
          <View style={styles.linkBox}>
            <Text numberOfLines={2} style={styles.linkText}>
              {single.link}
            </Text>
          </View>
          <TouchableOpacity style={styles.primaryButton} onPress={shareAll}>
            <Text style={styles.primaryButtonText}>Share link</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.secondaryButton} onPress={copyAll}>
            <Text style={styles.secondaryButtonText}>{copiedAll ? "Copied!" : "Copy link"}</Text>
          </TouchableOpacity>
        </>
      ) : (
        <>
          <FlatList
            style={styles.list}
            data={results}
            keyExtractor={(item) => item.fileId}
            renderItem={({ item }) => <ResultRow result={item} />}
          />
          <TouchableOpacity style={styles.primaryButton} onPress={shareAll}>
            <Text style={styles.primaryButtonText}>Share all links</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.secondaryButton} onPress={copyAll}>
            <Text style={styles.secondaryButtonText}>{copiedAll ? "Copied!" : "Copy all links"}</Text>
          </TouchableOpacity>
        </>
      )}

      {failed && failed.length > 0 && (
        <View style={styles.failedBox}>
          <Text style={styles.failedHeading}>
            {failed.length} file{failed.length > 1 ? "s" : ""} didn't upload
          </Text>
          {failed.map((f, i) => (
            <Text key={i} style={styles.failedRow} numberOfLines={1}>
              {f.fileName} — {f.message}
            </Text>
          ))}
        </View>
      )}

      <TouchableOpacity
        style={styles.doneButton}
        onPress={() => navigation.reset({ index: 0, routes: [{ name: "Home" }] })}
      >
        <Text style={styles.doneButtonText}>Done</Text>
      </TouchableOpacity>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, backgroundColor: "#fff", alignItems: "center" },
  checkCircle: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: "#e6f4ea",
    alignItems: "center",
    justifyContent: "center",
    marginTop: 40,
  },
  heading: { fontSize: 24, fontWeight: "700", marginTop: 16 },
  caption: { color: "#666", marginTop: 6, textAlign: "center" },
  linkBox: {
    backgroundColor: "#f7f7f7",
    borderRadius: 12,
    padding: 16,
    marginTop: 28,
    width: "100%",
  },
  linkText: { color: "#1a73e8" },
  list: { width: "100%", marginTop: 24, maxHeight: 320 },
  row: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "#eee",
  },
  rowName: { fontSize: 15, fontWeight: "600" },
  rowProvider: { color: "#888", marginTop: 2, fontSize: 12 },
  rowAction: { color: "#1a73e8", fontWeight: "600" },
  primaryButton: {
    marginTop: 24,
    backgroundColor: "#1a73e8",
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: "center",
    width: "100%",
  },
  primaryButtonText: { color: "#fff", fontSize: 16, fontWeight: "700" },
  secondaryButton: { marginTop: 12, paddingVertical: 14, alignItems: "center", width: "100%" },
  secondaryButtonText: { color: "#1a73e8", fontSize: 16, fontWeight: "600" },
  failedBox: {
    marginTop: 20,
    width: "100%",
    backgroundColor: "#fdecea",
    borderRadius: 12,
    padding: 14,
  },
  failedHeading: { color: "#c0392b", fontWeight: "700", marginBottom: 6 },
  failedRow: { color: "#c0392b", fontSize: 13, marginTop: 2 },
  doneButton: { marginTop: 20, paddingVertical: 10 },
  doneButtonText: { color: "#888" },
});
