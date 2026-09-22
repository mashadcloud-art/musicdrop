package com.musicdrop.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.blur
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
import com.musicdrop.app.ui.theme.*
import com.musicdrop.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    val hasVideoTrack by connection.hasVideoTrack.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val appColors = LocalAppColors.current
    val appTheme by viewModel.appTheme.collectAsState()

    val playerSkinLayout by viewModel.playerSkinLayout.collectAsState()

    // 0: Song, 1: Video, 2: Lyrics
    var activeTab by remember { mutableIntStateOf(if (currentTrack?.mediaType == MediaType.VIDEO || viewModel.isVideoMode.value) 1 else 0) }
    var sliderDragging by remember { mutableFloatStateOf(-1f) }
    var isLiked by remember { mutableStateOf(false) }

    LaunchedEffect(currentTrack?.id, currentTrack?.mediaType) {
        if (currentTrack?.mediaType == MediaType.VIDEO) {
            activeTab = 1
        }
    }

    // Dialog toggles
    var showEqualizerModal by remember { mutableStateOf(false) }
    var showThemeModal by remember { mutableStateOf(false) }
    var showSkinChooserModal by remember { mutableStateOf(false) }
    var showAppThemeModal by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showQueueModal by remember { mutableStateOf(false) }
    var showHalfQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerModal by remember { mutableStateOf(false) }
    var showDownloadModal by remember { mutableStateOf(false) }
    var sleepTimerTargetMs by remember { mutableLongStateOf(0L) }
    var sleepTimerStopAtEndOfSong by remember { mutableStateOf(false) }
    var sleepTimerInitialTrackId by remember { mutableLongStateOf(0L) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var videoResizeMode by remember { mutableIntStateOf(1) } // 0: 16:9 Fit, 1: Fill (Zoom), 2: Wide (Stretch)
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

    val isDirectExoVideo = currentTrack?.mediaType == MediaType.VIDEO &&
        (currentTrack?.filePath?.endsWith(".mp4", ignoreCase = true) == true ||
         currentTrack?.filePath?.startsWith("/") == true ||
         currentTrack?.filePath?.startsWith("content://") == true ||
         currentTrack?.uri?.scheme == "content" ||
         currentTrack?.uri?.scheme == "file")
    val isVideoMode by viewModel.isVideoMode.collectAsState()

    // Resolve video ID from current track metadata or online search
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
            ytCurrentVideo?.videoId?.isNotBlank() == true && isYtMatchingTrack(ytCurrentVideo, track) -> ytCurrentVideo?.videoId
            else -> null
        }
    }

    // Automatically switch to video mode and resolve video when Video tab is selected or track changes
    LaunchedEffect(activeTab, currentTrack?.id) {
        if (activeTab == 1) {
            if (!isVideoMode) {
                viewModel.setVideoMode(true)
            }
            viewModel.resolveVideoForCurrentTrack()
        }
    }

    // Automatically return to audio stream when switching away from Video tab
    LaunchedEffect(activeTab) {
        if (activeTab != 1) {
            isVideoControlsCollapsed = false
            if (isVideoMode) {
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
                        try {
                            if (view is androidx.media3.ui.PlayerView) {
                                view.useArtwork = false
                                view.resizeMode = pipResizeMode
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
                YouTubeIFramePlayer(
                    videoId = effectiveVideoId,
                    title = currentTrack?.name,
                    channel = currentTrack?.artist,
                    thumbnailUrl = currentTrack?.albumArtUri?.toString(),
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

    // Check favorite status and handle sleep timer stop at end of song
    LaunchedEffect(currentTrack?.id) {
        val cur = currentTrack
        if (cur != null) {
            // If timer was waiting for current song to end and a new song is now playing
            if (sleepTimerStopAtEndOfSong && sleepTimerInitialTrackId != 0L && cur.id != sleepTimerInitialTrackId) {
                connection.pause()
                sleepTimerStopAtEndOfSong = false
                sleepTimerInitialTrackId = 0L
                Toast.makeText(context, "Sleep timer: Stopped at end of song", Toast.LENGTH_SHORT).show()
                return@LaunchedEffect
            }
            isLiked = viewModel.isTrackFavorite(cur)
            viewModel.fetchLyrics(cur.name, cur.artist)
        }
    }

    // Handle end-of-song sleep timer when song finishes playing
    LaunchedEffect(sleepTimerStopAtEndOfSong, positionMs, durationMs) {
        if (sleepTimerStopAtEndOfSong && durationMs > 5000L && positionMs >= durationMs - 500L) {
            connection.pause()
            sleepTimerStopAtEndOfSong = false
            sleepTimerInitialTrackId = 0L
            Toast.makeText(context, "Sleep timer: Stopped at end of song", Toast.LENGTH_SHORT).show()
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

    // Active accent color syncing with app theme and player theme
    val activeAccent = remember(playerTheme, appColors) {
        if (playerTheme == PlayerThemeId.MIDNIGHT_OLED || playerTheme == PlayerThemeId.DYNAMIC_BLUR) {
            appColors.accentPrimary
        } else {
            playerTheme.accentColor
        }
    }

    // Background dynamic ambient gradient (Pure aesthetic theme colors, no blown-up cover art)
    val dynamicGradient = remember(playerTheme, activeAccent) {
        playerTheme.getBackgroundBrush(fallbackAccent = activeAccent)
    }

    val isGlassMode = playerSkinLayout == PlayerSkinLayout.FROSTED_GLASS ||
        playerTheme == PlayerThemeId.PURE_FROST ||
        appTheme == com.musicdrop.app.ui.theme.AppThemeMode.GLASSMORPHISM ||
        appColors.isGlassmorphism

    // Dynamic docked bottom card surface (Frosted glass transparent if skin is FROSTED_GLASS, glass mode, or in VIDEO mode)
    val isTransparentCard = isGlassMode || activeTab == 1

    val bottomCardSurface = remember(appColors, playerTheme, playerSkinLayout, activeTab, isGlassMode) {
        if (isTransparentCard) {
            Color.White.copy(alpha = 0.12f)
        } else when {
            playerTheme == PlayerThemeId.MIDNIGHT_OLED || playerTheme == PlayerThemeId.VINYL_MIDNIGHT -> Color(0xFF070708)
            playerTheme == PlayerThemeId.CARBON_SLATE -> Color(0xFF101216)
            playerTheme == PlayerThemeId.PURE_FROST -> Color(0xFF181F28)
            appColors.isDark -> Color(
                red = (appColors.surfaceElevated.red * 0.45f).coerceIn(0.04f, 0.12f),
                green = (appColors.surfaceElevated.green * 0.45f).coerceIn(0.05f, 0.13f),
                blue = (appColors.surfaceElevated.blue * 0.45f).coerceIn(0.07f, 0.18f),
                alpha = 1.0f
            )
            else -> Color(0xFF0F172A)
        }
    }

    val bottomCardBorder = remember(isTransparentCard) {
        if (isTransparentCard) {
            androidx.compose.foundation.BorderStroke(1.2.dp, Color.White.copy(alpha = 0.25f))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        }
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
        // Blurred Album Artwork Background for Frosted Glass Skin & Video Songs
        if (isGlassMode || activeTab == 1) {
            val artUri = currentTrack?.albumArtUri
            if (artUri != null && artUri.toString().isNotBlank()) {
                AsyncImage(
                    model = artUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 50.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.42f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1E1B4B),
                                    Color(0xFF0F172A),
                                    Color(0xFF1E293B),
                                    Color(0xFF0F0A1C)
                                )
                            )
                        )
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. UPPER MEDIA SURFACE (SPANS FROM y=0 TO TOP OF DOCKED SHEET) ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (activeTab) {
                    0 -> {
                        // ── SONG VIEW: SUPPORTS ALL PLAYER SKINS ──
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                                .padding(top = 52.dp),
                            contentAlignment = Alignment.Center
                        ) {
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
                            PlayerSkinLayout.FROSTED_GLASS -> {
                                // ── 6. FROSTED GLASS TRANSPARENT SKIN ──
                                Surface(
                                    shape = RoundedCornerShape(28.dp),
                                    color = Color.White.copy(alpha = 0.12f),
                                    shadowElevation = 24.dp,
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.28f)),
                                    modifier = Modifier
                                        .size(255.dp)
                                        .shadow(32.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.7f))
                                ) {
                                    val artUri = currentTrack?.albumArtUri
                                    if (artUri != null && artUri.toString().isNotBlank()) {
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
                                                        listOf(
                                                            Color.White.copy(alpha = 0.15f),
                                                            Color(0xFF38BDF8).copy(alpha = 0.25f),
                                                            Color(0xFF1E1B4B).copy(alpha = 0.65f)
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MusicNote,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(76.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                // ── 5. MODERN CARD SKIN (HERO ALBUM COVER) ──
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = if (isGlassMode) Color.White.copy(alpha = 0.12f) else Color(0xFF1B1B22),
                                    shadowElevation = 18.dp,
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isGlassMode) 1.5.dp else 1.dp,
                                        if (isGlassMode) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f)
                                    ),
                                    modifier = Modifier
                                        .size(285.dp)
                                        .shadow(24.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.8f))
                                ) {
                                    val artUri = currentTrack?.albumArtUri
                                    if (artUri != null && artUri.toString().isNotBlank()) {
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
                                                    if (isGlassMode) {
                                                        Brush.linearGradient(
                                                            listOf(
                                                                Color.White.copy(alpha = 0.15f),
                                                                Color(0xFF38BDF8).copy(alpha = 0.25f),
                                                                Color(0xFF1E1B4B).copy(alpha = 0.65f)
                                                            )
                                                        )
                                                    } else {
                                                        Brush.radialGradient(listOf(Color(0xFF2C3E50), Color(0xFF0F2027)))
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MusicNote,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.75f),
                                                modifier = Modifier.size(80.dp)
                                            )
                                        }
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
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            val isDirectVideoRendering = isDirectExoVideo && hasVideoTrack
                            val hasYouTubeVideo = !effectiveVideoId.isNullOrBlank()

                            if (isDirectVideoRendering) {
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
                                        try {
                                            if (view is androidx.media3.ui.PlayerView) {
                                                view.useArtwork = false
                                                view.resizeMode = playerResizeMode
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
                            } else if (hasYouTubeVideo && !isVideoLoading) {
                                YouTubeIFramePlayer(
                                    videoId = effectiveVideoId!!,
                                    title = currentTrack?.name,
                                    channel = currentTrack?.artist,
                                    thumbnailUrl = currentTrack?.albumArtUri?.toString(),
                                    resizeMode = videoResizeMode,
                                    isPlaying = isPlaying,
                                    currentPositionMs = positionMs,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // ── NO VIDEO / AUDIO FALLBACK: SHOW COVER IMAGE (NEVER PITCH BLACK VOID!) ──
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Ambient blurred cover backdrop
                                    if (currentTrack?.albumArtUri != null) {
                                        AsyncImage(
                                            model = currentTrack?.albumArtUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer { alpha = 0.4f }
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Black.copy(alpha = 0.5f), Color.Black.copy(alpha = 0.85f))
                                                )
                                            )
                                    )

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .padding(20.dp)
                                            .padding(bottom = 24.dp)
                                    ) {
                                        // Centered high-definition artwork with glow and rounded corners
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            shadowElevation = 18.dp,
                                            color = Color(0xFF141418),
                                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f)),
                                            modifier = Modifier.size(200.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (currentTrack?.albumArtUri != null) {
                                                    AsyncImage(
                                                        model = currentTrack?.albumArtUri,
                                                        contentDescription = currentTrack?.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Rounded.MusicNote,
                                                        contentDescription = null,
                                                        tint = appColors.accentPrimary,
                                                        modifier = Modifier.size(64.dp)
                                                    )
                                                }

                                                if (isVideoLoading) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.6f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(
                                                            color = appColors.accentPrimary,
                                                            strokeWidth = 3.dp,
                                                            modifier = Modifier.size(36.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(14.dp))

                                        // Status Pill Badge
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.Black.copy(alpha = 0.65f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isVideoLoading) Icons.Rounded.Sync else Icons.Rounded.Headphones,
                                                    contentDescription = null,
                                                    tint = appColors.accentPrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = if (isVideoLoading) "Searching online video..." else "Audio Track • No Video Stream",
                                                    color = Color.White.copy(alpha = 0.88f),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        if (!isVideoLoading) {
                                            Spacer(Modifier.height(10.dp))
                                            Button(
                                                onClick = { viewModel.setVideoMode(true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = appColors.accentPrimary),
                                                shape = RoundedCornerShape(16.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(5.dp))
                                                Text("Search & Play Video", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
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
                            } else if (isDirectVideoRendering || hasYouTubeVideo) {
                                // Bottom Strip on Video: Quick Full Screen & Resize Mode buttons (Screenshot 1)
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Fullscreen, null, tint = Color.White, modifier = Modifier.size(16.dp))
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
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.AspectRatio, null, tint = Color.White, modifier = Modifier.size(15.dp))
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
                                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                                .padding(top = 52.dp, start = 8.dp, end = 8.dp),
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

                // ── OVERLAID TOP BAR WITH SOFT GRADIENT SCRIM (TRUE EDGE-TO-EDGE MEDIA SURFACE) ──
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
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

                        // Center: Audio / Video Capsule Switcher or Song | Lyrics toggle (Matching Screenshot 2)
                        if (activeTab != 1) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.50f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Song Tab
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (activeTab == 0) activeAccent else Color.Transparent)
                                            .clickable { activeTab = 0 }
                                            .padding(horizontal = 12.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Song",
                                            color = if (activeTab == 0) Color.White else Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }

                                    // Lyrics Tab
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (activeTab == 2) activeAccent else Color.Transparent)
                                            .clickable { activeTab = 2 }
                                            .padding(horizontal = 12.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Lyrics",
                                            color = if (activeTab == 2) Color.White else Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        } else {
                            // Video View Center: Audio / Video switcher capsule
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.55f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (activeTab == 0) activeAccent else Color.Transparent)
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

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (activeTab == 1) activeAccent else Color.Transparent)
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
                        }

                        // Right Icons: Video toggle (in audio mode), Quick Skin Switcher (Hanger), Cast (📺), 3-Dots (⋮)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (activeTab != 1) {
                                IconButton(
                                    onClick = { activeTab = 1 },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Switch to Video",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Quick Skin Chooser Button (T-shirt / Hanger matching Screenshot 2)
                            IconButton(
                                onClick = { showSkinChooserModal = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Checkroom,
                                    contentDescription = "Change Player Skin",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(19.dp)
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
                                        text = { Text("Player Themes & Skins", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Rounded.Palette, null, tint = activeAccent) },
                                        onClick = {
                                            showOptionsMenu = false
                                            showThemeModal = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Player Skin: ${playerSkinLayout.title}", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Rounded.Layers, null, tint = Color(0xFF38BDF8)) },
                                        onClick = {
                                            showOptionsMenu = false
                                            showSkinChooserModal = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("App Theme: ${appTheme.displayName}", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Rounded.ColorLens, null, tint = Color(0xFFA855F7)) },
                                        onClick = {
                                            showOptionsMenu = false
                                            showAppThemeModal = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Download (Audio / Video)", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Rounded.Download, null, tint = Color(0xFFF59E0B)) },
                                        onClick = {
                                            showOptionsMenu = false
                                            showDownloadModal = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (activeTab == 1) "Listen to Audio Mode" else "Watch Official Video", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Rounded.PlayCircle, null, tint = Color(0xFFE11D48)) },
                                        onClick = {
                                            showOptionsMenu = false
                                            activeTab = if (activeTab == 1) 0 else 1
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
                                    DropdownMenuItem(
                                        text = { Text("Share Track / File", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Rounded.Share, null, tint = Color(0xFF38BDF8)) },
                                        onClick = {
                                            showOptionsMenu = false
                                            viewModel.shareCurrentTrack(context)
                                        }
                                    )
                                    if (activeTab == 1) {
                                        DropdownMenuItem(
                                            text = {
                                                val modeName = when (videoResizeMode) {
                                                    1 -> "Video Fit: Fill (Crop)"
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
                                            text = { Text(if (isVideoControlsCollapsed) "Expand Music Controls" else "Full Screen (Collapse Controls)", color = Color.White) },
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
                }
            }

            // ── 3. DOCKED BOTTOM CONTROLS CARD (MATCHING SCREENSHOT 1) ──
            if (!(activeTab == 1 && isVideoControlsCollapsed)) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = bottomCardSurface,
                    border = bottomCardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(36.dp)
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
                                        .background(activeAccent)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "NOW PLAYING",
                                    color = activeAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = activeAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(1.dp, activeAccent.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = if (activeTab == 1) "HD VIDEO" else "HQ AUDIO",
                                    color = activeAccent,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

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
                                        fontSize = 16.5.sp,
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
                                    fontSize = 13.sp,
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

                        Spacer(Modifier.height(6.dp))

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
                                thumbColor = activeAccent,
                                activeTrackColor = activeAccent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
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

                        Spacer(Modifier.height(8.dp))

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
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffle) Color(0xFFF97316) else Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Previous
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

                            // Solid White Circle Play/Pause Button
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 10.dp,
                                onClick = { connection.togglePlayPause() },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            // Next
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

                            // Repeat
                            IconButton(
                                onClick = { connection.toggleRepeat() },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = if (isRepeat) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                    contentDescription = "Repeat",
                                    tint = if (isRepeat) Color(0xFFF97316) else Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

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
                                    fontSize = 10.5.sp
                                )
                                Text(
                                    text = currentTrack?.album?.ifBlank { "Continuous Radio Mix" } ?: "Continuous Radio Mix",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                onClick = { showHalfQueueSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                        contentDescription = null,
                                        tint = activeAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "Up Next",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                    }
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

        if (showSkinChooserModal) {
            PlayerSkinChooserDialog(
                currentSkin = playerSkinLayout,
                onSelectSkin = { selectedSkin ->
                    viewModel.setPlayerSkinLayout(selectedSkin)
                    Toast.makeText(context, "Skin applied: ${selectedSkin.title}", Toast.LENGTH_SHORT).show()
                    showSkinChooserModal = false
                },
                onDismiss = { showSkinChooserModal = false }
            )
        }

        if (showAppThemeModal) {
            com.musicdrop.app.ui.components.SkinThemeDialog(
                currentTheme = appTheme,
                onSelectTheme = { selectedTheme ->
                    viewModel.setAppTheme(selectedTheme)
                    Toast.makeText(context, "Theme: ${selectedTheme.displayName}", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showAppThemeModal = false },
                viewModel = viewModel
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

        // ── UP NEXT HALF-SCREEN BOTTOM SHEET (ANCHORED STRICTLY TO BOTTOM OF SCREEN) ──
        BackHandler(enabled = showHalfQueueSheet || showQueueModal) {
            showHalfQueueSheet = false
            showQueueModal = false
        }

        // Tap-outside Scrim overlay over upper media surface
        if (showHalfQueueSheet || showQueueModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable {
                        showHalfQueueSheet = false
                        showQueueModal = false
                    }
            )
        }

        AnimatedVisibility(
            visible = showHalfQueueSheet || showQueueModal,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.60f)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFF13121C),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shadowElevation = 32.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp)
                ) {
                    // Drag Handle pill at top
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 10.dp, bottom = 6.dp)
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Up Next",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = activeAccent.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, activeAccent.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${upNextQueue.size + (if (currentTrack != null) 1 else 0)} tracks",
                                    color = activeAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (upNextQueue.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearQueue() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Clear", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            IconButton(
                                onClick = {
                                    showHalfQueueSheet = false
                                    showQueueModal = false
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Currently Playing Item
                        if (currentTrack != null) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = activeAccent.copy(alpha = 0.14f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, activeAccent.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Thumbnail
                                        val art = currentTrack?.albumArtUri
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (art != null) {
                                                AsyncImage(
                                                    model = art,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(Icons.Rounded.MusicNote, null, tint = activeAccent, modifier = Modifier.size(22.dp))
                                            }
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "NOW PLAYING",
                                                    color = activeAccent,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 0.6.sp
                                                )
                                                Spacer(Modifier.width(5.dp))
                                                Icon(
                                                    imageVector = Icons.Rounded.VolumeUp,
                                                    contentDescription = null,
                                                    tint = activeAccent,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Text(
                                                text = currentTrack?.name ?: "Current Song",
                                                color = Color.White,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = currentTrack?.artist.orEmpty(),
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.5.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (upNextQueue.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.3f),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "No upcoming tracks in queue",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Songs you choose to play next will appear here",
                                            color = Color.White.copy(alpha = 0.35f),
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(upNextQueue) { index, item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.05f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.playTrackFromQueue(item)
                                            showHalfQueueSheet = false
                                            showQueueModal = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(22.dp)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (item.thumbnailUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = item.thumbnailUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                            }
                                        }

                                        Spacer(Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.channelTitle,
                                                color = Color.White.copy(alpha = 0.55f),
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }

                                        // Move up button
                                        if (index > 0) {
                                            IconButton(
                                                onClick = { viewModel.moveQueueItem(index, index - 1) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                                    contentDescription = "Move Up",
                                                    tint = Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // Move down button
                                        if (index < upNextQueue.size - 1) {
                                            IconButton(
                                                onClick = { viewModel.moveQueueItem(index, index + 1) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                                    contentDescription = "Move Down",
                                                    tint = Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.removeFromQueue(item) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.size(16.dp)
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

                        // "End of current song" option
                        val isEndOfSongActive = sleepTimerStopAtEndOfSong
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isEndOfSongActive) activeAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (isEndOfSongActive) activeAccent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .clickable {
                                    sleepTimerStopAtEndOfSong = true
                                    sleepTimerInitialTrackId = currentTrack?.id ?: 0L
                                    sleepTimerTargetMs = 0L
                                    showSleepTimerModal = false
                                    Toast.makeText(context, "Timer set: Stop at end of current song", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp, horizontal = 12.dp)
                        ) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = activeAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "End of current song",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(8.dp))

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
                                        sleepTimerStopAtEndOfSong = false
                                        sleepTimerInitialTrackId = 0L
                                        sleepTimerTargetMs = System.currentTimeMillis() + minutes * 60 * 1000L
                                        showSleepTimerModal = false
                                        Toast.makeText(context, "Timer set for $minutes minutes", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            )
                        }
                        if (sleepTimerTargetMs > 0L || sleepTimerStopAtEndOfSong) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Turn off timer",
                                color = Color(0xFFEF4444),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        sleepTimerTargetMs = 0L
                                        sleepTimerStopAtEndOfSong = false
                                        sleepTimerInitialTrackId = 0L
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
                        Toast.makeText(context, "Saving MP3 Audio to Music/MusicDrop...", Toast.LENGTH_SHORT).show()
                        viewModel.downloadCurrentTrack { success, path ->
                            if (success) {
                                Toast.makeText(context, "Saved to Music/MusicDrop", Toast.LENGTH_SHORT).show()
                                viewModel.shareMediaFile(context, path, "audio/mp4", cur.name)
                            } else {
                                Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onDownloadVideo = {
                    if (cur != null) {
                        Toast.makeText(context, "Saving HD Video to Movies/MusicDrop...", Toast.LENGTH_SHORT).show()
                        viewModel.downloadCurrentVideo { success, path ->
                            if (success) {
                                Toast.makeText(context, "Saved to Movies/MusicDrop", Toast.LENGTH_SHORT).show()
                                viewModel.shareMediaFile(context, path, "video/mp4", cur.name)
                            } else {
                                Toast.makeText(context, "Video download unavailable for this track", Toast.LENGTH_SHORT).show()
                            }
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

private fun swatchForMode(mode: AppThemeMode): AppColors = when (mode) {
    AppThemeMode.YOUTUBE_MUSIC -> YouTubeMusicAppColors
    AppThemeMode.MUSIC_PULSE -> MusicPulseAppColors
    AppThemeMode.MUSIC_ORBIT -> MusicOrbitAppColors
    AppThemeMode.MUSIC_GREENROOM -> MusicGreenroomAppColors
    AppThemeMode.CYBER_DARK -> CyberDarkAppColors
    AppThemeMode.CLEAN_LIGHT -> CleanLightAppColors
    AppThemeMode.OLED_BLACK -> OledBlackAppColors
    AppThemeMode.SUNSET_NEBULA -> SunsetNebulaAppColors
    AppThemeMode.IOS_LIGHT -> IosLightAppColors
    AppThemeMode.NEARBY_SHARE -> NearbyShareAppColors
    AppThemeMode.TURBO_CONNECT -> TurboConnectAppColors
    AppThemeMode.RETRO -> RetroAppColors
    AppThemeMode.GLASSMORPHISM -> GlassmorphismAppColors
    AppThemeMode.ROYAL_PLUM -> RoyalPlumAppColors
    AppThemeMode.DEEP_NAVY -> DeepNavyAppColors
    AppThemeMode.ROSE_GOLD -> RoseGoldAppColors
}

@Composable
private fun PlayerSkinChooserDialog(
    currentSkin: PlayerSkinLayout,
    onSelectSkin: (PlayerSkinLayout) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF14131D),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Player Skin Layout",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose audio player layout style",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PlayerSkinLayout.values().toList()) { skin ->
                        val isSelected = skin == currentSkin
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFF97316) else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectSkin(skin) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val skinIcon = when (skin) {
                                PlayerSkinLayout.VINYL_TURNTABLE -> "💽"
                                PlayerSkinLayout.RADIAL_DRAWER -> "⭕"
                                PlayerSkinLayout.ROUNDED_CARD -> "🎴"
                                PlayerSkinLayout.RADIAL_RING -> "💿"
                                PlayerSkinLayout.IMMERSIVE_DRAWER -> "🌄"
                                PlayerSkinLayout.FROSTED_GLASS -> "🫧"
                            }
                            Text(skinIcon, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = skin.title,
                                    color = if (isSelected) Color(0xFFF97316) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = skin.subtitle,
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppThemeChooserDialog(
    currentTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF14131D),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "App Theme",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Syncs with Music Player Theme",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppThemeMode.values().toList()) { mode ->
                        val isSelected = mode == currentTheme
                        val swatch = swatchForMode(mode)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
                                .border(
                                    1.dp,
                                    if (isSelected) swatch.accentPrimary else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectTheme(mode) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(swatch.background)
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(swatch.accentPrimary)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.displayName,
                                    color = if (isSelected) swatch.accentPrimary else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = mode.subtitle,
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = swatch.accentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isYtMatchingTrack(yt: com.musicdrop.app.data.youtube.YouTubeSearchResult?, track: MediaItem?): Boolean {
    if (yt == null || track == null) return false
    val vid = yt.videoId.trim()
    if (vid.isBlank()) return false
    val trackPath = track.filePath.orEmpty()
    if (trackPath == vid || trackPath == "yt:$vid") return true
    if (track.uri.toString().contains(vid)) return true
    if (track.id == vid.hashCode().toLong()) return true

    val tName = track.name.lowercase().replace(Regex("[^a-z0-9 ]"), " ").trim()
    val yTitle = yt.title.lowercase().replace(Regex("[^a-z0-9 ]"), " ").trim()
    if (tName.isBlank() || yTitle.isBlank()) return false
    if (yTitle.contains(tName) || tName.contains(yTitle)) return true

    val tWords = tName.split(Regex("\\s+")).filter { it.length > 2 && it !in setOf("the", "and", "audio", "song", "official", "video") }
    if (tWords.isNotEmpty()) {
        val matchedWords = tWords.count { yTitle.contains(it) }
        if (matchedWords >= 2 || (tWords.size == 1 && matchedWords == 1)) {
            return true
        }
    }
    return false
}

