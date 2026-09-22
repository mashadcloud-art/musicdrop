package com.musicdrop.app.ui.tv

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.musicdrop.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

/**
 * YouTube on TV Styled In-Player Overlay:
 *  - 🎬 HD Full-Screen Native ExoPlayer Video
 *  - 💿 Vinyl Audio Mode fallback toggle
 *  - 🎛️ Exact YouTube on TV HUD Layout (Reference Photos 3 & 4):
 *      - Top-Left: Large video title & channel / views
 *      - Middle: Full-width progress seekbar with elapsed & total duration
 *      - Controls Bar:
 *          - Channel Avatar & Subscribe pill on left
 *          - Previous, Big Solid White Play/Pause Button, Next in center
 *          - Like, Download, Mode toggle on right
 *      - Bottom: Horizontal "Up Next" video carousel with 16:9 cards
 *  - 🎮 Full Remote Control:
 *      - D-pad Center / OK: Play/Pause or select Up Next card
 *      - D-pad Left / Right: Prev / Next / Seek
 *      - D-pad Down: Navigate to Up Next cards
 *      - Back: Dismiss HUD or minimize
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
    val upNextQueue by viewModel.upNextQueue.collectAsState()
    val trendingQuick by viewModel.indiaQuickPicks.collectAsState()

    // Mode: Video (true) vs Audio Vinyl (false)
    var isVideoMode by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var controlsTimeoutKey by remember { mutableIntStateOf(0) }
    var isDownloading by remember { mutableStateOf(false) }

    // Auto-hide controls in video mode after 6 seconds of inactivity
    LaunchedEffect(showControls, isPlaying, isVideoMode, controlsTimeoutKey) {
        if (showControls && isPlaying && isVideoMode) {
            delay(6000L)
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

    // A track is a direct video if its mediaType is VIDEO (local file, content uri, or extracted https MP4)
    val isDirectExoVideo = currentTrack?.mediaType == MediaType.VIDEO

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

    val upNextList = remember(upNextQueue, trendingQuick) {
        if (upNextQueue.isNotEmpty()) upNextQueue else trendingQuick
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
                            connection.seekTo((positionMs - 10_000L).coerceAtLeast(0L))
                        }
                        true
                    }
                    Key.DirectionRight, Key.MediaNext -> {
                        if (!showControls) {
                            connection.seekTo((positionMs + 10_000L).coerceAtMost(durationMs))
                        }
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (!showControls) {
                            showControls = true
                            true
                        } else {
                            false // let focused button handle it
                        }
                    }
                    Key.DirectionUp -> {
                        showControls = true
                        true
                    }
                    Key.DirectionDown -> {
                        showControls = true
                        true
                    }
                    Key.MediaPlayPause -> {
                        connection.togglePlayPause()
                        true
                    }
                    Key.MediaPlay -> {
                        connection.play()
                        true
                    }
                    Key.MediaPause, Key.MediaStop -> {
                        connection.pause()
                        true
                    }
                    Key.MediaFastForward -> {
                        connection.seekTo((positionMs + 10_000L).coerceAtMost(durationMs))
                        true
                    }
                    Key.MediaRewind -> {
                        connection.seekTo((positionMs - 10_000L).coerceAtLeast(0L))
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
        // ── 1. BACKGROUND VIDEO SURFACE (100% Fullscreen) ──────────────
        if (isVideoMode && (isDirectExoVideo || effectiveVideoId != null)) {
            if (isDirectExoVideo) {
                AndroidView(
                    factory = { ctx ->
                        try {
                            androidx.media3.ui.PlayerView(ctx).apply {
                                useController = false
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
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Loading HD Video...",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            // Vinyl Audio Mode: Blurred artwork background with spinning vinyl
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
                        .size(280.dp)
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
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF111111))
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = currentTrack?.name ?: "No track",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = currentTrack?.artist ?: "",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── 2. YOUTUBE ON TV HUD OVERLAY (Matches Photo 3 & 4) ─────────
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(0.85f),
                                Color.Black.copy(0.2f),
                                Color.Black.copy(0.92f)
                            )
                        )
                    )
            ) {
                // ── Top Header: Video Title & Channel (Photo 3) ─────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack?.name ?: "",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 28.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = currentTrack?.artist ?: "Unknown Artist",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(0.75f)
                            )
                            Text("•", color = Color.White.copy(0.5f))
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isVideoMode) "HD VIDEO" else "AUDIO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVideoMode) Color(0xFFFF0033) else Color(0xFF60A5FA)
                                )
                            }
                        }
                    }

                    // Mode Toggle & Close buttons at top right
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        TvFocusButton(
                            onClick = {
                                userInteracted()
                                isVideoMode = !isVideoMode
                            },
                            cornerRadius = 20.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        if (isVideoMode) Color.White.copy(0.2f) else Color(0xFF2563EB).copy(0.4f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isVideoMode) Icons.Filled.Videocam else Icons.Filled.Album,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isVideoMode) "Switch to Vinyl" else "Switch to Video",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        TvFocusButton(
                            onClick = onClose,
                            cornerRadius = 20.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.White.copy(0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Close, "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // ── Bottom Area: Seekbar + Playback Controls + Up Next (Photo 3 & 4) ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    // 1. Sleek Seekbar with Elapsed & Duration
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(positionMs),
                                color = Color.White.copy(0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formatDuration(durationMs),
                                color = Color.White.copy(0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        // Progress bar line
                        val progressFraction = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(0.25f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .fillMaxHeight()
                                    .background(Color.White)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 2. Main Playback Controls Bar (Exact Photo 3 layout)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Channel info / Subscribe pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF333333)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = currentTrack?.albumArtUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            }
                            Column {
                                Text(
                                    text = currentTrack?.artist ?: "",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "MusicDrop Player",
                                    color = Color.White.copy(0.55f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Center: Prev, BIG SOLID WHITE PLAY/PAUSE, Next (Photo 3!)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Previous
                            TvFocusButton(
                                onClick = {
                                    userInteracted()
                                    connection.skipPrevious()
                                },
                                cornerRadius = 50.dp,
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }

                            // Big Solid White Circular Play/Pause button with pure black icon
                            TvFocusButton(
                                onClick = {
                                    userInteracted()
                                    connection.togglePlayPause()
                                },
                                cornerRadius = 50.dp,
                                focusColor = Color.White,
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

                            // Next
                            TvFocusButton(
                                onClick = {
                                    userInteracted()
                                    connection.skipNext()
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

                        // Right: Like, Download & Queue
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Download button
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
                                    val onDone: (Boolean, String) -> Unit = { ok, _ ->
                                        isDownloading = false
                                        val msg = if (ok) "✓ Downloaded: ${track.name}" else "Download failed"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                    if (isVideoMode) {
                                        viewModel.downloadYouTubeVideo(searchResult, onDone)
                                    } else {
                                        viewModel.downloadYouTubeAudio(searchResult, onDone)
                                    }
                                    Toast.makeText(context, "Downloading \"${track.name}\"...", Toast.LENGTH_SHORT).show()
                                },
                                cornerRadius = 50.dp,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isDownloading) Icons.Filled.HourglassTop else Icons.Filled.Download,
                                        contentDescription = "Download",
                                        tint = if (isDownloading) Color(0xFFFFD600) else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Rewind 10s
                            TvFocusButton(
                                onClick = {
                                    userInteracted()
                                    connection.seekTo((positionMs - 10_000L).coerceAtLeast(0L))
                                },
                                cornerRadius = 50.dp,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Replay10, "-10s", tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }

                            // Forward 10s
                            TvFocusButton(
                                onClick = {
                                    userInteracted()
                                    connection.seekTo((positionMs + 10_000L).coerceAtMost(durationMs))
                                },
                                cornerRadius = 50.dp,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.White.copy(0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Forward10, "+10s", tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // ── 3. Up Next Recommendations Carousel (Photo 3 & Photo 4) ──────
                    if (upNextList.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp)
                        ) {
                            Text(
                                text = "Up Next",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(0.9f),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(end = 40.dp)
                            ) {
                                items(upNextList.take(12)) { item ->
                                    TvFocusButton(
                                        onClick = {
                                            userInteracted()
                                            viewModel.playYouTubeVideo(item, preferVideo = isVideoMode)
                                        },
                                        cornerRadius = 12.dp,
                                        modifier = Modifier.width(220.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .width(220.dp)
                                                .background(Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                                        ) {
                                            // 16:9 Thumbnail
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(124.dp)
                                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                            ) {
                                                AsyncImage(
                                                    model = item.thumbnailUrl,
                                                    contentDescription = item.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                if (item.duration.isNotBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(6.dp)
                                                            .background(Color.Black.copy(0.85f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(item.duration, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = item.title,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = item.channelTitle,
                                                    color = Color.White.copy(0.6f),
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return if (min >= 60) {
        val hr = min / 60
        val remMin = min % 60
        String.format("%d:%02d:%02d", hr, remMin, sec)
    } else {
        String.format("%d:%02d", min, sec)
    }
}
