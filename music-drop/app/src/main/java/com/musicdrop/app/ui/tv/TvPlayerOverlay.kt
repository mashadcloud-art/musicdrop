package com.musicdrop.app.ui.tv

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicdrop.app.ui.viewmodel.MainViewModel

/**
 * Full-screen TV music player overlay.
 *
 * D-pad key mapping:
 *  ← / MediaPrev  → Previous track
 *  → / MediaNext  → Next track
 *  OK / Enter     → Play/Pause
 *  ↑              → Skip forward 10 s
 *  ↓              → Skip back 10 s
 *  Back           → Close (return to home)
 */
@Composable
fun TvPlayerOverlay(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val connection = viewModel.playbackConnection
    val currentTrack by connection.currentTrack.collectAsState()
    val isPlaying by connection.isPlaying.collectAsState()
    val positionMs by connection.currentPositionMs.collectAsState()
    val durationMs by connection.durationMs.collectAsState()

    // Dismiss if nothing is playing
    LaunchedEffect(currentTrack) {
        if (currentTrack == null) onClose()
    }

    // Spinning vinyl animation
    val rotation by rememberInfiniteTransition(label = "vinyl_spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft, Key.MediaPrevious -> { connection.skipPrevious(); true }
                    Key.DirectionRight, Key.MediaNext    -> { connection.skipNext(); true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { connection.togglePlayPause(); true }
                    Key.DirectionUp   -> { connection.seekTo((positionMs + 10_000L).coerceAtMost(durationMs)); true }
                    Key.DirectionDown -> { connection.seekTo((positionMs - 10_000L).coerceAtLeast(0L)); true }
                    Key.Back, Key.Escape -> { onClose(); true }
                    else -> false
                }
            }
    ) {
        // Background: blurred album art
        AsyncImage(
            model = currentTrack?.albumArtUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(40.dp)
        )
        // Dark scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
        )

        // Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(48.dp)
        ) {
            // Spinning album art disc
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(360.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .then(if (isPlaying) Modifier.rotate(rotation) else Modifier)
            ) {
                AsyncImage(
                    model = currentTrack?.albumArtUri,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
                // Centre hole
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF111111))
                )
            }

            Spacer(Modifier.height(36.dp))

            // Track info
            Text(
                text = currentTrack?.name ?: "No track",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = currentTrack?.artist ?: "",
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(32.dp))

            // Seek bar
            if (durationMs > 0) {
                Slider(
                    value = positionMs.toFloat(),
                    onValueChange = { connection.seekTo(it.toLong()) },
                    valueRange = 0f..durationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth(0.72f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(0.72f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatDuration(positionMs), color = Color.White.copy(0.55f), fontSize = 13.sp)
                    Text(formatDuration(durationMs), color = Color.White.copy(0.55f), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(28.dp))

            // Controls row
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvFocusButton(
                    onClick = { connection.skipPrevious() },
                    cornerRadius = 50.dp,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape), Alignment.Center) {
                        Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                TvFocusButton(
                    onClick = { connection.togglePlayPause() },
                    cornerRadius = 50.dp,
                    focusColor = Color(0xFF7C3AED),
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                TvFocusButton(
                    onClick = { connection.skipNext() },
                    cornerRadius = 50.dp,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape), Alignment.Center) {
                        Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }
            }
        }

        // Back / close hint
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("← Back", color = Color.White.copy(0.45f), fontSize = 13.sp)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
