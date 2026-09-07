import * as SecureStore from "expo-secure-store";
import { CloudProvider, StoredTokens } from "../types";

// Tokens never leave the device except to talk directly to Google/Dropbox's
// own APIs — there is no app backend in the upload path, so there is no
// server that can leak them. expo-secure-store uses the OS keystore
// (Android Keystore / iOS Keychain), not plain storage.

const keyFor = (provider: CloudProvider) => `filedrop.tokens.${provider}`;

export async function saveTokens(
  provider: CloudProvider,
  tokens: StoredTokens
): Promise<void> {
  await SecureStore.setItemAsync(keyFor(provider), JSON.stringify(tokens));
}

export async function getTokens(
  provider: CloudProvider
): Promise<StoredTokens | null> {
  const raw = await SecureStore.getItemAsync(keyFor(provider));
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StoredTokens;
  } catch {
    return null;
  }
}

export async function clearTokens(provider: CloudProvider): Promise<void> {
  await SecureStore.deleteItemAsync(keyFor(provider));
}

export async function isConnected(provider: CloudProvider): Promise<boolean> {
  return (await getTokens(provider)) !== null;
}
