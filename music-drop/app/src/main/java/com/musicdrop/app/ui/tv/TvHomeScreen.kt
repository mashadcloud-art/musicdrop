package com.musicdrop.app.ui.tv

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
    TvQuickCategory("Trending", "🔥", "trending songs india"),
    TvQuickCategory("Bollywood", "🎬", "bollywood top songs"),
    TvQuickCategory("Malayalam", "🌴", "malayalam hits"),
    TvQuickCategory("Tamil", "🎶", "tamil hits"),
    TvQuickCategory("Punjabi", "🎤", "punjabi hits"),
    TvQuickCategory("Lofi Vibes", "🌙", "lofi chill music"),
    TvQuickCategory("Workout", "⚡", "workout gym music"),
    TvQuickCategory("Romantic", "❤️", "romantic love songs"),
    TvQuickCategory("Acoustic", "🎸", "acoustic guitar covers")
)

/**
 * TV Home screen:
 *  - Top Category Pills row for instant mood switching
 *  - Hero Banner with "Play/Pause" and "Watch Video" buttons
 *  - Horizontal genre rows with TV-proportioned 16:9 cards (not giant mobile cards)
 */
@Composable
fun TvHomeScreen(
    viewModel: MainViewModel,
    onPlaySong: (YouTubeSearchResult) -> Unit,
    onOpenPlayer: () -> Unit,
    onSelectCategory: (String) -> Unit = {},
    firstFocusRequester: FocusRequester = remember { FocusRequester() }
) {
    val currentTrack by viewModel.playbackConnection.currentTrack.collectAsState()
    val isPlaying   by viewModel.playbackConnection.isPlaying.collectAsState()
    val indiaQuick  by viewModel.indiaQuickPicks.collectAsState()
    val malayalam   by viewModel.malayalamQuickPicks.collectAsState()
    val tamil       by viewModel.tamilQuickPicks.collectAsState()
    val lofi        by viewModel.lofiQuickPicks.collectAsState()

    // Request focus on first element
    LaunchedEffect(Unit) {
        try { firstFocusRequester.requestFocus() } catch (_: Throwable) {}
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        // ── 1. Top Category Pills ─────────────────────────────────────
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 36.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(TV_QUICK_CATEGORIES) { cat ->
                    TvFocusButton(
                        onClick = { onSelectCategory(cat.query) },
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Color.White.copy(0.08f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(cat.emoji, fontSize = 16.sp)
                            Text(
                                cat.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ── 2. Hero Banner ───────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .padding(horizontal = 36.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF1E0533), Color(0xFF0C1445)))
                    )
            ) {
                // Background artwork
                currentTrack?.albumArtUri?.let { art ->
                    AsyncImage(
                        model = art,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Black.copy(0.92f),
                                        Color.Black.copy(0.65f),
                                        Color.Black.copy(0.85f)
                                    )
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 36.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = Color(0xFF7C3AED).copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = if (currentTrack != null) "● NOW PLAYING" else "FEATURED FOR TV",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA78BFA),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = currentTrack?.name ?: "Trending Music Hits",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = currentTrack?.artist ?: "Press Play to start the music experience",
                            fontSize = 15.sp,
                            color = Color.White.copy(0.7f),
                            maxLines = 1
                        )
                    }

                    // Action buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Watch Video Button
                        if (currentTrack != null) {
                            TvFocusButton(
                                onClick = onOpenPlayer,
                                cornerRadius = 14.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xFF7C3AED), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Videocam, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    Text("Watch Video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        // Play/Pause button
                        TvFocusButton(
                            onClick = {
                                if (currentTrack != null) {
                                    viewModel.playbackConnection.togglePlayPause()
                                } else if (indiaQuick.isNotEmpty()) {
                                    onPlaySong(indiaQuick.first())
                                }
                            },
                            focusRequester = firstFocusRequester,
                            cornerRadius = 50.dp,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF06B6D4))),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 3. TV Track Rows ─────────────────────────────────────────
        if (indiaQuick.isNotEmpty()) {
            item { TvTrackRow("🔥 India Trending", indiaQuick, onPlaySong) }
        }
        if (malayalam.isNotEmpty()) {
            item { TvTrackRow("🌴 Malayalam Hits", malayalam, onPlaySong) }
        }
        if (tamil.isNotEmpty()) {
            item { TvTrackRow("🎶 Tamil Hits", tamil, onPlaySong) }
        }
        if (lofi.isNotEmpty()) {
            item { TvTrackRow("🌙 Lofi & Chill", lofi, onPlaySong) }
        }
    }
}

@Composable
fun TvTrackRow(
    title: String,
    tracks: List<YouTubeSearchResult>,
    onPlay: (YouTubeSearchResult) -> Unit
) {
    Column(modifier = Modifier.padding(top = 22.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 36.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(tracks.take(15)) { _, track ->
                TvTrackCard(track = track, onClick = { onPlay(track) })
            }
        }
    }
}

/**
 * TV Card: Clean 16:9 widescreen ratio, 175dp wide.
 * Displays 5-6 cards across on 1080p / 4K TV screens instead of mobile oversized cards.
 */
@Composable
fun TvTrackCard(
    track: YouTubeSearchResult,
    onClick: () -> Unit
) {
    TvFocusButton(
        onClick = onClick,
        cornerRadius = 14.dp,
        modifier = Modifier.width(175.dp)
    ) {
        Column(
            modifier = Modifier
                .width(175.dp)
                .background(Color.White.copy(0.06f), RoundedCornerShape(14.dp))
        ) {
            // 16:9 Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            ) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient bottom fade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.75f))))
                )
                // Duration badge
                if (track.duration.isNotBlank()) {
                    Surface(
                        color = Color.Black.copy(0.8f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = track.duration,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = track.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = track.channelTitle,
                    fontSize = 11.sp,
                    color = Color.White.copy(0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
