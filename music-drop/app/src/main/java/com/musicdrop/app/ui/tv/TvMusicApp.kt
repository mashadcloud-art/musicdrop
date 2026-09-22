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
    val label: String,
    val emoji: String
)

private val NAV_ITEMS = listOf(
    TvNavItem(TvScreen.HOME,        Icons.Filled.Home,         "Home",        "🏠"),
    TvNavItem(TvScreen.BROWSE,      Icons.Filled.Category,     "Categories",  "📂"),
    TvNavItem(TvScreen.SEARCH,      Icons.Filled.Search,       "Search",      "🔍"),
    TvNavItem(TvScreen.DOWNLOADS,   Icons.Filled.Download,     "Downloads",   "📥"),
    TvNavItem(TvScreen.SETTINGS,    Icons.Filled.Settings,     "Settings",    "⚙️"),
)

/**
 * Root TV app scaffold.
 *
 * Layout: sidebar navigation (left) + content area (right).
 * The full-screen player slides over everything when a track plays.
 */
@Composable
fun TvMusicApp(
    viewModel: MainViewModel,
    onSwitchToMobile: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(TvScreen.HOME) }
    val currentTrack by viewModel.playbackConnection.currentTrack.collectAsState()
    val isPlaying   by viewModel.playbackConnection.isPlaying.collectAsState()
    var showPlayer  by remember { mutableStateOf(false) }

    // Helper: plays a YouTube search result using the existing ViewModel pipeline
    val onPlaySong: (YouTubeSearchResult) -> Unit = { track ->
        viewModel.playYouTubeVideo(track)
        showPlayer = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(Color(0xFF0A0B15), Color(0xFF0D0F1E)))
            )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // ─── Sidebar Navigation ─────────────────────────
            Column(
                modifier = Modifier
                    .width(230.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(0.45f))
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // App logo
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎵", fontSize = 18.sp)
                    }
                    Text(
                        "MusicDrop TV",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Nav items
                NAV_ITEMS.forEach { item ->
                    val isSelected = currentScreen == item.screen
                    TvFocusButton(
                        onClick = { currentScreen = item.screen },
                        cornerRadius = 14.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected)
                                        Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB)))
                                    else
                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color.White.copy(0.55f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                item.label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.White.copy(0.65f)
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Now Playing / Mini player shortcut at bottom of sidebar
                AnimatedVisibility(visible = currentTrack != null) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        HorizontalDivider(color = Color.White.copy(0.1f), modifier = Modifier.padding(bottom = 12.dp))
                        TvFocusButton(
                            onClick = { showPlayer = true },
                            cornerRadius = 12.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(0.07f), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
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
                                        "🎬 Watch Video",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA78BFA),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    null,
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ─── Content Area ───────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (currentScreen) {
                    TvScreen.HOME      -> TvHomeScreen(
                        viewModel = viewModel,
                        onPlaySong = onPlaySong,
                        onOpenPlayer = { showPlayer = true },
                        onSelectCategory = { query ->
                            viewModel.setYtSearchQuery(query)
                            currentScreen = TvScreen.BROWSE
                        }
                    )
                    TvScreen.BROWSE    -> TvBrowseScreen(viewModel = viewModel, onPlaySong = onPlaySong)
                    TvScreen.SEARCH    -> TvSearchScreen(viewModel = viewModel, onPlaySong = onPlaySong)
                    TvScreen.DOWNLOADS -> TvDownloadsScreen(
                        viewModel = viewModel,
                        onPlayTrack = { downloaded ->
                            viewModel.playDownloadedTrack(downloaded)
                            showPlayer = true
                        }
                    )
                    TvScreen.SETTINGS  -> TvSettingsScreen(viewModel = viewModel, onSwitchToMobile = onSwitchToMobile)
                }
            }
        }

        // ─── Full-screen Video/Audio Player Overlay ─────────
        AnimatedVisibility(
            visible = showPlayer && currentTrack != null,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
            exit  = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it }
        ) {
            TvPlayerOverlay(
                viewModel = viewModel,
                onClose = { showPlayer = false }
            )
        }
    }
}

// ─── Inline Settings ────────────────────────────────────────────────────────
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
        Text("⚙️ Settings", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)

        HorizontalDivider(color = Color.White.copy(0.12f))

        Text("App Mode", fontSize = 14.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)

        TvFocusButton(
            onClick = {
                clearTvModeChoice(context)
                onSwitchToMobile()
            },
            cornerRadius = 16.dp,
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(0.07f), RoundedCornerShape(16.dp))
                .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.PhoneAndroid, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(28.dp))
                Column {
                    Text("Switch to Mobile Music Mode", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Classic mobile interface with touch gestures", fontSize = 13.sp, color = Color.White.copy(0.5f))
                }
            }
        }
    }
}
