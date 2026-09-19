# MusicDrop

A native Android (Kotlin) music & video player — local library playback plus
search/streaming aggregated from multiple sources, Android Auto support, and
in-app updates via GitHub Releases.

Current release: **v1.3** (`versionCode 140`) — see [`version.json`](version.json)
and the [Releases](https://github.com/mashadcloud-art/musicdrop/releases) page.

## What's in this repo

- **`music-drop/app/`** — the native Android app. Built with Kotlin and
  Jetpack Compose, min SDK 26 / target SDK 34, application id
  `com.musicdrop.app`. Produces `MusicDrop-v1.3.apk`.
- **`docs/`** — the web app, landing page, and PWA player for iOS/web users,
  hosted on GitHub Pages, including direct APK downloads.
- **`supabase/schema.sql`** — database schema for the Supabase backend
  supporting user sync, playlists, and history.

## App features (v1.3)

- **Official Branding**: Custom Pink Bird mascot identity and adaptive launcher icons across all screen densities.
- **4-Tab Navigation**: Dedicated tabs for Home, Search, Explore, and Library.
- **Local & Streaming Playback**: Scans on-device media via Android's `MediaStore` and aggregates streams across YouTube Music, JioSaavn, Spotify, and Apple Music.
- **Resilient Video/Audio Switching**: Edge-to-edge video mode with background/minimize resume; track transitions automatically switch cleanly to prevent video error screens.
- **Dynamic India Trending Mix**: Auto-crossfading cover artwork every 3.5s with rotating trending queries and a one-tap refresh button.
- **Regional Hip-Hop Shelves**: Dedicated curated shelves for Desi Hip Hop & Gully Icons, Mallu Rappers (Kerala Hip-Hop), and Tamil Rappers.
- **Feed Management**: Option to hide or delete unwanted songs from category feeds with an instant Undo snackbar.
- **Ad-Skipping & Fast Streaming**: Automated ad-skipping engine with pre-warmed PoTokens to avoid YouTube bot throttling.
- **Triple-Layer Stream Extraction**: Redundant extraction architecture (NewPipe API + InnerTube direct streams + headless `PWebExtractor` WebView fallback).
- **Android Auto & PiP**: Car integration via `auto/FileDropCarService.kt` and floating video overlays via `auto/VideoOverlayService.kt`.
- **Lyrics & Offline Library**: Real-time synchronized lyrics (LrcLib), downloads, playlists, favorites, and recent history.
- **In-App Updater**: Over-the-air update detection via `version.json` and GitHub Releases.

## Project layout (`music-drop/app/src/main/java/com/musicdrop/app/`)

```
ui/screens/       Compose screens — Discover, SearchDashboard, Library, Explore,
                   Album/Artist/Playlist detail, MusicPlayer, Downloads, Settings
ui/viewmodel/      MainViewModel — unified state management and source dispatching
playback/          Media3/ExoPlayer session service + queue bridge
auto/              Android Auto car service + video overlay service
data/repository/   Repositories for local MediaStore, JioSaavn, Spotify, Apple Music,
                   Vimeo, lyrics, and persistent stores (HiddenTracksStore, etc.)
data/youtube/      Multi-strategy stream extraction (NewPipe, InnerTube, PWebExtractor)
                   and PoToken manager
data/model/        Shared data models (UnifiedTrack, MediaItem, StorageStats)
data/updater/      In-app update manager (reads version.json from GitHub Releases)
util/              Shared utilities (ShareHelper, etc.)
```

## Stream Extraction Architecture

The codebase includes three complementary YouTube extraction mechanisms in `data/youtube/`:

1. **`NewPipeYouTubeExtractor.kt`**: High-speed, lightweight API extractor for low-latency stream URL resolution.
2. **`YouTubeStreamExtractor.kt`**: Direct InnerTube API client providing format and bitrate alternatives when API signatures change.
3. **`PWebExtractor.kt`**: Headless Android `WebView` fallback that renders in the background to bypass aggressive YouTube bot throttling and HTTP 403 blocks with valid browser cookies.

These are not accidental duplicates; they serve as an intentional fallback chain to ensure uninterrupted music playback.

## Building

```bash
cd music-drop
./gradlew assembleRelease
```

Requires Android Studio / Android SDK with API 35 build tools installed. No `.env` or external API keys are required to build.
