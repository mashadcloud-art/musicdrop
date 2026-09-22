package com.musicdrop.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat

enum class AppThemeMode(val displayName: String, val subtitle: String, val isDark: Boolean) {
    YOUTUBE_MUSIC("YouTube Music", "Pure Black & YouTube Red · Official YTM Dark Theme (Default)", true),
    MUSIC_PULSE("Pulse", "Lime & Violet · Music Drop's own theme", true),
    MUSIC_ORBIT("Orbit", "Teal & Coral · Music Drop's own theme", true),
    MUSIC_GREENROOM("Greenroom", "Spotify-style Green · Music Drop's own theme", true),
    CYBER_DARK("Cyber Dark", "Neon Lime & Deep Obsidian", true),
    CLEAN_LIGHT("Clean Light", "Crisp Minimalist & Indigo", false),
    OLED_BLACK("OLED Black", "Pure AMOLED & Emerald", true),
    SUNSET_NEBULA("Sunset Nebula", "Midnight Plum & Sunset Coral", true),
    IOS_LIGHT("iOS Light", "Premium & Minimal · Apple-Inspired", false),
    NEARBY_SHARE("Nearby Share", "Clean & Minimal · Quick-Share Style", false),
    TURBO_CONNECT("Turbo Connect", "Bold & Energetic · Fast-Transfer Style", true),
    RETRO("Retro", "Warm Sunset & Vintage Tape", false),
    GLASSMORPHISM("Glassmorphism", "Frosted Glass & Soft Neon", true),
    ROYAL_PLUM("Royal Plum", "Rich Wine & Velvet Rose (Image 1 Theme)", true),
    DEEP_NAVY("Deep Navy", "Midnight Sea & Royal Blue", true),
    ROSE_GOLD("Rose Gold", "Soft Blush & Ruby Rose", false)
}

val LocalAppTheme = compositionLocalOf { AppThemeMode.YOUTUBE_MUSIC }

// Raw lime, used only to DEFINE the themes below. Color.kt's ElectricLime is a
// @Composable property that reads back from these theme definitions — using
// that here instead would be circular (and these AppColors/ColorScheme values
// are top-level, non-composable, so it wouldn't compile anyway).
private val RawElectricLime = Color(0xFFD2F801)

data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val isDark: Boolean,
    val isGlassmorphism: Boolean = false
)

// ── YouTube Music Official Dark Theme (Image 1 & 2) ──
val YouTubeMusicAppColors = AppColors(
    background = Color(0xFF030303), // AMOLED pure black
    surface = Color(0xFF212121),    // YTM bottom nav and cards
    surfaceElevated = Color(0xFF282828), // Elevated panels/dialogs
    surfaceBorder = Color(0xFF333333),
    textPrimary = Color(0xFFFFFFFF), // Pure white
    textSecondary = Color(0xFFAAAAAA), // YTM secondary gray
    textMuted = Color(0xFF717171),    // YTM muted gray
    accentPrimary = Color(0xFFFF0000), // YouTube Brand Red
    accentSecondary = Color(0xFFFF4E4E), // YouTube Coral/Light Red
    isDark = true
)

// ── Music Drop's own theme (music-stream/constants/colors.ts, dark variant) ──
val MusicPulseAppColors = AppColors(
    background = Color(0xFF09090B),
    surface = Color(0xFF15151A),
    surfaceElevated = Color(0xFF202027),
    surfaceBorder = Color(0xFF2D2D38),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFFA4A2B0),
    textMuted = Color(0xFFA4A2B0),
    accentPrimary = Color(0xFFD6FF4B),
    accentSecondary = Color(0xFF8E7CFF),
    isDark = true
)

val MusicOrbitAppColors = AppColors(
    background = Color(0xFF071A1C),
    surface = Color(0xFF102B2D),
    surfaceElevated = Color(0xFF173A3B),
    surfaceBorder = Color(0xFF255052),
    textPrimary = Color(0xFFEBFFFC),
    textSecondary = Color(0xFF91B5B2),
    textMuted = Color(0xFF91B5B2),
    accentPrimary = Color(0xFF29D3C2),
    accentSecondary = Color(0xFFFF8066),
    isDark = true
)

val MusicGreenroomAppColors = AppColors(
    background = Color(0xFF0B0B0B),
    surface = Color(0xFF181818),
    surfaceElevated = Color(0xFF282828),
    surfaceBorder = Color(0xFF383838),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFFA7A7A7),
    textMuted = Color(0xFFA7A7A7),
    accentPrimary = Color(0xFF1ED760),
    accentSecondary = Color(0xFFB7F35B),
    isDark = true
)

