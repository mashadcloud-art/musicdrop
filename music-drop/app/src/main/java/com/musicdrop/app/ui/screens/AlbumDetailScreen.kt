package com.musicdrop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicdrop.app.data.model.UnifiedTrack
import com.musicdrop.app.data.repository.YtMusicApiRepository
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import com.musicdrop.app.ui.components.AddToPlaylistDialog
import com.musicdrop.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun AlbumDetailScreen(
    browseId: String,
    initialTitle: String = "",
    initialArtist: String = "",
    initialThumb: String = "",
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenArtist: (channelId: String, artistName: String) -> Unit = { _, _ -> }
) {
    val cached = remember(browseId) { YtMusicApiRepository.peekAlbum(browseId) }
    var albumDetails by remember(browseId) { mutableStateOf<YtMusicApiRepository.YtAlbumDetails?>(cached) }
    var loading by remember(browseId) { mutableStateOf(cached == null) }

    val likedMusic by viewModel.likedMusic.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val savedAlbums by viewModel.savedAlbums.collectAsState()
    val preparingKey by viewModel.preparingKey.collectAsState()
    val downloadingKeys = remember { mutableStateOf(setOf<String>()) }
    val playlistTrack = remember { mutableStateOf<UnifiedTrack?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current

    LaunchedEffect(browseId) {
        if (albumDetails == null) {
            loading = true
        }
        val details = YtMusicApiRepository.getAlbum(browseId)
        if (details != null) {
            albumDetails = details
        }
        loading = false
    }

    val title = albumDetails?.title ?: initialTitle.ifBlank { "Album" }
    val artistName = albumDetails?.artistName ?: initialArtist.ifBlank { "Various Artists" }
    val thumbUrl = albumDetails?.thumbnailUrl ?: initialThumb
    val tracks = albumDetails?.tracks.orEmpty()
    val isSaved = savedAlbums.any { it.browseId == browseId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Top Navigation Bar ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Album",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Album Hero Header ───────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF222222))
                    ) {
                        if (thumbUrl.isNotBlank()) {
                            AsyncImage(
                                model = thumbUrl,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Album,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(50.dp).align(Alignment.Center)
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = artistName,
                            color = appColors.accentPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        val meta = buildString {
                            albumDetails?.year?.let { append("$it • ") }
                            if (albumDetails?.trackCount ?: 0 > 0) {
                                append("${albumDetails?.trackCount} songs")
                            } else if (tracks.isNotEmpty()) {
                                append("${tracks.size} songs")
                            }
                            albumDetails?.duration?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
                        }
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── Action Buttons ──────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                viewModel.playYouTubeVideoWithContext(tracks.first(), tracks)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Play All",
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                val shuffled = tracks.shuffled()
                                viewModel.playYouTubeVideoWithContext(shuffled.first(), shuffled)
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF444444)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Shuffle",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Save / Bookmark Album to Library
                    IconButton(
                        onClick = {
                            val cardItem = com.musicdrop.app.data.repository.YtMusicApiRepository.YtCardItem(
                                title = title,
                                browseId = browseId,
                                thumbnailUrl = thumbUrl,
                                year = albumDetails?.year.orEmpty(),
                                type = artistName
                            )
                            val nowSaved = viewModel.toggleSaveAlbum(cardItem)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(if (nowSaved) "Saved $title to Library" else "Removed from Library")
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isSaved) appColors.accentPrimary else Color(0xFF222222))
                    ) {
                        Icon(
                            if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = if (isSaved) "Saved" else "Save Album",
                            tint = if (isSaved) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFE53935))
                    }
                }
            } else {
                // ── Tracklist ───────────────────────────────────────────────────
                itemsIndexed(tracks, key = { index, track -> "album_${track.videoId}_$index" }) { index, track ->
                    val unified = UnifiedTrack.Youtube(track)
                    val isLiked = likedMusic.any { it.key == unified.key }
                    val isDownloaded = downloadedTracks.any { it.key == unified.key }
                    val isDownloading = track.videoId in downloadingKeys.value
                    val isPrep = preparingKey == unified.key

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playYouTubeVideoWithContext(track, tracks) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color(0xFF888888),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(26.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = track.channelTitle.ifBlank { artistName },
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (track.duration.isNotBlank()) {
                            Text(
                                text = track.duration,
                                color = Color(0xFF888888),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleLikeUnifiedTrack(unified) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) Color(0xFFE53935) else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { playlistTrack.value = unified },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (!isDownloading && !isDownloaded) {
                                    downloadingKeys.value = downloadingKeys.value + track.videoId
                                    viewModel.downloadYouTubeAudio(track) { success, _ ->
                                        downloadingKeys.value = downloadingKeys.value - track.videoId
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(if (success) "Downloaded: ${track.title}" else "Download failed")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(color = Color(0xFFE53935), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else if (isDownloaded) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Downloaded", tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Rounded.Download, contentDescription = "Download", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp)
        )

        playlistTrack.value?.let { track ->
            AddToPlaylistDialog(
                track = track,
                viewModel = viewModel,
                onDismiss = { playlistTrack.value = null },
                onAdded = { name ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Added to $name")
                    }
                }
            )
        }
    }
}
