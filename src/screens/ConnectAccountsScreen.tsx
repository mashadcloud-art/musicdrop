import React, { useCallback, useEffect, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  SafeAreaView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { getTokens } from "../lib/secureStore";
import { connectDropbox, disconnectDropbox } from "../services/dropbox";
import { connectGoogleDrive, disconnectGoogleDrive } from "../services/googleDrive";
import { CloudProvider } from "../types";

interface ProviderRowState {
  connected: boolean;
  label?: string;
  busy: boolean;
}

const EMPTY: ProviderRowState = { connected: false, busy: false };

export default function ConnectAccountsScreen() {
  const [google, setGoogle] = useState<ProviderRowState>(EMPTY);
  const [dropbox, setDropbox] = useState<ProviderRowState>(EMPTY);

  const refresh = useCallback(async () => {
    const [g, d] = await Promise.all([getTokens("google"), getTokens("dropbox")]);
    setGoogle({ connected: !!g, label: g?.accountLabel, busy: false });
    setDropbox({ connected: !!d, label: d?.accountLabel, busy: false });
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const toggle = async (provider: CloudProvider) => {
    const setState = provider === "google" ? setGoogle : setDropbox;
    const current = provider === "google" ? google : dropbox;
    setState((s) => ({ ...s, busy: true }));
    try {
      if (current.connected) {
        await (provider === "google" ? disconnectGoogleDrive() : disconnectDropbox());
      } else {
        await (provider === "google" ? connectGoogleDrive() : connectDropbox());
      }
      await refresh();
    } catch (err: any) {
      setState((s) => ({ ...s, busy: false }));
      Alert.alert("Something went wrong", err?.message ?? String(err));
    }
  };

  const Row = ({
    title,
    subtitle,
    state,
    provider,
  }: {
    title: string;
    subtitle: string;
    state: ProviderRowState;
    provider: CloudProvider;
  }) => (
    <View style={styles.row}>
      <View style={{ flex: 1 }}>
        <Text style={styles.rowTitle}>{title}</Text>
        <Text style={styles.rowSubtitle}>
          {state.connected ? state.label ?? "Connected" : subtitle}
        </Text>
      </View>
      {state.busy ? (
        <ActivityIndicator />
      ) : (
        <TouchableOpacity
          style={[styles.button, state.connected && styles.buttonConnected]}
          onPress={() => toggle(provider)}
        >
          <Text style={[styles.buttonText, state.connected && styles.buttonTextConnected]}>
            {state.connected ? "Disconnect" : "Connect"}
          </Text>
        </TouchableOpacity>
      )}
    </View>
  );

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.heading}>Connect your cloud</Text>
      <Text style={styles.caption}>
        Files upload straight from this phone into your own account — nothing is
        stored on our servers.
      </Text>
      <Row
        title="Google Drive"
        subtitle="Not connected"
        state={google}
        provider="google"
      />
      <Row title="Dropbox" subtitle="Not connected" state={dropbox} provider="dropbox" />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: "#fff" },
  heading: { fontSize: 24, fontWeight: "700", marginTop: 12 },
  caption: { color: "#666", marginTop: 6, marginBottom: 24, lineHeight: 20 },
  row: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 16,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "#e5e5e5",
  },
  rowTitle: { fontSize: 17, fontWeight: "600" },
  rowSubtitle: { color: "#888", marginTop: 2 },
  button: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: "#1a73e8",
  },
  buttonConnected: { backgroundColor: "#f1f1f1" },
  buttonText: { color: "#fff", fontWeight: "600" },
  buttonTextConnected: { color: "#c0392b" },
});