val CyberDarkAppColors = AppColors(
    background = Color(0xFF090714),
    surface = Color(0xFF120F24),
    surfaceElevated = Color(0xFF1C1736),
    surfaceBorder = Color(0xFF2C2552),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    accentPrimary = RawElectricLime,
    accentSecondary = PastelLilac,
    isDark = true
)

val CleanLightAppColors = AppColors(
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F5F9),
    surfaceBorder = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    accentPrimary = Color(0xFF2563EB),
    accentSecondary = Color(0xFF3B82F6),
    isDark = false
)

val OledBlackAppColors = AppColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0B0B0B),
    surfaceElevated = Color(0xFF141414),
    surfaceBorder = Color(0xFF242424),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA1A1AA),
    textMuted = Color(0xFF71717A),
    accentPrimary = DocumentEmerald,
    accentSecondary = RawElectricLime,
    isDark = true
)

val SunsetNebulaAppColors = AppColors(
    background = Color(0xFF130826),
    surface = Color(0xFF1E0E3D),
    surfaceElevated = Color(0xFF2F145C),
    surfaceBorder = Color(0xFF4C208C),
    textPrimary = Color(0xFFFFF0F5),
    textSecondary = Color(0xFFE2BBE9),
    textMuted = Color(0xFFA685B5),
    accentPrimary = VibrantCoral,
    accentSecondary = VideoAmber,
    isDark = true
)

// Apple Human Interface Guidelines-inspired light palette: white cards floating on the
// iOS "grouped" system gray, System Blue accent, near-black labels rather than pure
// black — the same restraint iOS itself uses instead of stark #000000 text.
val IosLightAppColors = AppColors(
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF2F2F7),
    surfaceBorder = Color(0xFFE5E5EA),
    textPrimary = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF6C6C70),
    textMuted = Color(0xFFAEAEB2),
    accentPrimary = Color(0xFF007AFF),
    accentSecondary = Color(0xFF32ADE6),
    isDark = false
)

// Clean, white/blue palette inspired by minimalist "nearby share" style
// device-to-device sharing UIs — light, airy, low-chroma.
val NearbyShareAppColors = AppColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF8FAFC),
    surfaceElevated = Color(0xFFF1F4F9),
    surfaceBorder = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF1F2937),
    textSecondary = Color(0xFF6B7686),
    textMuted = Color(0xFF8A93A3),
    accentPrimary = Color(0xFF4C8DFF),
    accentSecondary = Color(0xFF17B4CC),
    isDark = false
)

// Deep navy with a hot orange + cyan pop — bold, high-energy "fast transfer"
// aesthetic inspired by Xender-style transfer apps.
val TurboConnectAppColors = AppColors(
    background = Color(0xFF0A1628),
    surface = Color(0xFF0F2038),
    surfaceElevated = Color(0xFF16304F),
    surfaceBorder = Color(0xFF1F4468),
    textPrimary = Color(0xFFF0F6FF),
    textSecondary = Color(0xFF9DB8DC),
    textMuted = Color(0xFF6E88AA),
    accentPrimary = Color(0xFFFF7A33),
    accentSecondary = Color(0xFF17B4CC),
    isDark = true
)

// Warm cream & burnt-orange/teal palette — 70s/80s vintage-tape mood.
val RetroAppColors = AppColors(
    background = Color(0xFFFBF5EC),
    surface = Color(0xFFFFFDF9),
    surfaceElevated = Color(0xFFF3E7D5),
    surfaceBorder = Color(0xFFDFCDAF),
    textPrimary = Color(0xFF261910),
    textSecondary = Color(0xFF5E4331),
    textMuted = Color(0xFF8F7057),
    accentPrimary = Color(0xFFE86034),
    accentSecondary = Color(0xFF2A9D8F),
    isDark = false
)

// Deep indigo base with frosted ice-blue + soft pink accents — the color side
// of a glassmorphism look.
val GlassmorphismAppColors = AppColors(
    background = Color(0xFF12102A),
    surface = Color(0xFF1C1940),
    surfaceElevated = Color(0xFF2A2560),
    surfaceBorder = Color(0xFF3D3780),
    textPrimary = Color(0xFFF5F3FF),
    textSecondary = Color(0xFFB8B0E0),
    textMuted = Color(0xFF8078B0),
    accentPrimary = Color(0xFF7DD3FC),
    accentSecondary = Color(0xFFF472B6),
    isDark = true,
    isGlassmorphism = true
)

