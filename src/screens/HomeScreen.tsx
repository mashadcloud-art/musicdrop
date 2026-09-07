import { NativeStackScreenProps } from "@react-navigation/native-stack";
import * as DocumentPicker from "expo-document-picker";
import React, { useCallback, useEffect, useState } from "react";
import {
  FlatList,
  RefreshControl,
  SafeAreaView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { fetchShareHistory } from "../lib/supabase";
import { RootStackParamList } from "../navigation";
import { UploadResult } from "../types";

type Props = NativeStackScreenProps<RootStackParamList, "Home">;

export default function HomeScreen({ navigation }: Props) {
  const [history, setHistory] = useState<UploadResult[]>([]);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    setHistory(await fetchShareHistory());
  }, []);

  useEffect(() => {
    const unsubscribe = navigation.addListener("focus", load);
    return unsubscribe;
  }, [navigation, load]);

  const onRefresh = async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  };

  const pickFile = async () => {
    const picked = await DocumentPicker.getDocumentAsync({
      type: "*/*",
      copyToCacheDirectory: true,
      multiple: false,
    });
    if (picked.canceled || picked.assets.length === 0) return;
    const asset = picked.assets[0];
    navigation.navigate("ShareTarget", {
      files: [
        {
          path: asset.uri,
          fileName: asset.name,
          mimeType: asset.mimeType ?? "application/octet-stream",
          size: asset.size ?? undefined,
        },
      ],
    });
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>FileDrop</Text>
        <TouchableOpacity onPress={() => navigation.navigate("Connect")}>
          <Text style={styles.link}>Accounts</Text>
        </TouchableOpacity>
      </View>
      <Text style={styles.caption}>
        Share a photo, video or file into this app from anywhere on your phone — or pick
        one below.
      </Text>

      <TouchableOpacity style={styles.pickButton} onPress={pickFile}>
        <Text style={styles.pickButtonText}>Pick a file to send</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={styles.receiveButton}
        onPress={() => navigation.navigate("DirectReceive")}
      >
        <Text style={styles.receiveButtonText}>Receive from a nearby device</Text>
      </TouchableOpacity>

      <Text style={styles.sectionLabel}>Recent</Text>
      <FlatList
        data={history}
        keyExtractor={(item) => item.fileId + item.createdAt}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
        ListEmptyComponent={
          <Text style={styles.empty}>Nothing shared yet.</Text>
        }
        renderItem={({ item }) => (
          <View style={styles.historyRow}>
            <Text numberOfLines={1} style={styles.historyName}>
              {item.fileName}
            </Text>
            <Text style={styles.historyProvider}>
              {item.provider === "google" ? "Drive" : "Dropbox"}
            </Text>
          </View>
        )}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, paddingHorizontal: 20, paddingTop: 12, backgroundColor: "#fff" },
  header: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginTop: 12 },
  title: { fontSize: 28, fontWeight: "800" },
  link: { color: "#1a73e8", fontWeight: "600" },
  caption: { color: "#666", marginTop: 8, lineHeight: 20 },
  pickButton: {
    marginTop: 20,
    backgroundColor: "#1a73e8",
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: "center",
  },
  pickButtonText: { color: "#fff", fontSize: 16, fontWeight: "700" },
  receiveButton: {
    marginTop: 10,
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: "center",
    borderWidth: 1,
    borderColor: "#1a73e8",
  },
  receiveButtonText: { color: "#1a73e8", fontSize: 16, fontWeight: "700" },
  sectionLabel: { fontSize: 13, color: "#888", marginTop: 28, marginBottom: 8, textTransform: "uppercase" },
  empty: { color: "#aaa", paddingVertical: 20 },
  historyRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    paddingVertical: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "#eee",
  },
  historyName: { flex: 1, marginRight: 12 },
  historyProvider: { color: "#888" },
});
