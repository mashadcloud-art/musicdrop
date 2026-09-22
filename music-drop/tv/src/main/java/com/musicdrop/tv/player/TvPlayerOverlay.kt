package com.musicdrop.tv.player

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.musicdrop.tv.data.TvVideoItem
import com.musicdrop.tv.data.TvYtExtractor
import com.musicdrop.tv.download.TvDownloadManager
import com.musicdrop.tv.ui.components.TvFocusButton
import com.musicdrop.tv.ui.components.TvVideoCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TvPlayerOverlay(
    currentVideo: TvVideoItem?,
    upNextVideos: List<TvVideoItem>,
    onSelectVideo: (TvVideoItem) -> Unit,
    onClose: () -> Unit
) {
    if (currentVideo == null) return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val downloadManager = remember { TvDownloadManager.getInstance(context) }

    // ExoPlayer state
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var currentResolution by remember { mutableStateOf("1080p") }
    var activeStreamUrl by remember { mutableStateOf<String?>(null) }
    var extractionFailed by remember { mutableStateOf(false) }

    // HUD controls visibility
    var showControls by remember { mutableStateOf(true) }
    var controlsTimeoutKey by remember { mutableIntStateOf(0) }
    var isDownloading by remember { mutableStateOf(false) }

    // Build ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == Player.STATE_BUFFERING)
                if (state == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                } else if (state == Player.STATE_ENDED) {
                    // Auto-play next video in queue
                    val next = upNextVideos.firstOrNull()
                    if (next != null) onSelectVideo(next)
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Progress polling loop
    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(500L)
        }
    }

    // Auto-hide controls after 6s of inactivity
    LaunchedEffect(showControls, isPlaying, controlsTimeoutKey) {
        if (showControls && isPlaying) {
            delay(6000L)
            showControls = false
        }
    }

    fun userInteracted() {
        showControls = true
        controlsTimeoutKey++
    }

    // Load & extract video stream
    LaunchedEffect(currentVideo.id) {
        isBuffering = true
        extractionFailed = false
        val media = TvYtExtractor.getInstance().extractMedia(currentVideo.id)
        if (media != null && media.videoUrl != null) {
            activeStreamUrl = media.videoUrl
            currentResolution = media.resolution
            exoPlayer.setMediaItem(MediaItem.fromUri(media.videoUrl))
            exoPlayer.prepare()
            exoPlayer.play()
        } else {
            isBuffering = false
            extractionFailed = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                userInteracted()
                when (event.key) {
                    Key.DirectionLeft, Key.MediaPrevious -> {
                        if (!showControls) {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                        }
                        true
                    }
                    Key.DirectionRight, Key.MediaNext -> {
                        if (!showControls) {
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(exoPlayer.duration))
                        }
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (!showControls) {
                            showControls = true
                            true
                        } else false
                    }
                    Key.DirectionUp, Key.DirectionDown -> {
                        showControls = true
                        true
                    }
                    Key.MediaPlayPause -> {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        true
                    }
                    Key.MediaPlay -> {
                        exoPlayer.play()
                        true
                    }
                    Key.MediaPause, Key.MediaStop -> {
                        exoPlayer.pause()
                        true
                    }
                    Key.MediaFastForward -> {
                        exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(exoPlayer.duration))
                        true
                    }
                    Key.MediaRewind -> {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                        true
                    }
                    Key.Back, Key.Escape -> {
                        if (showControls) {
                            showControls = false
                            true
                        } else {
                            onClose()
                            true
                        }
                    }
                    else -> false
                }
            }
            .clickable { userInteracted() }
    ) {
        // ── 1. Fullscreen Native 4K/HD Video Surface ────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    defaultArtwork = null
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading or Buffering Spinner
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFF0033),
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        // Extraction failure fallback
        if (extractionFailed) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️ Video Stream Unavailable", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    TvFocusButton(
                        onClick = {
                            coroutineScope.launch {
                                isBuffering = true
                                extractionFailed = false
                                val media = TvYtExtractor.getInstance().extractMedia(currentVideo.id)
                                if (media?.videoUrl != null) {
                                    activeStreamUrl = media.videoUrl
                                    currentResolution = media.resolution
                                    exoPlayer.setMediaItem(MediaItem.fromUri(media.videoUrl))
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                } else {
                                    isBuffering = false
                                    extractionFailed = true
                                }
                            }
                        },
                        cornerRadius = 24.dp
                    ) {
                        Text("Retry Playback", color = Color.White, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                    }
                }
            }
        }

        // ── 2. Exact YouTube on Android TV HUD Controls Overlay ────────────
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            ) {
                // Top Header: Video Info & Close
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentVideo.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentVideo.channelTitle,
                                color = Color.White.copy(0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (currentVideo.views.isNotBlank()) {
                                Text(
                                    text = " • ${currentVideo.views}",
                                    color = Color.White.copy(0.5f),
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            // Resolution badge (4K, 1080p, etc.)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFF0033))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(currentResolution, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    TvFocusButton(
                        onClick = onClose,
                        cornerRadius = 50.dp,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.White.copy(0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Close, "Close", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                // Bottom Control Section: Seekbar + Main Controls + Up Next
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    // Seekbar with elapsed and total duration
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(positionMs), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(formatTime(durationMs), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(4.dp))
                        val progressFraction = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(0.25f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .fillMaxHeight()
                                    .background(Color(0xFFFF0033))
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Playback Controls Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Rewind & Fast Forward shortcuts
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            TvFocusButton(
                                onClick = {
                                    userInteracted()
                                    exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                                },
                                cornerRadius = 50.dp,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Replay10, "-10s", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                            TvFocusButton(
                                onClick = {
                                    userInteracted()
                                    exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(exoPlayer.duration))
                                },
                                cornerRadius = 50.dp,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Forward10, "+10s", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        // Center: Prev, BIG SOLID WHITE PLAY/PAUSE, Next (Exact YouTube TV style)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            TvFocusButton(
                                onClick = {
                                    userInteracted()
                                    exoPlayer.seekTo(0L)
                                },
                                cornerRadius = 50.dp,
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }

                            // Big Solid White Circular Play/Pause Button
                            TvFocusButton(
                                onClick = {
                                    userInteracted()
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                                cornerRadius = 50.dp,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            TvFocusButton(
                                onClick = {
                                    userInteracted()
                                    val next = upNextVideos.firstOrNull()
                                    if (next != null) onSelectVideo(next)
                                },
                                cornerRadius = 50.dp,
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        // Right: Download Video / Audio button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            TvFocusButton(
                                onClick = {
                                    userInteracted()
                                    if (isDownloading) {
                                        Toast.makeText(context, "Download in progress...", Toast.LENGTH_SHORT).show()
                                        return@TvFocusButton
                                    }
                                    isDownloading = true
                                    Toast.makeText(context, "Downloading \"${currentVideo.title}\"...", Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch {
                                        downloadManager.downloadMedia(
                                            video = currentVideo,
                                            downloadAsVideo = true,
                                            onComplete = { ok, path ->
                                                isDownloading = false
                                                val msg = if (ok) "✓ Downloaded Video: ${currentVideo.title}" else "Download failed"
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                },
                                cornerRadius = 24.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(Color.White.copy(0.12f), RoundedCornerShape(24.dp))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDownloading) Icons.Filled.HourglassTop else Icons.Filled.Download,
                                        contentDescription = "Download",
                                        tint = if (isDownloading) Color(0xFFFFD600) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (isDownloading) "Downloading..." else "Download 4K/HD",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Up Next Video Shelf
                    if (upNextVideos.isNotEmpty()) {
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = "Up Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 40.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(upNextVideos, key = { it.id }) { video ->
                                TvVideoCard(
                                    video = video,
                                    onClick = {
                                        userInteracted()
                                        onSelectVideo(video)
                                    },
                                    modifier = Modifier.width(260.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    val h = m / 60
    return if (h > 0) {
        val remM = m % 60
        "%d:%02d:%02d".format(h, remM, s)
    } else {
        "%02d:%02d".format(m, s)
    }
}
