package com.musicdrop.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Primary Accent — theme-aware. This used to be a fixed lime hex used
// everywhere (~200 call sites), which is why every theme, including the light
// ones, rendered the same neon lime. Each AppThemeMode already defines its own
// accentPrimary in Theme.kt (Cyber Dark = lime, OLED = emerald, Sunset = coral,
// Clean Light = indigo, iOS Light = system blue) — these now simply read that
// per-theme value, so dark themes keep their own accent (Cyber Dark stays
// lime/green) and light themes get a color that's actually legible against a
// light background, without editing every call site individually.
val ElectricLime: Color
    @Composable
    get() = LocalAppColors.current.accentPrimary

// A lighter tint of the current theme's accent — used for gradient highlights.
val ElectricLimeLight: Color
    @Composable
    get() = lerp(LocalAppColors.current.accentPrimary, Color.White, 0.35f)

// A dark, low-luminance tint of the current theme's accent — used as a subtle
// background tint (e.g. a chip background on a dark surface).
val ElectricLimeSubtle: Color
    @Composable
    get() = lerp(LocalAppColors.current.accentPrimary, Color.Black, 0.85f)

// Text color on accent backgrounds (e.g. badges, buttons, active tabs)
val TextOnAccent: Color
    @Composable
    get() = if (LocalAppColors.current.isDark) Color(0xFF0D0B1A) else Color.White

// Secondary Accent - Vibrant Coral & Crimson (from Reference 2)
val VibrantCoral = Color(0xFFFF3B5C)
val VibrantCoralLight = Color(0xFFFF6584)
val CoralGradientStart = Color(0xFFFF3B5C)
val CoralGradientEnd = Color(0xFFFF6584)
val CrimsonGlow = Color(0xFFE11D48)

// Purples & Ambient Glow (from Reference 1 & 3)
val PastelLilac = Color(0xFFA855F7)
val PastelLilacLight = Color(0xFFC084FC)
val PastelLilacSubtle = Color(0xFF2E1B4E)
val PurpleGradientStart = Color(0xFF9333EA)
val PurpleGradientEnd = Color(0xFF6366F1)

// Dynamic Backgrounds & Glassmorphic Surfaces that instantly adapt to selected theme
val DarkBg: Color
    @Composable
    get() = LocalAppColors.current.background

val SurfaceDark: Color
    @Composable
    get() = LocalAppColors.current.surface

val SurfaceElevated: Color
    @Composable
    get() = LocalAppColors.current.surfaceElevated

val SurfaceBorder: Color
    @Composable
    get() = LocalAppColors.current.surfaceBorder

val SurfaceBorderGlow = Color(0xFF433878)

// Category Colors
val PhotoCyan = Color(0xFF06B6D4)
val VideoAmber = Color(0xFFF59E0B)
val AudioPurple = Color(0xFFA855F7)
val DocumentEmerald = Color(0xFF10B981)

// Dynamic Text Colors that automatically adapt to light / dark / oled themes
val TextPrimary: Color
    @Composable
    get() = LocalAppColors.current.textPrimary

val TextSecondary: Color
    @Composable
    get() = LocalAppColors.current.textSecondary

val TextMuted: Color
    @Composable
    get() = LocalAppColors.current.textMuted

