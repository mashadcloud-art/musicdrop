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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.app.data.model.MediaType
import com.musicdrop.app.data.model.StorageStats
import com.musicdrop.app.ui.theme.AudioPurple
import com.musicdrop.app.ui.theme.CoralGradientEnd
import com.musicdrop.app.ui.theme.CoralGradientStart
import com.musicdrop.app.ui.theme.DocumentEmerald
import com.musicdrop.app.ui.theme.PastelLilac
import com.musicdrop.app.ui.theme.PhotoCyan
import com.musicdrop.app.ui.theme.SurfaceBorder
import com.musicdrop.app.ui.theme.SurfaceDark
import com.musicdrop.app.ui.theme.SurfaceElevated
import com.musicdrop.app.ui.theme.TextMuted
import com.musicdrop.app.ui.theme.TextPrimary
import com.musicdrop.app.ui.theme.TextSecondary
import com.musicdrop.app.ui.theme.VideoAmber

@Composable
fun StorageCard(
    stats: StorageStats,
    onCategoryClick: (MediaType) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Internal Storage",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${stats.formatBytes(stats.usedBytes)} / ${stats.formatBytes(stats.totalBytes)}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PastelLilac.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${stats.formatBytes(stats.availableBytes)} Free",
                        color = PastelLilac,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gradient Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stats.usedPercent.coerceIn(0.01f, 1.0f))
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(CoralGradientStart, CoralGradientEnd, PastelLilac)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4 Category Quick Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CategoryPill(
                    icon = Icons.Rounded.Image,
                    label = "Photos",
                    sizeText = stats.formatBytes(stats.photosBytes),
                    color = PhotoCyan,
                    onClick = { onCategoryClick(MediaType.IMAGE) }
                )

                CategoryPill(
                    icon = Icons.Rounded.Videocam,
                    label = "Videos",
                    sizeText = stats.formatBytes(stats.videosBytes),
                    color = VideoAmber,
                    onClick = { onCategoryClick(MediaType.VIDEO) }
                )

                CategoryPill(
                    icon = Icons.Rounded.Audiotrack,
                    label = "Audio",
                    sizeText = stats.formatBytes(stats.audioBytes),
                    color = AudioPurple,
                    onClick = { onCategoryClick(MediaType.AUDIO) }
                )

                CategoryPill(
                    icon = Icons.Rounded.Description,
                    label = "Docs",
                    sizeText = stats.formatBytes(stats.docsBytes),
                    color = DocumentEmerald,
                    onClick = { onCategoryClick(MediaType.DOCUMENT) }
                )
            }
        }
    }
}

@Composable
private fun CategoryPill(
    icon: ImageVector,
    label: String,
    sizeText: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = sizeText,
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}
