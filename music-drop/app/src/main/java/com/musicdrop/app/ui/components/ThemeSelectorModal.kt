package com.musicdrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.app.ui.theme.AppThemeMode
import com.musicdrop.app.ui.theme.DarkBg
import com.musicdrop.app.ui.theme.DocumentEmerald
import com.musicdrop.app.ui.theme.ElectricLime
import com.musicdrop.app.ui.theme.PastelLilac
import com.musicdrop.app.ui.theme.SurfaceBorder
import com.musicdrop.app.ui.theme.SurfaceDark
import com.musicdrop.app.ui.theme.SurfaceElevated
import com.musicdrop.app.ui.theme.TextMuted
import com.musicdrop.app.ui.theme.TextPrimary
import com.musicdrop.app.ui.theme.TextSecondary
import com.musicdrop.app.ui.theme.VibrantCoral
import com.musicdrop.app.ui.theme.VideoAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorModal(
    visible: Boolean,
    currentTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(SurfaceBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ColorLens,
                            contentDescription = "Theme",
                            tint = ElectricLime,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "App Theme & Appearance",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "9 custom crafted aesthetics",
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Theme Cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppThemeMode.values().forEach { mode ->
                    val isSelected = currentTheme == mode

                    val (bgBrush, accentColor, previewColors) = when (mode) {
                        AppThemeMode.CYBER_DARK -> Triple(
                            Brush.horizontalGradient(listOf(Color(0xFF1B1530), Color(0xFF100B20))),
                            ElectricLime,
                            listOf(Color(0xFF090714), Color(0xFFD2F801), Color(0xFFA855F7))
                        )
                        AppThemeMode.CLEAN_LIGHT -> Triple(
                            Brush.horizontalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))),
                            Color(0xFF6366F1),
                            listOf(Color(0xFFF8FAFC), Color(0xFF6366F1), Color(0xFF0F172A))
                        )
                        AppThemeMode.OLED_BLACK -> Triple(
                            Brush.horizontalGradient(listOf(Color(0xFF111111), Color(0xFF000000))),
                            DocumentEmerald,
                            listOf(Color(0xFF000000), Color(0xFF10B981), Color(0xFFCCFF00))
                        )
                        AppThemeMode.SUNSET_NEBULA -> Triple(
                            Brush.horizontalGradient(listOf(Color(0xFF28103A), Color(0xFF140822))),
                            VibrantCoral,
                            listOf(Color(0xFF120721), Color(0xFFFF3B5C), Color(0xFFF59E0B))
                        )
                        AppThemeMode.IOS_LIGHT -> Triple(
                            Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF2F2F7))),
                            Color(0xFF007AFF),
                            listOf(Color(0xFFFFFFFF), Color(0xFF007AFF), Color(0xFF1C1C1E))
                        )
                        AppThemeMode.NEARBY_SHARE -> Triple(
                            Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F4F9))),
                            Color(0xFF4C8DFF),
                            listOf(Color(0xFFF8FAFC), Color(0xFF4C8DFF), Color(0xFF17B4CC))
                        )
                        AppThemeMode.TURBO_CONNECT -> Triple(
                            Brush.horizontalGradient(listOf(Color(0xFF0F2038), Color(0xFF0A1628))),
                            Color(0xFFFF7A33),
                            listOf(Color(0xFF0A1628), Color(0xFFFF7A33), Color(0xFF17B4CC))
                        )
                        AppThemeMode.RETRO -> Triple(
                            Brush.horizontalGradient(listOf(Color(0xFFFFF8ED), Color(0xFFF0E4D0))),
                            Color(0xFFE8734A),
                            listOf(Color(0xFFFFF8ED), Color(0xFFE8734A), Color(0xFF2A9D8F))
                        )
                        AppThemeMode.GLASSMORPHISM -> Triple(
                            Brush.horizontalGradient(listOf(Color(0xFF1C1940), Color(0xFF12102A))),
                            Color(0xFF7DD3FC),
                            listOf(Color(0xFF12102A), Color(0xFF7DD3FC), Color(0xFFF472B6))
                        )
                        AppThemeMode.MUSIC_PULSE -> Triple(
                            Brush.horizontalGradient(listOf(Color(0xFF181024), Color(0xFF0D0814))),
                            Color(0xFFEC4899),
                            listOf(Color(0xFF0D0814), Color(0xFFEC4899), Color(0xFF8B5CF6))
                        )
                        AppThemeMode.MUSIC_ORBIT -> Triple(
                            Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617))),
                            Color(0xFF38BDF8),
                            listOf(Color(0xFF020617), Color(0xFF38BDF8), Color(0xFF6366F1))
                        )
                        AppThemeMode.MUSIC_GREENROOM -> Triple(
                            Brush.horizontalGradient(listOf(Color(0xFF052E16), Color(0xFF02170B))),
                            Color(0xFF22C55E),
                            listOf(Color(0xFF02170B), Color(0xFF22C55E), Color(0xFFEAB308))
                        )
                        else -> Triple(
                            Brush.horizontalGradient(listOf(Color(0xFF1B1530), Color(0xFF100B20))),
                            ElectricLime,
                            listOf(Color(0xFF090714), Color(0xFFD2F801), Color(0xFFA855F7))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(bgBrush)
                            .border(
                                1.5.dp,
                                if (isSelected) accentColor else SurfaceBorder,
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { onSelectTheme(mode) }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Mini Swatch Preview
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    previewColors.forEach { c ->
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = mode.displayName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!mode.isDark) Color(0xFF0F172A) else TextPrimary
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        fontSize = 11.5.sp,
                                        color = if (!mode.isDark) Color(0xFF64748B) else TextMuted
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
