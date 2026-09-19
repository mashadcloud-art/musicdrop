package com.musicdrop.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class PlayerThemeId(
    val title: String,
    val description: String,
    val previewColors: List<Color>
) {
    DYNAMIC_BLUR(
        "Dynamic Art Blur",
        "Adaptive real-time blur from current album cover",
        listOf(Color(0xFF2C3E50), Color(0xFF0F2027), Color(0xFF000000))
    ),
    DEEP_PURPLE(
        "Midnight Purple",
        "Vibrant cosmic purple glow with velvet backdrop",
        listOf(Color(0xFF2D114D), Color(0xFF170929), Color(0xFF0B0414))
    ),
    MIDNIGHT_OLED(
        "OLED Midnight",
        "True pitch black with sleek dark glass accents",
        listOf(Color(0xFF141414), Color(0xFF0A0A0A), Color(0xFF000000))
    ),
    SUNSET_AMBER(
        "Sunset Amber",
        "Warm golden hour radiance and terracotta tones",
        listOf(Color(0xFF5A2A18), Color(0xFF331409), Color(0xFF0F0602))
    ),
    EMERALD_FOREST(
        "Emerald Forest",
        "Lush mint and deep pine green ambient gradients",
        listOf(Color(0xFF104A3A), Color(0xFF082B21), Color(0xFF03140F))
    ),
    OCEAN_BLUE(
        "Sapphire Ocean",
        "Deep marine blue with luminous aquamarine touches",
        listOf(Color(0xFF123B5A), Color(0xFF081F33), Color(0xFF030D17))
    ),
    CYBERPUNK_NEON(
        "Cyberpunk Neon",
        "Electric magenta and neon cyan pulse gradient",
        listOf(Color(0xFF590E38), Color(0xFF1B0D38), Color(0xFF060312))
    ),
    PURE_FROST(
        "Frost Glass",
        "Bright modern glassmorphism aesthetic",
        listOf(Color(0xFF3B4856), Color(0xFF232C36), Color(0xFF13181E))
    );

    fun getBackgroundBrush(fallbackAccent: Color = Color(0xFFE91E63)): Brush {
        return when (this) {
            DYNAMIC_BLUR -> Brush.verticalGradient(
                colors = listOf(
                    fallbackAccent.copy(alpha = 0.45f),
                    Color(0xFF181528),
                    Color(0xFF0A0714)
                )
            )
            DEEP_PURPLE -> Brush.verticalGradient(
                colors = listOf(Color(0xFF3A1860), Color(0xFF1C0A33), Color(0xFF0C0317))
            )
            MIDNIGHT_OLED -> Brush.verticalGradient(
                colors = listOf(Color(0xFF121212), Color(0xFF080808), Color(0xFF000000))
            )
            SUNSET_AMBER -> Brush.verticalGradient(
                colors = listOf(Color(0xFF6B3012), Color(0xFF3B1505), Color(0xFF120501))
            )
            EMERALD_FOREST -> Brush.verticalGradient(
                colors = listOf(Color(0xFF145440), Color(0xFF0B3327), Color(0xFF04140F))
            )
            OCEAN_BLUE -> Brush.verticalGradient(
                colors = listOf(Color(0xFF154870), Color(0xFF0C2A42), Color(0xFF040E17))
            )
            CYBERPUNK_NEON -> Brush.verticalGradient(
                colors = listOf(Color(0xFF661042), Color(0xFF260D4F), Color(0xFF070314))
            )
            PURE_FROST -> Brush.verticalGradient(
                colors = listOf(Color(0xFF455566), Color(0xFF283440), Color(0xFF141B22))
            )
        }
    }
}
