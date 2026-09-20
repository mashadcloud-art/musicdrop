package com.musicdrop.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.rounded.*
import com.musicdrop.app.ui.components.YouTubeIFramePlayer
import com.musicdrop.app.ui.theme.PlayerSkinLayout
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.model.MediaType
import com.musicdrop.app.data.model.UnifiedTrack
import com.musicdrop.app.ui.components.AddToPlaylistDialog
import com.musicdrop.app.ui.components.EqualizerDialog
import com.musicdrop.app.ui.components.PlayerThemeDialog
import com.musicdrop.app.ui.theme.LocalAppColors
import com.musicdrop.app.ui.theme.PlayerThemeId
import com.musicdrop.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicPlayerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connection = viewModel.playbackConnection
    val currentTrack by connection.currentTrack.collectAsState()
    val isPlaying by connection.isPlaying.collectAsState()
    val positionMs by connection.currentPositionMs.collectAsState()
    val durationMs by connection.durationMs.collectAsState()
    val isShuffle by connection.isShuffle.collectAsState()
    val isRepeat by connection.isRepeat.collectAsState()
    val upNextQueue by viewModel.upNextQueue.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val lyricsLoading by viewModel.lyricsLoading.collectAsState()
    val playerTheme by viewModel.playerTheme.collectAsState()
    val equalizerManager = viewModel.equalizerManager
    val eqState by equalizerManager?.state?.collectAsState() ?: remember { mutableStateOf(null) }

    val isInPipMode by viewModel.isInPipMode.collectAsState()
    val ytCurrentVideo by viewModel.ytCurrentVideo.collectAsState()
    val isVideoLoading by viewModel.videoModeLoading.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val appColors = LocalAppColors.current

    val playerSkinLayout by viewModel.playerSkinLayout.collectAsState()

    // 0: Song, 1: Video, 2: Lyrics
    var activeTab by remember { mutableIntStateOf(0) }
    var sliderDragging by remember { mutableFloatStateOf(-1f) }
    var isLiked by remember { mutableStateOf(false) }

    // Dialog toggles
    var showEqualizerModal by remember { mutableStateOf(false) }
    var showThemeModal by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showQueueModal by remember { mutableStateOf(false) }
    var showSleepTimerModal by remember { mutableStateOf(false) }
    var showDownloadModal by remember { mutableStateOf(false) }
    var sleepTimerTargetMs by remember { mutableLongStateOf(0L) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var videoResizeMode by remember { mutableIntStateOf(0) } // 0: 16:9 Fit, 1: Fill (Zoom), 2: Wide (Stretch)
    var isVideoControlsCollapsed by remember { mutableStateOf(false) } // Edge-to-Edge video mode (collapses lower controls)

    // Fix mobile header hiding the mobile time: enforce white status bar icons when player is visible
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            val prevLight = insetsController.isAppearanceLightStatusBars
            insetsController.isAppearanceLightStatusBars = false
            onDispose { insetsController.isAppearanceLightStatusBars = prevLight }
        } else {
            onDispose {}
        }
    }

    val isDirectExoVideo = currentTrack?.mediaType == MediaType.VIDEO
    val isVideoMode by viewModel.isVideoMode.collectAsState()

    // Resolve video ID from current track metadata or online search
    val effectiveVideoId = remember(currentTrack, ytCurrentVideo) {
        val track = currentTrack
        val path = track?.filePath.orEmpty()
        val art = track?.albumArtUri?.toString().orEmpty()
        when {
            path.startsWith("yt:") -> path.removePrefix("yt:")
            path.length == 11 && !path.contains("/") && !path.contains(".") && !path.contains(":") -> path
            art.contains("/vi_webp/") -> art.substringAfter("/vi_webp/").substringBefore("/").substringBefore("?")
            art.contains("/vi/") -> art.substringAfter("/vi/").substringBefore("/").substringBefore("?")
            ytCurrentVideo?.videoId?.isNotBlank() == true -> ytCurrentVideo?.videoId
            else -> null
        }
    }

    // Automatically switch to video mode when Video tab is selected
    LaunchedEffect(activeTab, currentTrack?.id) {
        if (activeTab == 1) {
            if (currentTrack?.mediaType != MediaType.VIDEO && !isVideoMode) {
                viewModel.setVideoMode(true)
            }
        }
    }

    // Automatically return to audio stream when switching away from Video tab
    LaunchedEffect(activeTab) {
        if (activeTab != 1) {
            isVideoControlsCollapsed = false
            if (isVideoMode || currentTrack?.mediaType == MediaType.VIDEO) {
                viewModel.setVideoMode(false)
            }
        }
    }

    // Floating Screen / Picture-in-Picture mode display: pristine full screen video only
    if (isInPipMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isDirectExoVideo) {
                val pipResizeMode = when (videoResizeMode) {
                    1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    2 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                AndroidView(
                    factory = { ctx ->
                        try {
                            androidx.media3.ui.PlayerView(ctx).apply {
                                useController = false
                                useArtwork = false
                                defaultArtwork = null
                                resizeMode = pipResizeMode
                                connection.bindPlayerView(this)
                            }
                        } catch (_: Throwable) {
                            android.view.View(ctx)
                        }
                    },
                    update = { view ->
                        if (view is androidx.media3.ui.PlayerView) {
                            view.useArtwork = false
                            view.resizeMode = pipResizeMode
                            connection.bindPlayerView(view)
                        }
                    },
                    onRelease = { view ->
                        if (view is androidx.media3.ui.PlayerView) {
                            connection.unbindPlayerView(view)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (effectiveVideoId != null) {
                YouTubeIFramePlayer(
                    videoId = effectiveVideoId,
                    resizeMode = videoResizeMode,
                    isPlaying = isPlaying,
                    currentPositionMs = positionMs,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(color = Color(0xFFE11D48))
            }
        }
        return
    }

    // Check favorite status
    LaunchedEffect(currentTrack?.id) {
        val cur = currentTrack
        if (cur != null) {
            isLiked = viewModel.isTrackFavorite(cur)
            viewModel.fetchLyrics(cur.name, cur.artist)
        }
    }

    // Sleep Timer countdown
    LaunchedEffect(sleepTimerTargetMs) {
        if (sleepTimerTargetMs > 0L) {
            while (System.currentTimeMillis() < sleepTimerTargetMs) {
                kotlinx.coroutines.delay(1000L)
            }
            connection.pause()
            sleepTimerTargetMs = 0L
            Toast.makeText(context, "Sleep timer: Playback paused", Toast.LENGTH_SHORT).show()
        }
    }

    // Parsed synced lyrics
    val syncedLines = remember(lyrics) {
        lyrics?.parsedSyncedLines().orEmpty()
    }

    // Active lyric index
    val activeLyricIndex = remember(positionMs, syncedLines) {
        if (syncedLines.isEmpty()) -1
        else {
            val idx = syncedLines.indexOfLast { it.first <= positionMs }
            if (idx >= 0) idx else 0
        }
    }

    val lyricsListState = rememberLazyListState()
    LaunchedEffect(activeLyricIndex, activeTab) {
        if (activeTab == 2 && activeLyricIndex in syncedLines.indices) {
            try {
                lyricsListState.animateScrollToItem(maxOf(0, activeLyricIndex - 2))
            } catch (_: Exception) {}
        }
    }

    // Background dynamic ambient gradient (Pure aesthetic theme colors, no blown-up cover art)
    val dynamicGradient = remember(playerTheme) {
        playerTheme.getBackgroundBrush(fallbackAccent = Color(0xFF1E3A5F))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(dynamicGradient)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 30) {
                        onBack()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                .padding(top = 10.dp)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. TOP BAR (MATCHING SCREENSHOT 1) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Minimize Chevron & MusicDrop Pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(Modifier.width(2.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "MusicDrop",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Center: Audio / Video Capsule Switcher (🎧 | ▶)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Audio Tab (Headphones)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (activeTab == 0) Color(0xFFF97316) else Color.Transparent)
                                .clickable { activeTab = 0 }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = "Audio Mode",
                                tint = if (activeTab == 0) Color.White else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Video Tab (Play Video)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (activeTab == 1) Color(0xFFF97316) else Color.Transparent)
                                .clickable { activeTab = 1 }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Video Mode",
                                tint = if (activeTab == 1) Color.White else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Right Icons: Queue/Playlist (≡+), Cast (📺), 3-Dots (⋮)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showQueueModal = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val activity = context as? Activity
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                try {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                    activity?.enterPictureInPictureMode(params)
                                } catch (_: Throwable) {
                                    Toast.makeText(context, "Picture-in-Picture not supported on this device", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cast,
                            contentDescription = "Cast / PiP",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { showOptionsMenu = true }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            modifier = Modifier.background(Color(0xFF1E1B2E))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Download (Audio / Video)", color = Color.White) },
                                leadingIcon = { Icon(Icons.Rounded.Download, null, tint = Color(0xFFF59E0B)) },
                                onClick = {
                                    showOptionsMenu = false
                                    showDownloadModal = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Watch Official Video", color = Color.White) },
                                leadingIcon = { Icon(Icons.Rounded.PlayCircle, null, tint = Color(0xFFE11D48)) },
                                onClick = {
                                    showOptionsMenu = false
                                    activeTab = 1
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Equalizer", color = Color.White) },
                                leadingIcon = { Icon(Icons.Rounded.Tune, null, tint = Color(0xFF10B981)) },
                                onClick = {
                                    showOptionsMenu = false
                                    showEqualizerModal = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sleep Timer", color = Color.White) },
                                leadingIcon = { Icon(Icons.Rounded.Schedule, null, tint = Color(0xFF38BDF8)) },
                                onClick = {
                                    showOptionsMenu = false
                                    showSleepTimerModal = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Playlist", color = Color.White) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = Color.White) },
                                onClick = {
                                    showOptionsMenu = false
                                    showAddToPlaylist = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Queue / Up Next", color = Color.White) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = Color.White) },
                                onClick = {
                                    showOptionsMenu = false
                                    showQueueModal = true
                                }
                            )
                            if (activeTab == 1) {
                                DropdownMenuItem(
                                    text = {
                                        val modeName = when (videoResizeMode) {
                                            1 -> "Video Fit: Fill (Zoom)"
                                            2 -> "Video Fit: Stretch (Wide)"
                                            else -> "Video Fit: Standard (16:9)"
                                        }
                                        Text(modeName, color = Color.White)
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.AspectRatio, null, tint = Color(0xFF38BDF8)) },
                                    onClick = {
                                        showOptionsMenu = false
                                        videoResizeMode = (videoResizeMode + 1) % 3
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isVideoControlsCollapsed) "Expand Music Controls" else "Edge-to-Edge (Collapse Controls)", color = Color.White) },
                                    leadingIcon = { Icon(if (isVideoControlsCollapsed) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.Fullscreen, null, tint = Color(0xFFE11D48)) },
                                    onClick = {
                                        showOptionsMenu = false
                                        isVideoControlsCollapsed = !isVideoControlsCollapsed
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 2. CENTER CONTENT (SONG COVER, VIDEO, OR LYRICS VIEW) ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (activeTab) {
                    0 -> {
                        // ── SONG VIEW: SUPPORTS ALL 4 PLAYER SKINS ──
                        val effectiveProgress = if (sliderDragging >= 0f) sliderDragging else {
                            if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                        }
                        val displayPositionMs = if (sliderDragging >= 0f) (sliderDragging * durationMs).toLong() else positionMs

                        when (playerSkinLayout) {
                            PlayerSkinLayout.VINYL_TURNTABLE -> {
                                // ── 1. VINYL TURNTABLE SKIN (MATCHING SCREENSHOT 3) ──
                                VinylTurntableLayout(
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            PlayerSkinLayout.RADIAL_DRAWER -> {
                                // ── 2. RADIAL RING WITH FLANKING BUTTONS (MATCHING SCREENSHOT 2) ──
                                val sweepAngle = effectiveProgress * 360f
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Flanking Previous Button on Left of Circle
                                    IconButton(
                                        onClick = { connection.skipPrevious() },
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SkipPrevious,
                                            contentDescription = "Previous",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    // Center Circular Progress Ring & Artwork
                                    Box(
                                        modifier = Modifier.size(230.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val radius = size.minDimension / 2f
                                            val center = Offset(size.width / 2f, size.height / 2f)
                                            drawCircle(
                                                color = Color.White.copy(alpha = 0.22f),
                                                style = Stroke(width = 4.5.dp.toPx())
                                            )
                                            drawArc(
                                                color = Color.White,
                                                startAngle = -90f,
                                                sweepAngle = sweepAngle,
                                                useCenter = false,
                                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                            // Scrubber Knob dot at current progress
                                            val angleRad = Math.toRadians((sweepAngle - 90f).toDouble())
                                            val knobX = (center.x + (radius - 2.5.dp.toPx()) * Math.cos(angleRad)).toFloat()
                                            val knobY = (center.y + (radius - 2.5.dp.toPx()) * Math.sin(angleRad)).toFloat()
                                            drawCircle(
                                                color = Color.White,
                                                radius = 6.dp.toPx(),
                                                center = Offset(knobX, knobY)
                                            )
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF1B1B22),
                                            shadowElevation = 16.dp,
                                            modifier = Modifier.size(198.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                val artUri = currentTrack?.albumArtUri
                                                if (artUri != null) {
                                                    AsyncImage(
                                                        model = artUri,
                                                        contentDescription = "Cover Art",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .alpha(0.55f)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(
                                                                Brush.radialGradient(
                                                                    listOf(Color(0xFF38234A), Color(0xFF0F0B18))
                                                                )
                                                            )
                                                    )
                                                }

                                                // Center Digital Time Counter "1:40" (Screenshot 2)
                                                Text(
                                                    text = formatMs(displayPositionMs),
                                                    color = Color.White,
                                                    fontSize = 34.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 1.sp,
                                                    style = androidx.compose.ui.text.TextStyle(
                                                        shadow = androidx.compose.ui.graphics.Shadow(
                                                            color = Color.Black.copy(alpha = 0.85f),
                                                            blurRadius = 14f
                                                        )
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Flanking Next Button on Right of Circle
                                    IconButton(
                                        onClick = { connection.skipNext() },
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SkipNext,
                                            contentDescription = "Next",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                            PlayerSkinLayout.RADIAL_RING -> {
                                // ── 3. RADIAL VINYL / SWEEP RING SKIN ──
                                val sweepAngle = effectiveProgress * 360f
                                Box(
                                    modifier = Modifier
                                        .size(270.dp)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(260.dp)) {
                                        drawArc(
                                            color = Color.White.copy(alpha = 0.18f),
                                            startAngle = -90f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = Color.White,
                                            startAngle = -90f,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF1B1B22),
                                        shadowElevation = 16.dp,
                                        modifier = Modifier.size(236.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            val artUri = currentTrack?.albumArtUri
                                            if (artUri != null) {
                                                AsyncImage(
                                                    model = artUri,
                                                    contentDescription = "Cover Art",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .alpha(0.55f)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            Brush.radialGradient(
                                                                listOf(Color(0xFF38234A), Color(0xFF0F0B18))
                                                            )
                                                        )
                                                )
                                            }

                                            Text(
                                                text = formatMs(displayPositionMs),
                                                color = Color.White,
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.sp,
                                                style = androidx.compose.ui.text.TextStyle(
                                                    shadow = androidx.compose.ui.graphics.Shadow(
                                                        color = Color.Black.copy(alpha = 0.8f),
                                                        blurRadius = 12f
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            PlayerSkinLayout.IMMERSIVE_DRAWER -> {
                                // ── 4. SCENIC IMMERSIVE WALLPAPER SKIN ──
                                Surface(
                                    shape = RoundedCornerShape(26.dp),
                                    color = Color(0xFF1B1B22),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    shadowElevation = 20.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(290.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val artUri = currentTrack?.albumArtUri
                                        if (artUri != null) {
                                            AsyncImage(
                                                model = artUri,
                                                contentDescription = "Cover Art",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color(0xFF2C3E50), Color(0xFF101921))
                                                        )
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                // ── 5. MODERN CARD SKIN (HERO ALBUM COVER - SCREENSHOT 1) ──
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = Color(0xFF1B1B22),
                                    shadowElevation = 18.dp,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    modifier = Modifier
                                        .size(285.dp)
                                        .shadow(24.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.8f))
                                ) {
                                    val artUri = currentTrack?.albumArtUri
                                    if (artUri != null) {
                                        AsyncImage(
                                            model = artUri,
                                            contentDescription = "Cover Art",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Brush.radialGradient(listOf(Color(0xFF2C3E50), Color(0xFF0F2027)))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MusicNote,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(80.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // ── VIDEO VIEW: NATIVE HARDWARE-ACCELERATED VIDEO PLAYBACK & FLOATING SCREEN ──
                        val playerResizeMode = when (videoResizeMode) {
                            1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            2 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                            else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }

                        Box(
                            modifier = if (isVideoControlsCollapsed) {
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .height(285.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black)
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDirectExoVideo) {
                                AndroidView(
                                    factory = { ctx ->
                                        try {
                                            androidx.media3.ui.PlayerView(ctx).apply {
                                                useController = false
                                                useArtwork = false
                                                defaultArtwork = null
                                                resizeMode = playerResizeMode
                                                connection.bindPlayerView(this)
                                            }
                                        } catch (_: Throwable) {
                                            android.view.View(ctx)
                                        }
                                    },
                                    update = { view ->
                                        if (view is androidx.media3.ui.PlayerView) {
                                            view.useArtwork = false
                                            view.resizeMode = playerResizeMode
                                            connection.bindPlayerView(view)
                                        }
                                    },
                                    onRelease = { view ->
                                        if (view is androidx.media3.ui.PlayerView) {
                                            connection.unbindPlayerView(view)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (isVideoLoading) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFE11D48),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = "Loading official HD video...",
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else if (effectiveVideoId != null && effectiveVideoId.isNotBlank()) {
                                YouTubeIFramePlayer(
                                    videoId = effectiveVideoId,
                                    resizeMode = videoResizeMode,
                                    isPlaying = isPlaying,
                                    currentPositionMs = positionMs,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFE11D48),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = "Resolving official video stream...",
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.setVideoMode(true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("Search & Play Video", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }


                            // If controls collapsed (Edge-to-Edge Mode), show sleek floating bottom control pill
                            if (isVideoControlsCollapsed) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color.Black.copy(alpha = 0.78f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = currentTrack?.name ?: "Video Playing",
                                                    color = Color.White,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${currentTrack?.artist.orEmpty()} • ${formatMs(positionMs)} / ${formatMs(durationMs)}",
                                                    color = Color.White.copy(alpha = 0.65f),
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { connection.skipPrevious() },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        if (isPlaying) connection.pause() else connection.play()
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(appColors.accentPrimary)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { connection.skipNext() },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                }

                                                // Expand Controls Button
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = Color.White.copy(alpha = 0.16f),
                                                    onClick = { isVideoControlsCollapsed = false },
                                                    modifier = Modifier.padding(start = 4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Rounded.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(2.dp))
                                                        Text("Controls", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        // Mini Scrubber Slider
                                        val miniProgress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                                        Slider(
                                            value = miniProgress,
                                            onValueChange = { frac ->
                                                connection.seekTo((frac * durationMs).toLong())
                                            },
                                            colors = SliderDefaults.colors(
                                                thumbColor = appColors.accentPrimary,
                                                activeTrackColor = appColors.accentPrimary,
                                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(20.dp)
                                        )
                                    }
                                }
                            } else {
                                // Bottom Strip on 285dp card: Quick Edge-to-Edge & Resize Mode buttons
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.Black.copy(alpha = 0.65f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                        onClick = { isVideoControlsCollapsed = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Fullscreen, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Full Screen", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.Black.copy(alpha = 0.65f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                        onClick = { videoResizeMode = (videoResizeMode + 1) % 3 }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.AspectRatio, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = when (videoResizeMode) {
                                                    1 -> "Fill (Crop)"
                                                    2 -> "Wide"
                                                    else -> "16:9 Fit"
                                                },
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // ── LYRICS VIEW: SYNCHRONIZED SCROLLING KARAOKE ──
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                lyricsLoading -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp)
                                        Spacer(Modifier.height(12.dp))
                                        Text("Fetching lyrics...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                    }
                                }
                                syncedLines.isNotEmpty() -> {
                                    LazyColumn(
                                        state = lyricsListState,
                                        contentPadding = PaddingValues(vertical = 120.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        itemsIndexed(syncedLines) { index, linePair ->
                                            val isActive = index == activeLyricIndex
                                            val lineTimeMs = linePair.first
                                            val lineText = linePair.second

                                            Text(
                                                text = lineText,
                                                color = if (isActive) Color.White else Color.White.copy(alpha = 0.35f),
                                                fontSize = if (isActive) 22.sp else 16.sp,
                                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                                lineHeight = if (isActive) 30.sp else 24.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        connection.seekTo(lineTimeMs)
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                                lyrics?.plainLyrics?.isNotBlank() == true -> {
                                    LazyColumn(
                                        contentPadding = PaddingValues(vertical = 24.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        item {
                                            Text(
                                                text = lyrics?.plainLyrics.orEmpty(),
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 17.sp,
                                                lineHeight = 32.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lyrics,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Spacer(Modifier.height(14.dp))
                                        Text(
                                            "No lyrics found for this song",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                currentTrack?.let {
                                                    viewModel.fetchLyrics(it.name, it.artist)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text("Search Lyrics", color = Color.White, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!(activeTab == 1 && isVideoControlsCollapsed)) {
                Spacer(Modifier.height(10.dp))

                if (activeTab == 0 && playerSkinLayout == PlayerSkinLayout.RADIAL_DRAWER) {
                // ── RADIAL DRAWER SKIN: DEDICATED CONTROLS (MATCHING SCREENSHOT 2) ──
                Spacer(Modifier.height(10.dp))

                // Title row with Shuffle on Left & Comment on Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { connection.toggleShuffle() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) Color(0xFF10B981) else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = currentTrack?.name ?: "No Track Playing",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = currentTrack?.artist?.ifBlank { "MusicDrop" } ?: "MusicDrop",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { activeTab = 2 },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = "Lyrics",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 5 Icons Row: Favorite, Sleep Timer, Add to Playlist, Queue, Equalizer ON
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val cur = currentTrack
                            if (cur != null) {
                                val newFav = viewModel.toggleFavoriteTrack(cur)
                                isLiked = newFav
                                Toast.makeText(context, if (newFav) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isLiked) Color(0xFFEF4444) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSleepTimerModal = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerTargetMs > 0L) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showAddToPlaylist = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = "Add to Playlist",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showQueueModal = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    val isEqOn = eqState?.isEnabled == true
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showEqualizerModal = true }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Equalizer",
                            tint = if (isEqOn) Color(0xFF10B981) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(19.dp)
                        )
                        if (isEqOn) {
                            Spacer(Modifier.width(2.dp))
                            Text("ON", color = Color(0xFF10B981), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Up Next Drawer Sheet with Floating Amber Circle Play Button (Screenshot 2)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 135.dp, max = 165.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = Color.White,
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            val nextTrack = upNextQueue.firstOrNull()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (nextTrack != null) {
                                            viewModel.playYouTubeVideoWithContext(nextTrack, upNextQueue)
                                        } else {
                                            showQueueModal = true
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (nextTrack?.thumbnailUrl?.isNotBlank() == true) {
                                        AsyncImage(
                                            model = nextTrack.thumbnailUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = nextTrack?.title ?: "Up Next: Tap to view queue",
                                        color = Color(0xFF0F172A),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = nextTrack?.channelTitle ?: "MusicDrop Queue",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.5.sp,
                                        maxLines = 1
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("320K", color = Color(0xFF475569), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Floating Amber Circle Play Button on Right Side (Matching Screenshot 2)
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF59E0B),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 24.dp)
                            .size(50.dp)
                            .clickable { connection.togglePlayPause() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            } else {
                // ── SCREENSHOT 1 LOOK: DEDICATED STREAMLINED MUSIC CONTROLS ──
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                )

                Spacer(Modifier.height(8.dp))

                // 1. Status Indicator Row: [ 🟠 NOW PLAYING ∨ ] ... [ HD VIDEO / HQ AUDIO ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF97316))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "NOW PLAYING",
                            color = Color(0xFFF97316),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E1E26),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (activeTab == 1) "HD VIDEO" else "HQ AUDIO",
                            color = Color(0xFFF97316),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 2. Track Title & Artist (with Right Chevron, Heart & Download buttons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currentTrack?.name ?: "No Track Playing",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .basicMarquee(iterations = Int.MAX_VALUE)
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = currentTrack?.artist?.ifBlank { "Think Music India" } ?: "MusicDrop",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Heart / Like
                    IconButton(
                        onClick = {
                            val cur = currentTrack
                            if (cur != null) {
                                val newFav = viewModel.toggleFavoriteTrack(cur)
                                isLiked = newFav
                                Toast.makeText(context, if (newFav) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isLiked) Color(0xFFEF4444) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    // Download (Choice Dialog: MP3 Audio or MP4 Video)
                    IconButton(
                        onClick = { showDownloadModal = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Download",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 3. Orange Scrubber Slider with Start and End Timestamps
                val effectiveProgress = if (sliderDragging >= 0f) sliderDragging else {
                    if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                }
                val displayPositionMs = if (sliderDragging >= 0f) (sliderDragging * durationMs).toLong() else positionMs

                Slider(
                    value = effectiveProgress,
                    onValueChange = { frac -> sliderDragging = frac },
                    onValueChangeFinished = {
                        if (durationMs > 0L && sliderDragging >= 0f) {
                            connection.seekTo((sliderDragging * durationMs).toLong())
                        }
                        sliderDragging = -1f
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFF97316),
                        activeTrackColor = Color(0xFFF97316),
                        inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMs(displayPositionMs),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.5.sp
                    )
                    Text(
                        text = formatMs(durationMs),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.5.sp
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 4. Playback Controls Row: [ 🔀 ] [ ⏮ ] [ ⏸ (White Circle) ] [ ⏭ ] [ 🔁 ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(
                        onClick = { connection.toggleShuffle() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) Color(0xFFF97316) else Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = { connection.skipPrevious() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Solid White Circle Play/Pause Button
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 10.dp,
                        onClick = { connection.togglePlayPause() },
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Next
                    IconButton(
                        onClick = { connection.skipNext() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Repeat
                    IconButton(
                        onClick = { connection.toggleRepeat() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isRepeat) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeat) Color(0xFFF97316) else Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 5. Bottom Row: "Playing from Continuous Radio Mix" ... [ ＋ Save ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Playing from",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = currentTrack?.album?.ifBlank { "Continuous Radio Mix" } ?: "Continuous Radio Mix",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF22222C),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        onClick = { showAddToPlaylist = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                contentDescription = null,
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Save",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }
        }
    }

        // ── DIALOGS ──
        if (showEqualizerModal) {
            EqualizerDialog(
                equalizerManager = equalizerManager,
                onDismiss = { showEqualizerModal = false }
            )
        }

        if (showThemeModal) {
            PlayerThemeDialog(
                currentTheme = playerTheme,
                onSelectTheme = { selected ->
                    viewModel.setPlayerTheme(selected)
                    showThemeModal = false
                },
                onDismiss = { showThemeModal = false }
            )
        }

        val curTrack = currentTrack
        if (showAddToPlaylist && curTrack != null) {
            val unified = remember(curTrack.id) {
                UnifiedTrack.Local(
                    key = curTrack.id.toString(),
                    title = curTrack.name,
                    artist = curTrack.artist,
                    thumbnailUrl = curTrack.albumArtUri?.toString().orEmpty(),
                    duration = curTrack.formattedDuration,
                    sourceName = if (curTrack.filePath?.startsWith("yt:") == true) "YouTube" else "Local",
                    filePath = curTrack.filePath.orEmpty(),
                    mediaItem = curTrack
                )
            }
            AddToPlaylistDialog(
                track = unified,
                viewModel = viewModel,
                onDismiss = { showAddToPlaylist = false },
                onAdded = { playlistName ->
                    Toast.makeText(context, "Added to $playlistName", Toast.LENGTH_SHORT).show()
                    showAddToPlaylist = false
                }
            )
        }

        // Queue Dialog
        if (showQueueModal) {
            Dialog(onDismissRequest = { showQueueModal = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF14131D),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Up Next Queue", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showQueueModal = false }) {
                                Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.7f))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (upNextQueue.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No tracks queued up", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(upNextQueue) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .clickable {
                                                viewModel.playTrackFromQueue(item)
                                                showQueueModal = false
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.6f))
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(item.channelTitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sleep Timer Dialog
        if (showSleepTimerModal) {
            Dialog(onDismissRequest = { showSleepTimerModal = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF14131D),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.padding(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Set Sleep Timer", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        val options = listOf(15, 30, 45, 60, 90)
                        options.forEach { minutes ->
                            Text(
                                text = "$minutes minutes",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        sleepTimerTargetMs = System.currentTimeMillis() + minutes * 60 * 1000L
                                        showSleepTimerModal = false
                                        Toast.makeText(context, "Timer set for $minutes minutes", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            )
                        }
                        if (sleepTimerTargetMs > 0L) {
                            Text(
                                text = "Turn off timer",
                                color = Color(0xFFEF4444),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        sleepTimerTargetMs = 0L
                                        showSleepTimerModal = false
                                        Toast.makeText(context, "Timer cancelled", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Download Choice Dialog
        if (showDownloadModal) {
            val cur = currentTrack
            DownloadChoiceDialog(
                trackName = cur?.name ?: "Current Track",
                onDownloadAudio = {
                    if (cur != null) {
                        Toast.makeText(context, "Starting MP3 Audio download...", Toast.LENGTH_SHORT).show()
                        viewModel.downloadCurrentTrack { success, path ->
                            Toast.makeText(
                                context,
                                if (success) "Downloaded audio to Music" else "Download failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onDownloadVideo = {
                    if (cur != null) {
                        Toast.makeText(context, "Extracting HD Video stream...", Toast.LENGTH_SHORT).show()
                        viewModel.downloadCurrentVideo { success, path ->
                            Toast.makeText(
                                context,
                                if (success) "Downloaded HD video to Movies" else "Video download unavailable for this track",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onDismiss = { showDownloadModal = false }
            )
        }
    }
}

@Composable
fun DownloadChoiceDialog(
    trackName: String,
    onDownloadAudio: () -> Unit,
    onDownloadVideo: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1B2E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Download Options",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = trackName,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(20.dp))

                // Option 1: Audio (MP3 320k)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    onClick = {
                        onDownloadAudio()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.MusicNote, null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Download Audio (MP3)", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("High Quality 320kbps for offline listening", color = Color.White.copy(alpha = 0.5f), fontSize = 11.5.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Option 2: Video (MP4 HD)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    onClick = {
                        onDownloadVideo()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.PlayCircle, null, tint = Color(0xFFE11D48), modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Download Video (MP4 HD)", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("Crisp 1080p / 720p official music video", color = Color.White.copy(alpha = 0.5f), fontSize = 11.5.sp)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

/**
 * Layout 1: Vinyl Turntable with Rotating Grooved Disc, Center Album Art Label,
 * and Metallic Tonearm Needle (Matching Screenshot 3).
 */
@Composable
fun VinylTurntableLayout(
    currentTrack: MediaItem?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_turntable_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_angle"
    )
    val effectiveRotation = if (isPlaying) rotation else 0f

    // Animated tonearm angle: moves onto the vinyl track when playing
    val tonearmTargetAngle = if (isPlaying) 0f else -18f
    val animatedTonearmAngle by androidx.compose.animation.core.animateFloatAsState(
        targetValue = tonearmTargetAngle,
        animationSpec = tween(durationMillis = 700),
        label = "tonearm_angle"
    )

    Box(
        modifier = modifier
            .size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Black Grooved Vinyl Disc Canvas
        Canvas(
            modifier = Modifier
                .size(265.dp)
                .rotate(effectiveRotation)
        ) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer vinyl base
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1E24), Color(0xFF121216), Color(0xFF070709))
                ),
                radius = radius,
                center = center
            )

            // Concentric sound grooves
            for (i in 1..10) {
                val grooveRadius = radius * (0.44f + (i * 0.052f))
                drawCircle(
                    color = Color.White.copy(alpha = if (i % 2 == 0) 0.07f else 0.035f),
                    radius = grooveRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Vinyl outer rim highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = radius - 1.5f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // 2. Center Album Artwork Label
        val artUri = currentTrack?.albumArtUri
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                .rotate(effectiveRotation),
            contentAlignment = Alignment.Center
        ) {
            if (artUri != null) {
                AsyncImage(
                    model = artUri,
                    contentDescription = "Cover Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2C3E50), Color(0xFF4A2B68), Color(0xFF0F172A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // Center Spindle Hole
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F0F14))
                    .border(2.dp, Color(0xFF94A3B8), CircleShape)
            )
        }

        // 3. Metallic Tonearm Needle Arm Overlay
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(animatedTonearmAngle)
        ) {
            val pivot = Offset(size.width * 0.78f, size.height * 0.10f)
            val elbow = Offset(size.width * 0.90f, size.height * 0.36f)
            val stylus = Offset(size.width * 0.68f, size.height * 0.54f)

            // Pivot base circle
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFFE2E8F0), Color(0xFF475569))),
                radius = 11.dp.toPx(),
                center = pivot
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = pivot
            )

            // Metallic arm lines
            drawLine(
                brush = Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF64748B))),
                start = pivot,
                end = elbow,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                brush = Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFFE2E8F0))),
                start = elbow,
                end = stylus,
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Headshell / Stylus cartridge resting on record
            drawCircle(
                color = Color(0xFFF1F5F9),
                radius = 5.5.dp.toPx(),
                center = stylus
            )
        }
    }
}

