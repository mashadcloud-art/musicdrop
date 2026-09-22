package com.musicdrop.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicdrop.tv.data.repository.MusiXServerRepository
import com.musicdrop.tv.data.youtube.YouTubeSearchResult
import com.musicdrop.tv.ui.viewmodel.MainViewModel

/**
 * Full track list for one curated MusiX playlist — the screen the "Featured Playlists"
 * cards on [DiscoverScreen] open into. Every track gets both a play button and a
 * download button, wired to the exact same [MainViewModel.playYouTubeVideo] /
 * [MainViewModel.downloadYouTubeAudio] pipeline (NewPipeExtractor → PWebExtractor →
 * legacy extractor fallback chain) used everywhere else in the app — this is what
 * makes "play AND download" actually work here the same way it does in the reference app.
 */
@Composable
fun PlaylistDetailScreen(
    playlist: MusiXServerRepository.CuratedPlaylist,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val downloadingIds = remember { mutableStateOf(setOf<String>()) }
    val downloadedIds = remember { mutableStateOf(setOf<String>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMsg = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMsg.value) {
        snackbarMsg.value?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMsg.value = null
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F0F0F),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F0F0F))
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF1565C0), Color(0xFF0F0F0F)))
                    )
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Column {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF212121))
                        ) {
                            AsyncImage(
                                model = playlist.coverUrl,
                                contentDescription = playlist.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                playlist.title,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (playlist.description.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    playlist.description,
                                    color = Color(0xFFCCCCCC),
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${playlist.tracks.size} songs",
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            playlist.tracks.firstOrNull()?.let { viewModel.playYouTubeVideoWithContext(it, playlist.tracks) }
                        },
                        enabled = playlist.tracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF0F0F0F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Play All", color = Color(0xFF0F0F0F), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Track list ────────────────────────────────────────────────────
            if (playlist.tracks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No tracks in this playlist yet",
                        color = Color(0xFF666666),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                ) {
                    items(playlist.tracks, key = { it.videoId }) { track ->
                        PlaylistTrackRow(
                            result = track,
                            isDownloading = track.videoId in downloadingIds.value,
                            isDownloaded = track.videoId in downloadedIds.value,
                            onPlay = { viewModel.playYouTubeVideoWithContext(track, playlist.tracks) },
                            onDownload = {
                                downloadingIds.value = downloadingIds.value + track.videoId
                                viewModel.downloadYouTubeAudio(track) { success, _ ->
                                    downloadingIds.value = downloadingIds.value - track.videoId
                                    if (success) {
                                        downloadedIds.value = downloadedIds.value + track.videoId
                                        snackbarMsg.value = "Saved to Music folder ✓"
                                    } else {
                                        snackbarMsg.value = "Download failed — try again"
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistTrackRow(
    result: YouTubeSearchResult,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF212121))
        ) {
            AsyncImage(
                model = result.thumbnailUrl,
                contentDescription = result.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.channelTitle,
                    color = Color(0xFF999999),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (result.duration.isNotBlank()) {
                    Text(
                        text = "  ·  ${result.duration}",
                        color = Color(0xFF666666),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.width(4.dp))

        when {
            isDownloading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF1565C0)
                )
            }
            isDownloaded -> {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }
            else -> {
                IconButton(onClick = onDownload, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = "Download",
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    Divider(color = Color(0xFF1A1A1A), thickness = 0.5.dp, modifier = Modifier.padding(start = 84.dp))
}
