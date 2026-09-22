package com.musicdrop.tv.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.tv.data.TvVideoItem
import com.musicdrop.tv.ui.components.TvFocusButton
import com.musicdrop.tv.ui.components.TvVideoCard
import com.musicdrop.tv.viewmodel.TvViewModel

private val SEARCH_SUGGESTIONS = listOf(
    "New Hindi Songs 2026",
    "Arijit Singh Hits",
    "Anirudh Ravichander",
    "Diljit Dosanjh",
    "Malayalam Hits",
    "Tamil Songs",
    "Punjabi Trending",
    "Lofi Chill Beats",
    "Workout Motivation"
)

@Composable
fun TvSearchScreen(
    viewModel: TvViewModel,
    onPlayVideo: (TvVideoItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .padding(horizontal = 36.dp, vertical = 24.dp)
    ) {
        // Search Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White.copy(0.12f))
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(Icons.Filled.Search, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(24.dp))
            BasicTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.search(it)
                },
                singleLine = true,
                cursorBrush = SolidColor(Color(0xFFFF0033)),
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text("Search YouTube videos & music...", color = Color.White.copy(0.4f), fontSize = 16.sp)
                    }
                    innerTextField()
                }
            )
            if (query.isNotEmpty()) {
                TvFocusButton(
                    onClick = {
                        query = ""
                        viewModel.search("")
                    },
                    cornerRadius = 20.dp
                ) {
                    Icon(Icons.Filled.Close, "Clear", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Quick Suggestion Pills
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(SEARCH_SUGGESTIONS) { suggestion ->
                TvFocusButton(
                    onClick = {
                        query = suggestion
                        viewModel.search(suggestion)
                    },
                    cornerRadius = 18.dp
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(0.08f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(suggestion, color = Color.White.copy(0.85f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Loading or Results Grid
        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF0033), strokeWidth = 3.dp)
            }
        } else if (searchResults.isEmpty() && query.isNotBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No videos found for \"$query\"", color = Color.White.copy(0.5f), fontSize = 16.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(320.dp),
                contentPadding = PaddingValues(bottom = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults, key = { it.id }) { video ->
                    TvVideoCard(
                        video = video,
                        onClick = { onPlayVideo(video) }
                    )
                }
            }
        }
    }
}
