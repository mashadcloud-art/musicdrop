package com.musicdrop.tv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicdrop.tv.data.model.MediaItem
import kotlin.math.roundToInt

@Composable
fun MiniPlayerBar(
    track: MediaItem?,
    isPlaying: Boolean,
    progressMs: Long = 0L,
    durationMs: Long = 0L,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (track == null) return

    val appColors = com.musicdrop.tv.ui.theme.LocalAppColors.current
    val density = LocalDensity.current

    val normalProgress = if (durationMs > 0) (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var barWidthPx by remember { mutableFloatStateOf(1f) }

    val effectiveProgress = if (isDragging) dragProgress else normalProgress

    val shape = RoundedCornerShape(14.dp)
    val cardOpacity = com.musicdrop.tv.ui.theme.LocalCardOpacity.current
    val barBg = if (appColors.isGlassmorphism) {
        Color(0xFF1E1B4B).copy(alpha = (cardOpacity * 0.75f).coerceIn(0.35f, 0.85f))
    } else {
        appColors.surfaceElevated
    }
    val barBorder = if (appColors.isGlassmorphism) {
        Color.White.copy(alpha = 0.24f)
    } else {
        appColors.surfaceBorder.copy(alpha = 0.4f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .shadow(elevation = 10.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(shape)
            .background(barBg)
            .border(1.dp, barBorder, shape)
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -15f) {
                        onClick()
                    }
                }
            }
    ) {
        Column {
            // Premium Interactive Seeking & Progress Scrubber Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .pointerInput(durationMs) {
                        detectTapGestures { offset ->
                            if (durationMs > 0 && size.width > 0) {
                                val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                onSeek((fraction * durationMs).toLong())
                            }
                        }
                    }
                    .pointerInput(durationMs) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                barWidthPx = size.width.toFloat().coerceAtLeast(1f)
                                dragProgress = (offset.x / barWidthPx).coerceIn(0f, 1f)
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val frac = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                                dragProgress = frac
                            },
                            onDragEnd = {
                                if (durationMs > 0) {
                                    onSeek((dragProgress * durationMs).toLong())
                                }
                                isDragging = false
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    barWidthPx = size.width.coerceAtLeast(1f)
                    val centerY = size.height / 2f
                    val trackHeight = if (isDragging) 5.dp.toPx() else 3.5.dp.toPx()
                    val thumbRadius = if (isDragging) 6.dp.toPx() else 4.5.dp.toPx()

                    val trackBgColor = if (appColors.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f)
                    val accentCol = appColors.accentPrimary

                    // Inactive Background Track
                    drawLine(
                        color = trackBgColor,
                        start = Offset(0f, centerY),
                        end = Offset(size.width, centerY),
                        strokeWidth = trackHeight,
                        cap = StrokeCap.Round
                    )

                    // Active Glowing Progress Track
                    val activeEnd = (size.width * effectiveProgress).coerceIn(0f, size.width)
                    if (activeEnd > 0f) {
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(accentCol.copy(alpha = 0.85f), accentCol),
                                startX = 0f,
                                endX = activeEnd.coerceAtLeast(1f)
                            ),
                            start = Offset(0f, centerY),
                            end = Offset(activeEnd, centerY),
                            strokeWidth = trackHeight,
                            cap = StrokeCap.Round
                        )
                    }

                    // Scrubber Thumb Pip
                    val thumbCenter = Offset(activeEnd, centerY)
                    // Drop shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = thumbRadius + 1.5.dp.toPx(),
                        center = thumbCenter
                    )
                    // White core
                    drawCircle(
                        color = Color.White,
                        radius = thumbRadius,
                        center = thumbCenter
                    )
                    // Accent border ring
                    drawCircle(
                        color = accentCol,
                        radius = thumbRadius,
                        center = thumbCenter,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Floating Seek Time Tooltip while Dragging (e.g. "02:15")
                if (isDragging && durationMs > 0) {
                    val seekSecs = ((dragProgress * durationMs) / 1000).toLong().coerceAtLeast(0L)
                    val formattedTime = String.format(java.util.Locale.US, "%d:%02d", seekSecs / 60, seekSecs % 60)
                    val tooltipOffsetX = with(density) {
                        val pxPos = (dragProgress * barWidthPx) - 22.dp.toPx()
                        pxPos.coerceIn(4.dp.toPx(), (barWidthPx - 48.dp.toPx()).coerceAtLeast(4.dp.toPx())).roundToInt()
                    }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(tooltipOffsetX, -22.dp.roundToPx()) }
                            .shadow(6.dp, RoundedCornerShape(6.dp))
                            .background(
                                color = if (appColors.isDark) Color(0xFF1E1E24) else Color(0xFF2D2D34),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(1.dp, appColors.accentPrimary.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Square Album Artwork Thumbnail (YouTube Music Style)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (appColors.isDark) Color(0xFF252528) else Color(0xFFE2E4E8)),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.albumArtUri != null) {
                        AsyncImage(
                            model = track.albumArtUri,
                            contentDescription = track.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = appColors.accentPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track Title & Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.name,
                        color = appColors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist.ifBlank { "YouTube Music" },
                        color = appColors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Play / Pause Button with Frosted Touch
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (appColors.isDark) {
                                Brush.verticalGradient(listOf(Color(0xFF383842), Color(0xFF222228)))
                            } else {
                                Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEDEDF2)))
                            }
                        )
                        .border(
                            1.dp,
                            if (appColors.isDark) Color(0x33FFFFFF) else Color(0x22000000),
                            CircleShape
                        )
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = appColors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Skip Next Button
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = appColors.textPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
