package com.musicdrop.tv.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class PlayerSkinLayout(
    val title: String,
    val subtitle: String
) {
    VINYL_TURNTABLE(
        "Vinyl Turntable",
        "Classic rotating grooved vinyl record with tonearm needle"
    ),
    RADIAL_DRAWER(
        "Ring & Up Next Drawer",
        "Circular progress ring with side controls and queue sheet"
    ),
    ROUNDED_CARD(
        "Modern Card",
        "Centered album artwork with floating progress pill"
    ),
    RADIAL_RING(
        "Radial Vinyl Ring",
        "Circular sweep progress ring with center time counter"
    ),
    IMMERSIVE_DRAWER(
        "Scenic Wallpaper",
        "Full scenic atmospheric wallpaper with integrated playlist drawer"
    ),
    FROSTED_GLASS(
        "Frosted Glass Transparent",
        "Dynamic blurred artwork backdrop with transparent glass controls"
    )
}

enum class PlayerThemeId(
    val title: String,
    val description: String,
    val previewColors: List<Color>,
    val defaultSkin: PlayerSkinLayout = PlayerSkinLayout.ROUNDED_CARD,
    val accentColor: Color = Color(0xFFF97316)
) {
    VINYL_CLASSIC(
        "Vinyl Classic",
        "Classic grooved vinyl disc with tonearm needle & cosmic twilight",
        listOf(Color(0xFF2C2F4D), Color(0xFF1B1D35), Color(0xFF0D0E1A)),
        PlayerSkinLayout.VINYL_TURNTABLE,
        Color(0xFF818CF8)
    ),
    VINYL_MIDNIGHT(
        "Vinyl Midnight",
        "Jet-black vinyl disc on pure OLED darkness",
        listOf(Color(0xFF1A1A1A), Color(0xFF0F0F0F), Color(0xFF050505)),
        PlayerSkinLayout.VINYL_TURNTABLE,
        Color(0xFFFF2A55)
    ),
    VINYL_GOLD(
        "Vinyl Vintage Gold",
        "Warm vintage amber grooves with brass tonearm",
        listOf(Color(0xFF4A3416), Color(0xFF2B1C0B), Color(0xFF120A03)),
        PlayerSkinLayout.VINYL_TURNTABLE,
        Color(0xFFF59E0B)
    ),
    MIDNIGHT_OLED(
        "OLED Midnight",
        "True pitch black with sleek dark glass accents",
        listOf(Color(0xFF141414), Color(0xFF0A0A0A), Color(0xFF000000)),
        PlayerSkinLayout.ROUNDED_CARD,
        Color(0xFFFF0000)
    ),
    RADIAL_SUNSET(
        "Radial Sunset",
        "Circular sweep ring with warm dusky horizon glow",
        listOf(Color(0xFF5B3A60), Color(0xFF2E1C38), Color(0xFF150A1C)),
        PlayerSkinLayout.RADIAL_RING,
        Color(0xFFF43F5E)
    ),
    RADIAL_DRAWER_AMBER(
        "Horizon Ring Drawer",
        "Circular progress ring with live Up Next song sheet",
        listOf(Color(0xFF6B3A1C), Color(0xFF2B1D3A), Color(0xFF0C1428)),
        PlayerSkinLayout.RADIAL_DRAWER,
        Color(0xFFF97316)
    ),
    SCENIC_MOUNTAIN(
        "Mountain Dusk",
        "Atmospheric twilight landscape with bottom queue drawer",
        listOf(Color(0xFF2C3E50), Color(0xFF1A252F), Color(0xFF0E1318)),
        PlayerSkinLayout.IMMERSIVE_DRAWER,
        Color(0xFF38BDF8)
    ),
    CARBON_SLATE(
        "Carbon Slate",
        "High-tech carbon graphite and titanium gray",
        listOf(Color(0xFF2A2D34), Color(0xFF1B1C22), Color(0xFF0F1014)),
        PlayerSkinLayout.ROUNDED_CARD,
        Color(0xFF94A3B8)
    ),
    DEEP_PURPLE(
        "Midnight Purple",
        "Vibrant cosmic purple glow with velvet backdrop",
        listOf(Color(0xFF2D114D), Color(0xFF170929), Color(0xFF0B0414)),
        PlayerSkinLayout.ROUNDED_CARD,
        Color(0xFFA855F7)
    ),
    CRIMSON_VELVET(
        "Crimson Velvet",
        "Deep wine red and rich ruby radiance",
        listOf(Color(0xFF4A0E1C), Color(0xFF25060E), Color(0xFF0F0206)),
        PlayerSkinLayout.ROUNDED_CARD,
        Color(0xFFE11D48)
    ),
    SUNSET_AMBER(
        "Sunset Amber",
        "Warm golden hour radiance and terracotta tones",
        listOf(Color(0xFF5A2A18), Color(0xFF331409), Color(0xFF0F0602)),
        PlayerSkinLayout.ROUNDED_CARD,
        Color(0xFFF97316)
    ),
    EMERALD_FOREST(
        "Emerald Forest",
        "Lush mint and deep pine green ambient gradients",
        listOf(Color(0xFF104A3A), Color(0xFF082B21), Color(0xFF03140F)),
        PlayerSkinLayout.ROUNDED_CARD,
        Color(0xFF10B981)
    ),
    OCEAN_BLUE(
        "Sapphire Ocean",
        "Deep marine blue with luminous aquamarine touches",
        listOf(Color(0xFF123B5A), Color(0xFF081F33), Color(0xFF030D17)),
        PlayerSkinLayout.ROUNDED_CARD,
        Color(0xFF0EA5E9)
    ),
    CYBERPUNK_NEON(
        "Cyberpunk Neon",
        "Electric magenta and neon cyan pulse gradient",
        listOf(Color(0xFF590E38), Color(0xFF1B0D38), Color(0xFF060312)),
        PlayerSkinLayout.ROUNDED_CARD,
        Color(0xFFD6FF4B)
    ),
    ROYAL_GOLD(
        "Royal Gold",
        "Warm champagne bronze and burnished gold",
        listOf(Color(0xFF42350C), Color(0xFF211A06), Color(0xFF0C0902)),
        PlayerSkinLayout.ROUNDED_CARD,
        Color(0xFFEAB308)
    ),
    AURORA_BOREALIS(
        "Aurora Borealis",
        "Shimmering arctic teal, violet and indigo glow",
        listOf(Color(0xFF0F4C5C), Color(0xFF1D2D44), Color(0xFF0B132B)),
        PlayerSkinLayout.ROUNDED_CARD,
        Color(0xFF2DD4BF)
    ),
    PURE_FROST(
        "Frost Glass",
        "Bright modern glassmorphism aesthetic",
        listOf(Color(0xFF3B4856), Color(0xFF232C36), Color(0xFF13181E)),
        PlayerSkinLayout.FROSTED_GLASS,
        Color(0xFF60A5FA)
    ),
    DYNAMIC_BLUR(
        "Dynamic Vignette",
        "Adaptive dark gradient vignette with accent glow",
        listOf(Color(0xFF2C3E50), Color(0xFF0F2027), Color(0xFF000000)),
        PlayerSkinLayout.FROSTED_GLASS,
        Color(0xFFF97316)
    );

    fun getBackgroundBrush(fallbackAccent: Color = Color(0xFFE91E63)): Brush {
        return when (this) {
            VINYL_CLASSIC -> Brush.verticalGradient(
                colors = listOf(Color(0xFF33385E), Color(0xFF1E213D), Color(0xFF0D0E1C))
            )
            VINYL_MIDNIGHT -> Brush.verticalGradient(
                colors = listOf(Color(0xFF181818), Color(0xFF0D0D0D), Color(0xFF000000))
            )
            VINYL_GOLD -> Brush.verticalGradient(
                colors = listOf(Color(0xFF523B1A), Color(0xFF2E200C), Color(0xFF120B03))
            )
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
