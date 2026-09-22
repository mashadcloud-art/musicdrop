package com.musicdrop.tv.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.musicdrop.tv.data.TvVideoItem
import com.musicdrop.tv.ui.components.TvFocusButton
import com.musicdrop.tv.ui.components.TvVideoCard
import com.musicdrop.tv.viewmodel.TvViewModel

private val QUICK_CHIPS = listOf(
    "All", "Trending", "Bollywood", "Malayalam", "Tamil", "Punjabi", "Lofi Chill", "Workout", "Acoustic"
)

@Composable
fun TvHomeScreen(
    viewModel: TvViewModel,
    onPlayVideo: (TvVideoItem) -> Unit,
    onOpenSearch: () -> Unit
) {
    val shelves by viewModel.homeShelves.collectAsState()
    val isLoading by viewModel.isLoadingHome.collectAsState()
    var selectedChip by remember { mutableStateOf("All") }
    val featuredVideo = shelves.firstOrNull()?.videos?.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14)),
        contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // ── 0. YouTube TV Cinematic Hero Spotlight ───────────────────────────
        if (featuredVideo != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .padding(horizontal = 36.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E26))
                ) {
                    AsyncImage(
                        model = featuredVideo.thumbnailUrl,
                        contentDescription = featuredVideo.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // High-contrast gradient overlay (Dark from left & bottom)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.95f),
                                        Color.Black.copy(alpha = 0.75f),
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(520.dp)
                            .padding(start = 36.dp, top = 28.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFF0033))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("FEATURED", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = featuredVideo.channelTitle,
                                color = Color.White.copy(0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (featuredVideo.duration.isNotBlank()) {
                                Text("•  ${featuredVideo.duration}", color = Color.White.copy(0.6f), fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = featuredVideo.title,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 30.sp
                        )

                        Spacer(Modifier.height(18.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            TvFocusButton(
                                onClick = { onPlayVideo(featuredVideo) },
                                cornerRadius = 24.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(Color.White, RoundedCornerShape(24.dp))
                                        .padding(horizontal = 24.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                    Text("Play Video", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        // ── 1. Top Search Bar Pill & Mood Chips ─────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search Pill
                TvFocusButton(
                    onClick = onOpenSearch,
                    cornerRadius = 24.dp,
                    modifier = Modifier.width(360.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color.White.copy(0.12f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Search, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(20.dp))
                        Text("Search videos, music, artists...", color = Color.White.copy(0.6f), fontSize = 14.sp)
                    }
                }

                // Mood / Category Filter Chips Row
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(QUICK_CHIPS) { chip ->
                        val isSelected = (chip == selectedChip)
                        TvFocusButton(
                            onClick = {
                                selectedChip = chip
                                if (chip != "All") {
                                    viewModel.loadGenre("$chip hits 2026", chip)
                                }
                            },
                            cornerRadius = 20.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) Color.White else Color.White.copy(0.1f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = chip,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 2. Loading Spinner ─────────────────────────────────────────────
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF0033), strokeWidth = 3.dp)
                }
            }
        }

        // ── 3. Horizontal 16:9 Cinema Shelves ──────────────────────────────
        items(shelves, key = { it.title }) { shelf ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = shelf.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(shelf.videos, key = { it.id }) { video ->
                        TvVideoCard(
                            video = video,
                            onClick = { onPlayVideo(video) }
                        )
                    }
                }
            }
        }
    }
}
