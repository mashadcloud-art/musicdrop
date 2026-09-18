package com.musicdrop.app

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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.musicdrop.app.data.repository.MusiXServerRepository
import com.musicdrop.app.ui.screens.*
import com.musicdrop.app.ui.theme.DarkBg
import com.musicdrop.app.ui.theme.FileDropTheme
import com.musicdrop.app.ui.theme.LocalAppColors
import com.musicdrop.app.ui.viewmodel.MainViewModel

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.ui.viewinterop.AndroidView

enum class NavigationTab(val label: String, val icon: ImageVector) {
    HOME    ("Home",    Icons.Default.Home),
    EXPLORE ("Explore", Icons.Outlined.Explore),
    LIBRARY ("Library", Icons.Default.LibraryMusic)
}

class MainActivity : ComponentActivity() {

    companion object {
        var hasShownOpeningSplashThisProcess = false
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
            val appTheme by viewModel.appTheme.collectAsState()
            val availableUpdate by viewModel.availableUpdate.collectAsState()
            val updateProgress by viewModel.updateDownloadProgress.collectAsState()
            var showOpeningSplash by remember { mutableStateOf(shouldShowSplash) }

            FileDropTheme(themeMode = appTheme) {
                val appColors = LocalAppColors.current
                Surface(modifier = Modifier.fillMaxSize(), color = appColors.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainAppContent(viewModel = viewModel)

                        availableUpdate?.let { updateInfo ->
                            com.musicdrop.app.ui.components.AppUpdateDialog(
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
    var openArtist by remember { mutableStateOf<com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist?>(null) }
    var openAlbum by remember { mutableStateOf<com.musicdrop.app.data.repository.YtMusicApiRepository.YtCardItem?>(null) }
    var showSettingsOverlay by remember { mutableStateOf(false) }

    val currentTrack by viewModel.playbackConnection.currentTrack.collectAsState()
    val isPlaying by viewModel.playbackConnection.isPlaying.collectAsState()
    val positionMs by viewModel.playbackConnection.currentPositionMs.collectAsState()
    val durationMs by viewModel.playbackConnection.durationMs.collectAsState()
    val showFullPlayer by viewModel.showFullPlayer.collectAsState()
    val appColors = LocalAppColors.current



    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = appColors.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(appColors.background)
                ) {
                    // Persistent Mini Player Bar docked directly above the 3 tabs
                    if (!showFullPlayer && currentTrack != null) {
                        com.musicdrop.app.ui.components.MiniPlayerBar(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            progressMs = positionMs,
                            durationMs = durationMs,
                            onClick = { viewModel.openFullPlayer() },
                            onPlayPause = { viewModel.playbackConnection.togglePlayPause() },
                            onSkipNext = { viewModel.playbackConnection.skipNext() }
                        )
                    }

                    // Sleek YouTube Music 3-Tab Bottom Nav (Home, Explore, Library)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavigationTab.values().forEach { tab ->
                            val isSelected = currentTab == tab
                            val itemColor = if (isSelected) appColors.textPrimary else appColors.textSecondary
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { currentTab = tab },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = itemColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = tab.label,
                                    color = itemColor,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
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
                    NavigationTab.HOME -> DiscoverScreen(
                        viewModel = viewModel,
                        onOpenSearchWithQuery = { query ->
                            searchPreFill = query
                            viewModel.setYtSearchQuery(query)
                            isSearchOpen = true
                        },
                        onOpenPlaylist = { playlist -> openPlaylist = playlist },
                        onOpenSource = { source -> openSource = source },
                        onOpenArtist = { openArtist = it },
                        onOpenAlbum = { openAlbum = it },
                        onOpenSettings = { showSettingsOverlay = true }
                    )
                    NavigationTab.EXPLORE -> ExploreScreen(
                        viewModel = viewModel,
                        onOpenSearch = { query ->
                            searchPreFill = query
                            if (query.isNotBlank()) viewModel.setYtSearchQuery(query)
                            isSearchOpen = true
                        }
                    )
                    NavigationTab.LIBRARY -> LibraryScreen(
                        viewModel = viewModel,
                        onOpenSearch = { query ->
                            searchPreFill = query
                            if (query.isNotBlank()) viewModel.setYtSearchQuery(query)
                            isSearchOpen = true
                        },
                        onOpenArtist = { openArtist = it },
                        onOpenAlbum = { openAlbum = it }
                    )
                }
            }
        }

        // Search Overlay
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
                    initialArtist = albumSnapshot.type.orEmpty(),
                    initialThumb = albumSnapshot.thumbnailUrl.orEmpty(),
                    viewModel = viewModel,
                    onBack = { openAlbum = null },
                    onOpenArtist = { channelId, name ->
                        openArtist = com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist(
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
