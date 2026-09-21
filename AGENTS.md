# Expo HAS CHANGED

Read the exact versioned docs at https://docs.expo.dev/versions/v57.0.0/ before writing any code.

# Automatic Version Bump & In-App APK Release Rule (MANDATORY)
Whenever making any app changes, bug fixes, or new features in the MusicDrop codebase:
1. ALWAYS increment `versionCode` (+1) and `versionName` (patch bump) in `music-drop/app/build.gradle.kts`.
2. ALWAYS update `version.json` and `docs/version.json` with the new version code, version name, title, changelog, and download URL.
3. ALWAYS compile the APK with `./gradlew assembleRelease`.
4. ALWAYS copy the compiled APK (`music-drop/app/build/outputs/apk/release/app-release.apk`) to the root workspace directory as:
   - `MusicDrop-v<versionName>.apk`
   - `MusicDrop-latest.apk`
   - `MusicDrop.apk`
5. ALWAYS commit all changes, push to GitHub (`origin main`), and publish the release via GitHub CLI:
   `gh release create v<versionName> MusicDrop-v<versionName>.apk --title "MusicDrop v<versionName>" --notes "<changelog>"`
   This is critical so the in-app updater (`https://raw.githubusercontent.com/.../version.json` and release download URL) immediately delivers the update to user devices.
Never require the user to ask or remind you to update the version and produce the released APKs.
