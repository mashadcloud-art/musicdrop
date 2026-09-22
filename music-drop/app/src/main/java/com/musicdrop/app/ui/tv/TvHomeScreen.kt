package com.musicdrop.app.ui.tv

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import com.musicdrop.app.ui.viewmodel.MainViewModel

data class TvQuickCategory(val title: String, val emoji: String, val query: String)

private val TV_QUICK_CATEGORIES = listOf(
    TvQuickCategory("All", "✨", ""),
    TvQuickCategory("Trending", "🔥", "trending songs india"),
    TvQuickCategory("Bollywood", "🎬", "bollywood top songs"),
    TvQuickCategory("Malayalam", "🌴", "malayalam hits"),
    TvQuickCategory("Tamil", "🎶", "tamil hits"),
    TvQuickCategory("Punjabi", "🎤", "punjabi hits"),
    TvQuickCategory("Lofi Chill", "🌙", "lofi chill music"),
    TvQuickCategory("Workout", "⚡", "workout gym music"),
    TvQuickCategory("Romantic", "❤️", "romantic love songs"),
    TvQuickCategory("Acoustic", "🎸", "acoustic guitar covers")
)

/**
 * YouTube on Android TV Styled Home Screen (Matches Photos 1 & 2):
 *  - Top Search Pill & MusicDrop Logo
 *  - Mood / Category Filter Chips
 *  - Large 16:9 Widescreen Cinema Cards (330dp wide, 186dp high, 2.5-3 cards per screen)
 *  - High-Contrast White Focused Border on remote D-pad selection
 *  - Rows for Recommended, Music Mixes, Regional hits, and Chill
 */
@Composable
fun TvHomeScreen(
    viewModel: MainViewModel,
    onPlaySong: (YouTubeSearchResult) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onSelectCategory: (String) -> Unit = {},
    firstFocusRequester: FocusRequester = remember { FocusRequester() }
) {
    val currentTrack by viewModel.playbackConnection.currentTrack.collectAsState()
    val isPlaying by viewModel.playbackConnection.isPlaying.collectAsState()
    val indiaQuick by viewModel.indiaQuickPicks.collectAsState()
    val malayalam by viewModel.malayalamQuickPicks.collectAsState()
    val tamil by viewModel.tamilQuickPicks.collectAsState()
    val lofi by viewModel.lofiQuickPicks.collectAsState()

    var activeCategoryQuery by remember { mutableStateOf("") }

    // Request focus on first element
    LaunchedEffect(Unit) {
        try { firstFocusRequester.requestFocus() } catch (_: Throwable) {}
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // ── 1. Top Header: Search Pill & Logo (Photo 1 & 2) ───────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Search Pill (Photo 1 & 2)
                TvFocusButton(
                    onClick = onOpenSearch,
                    cornerRadius = 24.dp,
                    modifier = Modifier.width(380.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color.White.copy(0.12f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Search music, artists, videos",
                            color = Color.White.copy(0.6f),
                            fontSize = 14.sp
                        )
                    }
                }

                // Brand Logo on top right (Matches YouTube logo in Photo 1 & 2)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFFF0033), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "MusicDrop",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }

        // ── 2. Category Filter Chips (Photo 1) ────────────────────────
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(TV_QUICK_CATEGORIES) { cat ->
                    val isSelected = activeCategoryQuery == cat.query
                    TvFocusButton(
                        onClick = {
                            activeCategoryQuery = cat.query
                            if (cat.query.isNotBlank()) {
                                onSelectCategory(cat.query)
                            }
                        },
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color.White else Color.White.copy(0.10f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(cat.emoji, fontSize = 14.sp)
                            Text(
                                text = cat.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color.White.copy(0.9f)
                            )
                        }
                    }
                }
            }
        }

        // ── 3. TV Video Rows (Large 16:9 Widescreen Cards - Photo 1 & 2) ──
        if (indiaQuick.isNotEmpty()) {
            item {
                TvVideoRow(
                    sectionTitle = "Recommended",
                    tracks = indiaQuick,
                    onPlay = onPlaySong
                )
            }
        }

        if (malayalam.isNotEmpty()) {
            item {
                TvVideoRow(
                    sectionTitle = "Start a Music Mix",
                    tracks = malayalam,
                    onPlay = onPlaySong
                )
            }
        }

        if (tamil.isNotEmpty()) {
            item {
                TvVideoRow(
                    sectionTitle = "Tamil & Regional Hits",
                    tracks = tamil,
                    onPlay = onPlaySong
                )
            }
        }

        if (lofi.isNotEmpty()) {
            item {
                TvVideoRow(
                    sectionTitle = "Lofi & Relaxing Chill",
                    tracks = lofi,
                    onPlay = onPlaySong
                )
            }
        }
    }
}

/**
 * YouTube on TV Horizontal Video Row:
 * 16:9 cinema thumbnails with titles, channel names, and duration badges.
 */
@Composable
fun TvVideoRow(
    sectionTitle: String,
    tracks: List<YouTubeSearchResult>,
    onPlay: (YouTubeSearchResult) -> Unit
) {
    Column(modifier = Modifier.padding(top = 18.dp)) {
        Text(
            text = sectionTitle,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 36.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(tracks.take(15)) { track ->
                TvWidescreenCard(track = track, onClick = { onPlay(track) })
            }
        }
    }
}

/**
 * YouTube on TV 16:9 Widescreen Card (Matches Photo 1 & 2):
 *  - 330dp wide x 186dp thumbnail
 *  - 16dp rounded corners
 *  - Thick 3dp white border when focused via remote
 *  - 2-line title + channel/views subtitle
 *  - Bottom-right duration badge
 */
@Composable
fun TvWidescreenCard(
    track: YouTubeSearchResult,
    onClick: () -> Unit
) {
    TvFocusButton(
        onClick = onClick,
        cornerRadius = 16.dp,
        modifier = Modifier.width(330.dp)
    ) {
        Column(
            modifier = Modifier
                .width(330.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            // 16:9 Thumbnail Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1E28))
            ) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom subtle shadow vignette
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
                )

                // Duration badge on bottom-right (Photo 2)
                if (track.duration.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(0.85f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = track.duration,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Title & Channel Subtitle (Photo 1 & 2)
            Column(modifier = Modifier.padding(top = 10.dp, start = 2.dp, end = 2.dp, bottom = 6.dp)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 20.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (track.channelTitle.isNotBlank()) track.channelTitle else "MusicDrop",
                    color = Color.White.copy(0.6f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TvTrackCard(
    track: YouTubeSearchResult,
    onClick: () -> Unit
) {
    TvWidescreenCard(track = track, onClick = onClick)
}
