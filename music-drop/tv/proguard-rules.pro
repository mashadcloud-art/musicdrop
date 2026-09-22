# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Coil
-keep class coil.** { *; }

# Keep TV classes
-keep class com.musicdrop.tv.** { *; }
-dontwarn com.musicdrop.tv.**

# NewPipeExtractor + Rhino JS engine
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.javascript.tools.**
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**
