# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Coil
-keep class coil.** { *; }

# Keep Music models & repositories
-keep class com.musicdrop.app.data.** { *; }

# Keep entire app classes (ViewModels, Playback, UI, Stores) to prevent R8 stripping
-keep class com.musicdrop.app.** { *; }
-dontwarn com.musicdrop.app.**

# NewPipeExtractor + its bundled Rhino JS engine (used to solve YouTube's
# signatureCipher / n-parameter) — required per NewPipeExtractor's own README.
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.javascript.tools.**
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

