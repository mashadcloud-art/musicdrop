package com.musicdrop.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.musicdrop.tv.R

// Authentic YouTube Sans font extracted from official YouTube on Android TV
val YouTubeSansFamily = FontFamily(
    Font(R.font.youtube_sans_bold, FontWeight.Bold),
    Font(R.font.youtube_sans_bold, FontWeight.Normal),
    Font(R.font.youtube_sans_bold, FontWeight.Medium),
    Font(R.font.youtube_sans_bold, FontWeight.SemiBold)
)

val TvTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = YouTubeSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = YouTubeSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = YouTubeSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = YouTubeSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = YouTubeSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    labelLarge = TextStyle(
        fontFamily = YouTubeSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF0000),      // YouTube Red
    onPrimary = Color.White,
    background = Color(0xFF0F0F0F),   // YouTube TV dark background
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White
)

@Composable
fun MusicDropTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = TvTypography,
        content = content
    )
}
