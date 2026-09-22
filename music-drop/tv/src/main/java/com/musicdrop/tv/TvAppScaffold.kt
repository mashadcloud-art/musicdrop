package com.musicdrop.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.musicdrop.tv.data.TvNavScreen
import com.musicdrop.tv.player.TvPlayerOverlay
import com.musicdrop.tv.screens.*
import com.musicdrop.tv.ui.components.TvFocusButton
import com.musicdrop.tv.viewmodel.TvViewModel

data class NavItem(val screen: TvNavScreen, val icon: ImageVector, val label: String)

private val NAV_ITEMS = listOf(
    NavItem(TvNavScreen.SEARCH, Icons.Filled.Search, "Search"),
    NavItem(TvNavScreen.HOME, Icons.Filled.Home, "Home"),
    NavItem(TvNavScreen.BROWSE, Icons.Filled.Category, "Music & Mixes"),
    NavItem(TvNavScreen.DOWNLOADS, Icons.Filled.Download, "Downloads"),
    NavItem(TvNavScreen.SETTINGS, Icons.Filled.Settings, "Settings")
)

@Composable
fun TvAppScaffold(viewModel: TvViewModel) {
    var currentScreen by remember { mutableStateOf(TvNavScreen.HOME) }
    val currentVideo by viewModel.currentVideo.collectAsState()
    val upNextVideos by viewModel.upNextVideos.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ── Left Navigation Rail (Matches YouTube on TV Photo 1 & 2) ───
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF0A0A0E))
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Header Logo: Official MusicDrop Icon + MusicDrop TV
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_musicdrop_bird),
                        contentDescription = "MusicDrop",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("MusicDrop", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("TV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF0000))
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Navigation Items (Selected item is Solid White Pill with Black text)
                NAV_ITEMS.forEach { item ->
                    val isSelected = (currentScreen == item.screen)
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
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) Color.Black else Color.White.copy(0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = item.label,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color.White.copy(0.85f)
                            )
                        }
                    }
                }
            }

            // ── Right Main Content Viewport ────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (currentScreen) {
                    TvNavScreen.HOME -> TvHomeScreen(
                        viewModel = viewModel,
                        onPlayVideo = { viewModel.playVideo(it) },
                        onOpenSearch = { currentScreen = TvNavScreen.SEARCH }
                    )
                    TvNavScreen.SEARCH -> TvSearchScreen(
                        viewModel = viewModel,
                        onPlayVideo = { viewModel.playVideo(it) }
                    )
                    TvNavScreen.BROWSE -> TvBrowseScreen(
                        viewModel = viewModel,
                        onPlayVideo = { viewModel.playVideo(it) }
                    )
                    TvNavScreen.DOWNLOADS -> TvDownloadsScreen(
                        viewModel = viewModel,
                        onPlayVideo = { viewModel.playVideo(it) }
                    )
                    TvNavScreen.SETTINGS -> TvSettingsScreen(viewModel = viewModel)
                }
            }
        }

        // ── Fullscreen YouTube TV Video Overlay ─────────────────────────────
        AnimatedVisibility(
            visible = currentVideo != null,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250))
        ) {
            TvPlayerOverlay(
                currentVideo = currentVideo,
                upNextVideos = upNextVideos,
                onSelectVideo = { viewModel.playVideo(it) },
                onClose = { viewModel.closePlayer() }
            )
        }
    }
}
