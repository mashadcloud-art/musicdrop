# MusicDrop

A native Android (Kotlin) music & video player — local library playback plus
search/streaming aggregated from several sources, Android Auto support, and
in-app updates via GitHub Releases.

Current release: **v1.1** (`versionCode 138`) — see [`version.json`](version.json)
and the [Releases](https://github.com/mashadcloud-art/musicdrop/releases) page.

## What's in this repo

This repo currently holds a few things side by side:

- **`music-drop/app/`** — the real, released app. Native Android, Kotlin,
  Jetpack Compose UI, min SDK 26 / target SDK 34, application id
  `com.musicdrop.app`. This is what `MusicDrop-v1.1.apk` actually is.
- **`music-drop/music-stream/`** — a separate Expo/React Native prototype.
  It is **not** part of the released app and isn't wired to it — treat it as
  a parked experiment, not active code, until/unless that changes.
- **`saavn-proxy/`** — source for a small proxy service. The app's live
  Saavn integration currently points at a proxy already deployed at
  `filedrop-saavn.fly.dev`; this folder's own `server.js` isn't committed
  here yet, so it won't run as-is from this checkout. See
  `saavn-proxy/README.md`.
- **`supabase/schema.sql`** — schema for the Supabase project the app talks
  to (history/sync — check the app's data layer for exactly what uses it).

## App features (v1.1)

- Local on-device library (scans media via Android's `MediaStore`)
- Search and playback aggregated across multiple external sources
- Edge-to-edge video player with background/minimize resume
- Instant next/previous track sync
- Android Auto integration (`auto/FileDropCarService.kt`)
- Picture-in-picture style video overlay (`auto/VideoOverlayService.kt`)
- Lyrics lookup
- Downloads, playlists, favorites/liked tracks, recent plays & search history
- Self-hosted in-app update check (`version.json` + GitHub Releases)

## Project layout (`music-drop/app/src/main/java/com/musicdrop/app/`)

```
ui/screens/       Compose screens — Library, Search, Discover, Explore,
                   Album/Artist/Playlist detail, Player, Downloads, Settings, ...
ui/viewmodel/      MainViewModel — wires screens to the repositories below
playback/          Media3/ExoPlayer session service + queue bridge
auto/              Android Auto car service + video overlay service
data/repository/   One repository per content source (local MediaStore,
                   YouTube Music, Saavn, Spotify, Apple Music, Vimeo, lyrics, ...)
data/youtube/      YouTube stream extraction + poToken generation
data/model/        Shared data models (UnifiedTrack, MediaItem, ...)
data/updater/      In-app update manager (reads version.json from GitHub)
util/              Shared utilities
```

## Building

```bash
cd music-drop
./gradlew assembleDebug
```

Needs Android Studio / an Android SDK with API 34 installed. No `.env` or
API keys are required to build — the app's external integrations are
configured directly in source.

## Known gaps

- `music-drop/music-stream/` (Expo) is not integrated with the native app.
- `saavn-proxy/server.js` is not committed — the deployed proxy this app
  actually uses lives outside this repo.
