# FileDrop

Share a photo, video, or any file from anywhere on your phone — the OS Share
sheet, your gallery, WhatsApp, downloads — straight into your own Google
Drive or Dropbox, and get back a link. No account with us, no file ever
touches a server we run: everything streams directly from the phone to
whichever cloud you connect. There's also a third option that skips the
cloud entirely: pair two phones directly (QR code or a 6-character code)
and send a file straight from one to the other over WebRTC — see
[Direct device-to-device transfer](#direct-device-to-device-transfer) below.

## How it works

1. You connect Google Drive and/or Dropbox once (OAuth, on-device, tokens
   stored in the OS keychain via `expo-secure-store` — never sent anywhere
   but Google/Dropbox themselves).
2. Share a file into the app (or pick one from inside it).
3. The file streams directly from your phone to your cloud account —
   nothing is re-encoded, re-compressed, or proxied through us, so quality
   is untouched and speed is whatever your connection + the provider's API
   allow.
4. The app flips on link sharing and hands you a "anyone with the link"
   URL, ready to copy or send.

A Supabase table remembers *which* files you've shared (name, provider,
link) so you have a history — it never sees the files themselves. Supabase
also relays the tiny handshake messages for direct device-to-device
transfer (see below) — so unlike the initial build, it's no longer purely
optional if you want that feature working.

## Before you can run this

This needs real OAuth apps registered with Google and Dropbox — there's no
way around that step, it's who lets your app talk to their APIs on a
user's behalf.

### 1. Google Drive

1. [Google Cloud Console](https://console.cloud.google.com/) → create (or
   pick) a project.
2. **APIs & Services → Library** → enable **Google Drive API**.
3. **APIs & Services → OAuth consent screen** → set it up as **External**,
   add your own email as a test user while developing (this avoids
   Google's review process during development — you only need review
   before a public launch, and only then because of the broad `drive`
   scope, which this app deliberately avoids: see the comment in
   `src/services/googleDrive.ts`).
