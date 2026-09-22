package com.musicdrop.tv

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.musicdrop.tv.data.repository.MusiXServerRepository
import com.musicdrop.tv.ui.screens.*
import com.musicdrop.tv.ui.theme.DarkBg
import com.musicdrop.tv.ui.theme.FileDropTheme
import com.musicdrop.tv.ui.theme.LocalAppColors
import com.musicdrop.tv.ui.viewmodel.MainViewModel

import androidx.compose.animation.fadeIn
import androidx.compose.ui.zIndex
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.ui.viewinterop.AndroidView

import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.focusable
import androidx.compose.ui.platform.LocalContext

enum class NavigationTab(val label: String, val icon: ImageVector) {
    HOME    ("Home",    Icons.Default.Home),
    SEARCH  ("Search",  Icons.Default.Search),
    EXPLORE ("Explore", Icons.Outlined.Explore),
    LIBRARY ("Library", Icons.Default.LibraryMusic)
}

class MainActivity : ComponentActivity() {

    companion object {
        var hasShownOpeningSplashThisProcess = false
    }

    private var activeViewModel: MainViewModel? = null
    var onTvBackPressed: (() -> Boolean)? = null

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            val vm = activeViewModel
            if (vm?.showFullPlayer?.value == true) {
                when (event.keyCode) {
                    android.view.KeyEvent.KEYCODE_BACK -> {
                        if (onTvBackPressed?.invoke() == true) return true
                        vm.closeFullPlayer()
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                    android.view.KeyEvent.KEYCODE_ENTER,
                    android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                    android.view.KeyEvent.KEYCODE_BUTTON_A,
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        vm.playbackConnection.togglePlayPause()
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        vm.playbackConnection.play()
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        vm.playbackConnection.pause()
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        vm.playbackConnection.skipNext()
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        vm.playbackConnection.skipPrevious()
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        val pos = vm.playbackConnection.currentPositionMs.value
                        val dur = vm.playbackConnection.durationMs.value
                        val target = if (dur > 0) minOf(dur, pos + 15_000L) else pos + 15_000L
                        vm.playbackConnection.seekTo(target)
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        val pos = vm.playbackConnection.currentPositionMs.value
                        val target = maxOf(0L, pos - 15_000L)
                        vm.playbackConnection.seekTo(target)
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        val pos = vm.playbackConnection.currentPositionMs.value
                        val dur = vm.playbackConnection.durationMs.value
                        val target = if (dur > 0) minOf(dur, pos + 10_000L) else pos + 10_000L
                        vm.playbackConnection.seekTo(target)
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                        val pos = vm.playbackConnection.currentPositionMs.value
                        val target = maxOf(0L, pos - 10_000L)
                        vm.playbackConnection.seekTo(target)
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                        vm.setVideoMode(!vm.isVideoMode.value)
                        return true
                    }
                }
            } else {
                when (event.keyCode) {
                    android.view.KeyEvent.KEYCODE_BACK -> {
                        if (onTvBackPressed?.invoke() == true) return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        vm?.playbackConnection?.togglePlayPause()
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        vm?.playbackConnection?.skipNext()
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        vm?.playbackConnection?.skipPrevious()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        activeViewModel?.setIsInPipMode(isInPictureInPictureMode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemNavigationBar()

        val shouldShowSplash = !hasShownOpeningSplashThisProcess && savedInstanceState == null
        if (shouldShowSplash) {
            hasShownOpeningSplashThisProcess = true
        }

        // Request notification permission for Android 13+ (needed for lock-screen playback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val viewModel: MainViewModel = viewModel()
            activeViewModel = viewModel
            val appTheme by viewModel.appTheme.collectAsState()
            val cardOpacity by viewModel.cardOpacity.collectAsState()
            val appFontFamily by viewModel.appFontFamily.collectAsState()
            val appFontColorOption by viewModel.appFontColorOption.collectAsState()
            val currentWallpaperUri by viewModel.themeWallpaperUri.collectAsState()
            val availableUpdate by viewModel.availableUpdate.collectAsState()
            val updateProgress by viewModel.updateDownloadProgress.collectAsState()
            val networkBanner by viewModel.networkStatusBanner.collectAsState()
            var showOpeningSplash by remember { mutableStateOf(shouldShowSplash) }

            FileDropTheme(
                themeMode = appTheme,
                cardOpacity = cardOpacity,
                fontFamilyName = appFontFamily,
                fontColorOption = appFontColorOption
            ) {
                val appColors = LocalAppColors.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (currentWallpaperUri != null) Color.Black else appColors.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (currentWallpaperUri != null) {
                            AsyncImage(
                                model = currentWallpaperUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Ambient dark scrim for high contrast and readability over photos
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = if (appColors.isDark) 0.38f else 0.18f))
                            )
                        }

                        MainAppContent(viewModel = viewModel)

                        // Floating Offline / Online Status Banner
                        AnimatedVisibility(
                            visible = networkBanner != null,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .zIndex(100f)
                        ) {
                            networkBanner?.let { message ->
                                val isBackOnline = message.contains("back online", ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isBackOnline) Color(0xFF059669) else Color(0xFFDC2626),
                                    shadowElevation = 8.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isBackOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = message,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.dismissNetworkBanner() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Dismiss",
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        availableUpdate?.let { updateInfo ->
                            com.musicdrop.tv.ui.components.AppUpdateDialog(
                                updateInfo = updateInfo,
                                progress = updateProgress,
                                onUpdate = { viewModel.startAppUpdate(this@MainActivity) },
                                onDismiss = { viewModel.dismissUpdate() }
                            )
                        }

                        if (shouldShowSplash) {
                            AnimatedVisibility(
                                visible = showOpeningSplash,
                                enter = fadeIn(),
                                exit = fadeOut(animationSpec = tween(650))
                            ) {
                                OpeningSplashScreen(
                                    isDark = appColors.isDark,
                                    onFinished = { showOpeningSplash = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hideSystemNavigationBar() {
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        } catch (_: Throwable) {}
    }

    override fun onResume() {
        super.onResume()
        hideSystemNavigationBar()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemNavigationBar()
        }
    }
}

@Composable
fun OpeningSplashScreen(
    isDark: Boolean = true,
    onFinished: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isDone by remember { mutableStateOf(false) }
    var isFirstFrameRendered by remember { mutableStateOf(false) }

    val isSystemDark = try {
        (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    } catch (_: Exception) { false }

    val resolvedDark = try {
        isDark || isSystemDark
    } catch (_: Exception) { true }

    val videoResId = if (resolvedDark) R.raw.opening_video_dark else R.raw.opening_video

    val rawUri = try {
        androidx.media3.datasource.RawResourceDataSource.buildRawResourceUri(videoResId)
    } catch (_: Exception) {
        android.net.Uri.parse("android.resource://${context.packageName}/$videoResId")
    }

    val exoPlayer = remember(videoResId) {
        try {
            androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                val mediaItem = androidx.media3.common.MediaItem.fromUri(rawUri)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
                repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
                volume = 0.85f
            }
        } catch (_: Throwable) {
            null
        }
    }

    LaunchedEffect(exoPlayer) {
        if (exoPlayer == null && !isDone) {
            isDone = true
            onFinished()
        }
    }

    DisposableEffect(exoPlayer) {
        if (exoPlayer == null) return@DisposableEffect onDispose {}
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onRenderedFirstFrame() {
                isFirstFrameRendered = true
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    if (!isDone) {
                        isDone = true
                        onFinished()
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (!isDone) {
                    isDone = true
                    onFinished()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            try {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            } catch (_: Throwable) {}
        }
    }

    // Generous safety timer (8.5s) to allow the full 7.12s video animation to play completely
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(8500L)
        if (!isDone) {
            isDone = true
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (!isDone) {
                    isDone = true
                    onFinished()
                }
            }
    ) {
        // 1. Video Player Surface (Direct playback without any cover image)
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    try {
                        androidx.media3.ui.PlayerView(ctx).apply {
                            useController = false
                            useArtwork = false
                            defaultArtwork = null
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            player = exoPlayer
                        }
                    } catch (_: Throwable) {
                        android.view.View(ctx)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Subtle Skip Button
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            onClick = {
                if (!isDone) {
                    isDone = true
                    onFinished()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Text(
                text = "Skip",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchPreFill by remember { mutableStateOf("") }
    // Which curated MusiX playlist (if any) is open in full-screen detail view
    var openPlaylist by remember { mutableStateOf<MusiXServerRepository.CuratedPlaylist?>(null) }
    // Which single-source page (YouTube/JioSaavn/Vimeo/Apple Music/Spotify) is open
    var openSource by remember { mutableStateOf<MusicSource?>(null) }
    var openArtist by remember { mutableStateOf<com.musicdrop.tv.data.repository.YtMusicApiRepository.YtChartArtist?>(null) }
    var openAlbum by remember { mutableStateOf<com.musicdrop.tv.data.repository.YtMusicApiRepository.YtCardItem?>(null) }
    var showSettingsOverlay by remember { mutableStateOf(false) }

    val currentTrack by viewModel.playbackConnection.currentTrack.collectAsState()
    val isPlaying by viewModel.playbackConnection.isPlaying.collectAsState()
    val positionMs by viewModel.playbackConnection.currentPositionMs.collectAsState()
    val durationMs by viewModel.playbackConnection.durationMs.collectAsState()
    val showFullPlayer by viewModel.showFullPlayer.collectAsState()
    val showBottomNav by viewModel.showBottomNav.collectAsState()
    val appColors = LocalAppColors.current

    val activity = LocalContext.current as? MainActivity
    DisposableEffect(currentTab, isSearchOpen, openPlaylist, openSource, openArtist, openAlbum, showSettingsOverlay, showFullPlayer) {
        activity?.onTvBackPressed = {
            when {
                showFullPlayer -> {
                    viewModel.closeFullPlayer()
                    true
                }
                openPlaylist != null -> {
                    openPlaylist = null
                    true
                }
                openSource != null -> {
                    openSource = null
                    true
                }
                openArtist != null -> {
                    openArtist = null
                    true
                }
                openAlbum != null -> {
                    openAlbum = null
                    true
                }
                showSettingsOverlay -> {
                    showSettingsOverlay = false
                    true
                }
                isSearchOpen -> {
                    isSearchOpen = false
                    true
                }
                currentTab != NavigationTab.HOME -> {
                    currentTab = NavigationTab.HOME
                    true
                }
                else -> false
            }
        }
        onDispose {
            activity?.onTvBackPressed = null
        }
    }

    val isAnyOverlayOpen = (showFullPlayer && currentTrack != null) || openPlaylist != null || openSource != null || openArtist != null || openAlbum != null || isSearchOpen || showSettingsOverlay

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = !isAnyOverlayOpen },
            containerColor = appColors.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(appColors.background)
                ) {
                    // Persistent Mini Player Bar docked directly above edge or tabs
                    if (!showFullPlayer && currentTrack != null) {
                        com.musicdrop.tv.ui.components.MiniPlayerBar(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            progressMs = positionMs,
                            durationMs = durationMs,
                            onClick = { viewModel.openFullPlayer() },
                            onPlayPause = { viewModel.playbackConnection.togglePlayPause() },
                            onSkipNext = { viewModel.playbackConnection.skipNext() },
                            onSeek = { seekPos -> viewModel.playbackConnection.seekTo(seekPos) },
                            modifier = if (!showBottomNav) Modifier.navigationBarsPadding() else Modifier
                        )
                    }

                    // Sleek TV Navigation Bar with High-Contrast Remote Focus Indicators
                    if (showBottomNav) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .height(56.dp)
                                .background(appColors.surfaceElevated.copy(alpha = 0.95f)),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavigationTab.values().forEach { tab ->
                                val isSelected = currentTab == tab
                                var isFocused by remember { mutableStateOf(false) }
                                val itemColor = if (isSelected || isFocused) appColors.accentPrimary else appColors.textSecondary
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(vertical = 4.dp, horizontal = 6.dp)
                                        .onFocusChanged { isFocused = it.isFocused }
                                        .focusable()
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                                            color = if (isFocused) Color.White else if (isSelected) appColors.accentPrimary.copy(alpha = 0.5f) else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .background(
                                            if (isFocused) Color.White.copy(alpha = 0.18f) else if (isSelected) appColors.accentPrimary.copy(alpha = 0.12f) else Color.Transparent
                                        )
                                        .clickable { currentTab = tab },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = itemColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = tab.label,
                                        color = itemColor,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                when (currentTab) {
                    NavigationTab.HOME, NavigationTab.LIBRARY -> LibraryScreen(
                        viewModel = viewModel,
                        onOpenSearch = { query ->
                            searchPreFill = query
                            viewModel.setYtSearchQuery(query)
                            currentTab = NavigationTab.SEARCH
                        },
                        onOpenPlaylist = { playlist -> openPlaylist = playlist },
                        onOpenSource = { source -> openSource = source },
                        onOpenArtist = { openArtist = it },
                        onOpenAlbum = { openAlbum = it },
                        onOpenSettings = { showSettingsOverlay = true }
                    )
                    NavigationTab.SEARCH -> SearchDashboardScreen(
                        viewModel = viewModel,
                        initialQuery = searchPreFill,
                        onBack = { currentTab = NavigationTab.HOME },
                        onOpenYouTube = {},
                        onOpenArtist = { openArtist = it },
                        onOpenAlbum = { openAlbum = it }
                    )
                    NavigationTab.EXPLORE -> ExploreScreen(
                        viewModel = viewModel,
                        onOpenSearch = { query ->
                            searchPreFill = query
                            if (query.isNotBlank()) viewModel.setYtSearchQuery(query)
                            currentTab = NavigationTab.SEARCH
                        }
                    )
                }
            }
        }

        // Search Overlay (if opened as modal)
        if (isSearchOpen) {
            Box(modifier = Modifier.fillMaxSize()) {
                SearchDashboardScreen(
                    viewModel = viewModel,
                    initialQuery = searchPreFill,
                    onBack = { isSearchOpen = false },
                    onOpenYouTube = {},
                    onOpenArtist = { openArtist = it },
                    onOpenAlbum = { openAlbum = it }
                )
            }
        }

        // Curated Playlist Detail Overlay — tapping a "Featured Playlist" card on
        // Discover opens here, with every track playable AND downloadable.
        val playlistSnapshot = openPlaylist
        AnimatedVisibility(
            visible = playlistSnapshot != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            if (playlistSnapshot != null) {
                PlaylistDetailScreen(
                    playlist = playlistSnapshot,
                    viewModel = viewModel,
                    onBack = { openPlaylist = null }
                )
            }
        }

        // Single-source page overlay — YouTube/JioSaavn/Vimeo/Apple Music/Spotify,
        // each browsable and searchable on its own, separate from the blended feeds.
        val sourceSnapshot = openSource
        AnimatedVisibility(
            visible = sourceSnapshot != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            if (sourceSnapshot != null) {
                MusicSourcePage(
                    source = sourceSnapshot,
                    viewModel = viewModel,
                    onBack = { openSource = null }
                )
            }
        }

        // Dedicated YouTube Music Artist Discography Overlay
        val artistSnapshot = openArtist
        AnimatedVisibility(
            visible = artistSnapshot != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            if (artistSnapshot != null) {
                ArtistDetailScreen(
                    channelId = artistSnapshot.browseId,
                    initialName = artistSnapshot.title,
                    initialThumb = artistSnapshot.thumbnailUrl,
                    viewModel = viewModel,
                    onBack = { openArtist = null },
                    onOpenAlbum = { album -> openAlbum = album }
                )
            }
        }

        // Dedicated YouTube Music Album Tracklist Overlay
        val albumSnapshot = openAlbum
        AnimatedVisibility(
            visible = albumSnapshot != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            if (albumSnapshot != null) {
                AlbumDetailScreen(
                    browseId = albumSnapshot.browseId.orEmpty(),
                    initialTitle = albumSnapshot.title,
                    initialArtist = albumSnapshot.artistName?.ifBlank { null } ?: albumSnapshot.type.orEmpty(),
                    initialThumb = albumSnapshot.thumbnailUrl.orEmpty(),
                    viewModel = viewModel,
                    onBack = { openAlbum = null },
                    onOpenArtist = { channelId, name ->
                        openArtist = com.musicdrop.tv.data.repository.YtMusicApiRepository.YtChartArtist(
                            rank = "1",
                            title = name,
                            browseId = channelId,
                            subscribers = "",
                            thumbnailUrl = "",
                            trend = "up"
                        )
                    }
                )
            }
        }

        // Dedicated Theme & Settings Overlay (from long-pressing top brand logo)
        AnimatedVisibility(
            visible = showSettingsOverlay,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            MoreSettingsScreen(
                viewModel = viewModel,
                onBack = { showSettingsOverlay = false }
            )
        }



        // Full Screen Vinyl Music Player Overlay
        AnimatedVisibility(
            visible = showFullPlayer && currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            MusicPlayerScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeFullPlayer() }
            )
        }
    }
}
