package com.musicdrop.tv.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.tv.data.model.MediaType
import com.musicdrop.tv.data.model.StorageStats
import com.musicdrop.tv.ui.theme.AudioPurple
import com.musicdrop.tv.ui.theme.DarkBg
import com.musicdrop.tv.ui.theme.DocumentEmerald
import com.musicdrop.tv.ui.theme.ElectricLime
import com.musicdrop.tv.ui.theme.ElectricLimeSubtle
import com.musicdrop.tv.ui.theme.PastelLilac
import com.musicdrop.tv.ui.theme.PhotoCyan
import com.musicdrop.tv.ui.theme.SurfaceBorder
import com.musicdrop.tv.ui.theme.SurfaceDark
import com.musicdrop.tv.ui.theme.SurfaceElevated
import com.musicdrop.tv.ui.theme.TextMuted
import com.musicdrop.tv.ui.theme.TextPrimary
import com.musicdrop.tv.ui.theme.TextSecondary
import com.musicdrop.tv.ui.theme.VibrantCoral
import com.musicdrop.tv.ui.theme.VideoAmber

@Composable
fun CircularStorageCard(
    stats: StorageStats,
    onCategoryClick: (MediaType) -> Unit,
    modifier: Modifier = Modifier
) {
    val usedPercent = stats.usedPercent.coerceIn(0.01f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = usedPercent,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "storageProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(SurfaceDark, SurfaceElevated)
                )
            )
            .border(1.dp, SurfaceBorder, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Device Storage",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${stats.formatBytes(stats.usedBytes)} of ${stats.formatBytes(stats.totalBytes)}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, ElectricLime.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${stats.formatBytes(stats.availableBytes)} Free",
                        color = ElectricLime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Center Circular Radial Gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radial Arc Gauge
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = SurfaceBorder
                    val glowGradientColors = listOf(
                        Color(0xFF3B82F6),
                        Color(0xFF8B5CF6),
                        Color(0xFF6366F1),
                        Color(0xFF3B82F6)
                    )
                    Canvas(modifier = Modifier.size(130.dp)) {
                        val strokeWidth = 14.dp.toPx()

                        // Background track circle
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Glowing progress arc
                        drawArc(
                            brush = Brush.sweepGradient(glowGradientColors),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(usedPercent * 100).toInt()}%",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "USED",
                            color = Color(0xFF3B82F6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Storage Breakdown Labels (Matching screenshot: Blue = Used, Purple = Available, Red = Capacity)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StorageMetricRow(
                        color = Color(0xFF3B82F6),
                        label = "Used Space",
                        value = stats.formatBytes(stats.usedBytes)
                    )
                    StorageMetricRow(
                        color = Color(0xFF8B5CF6),
                        label = "Available",
                        value = stats.formatBytes(stats.availableBytes)
                    )
                    StorageMetricRow(
                        color = Color(0xFFEF4444),
                        label = "Capacity",
                        value = stats.formatBytes(stats.totalBytes)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4-Category Storage Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StorageCategoryPill(
                    label = "Photos",
                    size = stats.formatBytes(stats.photosBytes),
                    count = "${stats.photosCount}",
                    color = PhotoCyan,
                    icon = Icons.Rounded.Image,
                    onClick = { onCategoryClick(MediaType.IMAGE) }
                )
                StorageCategoryPill(
                    label = "Videos",
                    size = stats.formatBytes(stats.videosBytes),
                    count = "${stats.videosCount}",
                    color = VideoAmber,
                    icon = Icons.Rounded.Videocam,
                    onClick = { onCategoryClick(MediaType.VIDEO) }
                )
                StorageCategoryPill(
                    label = "Music",
                    size = stats.formatBytes(stats.audioBytes),
                    count = "${stats.audioCount}",
                    color = AudioPurple,
                    icon = Icons.Rounded.Audiotrack,
                    onClick = { onCategoryClick(MediaType.AUDIO) }
                )
                StorageCategoryPill(
                    label = "Files",
                    size = stats.formatBytes(stats.docsBytes),
                    count = "${stats.docsCount}",
                    color = DocumentEmerald,
                    icon = Icons.Rounded.Description,
                    onClick = { onCategoryClick(MediaType.DOCUMENT) }
                )
            }
        }
    }
}

@Composable
private fun StorageMetricRow(
    color: Color,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StorageCategoryPill(
    label: String,
    size: String,
    count: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = size,
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}
