import { createNavigationContainerRef, NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { StatusBar } from "expo-status-bar";
import { ShareIntentProvider, useShareIntentContext } from "expo-share-intent";
import { registerGlobals } from "react-native-webrtc";
import React, { useEffect } from "react";
import ConnectAccountsScreen from "./src/screens/ConnectAccountsScreen";
import DirectReceiveScreen from "./src/screens/DirectReceiveScreen";
import DirectSendScreen from "./src/screens/DirectSendScreen";
import HomeScreen from "./src/screens/HomeScreen";
import LinkResultScreen from "./src/screens/LinkResultScreen";
import ShareTargetScreen from "./src/screens/ShareTargetScreen";
import { RootStackParamList } from "./src/navigation";
import { SharedFile } from "./src/types";

// Required one-time setup so react-native-webrtc's classes (RTCPeerConnection
// and friends) are available the way the spec expects — see transfer.ts.
registerGlobals();

const Stack = createNativeStackNavigator<RootStackParamList>();
const navigationRef = createNavigationContainerRef<RootStackParamList>();

/** No UI of its own — just watches for an incoming OS share and routes to it. */
function ShareIntentBridge() {
  const { hasShareIntent, shareIntent, resetShareIntent } = useShareIntentContext();

  useEffect(() => {
    if (!hasShareIntent || !navigationRef.isReady()) return;

    const files: SharedFile[] = (shareIntent.files ?? []).map((f) => ({
      path: f.path,
      fileName: f.fileName ?? "shared-file",
      mimeType: f.mimeType ?? "application/octet-stream",
      size: f.size ?? undefined,
    }));

    if (files.length === 0) {
      // Text/URL shares aren't the app's job — nothing to upload.
      resetShareIntent();
      return;
    }

    navigationRef.navigate("ShareTarget", { files });
    resetShareIntent();
  }, [hasShareIntent, shareIntent, resetShareIntent]);

  return null;
}

export default function App() {
  return (
    <ShareIntentProvider>
      <NavigationContainer ref={navigationRef}>
        <ShareIntentBridge />
        <Stack.Navigator screenOptions={{ headerBackTitle: "Back" }}>
          <Stack.Screen name="Home" component={HomeScreen} options={{ title: "FileDrop" }} />
          <Stack.Screen
            name="Connect"
            component={ConnectAccountsScreen}
            options={{ title: "Accounts" }}
          />
          <Stack.Screen
            name="ShareTarget"
            component={ShareTargetScreen}
            options={{ title: "Send" }}
          />
          <Stack.Screen
            name="LinkResult"
            component={LinkResultScreen}
            options={{ title: "Done", headerBackVisible: false }}
          />
          <Stack.Screen
            name="DirectSend"
            component={DirectSendScreen}
            options={{ title: "Send directly" }}
          />
          <Stack.Screen
            name="DirectReceive"
            component={DirectReceiveScreen}
            options={{ title: "Receive" }}
          />
        </Stack.Navigator>
        <StatusBar style="auto" />
      </NavigationContainer>
    </ShareIntentProvider>
  );
}