// Rich wine & dark velvet plum palette matching Image 1
val RoyalPlumAppColors = AppColors(
    background = Color(0xFF24071A),
    surface = Color(0xFF350B28),
    surfaceElevated = Color(0xFF491238),
    surfaceBorder = Color(0xFF671D4F),
    textPrimary = Color(0xFFFFF0F7),
    textSecondary = Color(0xFFE4B5D4),
    textMuted = Color(0xFFAE7A9C),
    accentPrimary = Color(0xFFE11D48),
    accentSecondary = Color(0xFFF472B6),
    isDark = true
)

// Deep midnight oceanic navy with electric cobalt
val DeepNavyAppColors = AppColors(
    background = Color(0xFF071224),
    surface = Color(0xFF0E1E3A),
    surfaceElevated = Color(0xFF162B50),
    surfaceBorder = Color(0xFF224074),
    textPrimary = Color(0xFFF0F6FF),
    textSecondary = Color(0xFF94B5E6),
    textMuted = Color(0xFF6285B8),
    accentPrimary = Color(0xFF3B82F6),
    accentSecondary = Color(0xFF60A5FA),
    isDark = true
)

// Light rose gold & soft blush aesthetic
val RoseGoldAppColors = AppColors(
    background = Color(0xFFFFF1F4),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFE4EB),
    surfaceBorder = Color(0xFFFBCFE8),
    textPrimary = Color(0xFF4C1528),
    textSecondary = Color(0xFF83334F),
    textMuted = Color(0xFFA8667E),
    accentPrimary = Color(0xFFE11D48),
    accentSecondary = Color(0xFFFB7185),
    isDark = false
)

val LocalAppColors = compositionLocalOf { YouTubeMusicAppColors }
val LocalCardOpacity = compositionLocalOf { 0.55f }
val LocalCustomFontFamily = compositionLocalOf<androidx.compose.ui.text.font.FontFamily?> { null }
val LocalCustomFontColor = compositionLocalOf<Color?> { null }

private val YouTubeMusicColorScheme = darkColorScheme(
    primary = Color(0xFFFF0000),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B0000),
    onPrimaryContainer = Color(0xFFFF8080),
    secondary = Color.White,
    onSecondary = Color(0xFF030303),
    secondaryContainer = Color(0xFF282828),
    onSecondaryContainer = Color.White,
    background = Color(0xFF030303),
    onBackground = Color.White,
    surface = Color(0xFF212121),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF282828),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF383838)
)

private val MusicPulseColorScheme = darkColorScheme(
    primary = Color(0xFFD6FF4B),
    onPrimary = Color(0xFF09090B),
    primaryContainer = Color(0xFF2A2E0E),
    onPrimaryContainer = Color(0xFFD6FF4B),
    secondary = Color(0xFF8E7CFF),
    onSecondary = Color(0xFFF5F5F7),
    secondaryContainer = Color(0xFF202027),
    onSecondaryContainer = Color(0xFF8E7CFF),
    background = Color(0xFF09090B),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF15151A),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF202027),
    onSurfaceVariant = Color(0xFFA4A2B0),
    outline = Color(0xFF2D2D38)
)

private val MusicOrbitColorScheme = darkColorScheme(
    primary = Color(0xFF29D3C2),
    onPrimary = Color(0xFF071A1C),
    primaryContainer = Color(0xFF0F3A38),
    onPrimaryContainer = Color(0xFF29D3C2),
    secondary = Color(0xFFFF8066),
    onSecondary = Color(0xFF071A1C),
    secondaryContainer = Color(0xFF173A3B),
    onSecondaryContainer = Color(0xFFFF8066),
    background = Color(0xFF071A1C),
    onBackground = Color(0xFFEBFFFC),
    surface = Color(0xFF102B2D),
    onSurface = Color(0xFFEBFFFC),
    surfaceVariant = Color(0xFF173A3B),
    onSurfaceVariant = Color(0xFF91B5B2),
    outline = Color(0xFF255052)
)

