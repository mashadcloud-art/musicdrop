package com.musicdrop.app.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import com.musicdrop.app.ui.viewmodel.MainViewModel

data class TvGenreCategory(
    val label: String,
    val emoji: String,
    val gradient: Brush,
    val searchQuery: String
)

private val TV_GENRES = listOf(
    TvGenreCategory("Bollywood Hits", "🎬",
        Brush.linearGradient(listOf(Color(0xFFBF360C), Color(0xFFE91E63))),
        "bollywood hits 2026"),
    TvGenreCategory("Desi Hip Hop", "🎤",
        Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF7B1FA2))),
        "desi hip hop trending"),
    TvGenreCategory("Malayalam Songs", "🌴",
        Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF00897B))),
        "malayalam hits 2026"),
    TvGenreCategory("Tamil Songs", "🎶",
        Brush.linearGradient(listOf(Color(0xFF880E4F), Color(0xFFAD1457))),
        "tamil hits 2026"),
    TvGenreCategory("Lofi Chill", "🌙",
        Brush.linearGradient(listOf(Color(0xFF263238), Color(0xFF37474F))),
        "lofi chill music"),
    TvGenreCategory("Punjabi Hits", "🔥",
        Brush.linearGradient(listOf(Color(0xFFE65100), Color(0xFFFF8F00))),
        "punjabi hits 2026"),
    TvGenreCategory("Acoustic / Guitar", "🎸",
        Brush.linearGradient(listOf(Color(0xFF3E2723), Color(0xFF6D4C41))),
        "acoustic guitar covers"),
    TvGenreCategory("Trending Now", "📈",
        Brush.linearGradient(listOf(Color(0xFF006064), Color(0xFF00838F))),
        "trending songs india 2026"),
)

/**
 * TV Browse screen — genre selector + track results shown side-by-side.
 */
@Composable
fun TvBrowseScreen(
    viewModel: MainViewModel,
    onPlaySong: (YouTubeSearchResult) -> Unit
) {
    var selectedGenre by remember { mutableStateOf(TV_GENRES.first()) }
    val searchResults by viewModel.ytSearchResults.collectAsState()
    val isLoading by viewModel.ytSearchLoading.collectAsState()

    // Load first genre on enter
    LaunchedEffect(selectedGenre) {
        viewModel.setYtSearchQuery(selectedGenre.searchQuery)
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // ─── Left: Genre selector ─────────────────────────
        LazyColumn(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(Color.Black.copy(0.35f)),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    "Browse",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
            items(TV_GENRES) { genre ->
                val isSelected = genre == selectedGenre
                TvFocusButton(
                    onClick = { selectedGenre = genre },
                    cornerRadius = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected)
                                    Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB)))
                                else
                                    Brush.linearGradient(listOf(Color.White.copy(0.05f), Color.White.copy(0.05f))),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(genre.emoji, fontSize = 20.sp)
                        Text(
                            genre.label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // ─── Right: Track results ─────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF7C3AED),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (searchResults.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No results", color = Color.White.copy(0.4f), fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            "${selectedGenre.emoji} ${selectedGenre.label}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }
                    items(searchResults.take(30)) { track ->
                        TvBrowseTrackRow(track = track, onClick = { onPlaySong(track) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TvBrowseTrackRow(
    track: YouTubeSearchResult,
    onClick: () -> Unit
) {
    TvFocusButton(
        onClick = onClick,
        cornerRadius = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(0.06f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(0.1f), RoundedCornerShape(8.dp))
            ) {
                coil.compose.AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                        .then(Modifier.background(Color.Transparent, RoundedCornerShape(8.dp)))
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    track.channelTitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(0.55f),
                    maxLines = 1
                )
            }
            Text(
                track.duration,
                fontSize = 12.sp,
                color = Color.White.copy(0.45f)
            )
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color(0xFF7C3AED),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
