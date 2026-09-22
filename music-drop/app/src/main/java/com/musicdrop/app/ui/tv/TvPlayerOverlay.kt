package com.musicdrop.app.ui.tv

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.musicdrop.app.data.model.MediaType
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import com.musicdrop.app.ui.components.YouTubeIFramePlayer
import com.musicdrop.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

/**
 * Full-screen TV Player Overlay with:
 *  - 🎬 HD Video Playback (YouTube Video / Direct ExoPlayer)
 *  - 💿 Spinning Vinyl Audio Mode
 *  - 📥 Direct Download button
 *  - 🎛️ Full Remote D-pad control:
 *      ← / →: Prev / Next
 *      OK: Play / Pause
 *      ↑ / ↓: Seek +10s / -10s
 *      Back: Minimize player
 *  - Auto-hiding controls overlay for immersive video viewing
 */
@Composable
fun TvPlayerOverlay(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val connection = viewModel.playbackConnection
    val currentTrack by connection.currentTrack.collectAsState()
    val isPlaying by connection.isPlaying.collectAsState()
    val positionMs by connection.currentPositionMs.collectAsState()
    val durationMs by connection.durationMs.collectAsState()
    val ytCurrentVideo by viewModel.ytCurrentVideo.collectAsState()

    // Mode: Video (true) vs Audio Vinyl (false)
    var isVideoMode by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var controlsTimeoutKey by remember { mutableIntStateOf(0) }
    var isDownloading by remember { mutableStateOf(false) }

    // Auto-hide controls in video mode after 5 seconds of inactivity
    LaunchedEffect(showControls, isPlaying, isVideoMode, controlsTimeoutKey) {
        if (showControls && isPlaying && isVideoMode) {
            delay(5000L)
            showControls = false
        }
    }

    // Dismiss if nothing is playing
    LaunchedEffect(currentTrack) {
        if (currentTrack == null) onClose()
    }

    // Resolve video ID from current track
    val effectiveVideoId = remember(currentTrack, ytCurrentVideo) {
        val track = currentTrack ?: return@remember null
        val path = track.filePath.orEmpty()
        val art = track.albumArtUri?.toString().orEmpty()
        val uriStr = track.uri.toString()
        when {
            path.startsWith("yt:") -> path.removePrefix("yt:")
            path.length == 11 && !path.contains("/") && !path.contains(".") && !path.contains(":") && !path.contains(" ") -> path
            art.contains("/vi_webp/") -> art.substringAfter("/vi_webp/").substringBefore("/").substringBefore("?")
            art.contains("/vi/") -> art.substringAfter("/vi/").substringBefore("/").substringBefore("?")
            uriStr.contains("v=") -> uriStr.substringAfter("v=").substringBefore("&").substringBefore("?")
            uriStr.contains("youtu.be/") -> uriStr.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            ytCurrentVideo?.videoId?.isNotBlank() == true -> ytCurrentVideo?.videoId
            else -> null
        }
    }

    val isDirectExoVideo = currentTrack?.mediaType == MediaType.VIDEO &&
        (currentTrack?.filePath?.endsWith(".mp4", ignoreCase = true) == true ||
         currentTrack?.filePath?.startsWith("/") == true ||
         currentTrack?.filePath?.startsWith("content://") == true ||
         currentTrack?.uri?.scheme == "content" ||
         currentTrack?.uri?.scheme == "file")

    // Vinyl spin animation
    val rotation by rememberInfiniteTransition(label = "vinyl_spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_rotation"
    )

    fun userInteracted() {
        showControls = true
        controlsTimeoutKey++
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                userInteracted()
                when (event.key) {
                    Key.DirectionLeft, Key.MediaPrevious -> { connection.skipPrevious(); true }
                    Key.DirectionRight, Key.MediaNext    -> { connection.skipNext(); true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (!showControls) {
                            showControls = true
                            true
                        } else {
                            connection.togglePlayPause()
                            true
                        }
                    }
                    Key.DirectionUp   -> { connection.seekTo((positionMs + 10_000L).coerceAtMost(durationMs)); true }
                    Key.DirectionDown -> { connection.seekTo((positionMs - 10_000L).coerceAtLeast(0L)); true }
                    Key.Back, Key.Escape -> { onClose(); true }
                    else -> false
                }
            }
            .clickable { userInteracted() }
    ) {
        // ── 1. BACKGROUND / VIDEO SURFACE ──────────────────────────────
        if (isVideoMode && (isDirectExoVideo || effectiveVideoId != null)) {
            if (isDirectExoVideo) {
                AndroidView(
                    factory = { ctx ->
                        try {
                            androidx.media3.ui.PlayerView(ctx).apply {
                                useController = false
                                useArtwork = false
                                defaultArtwork = null
                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                connection.bindPlayerView(this)
                            }
                        } catch (_: Throwable) {
                            android.view.View(ctx)
                        }
                    },
                    update = { view ->
                        try {
                            if (view is androidx.media3.ui.PlayerView) {
                                view.useArtwork = false
                                connection.bindPlayerView(view)
                            }
                        } catch (_: Throwable) {}
                    },
                    onRelease = { view ->
                        try {
                            if (view is androidx.media3.ui.PlayerView) {
                                connection.unbindPlayerView(view)
                            }
                        } catch (_: Throwable) {}
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (effectiveVideoId != null) {
                LaunchedEffect(effectiveVideoId) {
                    viewModel.switchCurrentTrackToVideo(effectiveVideoId)
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = currentTrack?.albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(32.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f))
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF0033),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Loading Video Stream...",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // Vinyl Audio Mode: Blurred artwork background
            AsyncImage(
                model = currentTrack?.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(40.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
            )

            // Center Spinning Vinyl
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .then(if (isPlaying) Modifier.rotate(rotation) else Modifier)
                ) {
                    AsyncImage(
                        model = currentTrack?.albumArtUri,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                    // Centre hole
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF111111))
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = currentTrack?.name ?: "No track",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = currentTrack?.artist ?: "",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── 2. CONTROLS & HUD OVERLAY ──────────────────────────────────
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(350)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(0.7f),
                                Color.Transparent,
                                Color.Black.copy(0.85f)
                            )
                        )
                    )
            ) {
                // Top Header: Title, Artist & Close
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack?.name ?: "",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentTrack?.artist ?: "",
                            fontSize = 14.sp,
                            color = Color.White.copy(0.7f),
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Toggle Video / Vinyl Mode
                        TvFocusButton(
                            onClick = {
                                userInteracted()
                                isVideoMode = !isVideoMode
                            },
                            cornerRadius = 12.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        if (isVideoMode) Color(0xFF7C3AED) else Color.White.copy(0.18f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isVideoMode) Icons.Filled.Videocam else Icons.Filled.Album,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isVideoMode) "🎬 Video ON" else "💿 Vinyl Audio",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Close / Back button
                        TvFocusButton(
                            onClick = onClose,
                            cornerRadius = 12.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(0.15f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("← Back", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Bottom Controls: Seekbar, Playback buttons, Download
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 48.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Seek bar
                    if (durationMs > 0) {
                        Slider(
                            value = positionMs.toFloat(),
                            onValueChange = {
                                userInteracted()
                                connection.seekTo(it.toLong())
                            },
                            valueRange = 0f..durationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF7C3AED),
                                activeTrackColor = Color(0xFF7C3AED),
                                inactiveTrackColor = Color.White.copy(0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(0.85f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatDuration(positionMs), color = Color.White.copy(0.7f), fontSize = 13.sp)
                            Text(formatDuration(durationMs), color = Color.White.copy(0.7f), fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Buttons Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10s
                        TvFocusButton(
                            onClick = {
                                userInteracted()
                                connection.seekTo((positionMs - 10_000L).coerceAtLeast(0L))
                            },
                            cornerRadius = 50.dp,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape), Alignment.Center) {
                                Icon(Icons.Filled.Replay10, "-10s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }

                        // Previous
                        TvFocusButton(
                            onClick = {
                                userInteracted()
                                connection.skipPrevious()
                            },
                            cornerRadius = 50.dp,
                            modifier = Modifier.size(62.dp)
                        ) {
                            Box(Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape), Alignment.Center) {
                                Icon(Icons.Filled.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }

                        // Play/Pause Hero Button
                        TvFocusButton(
                            onClick = {
                                userInteracted()
                                connection.togglePlayPause()
                            },
                            cornerRadius = 50.dp,
                            focusColor = Color(0xFF7C3AED),
                            modifier = Modifier.size(80.dp)
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
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        // Next
                        TvFocusButton(
                            onClick = {
                                userInteracted()
                                connection.skipNext()
                            },
                            cornerRadius = 50.dp,
                            modifier = Modifier.size(62.dp)
                        ) {
                            Box(Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape), Alignment.Center) {
                                Icon(Icons.Filled.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }

                        // Forward 10s
                        TvFocusButton(
                            onClick = {
                                userInteracted()
                                connection.seekTo((positionMs + 10_000L).coerceAtMost(durationMs))
                            },
                            cornerRadius = 50.dp,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape), Alignment.Center) {
                                Icon(Icons.Filled.Forward10, "+10s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        // Download Button
                        TvFocusButton(
                            onClick = {
                                userInteracted()
                                val track = currentTrack ?: return@TvFocusButton
                                val searchResult = YouTubeSearchResult(
                                    videoId = effectiveVideoId ?: "",
                                    title = track.name,
                                    channelTitle = track.artist ?: "",
                                    thumbnailUrl = track.albumArtUri?.toString() ?: "",
                                    duration = formatDuration(durationMs)
                                )
                                isDownloading = true
                                Toast.makeText(context, "Downloading: ${track.name}...", Toast.LENGTH_SHORT).show()
                                viewModel.downloadYouTubeAudio(searchResult) { success, path ->
                                    isDownloading = false
                                    val msg = if (success) "Saved to Downloads/MusicDrop" else "Download error"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            cornerRadius = 50.dp,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isDownloading) Color(0xFF059669) else Color.White.copy(0.12f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDownloading) Icons.Filled.DownloadDone else Icons.Filled.Download,
                                    contentDescription = "Download",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
