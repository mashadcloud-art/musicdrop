import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}
val youtubeApiKey: String = localProperties.getProperty("YOUTUBE_API_KEY", "")

android {
    namespace = "com.musicdrop.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.musicdrop.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 169
        versionName = "1.5.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")
    }

    signingConfigs {
        named("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ""
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Required by NewPipeExtractor on API levels below 33
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Media3 & ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    // Adaptive (HLS) fallback for Vimeo videos with no progressive MP4 rendition
    implementation(libs.media3.exoplayer.hls)

    // Coil for Compose
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // ZXing for QR Code generation & camera scanning
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // YouTube IFrame Player API wrapper — official playback surface, no stream extraction
    implementation(libs.youtube.player.core)

    // NewPipeExtractor — real YouTube signatureCipher/n-param decryption (via bundled Rhino),
    // replacing the old hand-rolled "hope it's unencrypted" extractor.
    implementation(libs.newpipe.extractor)
    implementation(libs.okhttp)
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)

    // Android Auto Car App Library
    implementation(libs.car.app)
    implementation(libs.car.app.projected)

    debugImplementation(libs.androidx.ui.tooling)
}