4. **APIs & Services → Credentials → Create Credentials → OAuth client
   ID** — create one for **Android** (needs your `app.json` package name
   `com.yourcompany.filedrop` + your signing certificate's SHA-1) and one
   for **iOS** (needs the `bundleIdentifier`). Expo's docs walk through
   getting the SHA-1 for a dev/EAS build.
5. Put the client ID in `.env` as `EXPO_PUBLIC_GOOGLE_CLIENT_ID`.

### 2. Dropbox

1. [Dropbox App Console](https://www.dropbox.com/developers/apps) →
   **Create app** → **Scoped access** → **App folder** (simplest — your
   app only ever sees its own `/FileDrop` folder in the user's Dropbox,
   nothing else) or **Full Dropbox** if you'd rather.
2. **Permissions** tab → enable `files.content.write`,
   `files.content.read`, `sharing.write`, `account_info.read`.
3. **Settings** tab → add your redirect URI. For a dev client this is
   `filedrop://` (the `scheme` in `app.json`) — Dropbox's PKCE flow for
   mobile apps needs no client secret.
4. Copy the **App key** into `.env` as `EXPO_PUBLIC_DROPBOX_CLIENT_ID`.

### 3. Supabase — powers direct device-to-device transfer, and share history

Needed if you want the "send directly to a nearby device" feature to work;
without it, cloud upload (Google Drive/Dropbox) still works fine on its own,
you just won't get the direct-transfer option or a synced history.

1. Create a project at [supabase.com](https://supabase.com).
2. **Authentication → Providers** → turn on **Anonymous Sign-Ins** (each
   device gets its own private, account-less identity — see
   `supabase/schema.sql` for why).
3. **SQL Editor** → paste and run `supabase/schema.sql`.
4. Copy **Project URL** and **anon public key** (Settings → API) into
   `.env`. Leave both blank and the app still works for cloud uploads —
   you'll just lose history syncing *and* the direct-transfer option.

### 4. Fill in `.env`

```
cp .env.example .env
# then fill in the four values above
```

## Running it

**Important:** this app uses native modules (the share-intent receiver,
secure storage) and therefore **cannot run in Expo Go**. You need a
"dev client" — a custom build of the app with those native modules baked
in, built once, then reused for fast-refresh development like normal.

```bash
npm install

# builds native android/ + ios/ projects from app.json (gitignored — this
# regenerates them, never hand-edit what's inside)
npx expo prebuild

# Android (needs Android Studio + an emulator or a USB-connected phone)
npx expo run:android

# iOS (needs a Mac + Xcode)
npx expo run:ios
```

After the first native build, `npx expo start --dev-client` gives you the
normal fast-refresh loop.

To test the actual Share-sheet integration, install the dev client build
on a real device or emulator, then try sharing a photo from your gallery
app — FileDrop should show up as a share target.

**If you already built the dev client before the direct-transfer feature
was added:** it added two new native modules (`react-native-webrtc` and
`expo-camera`, for QR scanning), so you need to rebuild the dev client —
`npx expo prebuild` then `npx expo run:android` / `npx expo run:ios`
again — a JS-only reload isn't enough. It also asks for the camera
permission the first time someone opens "Receive from a nearby device"
(only needed for QR scanning — typing the 6-character code works without
it).

## Direct device-to-device transfer

A third way to send a file, alongside Google Drive and Dropbox: two phones
pair directly and the file goes straight from one to the other, with no
cloud account and no server of ours ever touching the bytes.

1. **Sender** picks a file and taps "Send directly to a nearby device." The
   app shows a QR code and a 6-character fallback code.
2. **Receiver** taps "Receive from a nearby device" and either scans the QR
   code or types the 6-character code in by hand.
3. Both phones use that code to find each other through a Supabase Realtime
   channel — this is the *only* thing Supabase is used for here: relaying
   a few KB of WebRTC handshake messages (an SDP offer/answer and some ICE
   candidates) so the two phones can open a direct connection. The channel
   is torn down the moment that connection is up.
4. Once connected, the file streams phone-to-phone over a WebRTC data
   channel, in chunks, with backpressure so a fast sender can't overrun a
   slower link. Nothing is re-encoded — same lossless, direct-streaming
   principle as the cloud path.
5. The receiving phone saves the incoming file and offers to save/share it
   from there (files land in the app's private storage first, since that's
   the only place a mobile app can write to directly).

**Known limitation (v1): no TURN server.** The two phones connect using
only public STUN servers to discover their network paths, which is enough
for the great majority of home Wi-Fi and mobile-data setups. It will *not*
work on networks that do symmetric NAT or carrier-grade NAT with strict
filtering (some corporate/campus Wi-Fi, some mobile carriers) — those need
a TURN relay server to fall back to, which isn't set up yet. If a transfer
gets stuck at "Connecting…" and never moves to "Transferring," this is the
most likely reason; try both phones on the same Wi-Fi network as a
workaround for now.

Relevant files: `src/services/webrtc/signaling.ts` (the Supabase-relayed
handshake), `src/services/webrtc/transfer.ts` (the actual peer connection,
chunking, and backpressure), `src/screens/DirectSendScreen.tsx` and
`src/screens/DirectReceiveScreen.tsx` (the two screens above).

## What's deliberately simple right now (v1)

- Dropbox chunked upload has no resume-after-app-restart — if the app is
  killed mid-upload of a huge file, that one upload has to restart. Full
  crash-resume would mean persisting the session/cursor state to disk,
  worth adding once this is validated with real users.
- OneDrive isn't wired up yet — same shape as Dropbox/Google Drive, added
  later once the two-provider flow is proven out.
- No desktop/web app — this is intentionally mobile-first, per the
  original brief.
- Direct device-to-device transfer only uses STUN, no TURN fallback — see
  the limitation called out in
  [Direct device-to-device transfer](#direct-device-to-device-transfer)
  above. It also still only sends one file per pairing (unlike the cloud
  path, which now queues a whole multi-file share) — a straightforward
  follow-up once the single-file version has been used for a while.

## Project layout

```
App.tsx                          navigation + share-intent listener + WebRTC global setup
src/screens/                     Home, Connect Accounts, Share Target, Link Result,
                                  Direct Send, Direct Receive
src/services/googleDrive.ts      Google OAuth + resumable upload + link creation
src/services/dropbox.ts          Dropbox OAuth + single-shot/chunked upload + link creation
src/services/upload.ts           picks the right cloud provider, records history
src/services/webrtc/signaling.ts Supabase-relayed WebRTC handshake (pairing code based)
src/services/webrtc/transfer.ts  the peer connection + chunked send/receive + backpressure
src/lib/secureStore.ts           OS-keychain-backed token storage
src/lib/supabase.ts              share-history sync + direct-transfer signaling auth
supabase/schema.sql              the `shares` table + row-level-security policies
```
