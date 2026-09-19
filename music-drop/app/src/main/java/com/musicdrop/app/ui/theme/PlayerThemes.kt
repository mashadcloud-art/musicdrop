package com.musicdrop.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class PlayerSkinLayout(
    val title: String,
    val subtitle: String
) {
    ROUNDED_CARD(
        "Modern Card",
        "Centered album artwork with floating progress pill"
    ),
    RADIAL_RING(
        "Radial Vinyl Ring",
        "Circular sweep progress ring with center time counter"
    ),
    RADIAL_DRAWER(
        "Ring & Up Next Drawer",
        "Circular progress ring with side controls and queue sheet"
    ),
    IMMERSIVE_DRAWER(
        "Scenic Wallpaper",
        "Full scenic atmospheric wallpaper with integrated playlist drawer"
    )
}

enum class PlayerThemeId(
    val title: String,
    val description: String,
    val previewColors: List<Color>,
    val defaultSkin: PlayerSkinLayout = PlayerSkinLayout.ROUNDED_CARD
) {
    MIDNIGHT_OLED(
        "OLED Midnight",
        "True pitch black with sleek dark glass accents",
        listOf(Color(0xFF141414), Color(0xFF0A0A0A), Color(0xFF000000)),
        PlayerSkinLayout.ROUNDED_CARD
    ),
    RADIAL_SUNSET(
        "Radial Sunset",
        "Circular sweep ring with warm dusky horizon glow",
        listOf(Color(0xFF5B3A60), Color(0xFF2E1C38), Color(0xFF150A1C)),
        PlayerSkinLayout.RADIAL_RING
    ),
    RADIAL_DRAWER_AMBER(
        "Horizon Ring Drawer",
        "Circular progress ring with live Up Next song sheet",
        listOf(Color(0xFF6B3A1C), Color(0xFF2B1D3A), Color(0xFF0C1428)),
        PlayerSkinLayout.RADIAL_DRAWER
    ),
    SCENIC_MOUNTAIN(
        "Mountain Dusk",
        "Atmospheric twilight landscape with bottom queue drawer",
        listOf(Color(0xFF2C3E50), Color(0xFF1A252F), Color(0xFF0E1318)),
        PlayerSkinLayout.IMMERSIVE_DRAWER
    ),
    CARBON_SLATE(
        "Carbon Slate",
        "High-tech carbon graphite and titanium gray",
        listOf(Color(0xFF2A2D34), Color(0xFF1B1C22), Color(0xFF0F1014)),
        PlayerSkinLayout.ROUNDED_CARD
    ),
    DEEP_PURPLE(
        "Midnight Purple",
        "Vibrant cosmic purple glow with velvet backdrop",
        listOf(Color(0xFF2D114D), Color(0xFF170929), Color(0xFF0B0414)),
        PlayerSkinLayout.ROUNDED_CARD
    ),
    CRIMSON_VELVET(
        "Crimson Velvet",
        "Deep wine red and rich ruby radiance",
        listOf(Color(0xFF4A0E1C), Color(0xFF25060E), Color(0xFF0F0206)),
        PlayerSkinLayout.ROUNDED_CARD
    ),
    SUNSET_AMBER(
        "Sunset Amber",
        "Warm golden hour radiance and terracotta tones",
        listOf(Color(0xFF5A2A18), Color(0xFF331409), Color(0xFF0F0602)),
        PlayerSkinLayout.ROUNDED_CARD
    ),
    EMERALD_FOREST(
        "Emerald Forest",
        "Lush mint and deep pine green ambient gradients",
        listOf(Color(0xFF104A3A), Color(0xFF082B21), Color(0xFF03140F)),
        PlayerSkinLayout.ROUNDED_CARD
    ),
    OCEAN_BLUE(
        "Sapphire Ocean",
        "Deep marine blue with luminous aquamarine touches",
        listOf(Color(0xFF123B5A), Color(0xFF081F33), Color(0xFF030D17)),
        PlayerSkinLayout.ROUNDED_CARD
    ),
    CYBERPUNK_NEON(
        "Cyberpunk Neon",
        "Electric magenta and neon cyan pulse gradient",
        listOf(Color(0xFF590E38), Color(0xFF1B0D38), Color(0xFF060312)),
        PlayerSkinLayout.ROUNDED_CARD
    ),
    ROYAL_GOLD(
        "Royal Gold",
        "Warm champagne bronze and burnished gold",
        listOf(Color(0xFF42350C), Color(0xFF211A06), Color(0xFF0C0902)),
        PlayerSkinLayout.ROUNDED_CARD
    ),
    AURORA_BOREALIS(
        "Aurora Borealis",
        "Shimmering arctic teal, violet and indigo glow",
        listOf(Color(0xFF0F4C5C), Color(0xFF1D2D44), Color(0xFF0B132B)),
        PlayerSkinLayout.ROUNDED_CARD
    ),
    PURE_FROST(
        "Frost Glass",
        "Bright modern glassmorphism aesthetic",
        listOf(Color(0xFF3B4856), Color(0xFF232C36), Color(0xFF13181E)),
        PlayerSkinLayout.ROUNDED_CARD
    ),
    DYNAMIC_BLUR(
        "Dynamic Vignette",
        "Adaptive dark gradient vignette with accent glow",
        listOf(Color(0xFF2C3E50), Color(0xFF0F2027), Color(0xFF000000)),
        PlayerSkinLayout.ROUNDED_CARD
    );

    fun getBackgroundBrush(fallbackAccent: Color = Color(0xFFE91E63)): Brush {
        return when (this) {
            MIDNIGHT_OLED -> Brush.verticalGradient(
                colors = listOf(Color(0xFF121212), Color(0xFF080808), Color(0xFF000000))
            )
            RADIAL_SUNSET -> Brush.verticalGradient(
                colors = listOf(Color(0xFF503258), Color(0xFF281830), Color(0xFF110816))
            )
            RADIAL_DRAWER_AMBER -> Brush.verticalGradient(
                colors = listOf(Color(0xFF5C3318), Color(0xFF281C33), Color(0xFF0A1020))
            )
            SCENIC_MOUNTAIN -> Brush.verticalGradient(
                colors = listOf(Color(0xFF263544), Color(0xFF151E28), Color(0xFF0B1015))
            )
            CARBON_SLATE -> Brush.verticalGradient(
                colors = listOf(Color(0xFF24272E), Color(0xFF17181D), Color(0xFF0C0D10))
            )
            DEEP_PURPLE -> Brush.verticalGradient(
                colors = listOf(Color(0xFF3A1860), Color(0xFF1C0A33), Color(0xFF0C0317))
            )
            CRIMSON_VELVET -> Brush.verticalGradient(
                colors = listOf(Color(0xFF541222), Color(0xFF280811), Color(0xFF100206))
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
            ROYAL_GOLD -> Brush.verticalGradient(
                colors = listOf(Color(0xFF4C3E10), Color(0xFF292107), Color(0xFF0F0C02))
            )
            AURORA_BOREALIS -> Brush.verticalGradient(
                colors = listOf(Color(0xFF125666), Color(0xFF1F3047), Color(0xFF0C142E))
            )
            PURE_FROST -> Brush.verticalGradient(
                colors = listOf(Color(0xFF455566), Color(0xFF283440), Color(0xFF141B22))
            )
            DYNAMIC_BLUR -> Brush.verticalGradient(
                colors = listOf(
                    fallbackAccent.copy(alpha = 0.35f),
                    Color(0xFF141220),
                    Color(0xFF07050E)
                )
            )
        }
    }
}