private val MusicGreenroomColorScheme = darkColorScheme(
    primary = Color(0xFF1ED760),
    onPrimary = Color(0xFF071108),
    primaryContainer = Color(0xFF0F3A20),
    onPrimaryContainer = Color(0xFF1ED760),
    secondary = Color(0xFFB7F35B),
    onSecondary = Color(0xFF071108),
    secondaryContainer = Color(0xFF282828),
    onSecondaryContainer = Color(0xFFB7F35B),
    background = Color(0xFF0B0B0B),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF181818),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF282828),
    onSurfaceVariant = Color(0xFFA7A7A7),
    outline = Color(0xFF383838)
)

private val CyberDarkColorScheme = darkColorScheme(
    primary = PastelLilac,
    onPrimary = Color(0xFFF8FAFC),
    primaryContainer = PastelLilacSubtle,
    onPrimaryContainer = PastelLilacLight,
    secondary = VibrantCoral,
    onSecondary = Color(0xFFF8FAFC),
    secondaryContainer = Color(0xFF1C1736),
    onSecondaryContainer = VibrantCoralLight,
    background = Color(0xFF090714),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF120F24),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1C1736),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF2C2552)
)

private val CleanLightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = Color(0xFF1D4ED8),
    secondary = Color(0xFF0F172A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1)
)

private val OledBlackColorScheme = darkColorScheme(
    primary = DocumentEmerald,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF062B1D),
    onPrimaryContainer = DocumentEmerald,
    secondary = RawElectricLime,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF141414),
    onSecondaryContainer = RawElectricLime,
    background = Color(0xFF000000),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF0C0C0C),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF181818),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF27272A)
)

private val SunsetNebulaColorScheme = darkColorScheme(
    primary = VibrantCoral,
    onPrimary = Color(0xFFFFF0F5),
    primaryContainer = Color(0xFF3B0B1D),
    onPrimaryContainer = VibrantCoralLight,
    secondary = VideoAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF26103A),
    onSecondaryContainer = VideoAmber,
    background = Color(0xFF120721),
    onBackground = Color(0xFFFDE8FF),
    surface = Color(0xFF1B0D30),
    onSurface = Color(0xFFFDE8FF),
    surfaceVariant = Color(0xFF2C164D),
    onSurfaceVariant = Color(0xFFE9D5FF),
    outline = Color(0xFF4C2280)
)

private val IosLightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF004C99),
    secondary = Color(0xFF32ADE6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAF7FD),
    onSecondaryContainer = Color(0xFF1C6E8C),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF6C6C70),
    outline = Color(0xFFE5E5EA)
)

private val NearbyShareColorScheme = lightColorScheme(
    primary = Color(0xFF4C8DFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF1FF),
    onPrimaryContainer = Color(0xFF1D4ED8),
    secondary = Color(0xFF17B4CC),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F8FB),
    onSecondaryContainer = Color(0xFF0E7A8C),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1F2937),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFF1F4F9),
    onSurfaceVariant = Color(0xFF6B7686),
    outline = Color(0xFFE2E8F0)
)

private val TurboConnectColorScheme = darkColorScheme(
    primary = Color(0xFFFF7A33),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4A2410),
    onPrimaryContainer = Color(0xFFFFB380),
    secondary = Color(0xFF17B4CC),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF123C46),
    onSecondaryContainer = Color(0xFF7DE0EE),
    background = Color(0xFF0A1628),
    onBackground = Color(0xFFF0F6FF),
    surface = Color(0xFF0F2038),
    onSurface = Color(0xFFF0F6FF),
    surfaceVariant = Color(0xFF16304F),
    onSurfaceVariant = Color(0xFF9DB8DC),
    outline = Color(0xFF1F4468)
)

private val RetroColorScheme = lightColorScheme(
    primary = Color(0xFFE8734A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBE1D3),
    onPrimaryContainer = Color(0xFF8A3A1C),
    secondary = Color(0xFF2A9D8F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCF0EC),
    onSecondaryContainer = Color(0xFF14544C),
    background = Color(0xFFF5EDE0),
    onBackground = Color(0xFF3D2B1F),
    surface = Color(0xFFFFF8ED),
    onSurface = Color(0xFF3D2B1F),
    surfaceVariant = Color(0xFFF0E4D0),
    onSurfaceVariant = Color(0xFF7A5C46),
    outline = Color(0xFFE0CBA8)
)

