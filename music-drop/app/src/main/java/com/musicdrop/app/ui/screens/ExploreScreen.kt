package com.musicdrop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.rounded.PlayCircleFilled
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
import coil.compose.AsyncImage
import com.musicdrop.app.data.model.UnifiedTrack
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import com.musicdrop.app.ui.viewmodel.MainViewModel

data class MoodGenreItem(val name: String, val color: Color)

@Composable
fun ExploreScreen(
    viewModel: MainViewModel,
    onOpenSearch: (query: String) -> Unit
) {
    val newReleases by viewModel.exploreNewReleases.collectAsState()
    val loading by viewModel.exploreLoading.collectAsState()
    val preparingKey by viewModel.preparingKey.collectAsState()

    val moodsAndGenres = remember {
        listOf(
            MoodGenreItem("Metal", Color(0xFF757575)),
            MoodGenreItem("Iraqi", Color(0xFFE53935)),
            MoodGenreItem("Workout", Color(0xFFFF9800)),
            MoodGenreItem("Malaysian", Color(0xFFD32F2F)),
            MoodGenreItem("2000s", Color(0xFF8BC34A)),
            MoodGenreItem("Soundtracks & musicals", Color(0xFF00BCD4)),
            MoodGenreItem("Hip-Hop", Color(0xFFFFB300)),
            MoodGenreItem("Pop", Color(0xFFE91E63)),
            MoodGenreItem("Chill & Relax", Color(0xFF009688)),
            MoodGenreItem("Party", Color(0xFF9C27B0)),
            MoodGenreItem("Romance", Color(0xFFF06292)),
            MoodGenreItem("Focus & Study", Color(0xFF2196F3)),
            MoodGenreItem("Rock", Color(0xFFF44336)),
            MoodGenreItem("Bollywood & Desi", Color(0xFFFF7043)),
            MoodGenreItem("Electronic & Dance", Color(0xFF4CAF50)),
            MoodGenreItem("R&B & Soul", Color(0xFF3F51B5))
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loadExploreData()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        // ── TOP HEADER (Logo + Music text, Search, Avatar) ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF0000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "YouTube Music",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Music",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onOpenSearch("") }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5C6BC0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── TOP 3 ACTION TILES (New releases, Charts, Moods & genres) ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExploreTopActionTile(
                    title = "New\nreleases",
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSearch("new music releases 2026") }
                )
                ExploreTopActionTile(
                    title = "Charts",
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSearch("top charts songs") }
                )
                ExploreTopActionTile(
                    title = "Moods &\ngenres",
                    icon = Icons.Default.SentimentSatisfiedAlt,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSearch("moods and genres music") }
                )
            }
        }

        // ── SECTION: New albums & singles ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 12.dp)
                    .clickable { onOpenSearch("new albums singles") },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "New albums & singles",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "View all",
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(24.dp)
                )
            }

            if (loading && newReleases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF3B30), modifier = Modifier.size(32.dp))
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(newReleases.take(12), key = { it.videoId }) { result ->
                        val isPrep = preparingKey == "yt:${result.videoId}"
                        Column(
                            modifier = Modifier
                                .width(150.dp)
                                .clickable { viewModel.playYouTubeVideoWithContext(result, newReleases) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF222222))
                            ) {
                                AsyncImage(
                                    model = result.thumbnailUrl,
                                    contentDescription = result.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isPrep) {
                                    Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = Color.White,
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayCircleFilled,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = result.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (result.channelTitle.isNotBlank()) "Single • ${result.channelTitle}" else "Single",
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // ── SECTION: Moods & genres ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 12.dp)
                    .clickable { onOpenSearch("popular music genres") },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Moods & genres",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "View all",
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 2-Column mood & genre cards with left color strip (matching Screenshot 1)
        items(moodsAndGenres.chunked(2)) { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MoodGenreTile(
                    item = pair[0],
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSearch("${pair[0].name} music") }
                )
                if (pair.size > 1) {
                    MoodGenreTile(
                        item = pair[1],
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSearch("${pair[1].name} music") }
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        item {
            Spacer(Modifier.height(90.dp))
        }
    }
}

@Composable
private fun ExploreTopActionTile(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF212121))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun MoodGenreTile(
    item: MoodGenreItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF242424))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color strip
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(item.color)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
