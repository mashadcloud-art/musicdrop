package com.musicdrop.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.AspectRatio
import com.musicdrop.app.data.model.MediaType

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.musicdrop.app.data.model.UnifiedTrack
import com.musicdrop.app.ui.components.AddToPlaylistDialog
import com.musicdrop.app.ui.components.WaveformVisualizer
import com.musicdrop.app.ui.theme.*
import com.musicdrop.app.ui.viewmodel.MainViewModel
import androidx.compose.material.icons.filled.PlaylistAdd

/**
 * Visual styles for the "DISC" tab's album art, cycled by tapping the cover
 * image itself. VINYL is the app's original look; the other three are
 * inspired by — but not pixel copies of — well-known streaming-app player
 * screens, so players feel at home no matter which big app they came from.
 */
private enum class PlayerTheme { VINYL, APPLE, YOUTUBE, SPOTIFY }

private fun PlayerTheme.next(): PlayerTheme = when (this) {
    PlayerTheme.VINYL -> PlayerTheme.APPLE
    PlayerTheme.APPLE -> PlayerTheme.YOUTUBE
    PlayerTheme.YOUTUBE -> PlayerTheme.SPOTIFY
    PlayerTheme.SPOTIFY -> PlayerTheme.VINYL
}

private fun PlayerTheme.label(): String = when (this) {
    PlayerTheme.VINYL -> "Vinyl · tap cover to change theme"
    PlayerTheme.APPLE -> "Apple Music style · tap to change"
    PlayerTheme.YOUTUBE -> "YouTube Music style · tap to change"
    PlayerTheme.SPOTIFY -> "Spotify style · tap to change"
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val isVideoMode by viewModel.isVideoMode.collectAsState()
    val videoModeLoading by viewModel.videoModeLoading.collectAsState()
    val ytCurrentVideo by viewModel.ytCurrentVideo.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val appColors = LocalAppColors.current
    val isBrightAccent = (appColors.accentPrimary.red * 0.299f + appColors.accentPrimary.green * 0.587f + appColors.accentPrimary.blue * 0.114f) > 0.55f
    val onActiveAccent = if (isBrightAccent) Color.Black else Color.White
    var activeTab by remember { mutableIntStateOf(0) } // 0: DISC, 1: UP NEXT, 2: LYRICS
    var playerTheme by remember { mutableStateOf(PlayerTheme.VINYL) } // tap cover art to cycle
    var sliderDragging by remember { mutableFloatStateOf(-1f) }
    var isLiked by remember { mutableStateOf(false) }
    var showSleepTimerModal by remember { mutableStateOf(false) }
    var sleepTimerTargetMs by remember { mutableLongStateOf(0L) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var isOledScreenOff by remember { mutableStateOf(false) }
    var videoResizeMode by remember { mutableIntStateOf(1) }
    var showDownloadChoiceDialog by remember { mutableStateOf(false) }
    var isDownloadingCurrent by remember { mutableStateOf(false) }
    var isDetailsShrunk by remember { mutableStateOf(false) }
    var coverArtMode by remember { mutableIntStateOf(0) } // 0: Spinning Vinyl, 1: Full Cover (Crop), 2: Glow Card
    var isFullscreenVideo by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = isFullscreenVideo) { isFullscreenVideo = false }

    // Once playback begins, automatically shrink details to minimal music control bar after 2.5s
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            kotlinx.coroutines.delay(2500L)
            isDetailsShrunk = true
        }
    }

    val fallbackVidId = remember(currentTrack, ytCurrentVideo) {
        val cur = currentTrack ?: return@remember ""
        val fromYt = ytCurrentVideo?.takeIf {
            it.videoId == cur.filePath || it.title.equals(cur.name, ignoreCase = true) || cur.name.contains(it.title, ignoreCase = true)
        }?.videoId?.trim().orEmpty()
        if (fromYt.length == 11) return@remember fromYt
        val fp = cur.filePath?.removePrefix("yt:")?.trim().orEmpty()
        if (fp.length == 11 && !fp.contains("/") && !fp.contains(".")) return@remember fp
        val uriStr = cur.uri.toString()
        if (uriStr.contains("v=")) {
            val vid = uriStr.substringAfter("v=").substringBefore("&").substringBefore("?")
            if (vid.length == 11) return@remember vid
        }
        if (uriStr.contains("youtu.be/")) {
            val vid = uriStr.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            if (vid.length == 11) return@remember vid
        }
        val artUri = cur.albumArtUri?.toString().orEmpty()
        if (artUri.contains("/vi_webp/")) {
            val vid = artUri.substringAfter("/vi_webp/").substringBefore("/").substringBefore("?")
            if (vid.length == 11) return@remember vid
        }
        if (artUri.contains("/vi/")) {
            val vid = artUri.substringAfter("/vi/").substringBefore("/").substringBefore("?")
            if (vid.length == 11) return@remember vid
        }
        ""
    }

    val hasVideoAvailable = remember(currentTrack, ytCurrentVideo, fallbackVidId) {
        val cur = currentTrack
        if (cur == null) false
        else if (cur.mediaType == MediaType.VIDEO || cur.filePath?.endsWith(".mp4", ignoreCase = true) == true) true
        else if (fallbackVidId.isNotBlank()) true
        else if (cur.filePath?.length == 11 && !cur.filePath.contains("/") && !cur.filePath.contains(".")) true
        else if (cur.albumArtUri?.toString()?.contains("/vi/") == true || cur.albumArtUri?.toString()?.contains("/vi_webp/") == true) true
        else ytCurrentVideo != null && ytCurrentVideo?.title.equals(cur.name, ignoreCase = true)
    }

    LaunchedEffect(currentTrack?.id, currentTrack?.name, isVideoMode) {
        if (isVideoMode && currentTrack != null) {
            viewModel.resolveVideoForCurrentTrack()
        }
    }
    val isDirectExoVideo = currentTrack?.mediaType == MediaType.VIDEO

    LaunchedEffect(currentTrack?.name) {
        val track = currentTrack
        if (track != null) {
            viewModel.fetchLyrics(track.name, track.artist)
        }
    }

    LaunchedEffect(sleepTimerTargetMs) {
        if (sleepTimerTargetMs > 0L) {
            while (System.currentTimeMillis() < sleepTimerTargetMs) {
                kotlinx.coroutines.delay(1000L)
            }
            connection.pause()
            sleepTimerTargetMs = 0L
        }
    }

    // Vinyl Disc Rotation Animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylRotation"
    )

    // Pulse Animation for Play Button
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.07f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appColors.background)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 25) {
                        onBack()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── UPPER MEDIA AREA (Framed below Top Bar) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (activeTab) {
                    0 -> {
                        if (isVideoMode) {
                            // ── VIDEO MODE: Full Bleed Borderless Video to Top Edge ──
                            val videoBoxModifier = when (videoResizeMode) {
                                1 -> Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                                2 -> Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .aspectRatio(21f / 9f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black)
                                else -> Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                            }

                            Box(
                                modifier = videoBoxModifier,
                                contentAlignment = Alignment.Center
                            ) {
                                // Backdrop artwork so video card is never black while loading
                                currentTrack?.albumArtUri?.let { artUri ->
                                    AsyncImage(
                                        model = artUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                if (isDirectExoVideo) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                useController = false
                                                useArtwork = false
                                                defaultArtwork = null
                                                resizeMode = when (videoResizeMode) {
                                                    1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                    2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                }
                                                connection.bindPlayerView(this)
                                            }
                                        },
                                        update = { view ->
                                            view.useArtwork = false
                                            view.resizeMode = when (videoResizeMode) {
                                                1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            }
                                        },
                                        onRelease = { view -> connection.unbindPlayerView(view) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (fallbackVidId.isNotBlank()) {
                                    com.musicdrop.app.ui.components.YouTubeIFramePlayer(
                                        videoId = fallbackVidId,
                                        resizeMode = videoResizeMode,
                                        isPlaying = isPlaying,
                                        currentPositionMs = positionMs,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        "Loading video...",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 14.sp
                                    )
                                }

                                // Bottom Overlay Controls: Fullscreen (Left) and Fill/Crop/Fit Mode (Right)
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // ⛶ Fullscreen Button
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.Black.copy(alpha = 0.72f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                                        onClick = { isFullscreenVideo = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Fullscreen,
                                                contentDescription = "Full Screen",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "Full Screen",
                                                color = Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // Size / Crop Switcher Chip
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.Black.copy(alpha = 0.72f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                                        onClick = {
                                            videoResizeMode = (videoResizeMode + 1) % 3
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = when (videoResizeMode) {
                                                    1 -> Icons.Rounded.Fullscreen
                                                    2 -> Icons.Rounded.FitScreen
                                                    else -> Icons.Rounded.AspectRatio
                                                },
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = when (videoResizeMode) {
                                                    1 -> "Fill (Crop)"
                                                    2 -> "Wide"
                                                    else -> "16:9 Fit"
                                                },
                                                color = Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // ── SONG MODE: 3 Cover Image / Artwork Display Options ──
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                appColors.surfaceElevated.copy(alpha = 0.6f),
                                                appColors.surface.copy(alpha = 0.8f),
                                                appColors.background
                                            )
                                        )
                                    )
                                    .clickable {
                                        coverArtMode = (coverArtMode + 1) % 3
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (coverArtMode) {
                                    0 -> {
                                        // ── OPTION 0: Classic Spinning Vinyl Turntable ──
                                        Box(
                                            modifier = Modifier
                                                .size(265.dp)
                                                .shadow(elevation = 18.dp, shape = CircleShape, ambientColor = Color.Black, spotColor = appColors.accentPrimary.copy(alpha = 0.65f))
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        listOf(
                                                            Color(0xFF282828),
                                                            Color(0xFF161616),
                                                            Color(0xFF0A0A0A),
                                                            Color(0xFF000000)
                                                        )
                                                    )
                                                )
                                                .border(4.dp, Color(0xFF333333), CircleShape)
                                                .rotate(if (isPlaying) rotationAngle else 0f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Vinyl Grooves
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val stroke = 1.dp.toPx()
                                                val color = Color(0x28FFFFFF)
                                                drawCircle(color = color, radius = size.minDimension * 0.48f, style = Stroke(width = stroke))
                                                drawCircle(color = color, radius = size.minDimension * 0.44f, style = Stroke(width = stroke))
                                                drawCircle(color = color, radius = size.minDimension * 0.40f, style = Stroke(width = stroke))
                                                drawCircle(color = color, radius = size.minDimension * 0.36f, style = Stroke(width = stroke))
                                            }

                                            // Center Album Artwork with accent Border
                                            Box(
                                                modifier = Modifier
                                                    .size(165.dp)
                                                    .clip(CircleShape)
                                                    .border(2.5.dp, appColors.accentPrimary, CircleShape)
                                                    .background(SurfaceDark),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val artUri = currentTrack?.albumArtUri
                                                if (artUri != null) {
                                                    AsyncImage(
                                                        model = artUri,
                                                        contentDescription = "Album Art",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Rounded.MusicNote,
                                                        contentDescription = null,
                                                        tint = appColors.accentPrimary,
                                                        modifier = Modifier.size(50.dp)
                                                    )
                                                }

                                                // Center Spindle Hole
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(DarkBg)
                                                        .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                    1 -> {
                                        // ── OPTION 1: Full-Bleed Edge-to-Edge Cover Art (Borderless Crop Fill) ──
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(appColors.surfaceElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val artUri = currentTrack?.albumArtUri
                                            if (artUri != null) {
                                                AsyncImage(
                                                    model = artUri,
                                                    contentDescription = "Full Cover Art",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                // Subtle fade into bottom controls
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(100.dp)
                                                        .align(Alignment.BottomCenter)
                                                        .background(
                                                            Brush.verticalGradient(
                                                                listOf(
                                                                    Color.Transparent,
                                                                    appColors.background.copy(alpha = 0.85f),
                                                                    appColors.background
                                                                )
                                                            )
                                                        )
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Rounded.MusicNote,
                                                    contentDescription = null,
                                                    tint = appColors.accentPrimary,
                                                    modifier = Modifier.size(72.dp)
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        // ── OPTION 2: Floating Card with Ambient Glow ──
                                        Box(
                                            modifier = Modifier
                                                .size(270.dp)
                                                .shadow(
                                                    elevation = 20.dp,
                                                    shape = RoundedCornerShape(22.dp),
                                                    spotColor = appColors.accentPrimary.copy(alpha = 0.75f),
                                                    ambientColor = appColors.accentSecondary.copy(alpha = 0.45f)
                                                )
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(appColors.surfaceElevated)
                                                .border(1.5.dp, appColors.accentPrimary.copy(alpha = 0.45f), RoundedCornerShape(22.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val artUri = currentTrack?.albumArtUri
                                            if (artUri != null) {
                                                AsyncImage(
                                                    model = artUri,
                                                    contentDescription = "Ambient Cover Card",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Rounded.MusicNote,
                                                    contentDescription = null,
                                                    tint = appColors.accentPrimary,
                                                    modifier = Modifier.size(60.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Floating Cover Option Switcher Badge (Bottom End)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.Black.copy(alpha = 0.68f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                                    onClick = {
                                        coverArtMode = (coverArtMode + 1) % 3
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 16.dp, bottom = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when (coverArtMode) {
                                                0 -> Icons.Rounded.Album
                                                1 -> Icons.Rounded.CropSquare
                                                else -> Icons.Rounded.AutoAwesome
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = when (coverArtMode) {
                                                0 -> "Vinyl"
                                                1 -> "Fill Cover (Crop)"
                                                else -> "Glow Card"
                                            },
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // ── UP NEXT QUEUE LIST (Well-Compacted & Themed) ──
                        var selectedUpNextFilter by remember { mutableStateOf("All") }
                        val upNextFilters = listOf("All", "Familiar", "Discover", "Romance", "Party", "Chill")

                        val filteredQueue = remember(upNextQueue, selectedUpNextFilter) {
                            if (selectedUpNextFilter == "All") upNextQueue
                            else upNextQueue.filter { track ->
                                when (selectedUpNextFilter) {
                                    "Romance" -> track.title.contains("love", ignoreCase = true) || track.title.contains("romantic", ignoreCase = true)
                                    "Party" -> track.title.contains("dance", ignoreCase = true) || track.title.contains("dj", ignoreCase = true) || track.title.contains("remix", ignoreCase = true)
                                    "Chill" -> track.title.contains("lo-fi", ignoreCase = true) || track.title.contains("chill", ignoreCase = true) || track.title.contains("acoustic", ignoreCase = true)
                                    "Familiar" -> track.channelTitle.isNotBlank()
                                    else -> true
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(top = 56.dp, start = 14.dp, end = 14.dp, bottom = 6.dp)
                        ) {
                            // Up Next Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Playing from",
                                        color = appColors.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                    Text(
                                        text = currentTrack?.album?.takeIf { it.isNotBlank() && it != currentTrack?.name } ?: (currentTrack?.name ?: "Up Next Queue"),
                                        color = appColors.textPrimary,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = appColors.accentPrimary.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.accentPrimary.copy(alpha = 0.35f)),
                                    onClick = { showAddToPlaylist = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.PlaylistAdd,
                                            contentDescription = null,
                                            tint = appColors.accentPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Save",
                                            color = appColors.accentPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Interactive Filter Pills (All, Familiar, Discover, Romance, Party, Chill)
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(upNextFilters) { filter ->
                                    val isSelected = filter == selectedUpNextFilter
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) appColors.accentPrimary else appColors.surfaceElevated.copy(alpha = 0.7f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) appColors.accentPrimary else appColors.surfaceBorder.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.clickable { selectedUpNextFilter = filter }
                                    ) {
                                        Text(
                                            text = filter,
                                            color = if (isSelected) (if (isBrightAccent) Color.Black else Color.White) else appColors.textSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            if (upNextQueue.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = appColors.accentPrimary, modifier = Modifier.size(28.dp))
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(filteredQueue) { item ->
                                        val isCurrent = currentTrack?.uri?.toString()?.contains(item.videoId) == true ||
                                                currentTrack?.name.equals(item.title, ignoreCase = true)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isCurrent) appColors.accentPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                                .clickable { viewModel.playTrackFromQueue(item) }
                                                .padding(horizontal = 6.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Compact 40dp Album Art
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF222222))
                                            ) {
                                                AsyncImage(
                                                    model = item.thumbnailUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                if (isCurrent) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.45f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.GraphicEq,
                                                            contentDescription = "Playing",
                                                            tint = appColors.accentPrimary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.width(10.dp))

                                            // Track Title & Channel / Duration
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    color = if (isCurrent) appColors.accentPrimary else appColors.textPrimary,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.height(1.dp))
                                                Text(
                                                    text = buildString {
                                                        append(item.channelTitle.ifBlank { "YouTube Music" })
                                                        if (item.duration.isNotBlank()) {
                                                            append(" • ")
                                                            append(item.duration)
                                                        }
                                                    },
                                                    color = appColors.textSecondary,
                                                    fontSize = 11.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Reorder Drag Handle (=)
                                            Icon(
                                                imageVector = Icons.Rounded.Menu,
                                                contentDescription = "Reorder",
                                                tint = appColors.textSecondary.copy(alpha = 0.4f),
                                                modifier = Modifier.size(18.dp).padding(start = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // ── LYRICS TAB ──
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(top = 56.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                lyricsLoading -> CircularProgressIndicator(color = appColors.accentPrimary)
                                lyrics?.plainLyrics?.isNotBlank() == true -> {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        item {
                                            Text(
                                                lyrics?.plainLyrics.orEmpty(),
                                                color = appColors.textPrimary,
                                                fontSize = 17.sp,
                                                lineHeight = 30.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                                else -> Text("No synced lyrics available for this song", color = appColors.textSecondary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // ── LOWER AREA: Controls, Car Mode, Lyrics, Up Next (Glassmorphic & Dynamically Themed) ──
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                // 1. Seamless Video/Music Merge Backdrop Layer matching active theme
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    appColors.background.copy(alpha = 0.72f),
                                    appColors.background.copy(alpha = 0.92f),
                                    appColors.background
                                )
                            )
                        )
                )

                // 2. Glowing Frosted Glass Top Hairline Divider
                Canvas(modifier = Modifier.matchParentSize()) {
                    // Shimmering horizontal top frosted hairline border
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                appColors.accentPrimary.copy(alpha = 0.45f),
                                appColors.accentSecondary.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset.Zero,
                        size = androidx.compose.ui.geometry.Size(size.width, 1.5.dp.toPx())
                    )
                }

                if (isDetailsShrunk && activeTab == 0) {
                    // ── SHRUNK MINIMAL MUSIC CONTROL BAR (Maximum Video / Cover Immersion) ──
                    val safeDuration = if (durationMs > 0) durationMs else 1L
                    val currentPos = if (sliderDragging >= 0f) (sliderDragging * safeDuration).toLong() else positionMs
                    val progress = (currentPos.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDetailsShrunk = false }
                            .navigationBarsPadding()
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(2.5.dp),
                            color = appColors.accentPrimary,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = currentTrack?.albumArtUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF282828))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTrack?.name ?: "No Track",
                                        color = appColors.textPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentTrack?.artist?.ifBlank { "YouTube Music" } ?: "Music Drop",
                                        color = appColors.textSecondary,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { connection.skipPrevious() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Prev", tint = appColors.textPrimary, modifier = Modifier.size(24.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .shadow(elevation = 8.dp, shape = CircleShape, spotColor = appColors.accentPrimary.copy(alpha = 0.65f))
                                        .clip(CircleShape)
                                        .background(appColors.accentPrimary)
                                        .clickable { connection.togglePlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = onActiveAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.playNextTrackFromQueue() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = appColors.textPrimary, modifier = Modifier.size(24.dp))
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.12f),
                                    onClick = { isDetailsShrunk = false },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowUp,
                                            contentDescription = "Expand",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 3. Interactive Content Column (Full Detailed View)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Collapse handle to shrink
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDetailsShrunk = true }
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                        }

                        if (activeTab == 0) {
                            Spacer(Modifier.height(6.dp))

                            // ── "NOW PLAYING" Header Label ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlaying) appColors.accentPrimary else Color.White.copy(alpha = 0.4f))
                                    )
                                    Text(
                                        text = "NOW PLAYING",
                                        color = appColors.accentPrimary.copy(alpha = 0.95f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.6.sp
                                    )
                                    IconButton(
                                        onClick = { isDetailsShrunk = true },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "Shrink",
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (hasVideoAvailable) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = appColors.accentPrimary.copy(alpha = 0.14f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.accentPrimary.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = if (isVideoMode) "HD VIDEO" else "HQ AUDIO",
                                            color = appColors.accentPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                                        )
                                    }
                                }
                            }

                        Spacer(Modifier.height(4.dp))

                        // Track Title (Marquee Scrollable for full detail) & Artist
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeTab = 1 }
                                ) {
                                    Text(
                                        text = currentTrack?.name ?: "No Track Selected",
                                        color = appColors.textPrimary,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .horizontalScroll(rememberScrollState())
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = appColors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = currentTrack?.artist?.ifBlank { "YouTube Music" } ?: "Music Drop",
                                    color = appColors.textSecondary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                )
                            }

                            // Like & Download buttons (Always allows independent audio & video downloads)
                            val isCurrentLiked = remember(currentTrack, viewModel.likedMusic.collectAsState().value) {
                                viewModel.isCurrentTrackLiked()
                            }
                            val downloadedTracks by viewModel.downloadedTracks.collectAsState()
                            val isAudioDownloaded = remember(currentTrack, downloadedTracks) {
                                viewModel.isCurrentTrackDownloaded()
                            }
                            val isVideoDownloaded = remember(currentTrack, downloadedTracks) {
                                viewModel.isCurrentTrackVideoDownloaded()
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = { viewModel.toggleLikeCurrentTrack() }) {
                                    Icon(
                                        imageVector = if (isCurrentLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (isCurrentLiked) appColors.accentSecondary else appColors.textSecondary,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    if (!isDownloadingCurrent) {
                                        showDownloadChoiceDialog = true
                                    }
                                }) {
                                    if (isDownloadingCurrent) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                    } else if (isAudioDownloaded && isVideoDownloaded) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Downloaded Both", tint = Color(0xFF4CAF50), modifier = Modifier.size(23.dp))
                                    } else if (isAudioDownloaded || isVideoDownloaded) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Downloaded", tint = appColors.accentPrimary, modifier = Modifier.size(23.dp))
                                    } else {
                                        Icon(Icons.Rounded.Download, contentDescription = "Download", tint = appColors.textSecondary, modifier = Modifier.size(23.dp))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // ── SEEK BAR & PROGRESS TIMELINE ──
                        val safeDuration = if (durationMs > 0) durationMs else 1L
                        val currentPos = if (sliderDragging >= 0f) (sliderDragging * safeDuration).toLong() else positionMs
                        val progress = (currentPos.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Slider(
                                value = progress,
                                onValueChange = { sliderDragging = it },
                                onValueChangeFinished = {
                                    if (sliderDragging >= 0f) {
                                        connection.seekTo((sliderDragging * safeDuration).toLong())
                                        sliderDragging = -1f
                                    }
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = appColors.accentPrimary,
                                    activeTrackColor = appColors.accentPrimary,
                                    inactiveTrackColor = appColors.surfaceBorder.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val currentSecs = (currentPos / 1000).coerceAtLeast(0)
                                val durationSecs = (safeDuration / 1000).coerceAtLeast(0)
                                Text(
                                    text = String.format(java.util.Locale.US, "%d:%02d", currentSecs / 60, currentSecs % 60),
                                    color = appColors.textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%d:%02d", durationSecs / 60, durationSecs % 60),
                                    color = appColors.textSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Compact & Premium Playback Controls Row (64dp button, perfectly balanced)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { connection.toggleShuffle() }, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffle) appColors.accentPrimary else appColors.textSecondary.copy(alpha = 0.45f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.playPreviousTrack() }, modifier = Modifier.size(44.dp)) {
                                Icon(
                                    Icons.Rounded.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = appColors.textPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            // 64dp Circular Play Button with Theme Glow & Accent Border
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .shadow(
                                        elevation = 14.dp,
                                        shape = CircleShape,
                                        spotColor = appColors.accentPrimary.copy(alpha = 0.70f),
                                        ambientColor = appColors.accentPrimary.copy(alpha = 0.40f)
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White,
                                                Color(0xFFEBEBEB)
                                            )
                                        )
                                    )
                                    .border(1.5.dp, appColors.accentPrimary.copy(alpha = 0.45f), CircleShape)
                                    .clickable { connection.togglePlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            IconButton(onClick = { viewModel.playNextTrackFromQueue() }, modifier = Modifier.size(44.dp)) {
                                Icon(
                                    Icons.Rounded.SkipNext,
                                    contentDescription = "Next",
                                    tint = appColors.textPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            IconButton(onClick = { connection.toggleRepeat() }, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    if (isRepeat) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                    contentDescription = "Repeat",
                                    tint = if (isRepeat) appColors.accentPrimary else appColors.textSecondary.copy(alpha = 0.45f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Compact & Refined Bottom Up Next Bar
                        Spacer(Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                                .clickable { activeTab = if (activeTab == 1) 0 else 1 }
                                .padding(top = 2.dp, bottom = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                            Spacer(Modifier.height(5.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Playing from",
                                        color = appColors.textSecondary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                    Text(
                                        text = if (upNextQueue.isNotEmpty()) "Continuous Radio Mix" else (currentTrack?.album?.takeIf { it.isNotBlank() && it != currentTrack?.name } ?: "Continuous Autoplay Mix"),
                                        color = appColors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // + Save pill button
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = appColors.accentPrimary.copy(alpha = 0.16f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.accentPrimary.copy(alpha = 0.35f)),
                                    onClick = { showAddToPlaylist = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistAdd,
                                            contentDescription = null,
                                            tint = appColors.accentPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            "Save",
                                            color = appColors.accentPrimary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // ── COMPACT CONTROLS FOR UP NEXT & LYRICS (Saves 80% screen space) ──
                        val safeDuration = if (durationMs > 0) durationMs else 1L
                        val progress = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

                        Spacer(Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(appColors.surfaceElevated.copy(alpha = 0.85f))
                                .border(1.dp, appColors.surfaceBorder.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = currentTrack?.albumArtUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF282828))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f).clickable { activeTab = 0 }) {
                                    Text(
                                        text = currentTrack?.name ?: "No Track",
                                        color = appColors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentTrack?.artist?.ifBlank { "YouTube Music" } ?: "",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { connection.skipPrevious() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Prev", tint = appColors.textPrimary, modifier = Modifier.size(24.dp))
                                }
                                IconButton(
                                    onClick = { connection.togglePlayPause() },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(appColors.accentPrimary)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = onActiveAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.playNextTrackFromQueue() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = appColors.textPrimary, modifier = Modifier.size(24.dp))
                                }
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(2.5.dp),
                                color = appColors.accentPrimary,
                                trackColor = Color.White.copy(alpha = 0.12f)
                            )
                        }
                    }
                }
            }
        }
    }

    // ── SLEEP TIMER DIALOG ────────────────────────────────────────────────────
    if (showSleepTimerModal) {
        Dialog(onDismissRequest = { showSleepTimerModal = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = appColors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.surfaceBorder),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Sleep Timer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )
                    Spacer(Modifier.height(16.dp))
                    listOf(
                        "15 minutes" to 15,
                        "30 minutes" to 30,
                        "45 minutes" to 45,
                        "60 minutes" to 60,
                        "Turn Off" to 0
                    ).forEach { (label, mins) ->
                        TextButton(
                            onClick = {
                                sleepTimerTargetMs = if (mins > 0) System.currentTimeMillis() + (mins * 60 * 1000L) else 0L
                                showSleepTimerModal = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                label,
                                color = if (mins == 0) VibrantCoral else ElectricLime,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // ── ADD TO PLAYLIST DIALOG ───────────────────────────────────────────────
    if (showAddToPlaylist && currentTrack != null) {
        val track = currentTrack!!
        val unified = UnifiedTrack.Local(
            key = track.filePath.orEmpty().ifBlank { track.name },
            title = track.name,
            artist = track.artist,
            duration = track.formattedDuration,
            thumbnailUrl = track.albumArtUri?.toString().orEmpty(),
            filePath = track.filePath.orEmpty()
        )
        AddToPlaylistDialog(
            track = unified,
            viewModel = viewModel,
            onDismiss = { showAddToPlaylist = false },
            onAdded = { showAddToPlaylist = false }
        )
    }


    // ── FLOATING TOP ACTION BAR OVERLAY (Video & Cover Art Bleed Edge-To-Edge Underneath) ──
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
    ) {
        // Gradient Scrim for readable icons over any bright or dark video
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

                // ── TOP ACTION BAR (Cleanly framed below Status Bar) ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Collapse",
                                tint = appColors.textPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // (▶) MusicDrop YouTube-Music Style Brand Badge
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF0000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Text(
                                    text = "MusicDrop",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }
                    }
    
                    // Audio / Video Pill Switcher [ 🎧 Headphones ] [ ▶ Video ]
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(appColors.surfaceElevated.copy(alpha = 0.85f))
                            .border(1.dp, appColors.surfaceBorder.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Song Mode [🎧 Headphones]
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (!isVideoMode) appColors.accentPrimary else Color.Transparent)
                                .clickable {
                                    if (isVideoMode) viewModel.toggleVideoMode()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = "Song Mode",
                                tint = if (!isVideoMode) (if (isBrightAccent) Color.Black else Color.White) else appColors.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
    
                        // Video Mode [▶ Video Screen]
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isVideoMode) appColors.accentPrimary else Color.Transparent)
                                .clickable {
                                    if (!isVideoMode) viewModel.toggleVideoMode()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (videoModeLoading) {
                                CircularProgressIndicator(
                                    color = if (isVideoMode) (if (isBrightAccent) Color.Black else Color.White) else appColors.textPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.SmartDisplay,
                                    contentDescription = "Video Mode",
                                    tint = if (isVideoMode) (if (isBrightAccent) Color.Black else Color.White) else appColors.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
    
                    // Right: Up Next Queue Button, Cast & 3-Dots Menu
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Up Next Queue Button in Top Bar
                        IconButton(
                            onClick = { activeTab = if (activeTab == 1) 0 else 1 },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (activeTab == 1) appColors.accentPrimary.copy(alpha = 0.22f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "Up Next Queue",
                                tint = if (activeTab == 1) appColors.accentPrimary else appColors.textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
    
                        IconButton(onClick = {
                            android.widget.Toast.makeText(context, "Cast to device available on local Wi-Fi", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Cast,
                                contentDescription = "Cast",
                                tint = appColors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
    
                        Box {
                            var showMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = appColors.textPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(appColors.surfaceElevated)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (activeTab == 1) "View Player" else "View Up Next (${upNextQueue.size})", color = appColors.textPrimary) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = appColors.textSecondary) },
                                    onClick = {
                                        showMenu = false
                                        activeTab = if (activeTab == 1) 0 else 1
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (activeTab == 2) "View Player" else "View Synced Lyrics", color = appColors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Rounded.Lyrics, contentDescription = null, tint = appColors.textSecondary) },
                                    onClick = {
                                        showMenu = false
                                        activeTab = if (activeTab == 2) 0 else 2
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Car Screen Off (OLED)", color = appColors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = appColors.textSecondary) },
                                    onClick = {
                                        showMenu = false
                                        isOledScreenOff = true
                                    }
                                )
                                if (isVideoMode) {
                                    DropdownMenuItem(
                                        text = {
                                            val modeName = when (videoResizeMode) {
                                                1 -> "Video Fit: Fill (Zoom)"
                                                2 -> "Video Fit: Stretch (Wide)"
                                                else -> "Video Fit: Standard (16:9)"
                                            }
                                            Text(modeName, color = appColors.textPrimary)
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.AspectRatio, contentDescription = null, tint = appColors.textSecondary) },
                                        onClick = {
                                            videoResizeMode = (videoResizeMode + 1) % 3
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Sleep Timer", color = appColors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null, tint = appColors.textSecondary) },
                                    onClick = {
                                        showMenu = false
                                        showSleepTimerModal = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Playback Speed (${currentSpeed}x)", color = appColors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null, tint = appColors.textSecondary) },
                                    onClick = {
                                        currentSpeed = when (currentSpeed) {
                                            1.0f -> 1.25f
                                            1.25f -> 1.5f
                                            1.5f -> 2.0f
                                            2.0f -> 0.75f
                                            else -> 1.0f
                                        }
                                        connection.setPlaybackSpeed(currentSpeed)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Audio Equalizer", color = appColors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = appColors.textSecondary) },
                                    onClick = {
                                        showMenu = false
                                        try {
                                            val eqIntent = android.content.Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                                putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                                putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC)
                                            }
                                            context.startActivity(eqIntent)
                                        } catch (_: Exception) {
                                            android.widget.Toast.makeText(context, "Bass Boost & Equalizer active", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add to Playlist", color = appColors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = appColors.textSecondary) },
                                    onClick = {
                                        showMenu = false
                                        showAddToPlaylist = true
                                    }
                                )
                            }
                        }
                    }
                }
    }

        // ── FULL SCREEN VIDEO IMMERSIVE OVERLAY ────────────────────────────────
    if (isFullscreenVideo) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (isDirectExoVideo) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            useArtwork = false
                            defaultArtwork = null
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            connection.bindPlayerView(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (fallbackVidId.isNotBlank()) {
                com.musicdrop.app.ui.components.YouTubeIFramePlayer(
                    videoId = fallbackVidId,
                    resizeMode = 0,
                    isPlaying = isPlaying,
                    currentPositionMs = positionMs,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Bar Overlay with Title and Exit Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentTrack?.name.orEmpty(),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
                IconButton(
                    onClick = { isFullscreenVideo = false },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    // ── OLED SCREEN OFF (BLACKOUT) FULLSCREEN COVER ──────────────────────────
    if (isOledScreenOff) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { isOledScreenOff = false },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "🌙 Screen Off (OLED Power Saving)",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Video & Audio playing • Tap anywhere to wake",
                    color = Color.White.copy(alpha = 0.22f),
                    fontSize = 12.sp
                )
            }
        }
    }

    // ── Download Choice Dialog (Audio vs Video MP4 HD) ───────────────────
    if (showDownloadChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadChoiceDialog = false },
            title = {
                Text(
                    text = "Download Options",
                    color = appColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = currentTrack?.name ?: "Current Track",
                        color = appColors.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(16.dp))

                    // Option 1: Music (Audio)
                    val isAudioDownloadedInDialog = viewModel.isCurrentTrackDownloaded()
                    Surface(
                        onClick = {
                            showDownloadChoiceDialog = false
                            isDownloadingCurrent = true
                            viewModel.downloadCurrentTrack { success, _ ->
                                isDownloadingCurrent = false
                                android.widget.Toast.makeText(
                                    context,
                                    if (success) "Downloaded Music: ${currentTrack?.name}" else "Download failed",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = appColors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAudioDownloadedInDialog) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)),
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
                                    .background(if (isAudioDownloadedInDialog) Color(0xFF4CAF50).copy(alpha = 0.18f) else appColors.accentPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAudioDownloadedInDialog) Icons.Rounded.CheckCircle else Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = if (isAudioDownloadedInDialog) Color(0xFF4CAF50) else appColors.accentPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAudioDownloadedInDialog) "Music (Audio) ✓ Downloaded" else "Download Music (Audio)",
                                    color = appColors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isAudioDownloadedInDialog) "Already saved offline • Tap to re-download" else "Fast download for offline music listening",
                                    color = appColors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Option 2: Video (MP4 HD)
                    val isVideoDownloadedInDialog = viewModel.isCurrentTrackVideoDownloaded()
                    Surface(
                        onClick = {
                            showDownloadChoiceDialog = false
                            isDownloadingCurrent = true
                            viewModel.downloadCurrentVideo { success, _ ->
                                isDownloadingCurrent = false
                                android.widget.Toast.makeText(
                                    context,
                                    if (success) "Downloaded Video: ${currentTrack?.name}" else "Video download failed",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = appColors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isVideoDownloadedInDialog) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)),
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
                                    .background(if (isVideoDownloadedInDialog) Color(0xFF4CAF50).copy(alpha = 0.18f) else Color(0xFFFF3D00).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isVideoDownloadedInDialog) Icons.Rounded.CheckCircle else Icons.Rounded.Videocam,
                                    contentDescription = null,
                                    tint = if (isVideoDownloadedInDialog) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isVideoDownloadedInDialog) "Video (MP4 HD) ✓ Downloaded" else "Download Video (MP4 HD)",
                                    color = appColors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isVideoDownloadedInDialog) "Already saved offline • Tap to re-download" else "Official music video in HD for offline viewing",
                                    color = appColors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDownloadChoiceDialog = false }) {
                    Text("Cancel", color = appColors.accentPrimary)
                }
            },
            containerColor = appColors.surfaceElevated
        )
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