private val GlassmorphismColorScheme = darkColorScheme(
    primary = Color(0xFF7DD3FC),
    onPrimary = Color(0xFF0A2233),
    primaryContainer = Color(0xFF17415A),
    onPrimaryContainer = Color(0xFFBEE9FE),
    secondary = Color(0xFFF472B6),
    onSecondary = Color(0xFF3D0B26),
    secondaryContainer = Color(0xFF4E1D3B),
    onSecondaryContainer = Color(0xFFFBC7E0),
    background = Color(0xFF12102A),
    onBackground = Color(0xFFF5F3FF),
    surface = Color(0xFF1C1940),
    onSurface = Color(0xFFF5F3FF),
    surfaceVariant = Color(0xFF2A2560),
    onSurfaceVariant = Color(0xFFB8B0E0),
    outline = Color(0xFF3D3780)
)

private val RoyalPlumColorScheme = darkColorScheme(
    primary = Color(0xFFE11D48),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF491238),
    onPrimaryContainer = Color(0xFFF472B6),
    secondary = Color(0xFFF472B6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF350B28),
    onSecondaryContainer = Color(0xFFFFF0F7),
    background = Color(0xFF24071A),
    onBackground = Color(0xFFFFF0F7),
    surface = Color(0xFF350B28),
    onSurface = Color(0xFFFFF0F7),
    surfaceVariant = Color(0xFF491238),
    onSurfaceVariant = Color(0xFFE4B5D4),
    outline = Color(0xFF671D4F)
)

private val DeepNavyColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF162B50),
    onPrimaryContainer = Color(0xFF60A5FA),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0E1E3A),
    onSecondaryContainer = Color(0xFFF0F6FF),
    background = Color(0xFF071224),
    onBackground = Color(0xFFF0F6FF),
    surface = Color(0xFF0E1E3A),
    onSurface = Color(0xFFF0F6FF),
    surfaceVariant = Color(0xFF162B50),
    onSurfaceVariant = Color(0xFF94B5E6),
    outline = Color(0xFF224074)
)

private val RoseGoldColorScheme = lightColorScheme(
    primary = Color(0xFFE11D48),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4EB),
    onPrimaryContainer = Color(0xFF881337),
    secondary = Color(0xFFFB7185),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF1F4),
    onSecondaryContainer = Color(0xFF4C1528),
    background = Color(0xFFFFF1F4),
    onBackground = Color(0xFF4C1528),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF4C1528),
    surfaceVariant = Color(0xFFFFE4EB),
    onSurfaceVariant = Color(0xFF83334F),
    outline = Color(0xFFFBCFE8)
)

