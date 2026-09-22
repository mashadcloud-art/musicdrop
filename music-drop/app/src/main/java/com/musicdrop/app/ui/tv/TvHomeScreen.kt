package com.musicdrop.app.ui.tv

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

/**
 * TV Home screen: hero now-playing banner + horizontal genre rows.
 * All data comes from the shared [MainViewModel] — no new fetching needed.
 */
@Composable
fun TvHomeScreen(
    viewModel: MainViewModel,
    onPlaySong: (YouTubeSearchResult) -> Unit,
    firstFocusRequester: FocusRequester = remember { FocusRequester() }
) {
    val currentTrack by viewModel.playbackConnection.currentTrack.collectAsState()
    val isPlaying   by viewModel.playbackConnection.isPlaying.collectAsState()
    val indiaQuick  by viewModel.indiaQuickPicks.collectAsState()
    val malayalam   by viewModel.malayalamQuickPicks.collectAsState()
    val tamil       by viewModel.tamilQuickPicks.collectAsState()
    val lofi        by viewModel.lofiQuickPicks.collectAsState()

    // Request focus on first-visible element after composition
    LaunchedEffect(Unit) {
        try { firstFocusRequester.requestFocus() } catch (_: Throwable) {}
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ─── Hero Banner ───────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF1E0533), Color(0xFF0C1445)))
                )
        ) {
            // Background art from current track (if any)
            currentTrack?.albumArtUri?.let { art ->
                AsyncImage(
                    model = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.65f)))
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (currentTrack != null) "Now Playing" else "Welcome to MusicDrop TV",
                        fontSize = 14.sp,
                        color = Color.White.copy(0.6f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = currentTrack?.name ?: "Pick something to play",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (currentTrack != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = currentTrack?.artist ?: "",
                            fontSize = 16.sp,
                            color = Color.White.copy(0.7f)
                        )
                    }
                }

                // Play/Pause hero button
                if (currentTrack != null) {
                    Spacer(Modifier.width(24.dp))
                    TvFocusButton(
                        onClick = { viewModel.playbackConnection.togglePlayPause() },
                        focusRequester = firstFocusRequester,
                        cornerRadius = 50.dp,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB))),
                                    RoundedCornerShape(50)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ─── Genre Rows ────────────────────────────────────────
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (indiaQuick.isNotEmpty()) {
                item { TvTrackRow("🔥 India Trending", indiaQuick, onPlaySong) }
            }
            if (malayalam.isNotEmpty()) {
                item { TvTrackRow("🎵 Malayalam Hits", malayalam, onPlaySong) }
            }
            if (tamil.isNotEmpty()) {
                item { TvTrackRow("🎶 Tamil Hits", tamil, onPlaySong) }
            }
            if (lofi.isNotEmpty()) {
                item { TvTrackRow("🌙 Lofi Vibes", lofi, onPlaySong) }
            }
            if (indiaQuick.isEmpty() && malayalam.isEmpty() && tamil.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(64.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Loading music…", color = Color.White.copy(0.4f), fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TvTrackRow(
    title: String,
    tracks: List<YouTubeSearchResult>,
    onPlay: (YouTubeSearchResult) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(tracks.take(15)) { _, track ->
                TvTrackCard(track = track, onClick = { onPlay(track) })
            }
        }
    }
}

@Composable
fun TvTrackCard(
    track: YouTubeSearchResult,
    onClick: () -> Unit
) {
    TvFocusButton(
        onClick = onClick,
        cornerRadius = 16.dp,
        modifier = Modifier.width(180.dp)
    ) {
        Column(
            modifier = Modifier
                .width(180.dp)
                .background(Color.White.copy(0.06f), RoundedCornerShape(16.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
                )
                // Duration badge
                if (track.duration.isNotBlank()) {
                    Surface(
                        color = Color.Black.copy(0.75f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = track.duration,
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = track.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
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
