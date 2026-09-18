package com.musicdrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.app.ui.theme.AudioPurple
import com.musicdrop.app.ui.theme.ElectricLime
import com.musicdrop.app.ui.theme.LocalAppColors
import com.musicdrop.app.ui.theme.SurfaceBorder
import com.musicdrop.app.ui.theme.SurfaceElevated
import com.musicdrop.app.ui.theme.TextMuted
import com.musicdrop.app.ui.theme.TextPrimary
import com.musicdrop.app.ui.theme.TextSecondary

// Dark "ink" tone used where content sits on a plain page background (not on
// the accent chip itself) and needs to read as dark, e.g. the light-theme
// selected label below the icon.
private val SelectedInkColor = Color(0xFF140E2A)

/**
 * Premium rounded icon card with count badge and modern gradient styling.
 * Used across Home, Music, Media, and Files screens for a unified, cohesive look.
 */
@Composable
fun ModernRoundedIconCard(
    title: String,
    icon: ImageVector,
    count: Int? = null,
    isSelected: Boolean,
    accentColor: Color = ElectricLime,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDarkTheme = LocalAppColors.current.isDark
    // accentColor now comes from the active theme (lime for Cyber Dark, emerald
    // for OLED, coral for Sunset, indigo for Clean Light, blue for iOS Light) —
    // it's no longer always a bright color, so the icon tint that reads clearly
    // on top of it has to be picked per-theme rather than assumed. A bright
    // accent (like Cyber Dark's lime) needs dark ink; every other, darker accent
    // needs white.
    val onAccentInk = if (accentColor.luminance() > 0.5f) SelectedInkColor else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    // Selected = a solid, fully-opaque accent chip (not a low-alpha
                    // tint over the page background) so the icon reads clearly no
                    // matter what's behind it in any theme.
                    if (isSelected) Brush.linearGradient(listOf(accentColor, AudioPurple))
                    else Brush.linearGradient(listOf(SurfaceElevated, SurfaceElevated))
                )
                .border(
                    1.5.dp,
                    if (isSelected) Color.Transparent else SurfaceBorder,
                    RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) onAccentInk else TextSecondary,
                modifier = Modifier.size(25.dp)
            )

            // Mini badge on top-right corner if count provided
            if (count != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 3.dp, end = 3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color.White else SurfaceBorder)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (count > 999) "${count / 1000}k" else "$count",
                        color = if (isSelected) SelectedInkColor else TextPrimary,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            // The label sits directly on the page background (not the accent
            // chip). On a light page, raw accentColor is a bit low-contrast for
            // small text — fall back to dark ink there; on dark theme the accent
            // pops fine directly on the dark background.
            color = if (isSelected) (if (isDarkTheme) accentColor else SelectedInkColor) else TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
