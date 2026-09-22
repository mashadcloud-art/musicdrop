package com.musicdrop.app.ui.tv

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import android.app.UiModeManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build

// Preference key stored in SharedPreferences
private const val TV_PREFS = "musicdrop_tv_prefs"
private const val KEY_TV_MODE = "tv_mode_choice"  // "TV" | "MOBILE" | null (not yet chosen)

enum class TvModeChoice { TV, MOBILE }

fun getTvModeChoice(context: Context): TvModeChoice? {
    val raw = context.getSharedPreferences(TV_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_TV_MODE, null) ?: return null
    return runCatching { TvModeChoice.valueOf(raw) }.getOrNull()
}

fun saveTvModeChoice(context: Context, choice: TvModeChoice) {
    context.getSharedPreferences(TV_PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY_TV_MODE, choice.name).apply()
}

fun clearTvModeChoice(context: Context) {
    context.getSharedPreferences(TV_PREFS, Context.MODE_PRIVATE)
        .edit().remove(KEY_TV_MODE).apply()
}

/**
 * Checks if the current hardware device is an Android TV, Google TV, Fire TV,
 * or TV box with remote control.
 */
fun isTvDevice(context: Context): Boolean {
    val pm = context.packageManager

    // 1. Android TV / Google TV Leanback system features
    if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) return true
    if (pm.hasSystemFeature("android.hardware.type.television")) return true
    if (pm.hasSystemFeature("android.software.leanback")) return true
    if (pm.hasSystemFeature("android.software.leanback_only")) return true

    // 2. UiModeManager check (standard Android TV mode)
    try {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
    } catch (_: Throwable) {}

    // 3. Amazon Fire TV devices (Fire TV Stick, Cube, Omni TV)
    if (pm.hasSystemFeature("amazon.hardware.fire_tv")) return true
    if (Build.MANUFACTURER.equals("Amazon", ignoreCase = true)) return true
    if (Build.MODEL.contains("AFT", ignoreCase = true)) return true

    // 4. Any Android device without a touchscreen is almost certainly a TV box / console
    if (!pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) return true

    // 5. Hardware / Model / Brand strings typical of TV boxes & Android TVs
    val brandInfo = (Build.BRAND + " " + Build.MANUFACTURER + " " + Build.MODEL + " " + Build.PRODUCT + " " + Build.HARDWARE + " " + Build.DEVICE).lowercase()
    val tvKeywords = listOf(
        "tv", "box", "droidlogic", "amlogic", "rockchip", "allwinner", "realtek", "mstar",
        "shield", "bravia", "sony tv", "tcl", "hisense", "philips tv", "xiaomi tv", "mitv",
        "redmi tv", "mi box", "mibox", "atv", "googletv", "smarttv", "firetv", "stick"
    )
    if (tvKeywords.any { brandInfo.contains(it) }) return true

    // 6. Navigation configuration: D-pad / Trackball remote controller
    val config = context.resources.configuration
    if (config.navigation == Configuration.NAVIGATION_DPAD || config.navigation == Configuration.NAVIGATION_TRACKBALL) return true

    // 7. Input device check: presence of a TV remote or D-pad input device
    try {
        val deviceIds = android.view.InputDevice.getDeviceIds()
        for (id in deviceIds) {
            val dev = android.view.InputDevice.getDevice(id) ?: continue
            val sources = dev.sources
            val name = dev.name.lowercase()
            if (sources and android.view.InputDevice.SOURCE_DPAD != 0 && dev.isExternal) {
                if (name.contains("remote") || name.contains("cec") || name.contains("air") || name.contains("controller") || name.contains("tv")) {
                    return true
                }
            }
        }
    } catch (_: Throwable) {}

    // 8. Landscape widescreen display (>= 720dp) with no telephony and no camera (classic Android TV Box)
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWide = config.screenWidthDp >= 720 || config.smallestScreenWidthDp >= 540
    val hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    val hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    val hasTouch = pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    if (isLandscape && isWide && !hasTelephony && (!hasCamera || !hasTouch)) return true

    // 9. Non-battery box (HDMI media box / Android TV box plugged into wall)
    try {
        val batteryIntent = context.registerReceiver(
            null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        )
        val hasBattery = batteryIntent?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true) ?: true
        if (!hasBattery && !hasTelephony) return true
    } catch (_: Throwable) {}

    return false
}

/**
 * Full-screen animated mode chooser shown on first TV launch.
 * User picks "📺 TV Mode" or "📱 Music Mode".
 * Choice is persisted and skipped on subsequent launches.
 */
@Composable
fun TvModeChooser(onChoice: (TvModeChoice) -> Unit) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    // Animate in on first frame
    LaunchedEffect(Unit) {
        delay(200)
        visible = true
    }

    val tvFocus = remember { FocusRequester() }
    LaunchedEffect(visible) {
        if (visible) {
            delay(300)
            try { tvFocus.requestFocus() } catch (_: Throwable) {}
        }
    }

    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "chooser_bg")
    val gradientAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "gradient_rotate"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080A12))
    ) {
        // Ambient glow blobs
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset((-80).dp, (-60).dp)
                .blur(120.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF6C3FA8).copy(0.6f), Color.Transparent))
                )
        )
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.BottomEnd)
                .offset(60.dp, 80.dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF2563EB).copy(0.5f), Color.Transparent))
                )
        )

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 },
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(40.dp),
                modifier = Modifier.padding(40.dp)
            ) {
                // Logo + headline
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🎵 MusicDrop",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Choose your experience",
                        fontSize = 22.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )
                }

                // Mode cards row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeCard(
                        icon = Icons.Filled.Tv,
                        title = "📺 Android TV & Box",
                        description = "100% Remote Control\n16:9 Cinema Video\nYouTube TV Experience",
                        gradient = Brush.linearGradient(
                            listOf(Color(0xFF7C3AED), Color(0xFF2563EB))
                        ),
                        focusRequester = tvFocus,
                        onClick = {
                            saveTvModeChoice(context, TvModeChoice.TV)
                            onChoice(TvModeChoice.TV)
                        }
                    )
                    ModeCard(
                        icon = Icons.Filled.PhoneAndroid,
                        title = "📱 Mobile Touch",
                        description = "Touchscreen only\nDesigned for phones\nNo remote D-pad",
                        gradient = Brush.linearGradient(
                            listOf(Color(0xFF059669), Color(0xFF0891B2))
                        ),
                        onClick = {
                            saveTvModeChoice(context, TvModeChoice.MOBILE)
                            onChoice(TvModeChoice.MOBILE)
                        }
                    )
                }

                Text(
                    text = "You can change this anytime in Settings",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    description: String,
    gradient: Brush,
    onClick: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    TvFocusButton(
        onClick = onClick,
        focusRequester = focusRequester,
        cornerRadius = 24.dp,
        modifier = Modifier
            .width(280.dp)
            .height(280.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient, RoundedCornerShape(24.dp))
        ) {
            // Subtle inner glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(0.15f), Color.Transparent)
                        )
                    )
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
