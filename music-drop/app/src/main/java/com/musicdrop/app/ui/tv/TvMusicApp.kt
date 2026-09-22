package com.musicdrop.app.ui.tv

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import com.musicdrop.app.ui.viewmodel.MainViewModel

enum class TvScreen { HOME, BROWSE, SEARCH, DOWNLOADS, SETTINGS }

data class TvNavItem(
    val screen: TvScreen,
    val icon: ImageVector,
    val label: String
)

private val NAV_ITEMS = listOf(
    TvNavItem(TvScreen.SEARCH,    Icons.Filled.Search,       "Search"),
    TvNavItem(TvScreen.HOME,      Icons.Filled.Home,         "Home"),
    TvNavItem(TvScreen.DOWNLOADS, Icons.Filled.Download,     "Downloads"),
    TvNavItem(TvScreen.BROWSE,    Icons.Filled.Category,     "Music & Mixes"),
    TvNavItem(TvScreen.SETTINGS,  Icons.Filled.Settings,     "Settings")
)

/**
 * Root TV app scaffold styled like YouTube on Android TV (Reference Photos 1 & 2):
 *  - Left navigation rail with user avatar circle
 *  - Selected navigation pill is solid white with dark icon & text
 *  - Content area displays cinema 16:9 feed
 *  - Native ExoPlayer video overlay slides on top during playback
 */
@Composable
fun TvMusicApp(
    viewModel: MainViewModel,
    onSwitchToMobile: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(TvScreen.HOME) }
    val currentTrack by viewModel.playbackConnection.currentTrack.collectAsState()
    val isPlaying by viewModel.playbackConnection.isPlaying.collectAsState()
    var showPlayer by remember { mutableStateOf(false) }

    // Helper: plays a YouTube search result directly with native ExoPlayer video stream
    val onPlaySong: (YouTubeSearchResult) -> Unit = { track ->
        viewModel.playYouTubeVideo(track, preferVideo = true)
        showPlayer = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // ─── Sidebar Navigation (Matches Photo 1 & 2) ─────────────────────────
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF0A0A0E))
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // User Avatar Header (Photo 1)
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF3B82F6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("M", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text(
                            "MusicDrop",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Android TV",
                            fontSize = 12.sp,
                            color = Color.White.copy(0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Navigation Item List (Solid White Rounded Pill when selected - Photo 1)
                NAV_ITEMS.forEach { item ->
                    val isSelected = currentScreen == item.screen
                    TvFocusButton(
                        onClick = { currentScreen = item.screen },
                        cornerRadius = 24.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Color.White else Color.Transparent,
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) Color.Black else Color.White.copy(0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                item.label,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color.White.copy(0.85f)
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Bottom Now Playing Mini-Player shortcut
                AnimatedVisibility(visible = currentTrack != null) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                        HorizontalDivider(color = Color.White.copy(0.1f), modifier = Modifier.padding(bottom = 10.dp))
                        TvFocusButton(
                            onClick = { showPlayer = true },
                            cornerRadius = 14.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = currentTrack?.albumArtUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        currentTrack?.name ?: "",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "🎬 Playing Video",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFF0033),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ─── Main Content Area ───────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (currentScreen) {
                    TvScreen.HOME -> TvHomeScreen(
                        viewModel = viewModel,
                        onPlaySong = onPlaySong,
                        onOpenPlayer = { showPlayer = true },
                        onOpenSearch = { currentScreen = TvScreen.SEARCH },
                        onSelectCategory = { query ->
                            viewModel.setYtSearchQuery(query)
                            currentScreen = TvScreen.BROWSE
                        }
                    )
                    TvScreen.BROWSE -> TvBrowseScreen(viewModel = viewModel, onPlaySong = onPlaySong)
                    TvScreen.SEARCH -> TvSearchScreen(viewModel = viewModel, onPlaySong = onPlaySong)
                    TvScreen.DOWNLOADS -> TvDownloadsScreen(
                        viewModel = viewModel,
                        onPlayTrack = { downloaded ->
                            viewModel.playDownloadedTrack(downloaded)
                            showPlayer = true
                        }
                    )
                    TvScreen.SETTINGS -> TvSettingsScreen(viewModel = viewModel, onSwitchToMobile = onSwitchToMobile)
                }
            }
        }

        // ─── Full-screen Video/Audio Player Overlay (Photo 3 & 4) ─────────
        AnimatedVisibility(
            visible = showPlayer && currentTrack != null,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250))
        ) {
            TvPlayerOverlay(
                viewModel = viewModel,
                onClose = { showPlayer = false }
            )
        }
    }
}

// ─── Inline TV Settings ───────────────────────────────────────────────────────
@Composable
private fun TvSettingsScreen(
    viewModel: MainViewModel,
    onSwitchToMobile: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("⚙️ TV Settings", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)

        HorizontalDivider(color = Color.White.copy(0.12f))

        Text("Display Mode", fontSize = 14.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)

        TvFocusButton(
            onClick = {
                clearTvModeChoice(context)
                onSwitchToMobile()
            },
            cornerRadius = 16.dp,
            modifier = Modifier.fillMaxWidth(0.55f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(0.08f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.PhoneAndroid, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(30.dp))
                Column {
                    Text("Switch to Mobile Touch Mode", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Switch back to the mobile phone layout", fontSize = 13.sp, color = Color.White.copy(0.5f))
                }
            }
        }
    }
}
