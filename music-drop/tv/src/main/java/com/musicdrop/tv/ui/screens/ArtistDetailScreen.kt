package com.musicdrop.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicdrop.tv.data.model.UnifiedTrack
import com.musicdrop.tv.data.repository.YtMusicApiRepository
import com.musicdrop.tv.data.youtube.YouTubeSearchResult
import com.musicdrop.tv.ui.components.AddToPlaylistDialog
import com.musicdrop.tv.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun ArtistDetailScreen(
    channelId: String,
    initialName: String = "",
    initialThumb: String = "",
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenAlbum: (YtMusicApiRepository.YtCardItem) -> Unit = {}
) {
    val cached = remember(channelId) { YtMusicApiRepository.peekArtist(channelId) }
    var artistDetails by remember(channelId) { mutableStateOf<YtMusicApiRepository.YtArtistDetails?>(cached) }
    var loading by remember(channelId) { mutableStateOf(cached == null) }

    val likedMusic by viewModel.likedMusic.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val savedArtists by viewModel.savedArtists.collectAsState()
    val preparingKey by viewModel.preparingKey.collectAsState()
    val downloadingKeys = remember { mutableStateOf(setOf<String>()) }
    val playlistTrack = remember { mutableStateOf<UnifiedTrack?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val appColors = com.musicdrop.tv.ui.theme.LocalAppColors.current

    LaunchedEffect(channelId) {
        if (artistDetails == null) {
            loading = true
        }
        val details = YtMusicApiRepository.getArtist(channelId)
        if (details != null) {
            artistDetails = details
        }
        loading = false
    }

    val artistName = artistDetails?.name ?: initialName.ifBlank { "Artist" }
    val thumbUrl = artistDetails?.thumbnailUrl ?: initialThumb
    val isSaved = savedArtists.any { it.browseId == channelId }

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
                        text = artistName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ── Hero Banner & Info ─────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                            .border(2.dp, appColors.accentPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumbUrl.isNotBlank()) {
                            AsyncImage(
                                model = thumbUrl,
                                contentDescription = artistName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = artistName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val subs = artistDetails?.subscribers
                    if (!subs.isNullOrBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "$subs subscribers",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Play All, Shuffle & Save Artist Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val songs = artistDetails?.topSongs.orEmpty()
                                if (songs.isNotEmpty()) {
                                    viewModel.playYouTubeVideoWithContext(songs.first(), songs)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
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

                        Spacer(Modifier.width(10.dp))

                        OutlinedButton(
                            onClick = {
                                val songs = artistDetails?.topSongs.orEmpty()
                                if (songs.isNotEmpty()) {
                                    val shuffled = songs.shuffled()
                                    viewModel.playYouTubeVideoWithContext(shuffled.first(), shuffled)
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF444444)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
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

                        Spacer(Modifier.width(10.dp))

                        // Save / Bookmark Artist to Library
                        IconButton(
                            onClick = {
                                val chartArtist = com.musicdrop.tv.data.repository.YtMusicApiRepository.YtChartArtist(
                                    rank = "1",
                                    title = artistName,
                                    browseId = channelId,
                                    subscribers = subs.orEmpty(),
                                    thumbnailUrl = thumbUrl,
                                    trend = "up"
                                )
                                val nowSaved = viewModel.toggleSaveArtist(chartArtist)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(if (nowSaved) "Saved $artistName to Library" else "Removed from Library")
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSaved) appColors.accentPrimary else Color(0xFF222222))
                        ) {
                            Icon(
                                if (isSaved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (isSaved) "Saved" else "Save Artist",
                                tint = if (isSaved) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
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
                // ── Top Songs Shelf ─────────────────────────────────────────────
                val topSongs = artistDetails?.topSongs.orEmpty()
                if (topSongs.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text(
                                text = "Top Songs",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            topSongs.forEachIndexed { index, track ->
                                val unified = UnifiedTrack.Youtube(track)
                                val isLiked = likedMusic.any { it.key == unified.key }
                                val isDownloaded = downloadedTracks.any { it.key == unified.key }
                                val isDownloading = track.videoId in downloadingKeys.value
                                val isPrep = preparingKey == unified.key

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.playYouTubeVideoWithContext(track, topSongs) }
                                        .padding(horizontal = 16.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color(0xFF888888),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(24.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF222222))
                                    ) {
                                        AsyncImage(
                                            model = track.thumbnailUrl,
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (isPrep) {
                                            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.channelTitle,
                                            color = Color(0xFFAAAAAA),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                }

                // ── Albums Shelf ────────────────────────────────────────────────
                val albums = artistDetails?.albums.orEmpty()
                if (albums.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                            Text(
                                text = "Albums",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(albums) { album ->
                                    Column(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clickable { onOpenAlbum(album) }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF222222))
                                        ) {
                                            AsyncImage(
                                                model = album.thumbnailUrl,
                                                contentDescription = album.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            album.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            album.year ?: "Album",
                                            color = Color(0xFFAAAAAA),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Singles & EPs Shelf ─────────────────────────────────────────
                val singles = artistDetails?.singles.orEmpty()
                if (singles.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                            Text(
                                text = "Singles & EPs",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(singles) { single ->
                                    Column(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clickable { onOpenAlbum(single) }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF222222))
                                        ) {
                                            AsyncImage(
                                                model = single.thumbnailUrl,
                                                contentDescription = single.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            single.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            single.year ?: "Single",
                                            color = Color(0xFFAAAAAA),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
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
