package com.musicdrop.tv.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.tv.data.TvVideoItem
import com.musicdrop.tv.ui.components.TvFocusButton
import com.musicdrop.tv.ui.components.TvVideoCard
import com.musicdrop.tv.viewmodel.TvViewModel

data class TvGenre(val name: String, val emoji: String, val query: String)

private val GENRES = listOf(
    TvGenre("Bollywood Hits", "🎬", "bollywood hits 2026"),
    TvGenre("Desi Hip Hop", "🎤", "desi hip hop trending"),
    TvGenre("Punjabi Hits", "🔥", "punjabi hits 2026"),
    TvGenre("Malayalam Songs", "🌴", "malayalam hits 2026"),
    TvGenre("Tamil Hits", "🎶", "tamil hits 2026"),
    TvGenre("Lofi Chill", "🌙", "lofi chill beats 2026"),
    TvGenre("Workout / Gym", "⚡", "workout gym motivation music"),
    TvGenre("Romantic", "❤️", "romantic love songs hindi 2026"),
    TvGenre("Acoustic / Guitar", "🎸", "acoustic guitar covers")
)

@Composable
fun TvBrowseScreen(
    viewModel: TvViewModel,
    onPlayVideo: (TvVideoItem) -> Unit
) {
    val activeGenre by viewModel.activeGenre.collectAsState()
    val videos by viewModel.genreVideos.collectAsState()
    val isLoading by viewModel.isLoadingGenre.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .padding(horizontal = 36.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Music & Mixes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(14.dp))

        // Genre Selector Pills
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(GENRES) { genre ->
                val isSelected = (genre.name == activeGenre)
                TvFocusButton(
                    onClick = { viewModel.loadGenre(genre.query, genre.name) },
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
                            text = "${genre.emoji} ${genre.name}",
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF0033), strokeWidth = 3.dp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(320.dp),
                contentPadding = PaddingValues(bottom = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(videos, key = { it.id }) { video ->
                    TvVideoCard(
                        video = video,
                        onClick = { onPlayVideo(video) }
                    )
                }
            }
        }
    }
}