@Composable
fun FileDropTheme(
    themeMode: AppThemeMode = AppThemeMode.YOUTUBE_MUSIC,
    cardOpacity: Float = 0.55f,
    fontFamilyName: String = "DEFAULT",
    fontColorOption: String = "DEFAULT",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.YOUTUBE_MUSIC -> YouTubeMusicColorScheme
        AppThemeMode.MUSIC_PULSE -> MusicPulseColorScheme
        AppThemeMode.MUSIC_ORBIT -> MusicOrbitColorScheme
        AppThemeMode.MUSIC_GREENROOM -> MusicGreenroomColorScheme
        AppThemeMode.CYBER_DARK -> CyberDarkColorScheme
        AppThemeMode.CLEAN_LIGHT -> CleanLightColorScheme
        AppThemeMode.OLED_BLACK -> OledBlackColorScheme
        AppThemeMode.SUNSET_NEBULA -> SunsetNebulaColorScheme
        AppThemeMode.IOS_LIGHT -> IosLightColorScheme
        AppThemeMode.NEARBY_SHARE -> NearbyShareColorScheme
        AppThemeMode.TURBO_CONNECT -> TurboConnectColorScheme
        AppThemeMode.RETRO -> RetroColorScheme
        AppThemeMode.GLASSMORPHISM -> GlassmorphismColorScheme
        AppThemeMode.ROYAL_PLUM -> RoyalPlumColorScheme
        AppThemeMode.DEEP_NAVY -> DeepNavyColorScheme
        AppThemeMode.ROSE_GOLD -> RoseGoldColorScheme
    }

    val baseAppColors = when (themeMode) {
        AppThemeMode.YOUTUBE_MUSIC -> YouTubeMusicAppColors
        AppThemeMode.MUSIC_PULSE -> MusicPulseAppColors
        AppThemeMode.MUSIC_ORBIT -> MusicOrbitAppColors
        AppThemeMode.MUSIC_GREENROOM -> MusicGreenroomAppColors
        AppThemeMode.CYBER_DARK -> CyberDarkAppColors
        AppThemeMode.CLEAN_LIGHT -> CleanLightAppColors
        AppThemeMode.OLED_BLACK -> OledBlackAppColors
        AppThemeMode.SUNSET_NEBULA -> SunsetNebulaAppColors
        AppThemeMode.IOS_LIGHT -> IosLightAppColors
        AppThemeMode.NEARBY_SHARE -> NearbyShareAppColors
        AppThemeMode.TURBO_CONNECT -> TurboConnectAppColors
        AppThemeMode.RETRO -> RetroAppColors
        AppThemeMode.GLASSMORPHISM -> GlassmorphismAppColors
        AppThemeMode.ROYAL_PLUM -> RoyalPlumAppColors
        AppThemeMode.DEEP_NAVY -> DeepNavyAppColors
        AppThemeMode.ROSE_GOLD -> RoseGoldAppColors
    }

    val customTextPrimary = remember(fontColorOption, baseAppColors) {
        when (fontColorOption.uppercase()) {
            "PURE_WHITE" -> Color.White
            "WARM_CREAM" -> Color(0xFFFFFBEB)
            "GOLD_ACCENT" -> Color(0xFFFDE047)
            "CYAN_ICE" -> Color(0xFF67E8F9)
            "HIGH_CONTRAST" -> if (baseAppColors.isDark) Color.White else Color(0xFF0F172A)
            else -> baseAppColors.textPrimary
        }
    }

    val appColors = remember(baseAppColors, cardOpacity, customTextPrimary) {
        baseAppColors.copy(
            surface = baseAppColors.surface.copy(alpha = cardOpacity),
            surfaceElevated = baseAppColors.surfaceElevated.copy(alpha = cardOpacity),
            surfaceBorder = baseAppColors.surfaceBorder.copy(alpha = (cardOpacity * 1.35f).coerceIn(0.12f, 0.95f)),
            textPrimary = customTextPrimary
        )
    }

    val dynamicFamily = remember(fontFamilyName) {
        when (fontFamilyName.uppercase()) {
            "SERIF" -> FontFamily.Serif
            "MONOSPACE" -> FontFamily.Monospace
            "CURSIVE" -> FontFamily.Cursive
            "ROUNDED", "SANS_SERIF" -> FontFamily.SansSerif
            else -> FontFamily.Default
        }
    }

    val dynamicTypography = remember(dynamicFamily) {
        androidx.compose.material3.Typography(
            displayLarge = Typography.displayLarge.copy(fontFamily = dynamicFamily),
            displayMedium = Typography.displayMedium.copy(fontFamily = dynamicFamily),
            displaySmall = Typography.displaySmall.copy(fontFamily = dynamicFamily),
            headlineLarge = Typography.headlineLarge.copy(fontFamily = dynamicFamily),
            headlineMedium = Typography.headlineMedium.copy(fontFamily = dynamicFamily),
            headlineSmall = Typography.headlineSmall.copy(fontFamily = dynamicFamily),
            titleLarge = Typography.titleLarge.copy(fontFamily = dynamicFamily),
            titleMedium = Typography.titleMedium.copy(fontFamily = dynamicFamily),
            titleSmall = Typography.titleSmall.copy(fontFamily = dynamicFamily),
            bodyLarge = Typography.bodyLarge.copy(fontFamily = dynamicFamily),
            bodyMedium = Typography.bodyMedium.copy(fontFamily = dynamicFamily),
            bodySmall = Typography.bodySmall.copy(fontFamily = dynamicFamily),
            labelLarge = Typography.labelLarge.copy(fontFamily = dynamicFamily),
            labelMedium = Typography.labelMedium.copy(fontFamily = dynamicFamily),
            labelSmall = Typography.labelSmall.copy(fontFamily = dynamicFamily)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            // Ensure window background matches current theme so the status bar / cutout area matches
            val bgArgb = appColors.background.toArgb()
            window.decorView.setBackgroundColor(bgArgb)
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgArgb))

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !themeMode.isDark
            insetsController.isAppearanceLightNavigationBars = !themeMode.isDark
        }
    }

    CompositionLocalProvider(
        LocalAppTheme provides themeMode,
        LocalAppColors provides appColors,
        LocalCardOpacity provides cardOpacity,
        LocalCustomFontFamily provides dynamicFamily,
        LocalCustomFontColor provides customTextPrimary,
        androidx.compose.material3.LocalContentColor provides appColors.textPrimary
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = dynamicTypography,
            content = content
        )
    }
}
