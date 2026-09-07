import { createClient } from "@supabase/supabase-js";
import * as SecureStore from "expo-secure-store";
import { UploadResult } from "../types";

const SUPABASE_URL = process.env.EXPO_PUBLIC_SUPABASE_URL ?? "";
const SUPABASE_ANON_KEY = process.env.EXPO_PUBLIC_SUPABASE_ANON_KEY ?? "";

// expo-secure-store as the Supabase session adapter, so the (anonymous)
// session survives app restarts without ever touching plain AsyncStorage.
const SecureStoreAdapter = {
  getItem: (key: string) => SecureStore.getItemAsync(key),
  setItem: (key: string, value: string) => SecureStore.setItemAsync(key, value),
  removeItem: (key: string) => SecureStore.deleteItemAsync(key),
};

export const supabase =
  SUPABASE_URL && SUPABASE_ANON_KEY
    ? createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
        auth: {
          storage: SecureStoreAdapter,
          autoRefreshToken: true,
          persistSession: true,
          detectSessionInUrl: false,
        },
      })
    : null;

// The `shares` table's RLS policies key everything off auth.uid() (see
// supabase/schema.sql) — without a signed-in user, "no rows" and "not
// logged in" look identical from here, which would silently show an empty
// history forever. Each device gets its own anonymous Supabase user, so
// its history is only ever visible to itself, without asking anyone to
// create an account. The same anonymous session is also what the direct
// (WebRTC) transfer's signaling channel authenticates with — see
// src/services/webrtc/signaling.ts.
export async function ensureSession(): Promise<boolean> {
  if (!supabase) return false;
  const { data } = await supabase.auth.getSession();
  if (data.session) return true;
  const { error } = await supabase.auth.signInAnonymously();
  return !error;
}

/**
 * Purely a local history log — the actual files never pass through
 * Supabase or any server of ours, only the resulting metadata + link do
 * (see supabase/schema.sql for the `shares` table and its RLS policies).
 */
export async function recordShare(result: UploadResult): Promise<void> {
  if (!supabase) return; // Supabase is optional — history just won't sync.
  try {
    if (!(await ensureSession())) return;
    await supabase.from("shares").insert({
      provider: result.provider,
      file_name: result.fileName,
      link: result.link,
      file_id: result.fileId,
    });
  } catch {
    // Best-effort only — a failed history write should never block the
    // share flow the user actually cares about.
  }
}

export async function fetchShareHistory(limit = 50): Promise<UploadResult[]> {
  if (!supabase) return [];
  if (!(await ensureSession())) return [];
  const { data, error } = await supabase
    .from("shares")
    .select("provider, file_name, link, file_id, created_at")
    .order("created_at", { ascending: false })
    .limit(limit);
  if (error || !data) return [];
  return data.map((row) => ({
    provider: row.provider,
    fileName: row.file_name,
    link: row.link,
    fileId: row.file_id,
    createdAt: row.created_at,
  }));
}
