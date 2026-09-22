package com.musicdrop.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicdrop.app.data.model.UnifiedTrack
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import com.musicdrop.app.ui.components.AddToPlaylistDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.Mic
import com.musicdrop.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SearchDashboardScreen(
    viewModel: MainViewModel,
    initialQuery: String = "",
    onBack: () -> Unit = {},
    onOpenYouTube: (String) -> Unit = {},
    onOpenArtist: (com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist) -> Unit = {},
    onOpenAlbum: (com.musicdrop.app.data.repository.YtMusicApiRepository.YtCardItem) -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf(initialQuery) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                searchQuery = spokenText
                viewModel.setYtSearchQuery(spokenText)
                viewModel.addRecentSearch(spokenText)
            }
        }
    }

    // Handle physical and software back navigation
    BackHandler {
        if (searchQuery.isNotBlank()) {
            searchQuery = ""
            viewModel.setYtSearchQuery("")
        } else {
            onBack()
        }
    }

    val ytResults by viewModel.ytSearchResults.collectAsState()
    val detailedResult by viewModel.detailedSearchResult.collectAsState()
    val ytLoading by viewModel.ytSearchLoading.collectAsState()
    val ytError by viewModel.ytSearchError.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val selectedFilter by viewModel.selectedSearchFilter.collectAsState()
    val likedMusic by viewModel.likedMusic.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val madeForYouRecommendations by viewModel.madeForYouRecommendations.collectAsState()
    val madeForYouTitle by viewModel.madeForYouTitle.collectAsState()

    // Download & UI state
    val downloadingIds = remember { mutableStateOf(setOf<String>()) }
    val playlistTrack = remember { mutableStateOf<UnifiedTrack?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && initialQuery != searchQuery) {
            searchQuery = initialQuery
            viewModel.setYtSearchQuery(initialQuery)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── 1. YouTube Music Style Top Header with Back Navigation ────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        if (searchQuery.isNotBlank()) {
                            searchQuery = ""
                            viewModel.setYtSearchQuery("")
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))

                // Search Input with subtle coral border (Screenshot 3)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF1E1E1E))
                        .border(1.2.dp, Color(0xFFE53935).copy(alpha = 0.65f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFFB0B0B0),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { q: String ->
                                searchQuery = q
                                viewModel.setYtSearchQuery(q)
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 14.sp
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE53935)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                if (searchQuery.isNotBlank()) {
                                    viewModel.addRecentSearch(searchQuery)
                                    viewModel.setYtSearchQuery(searchQuery)
                                }
                            }),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search songs, artists, albums…",
                                        color = Color(0xFF888888),
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    viewModel.setYtSearchQuery("")
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFFB0B0B0), modifier = Modifier.size(18.dp))
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search songs, artists, or albums…")
                                    }
                                    try {
                                        speechLauncher.launch(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Voice search is not supported on this device", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Mic,
                                    contentDescription = "Voice Search",
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── 2. Filter Pills ────────────────────────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(com.musicdrop.app.data.youtube.YouTubeMusicRepository.SearchFilter.values().toList()) { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color.White else Color(0xFF262626))
                            .clickable { viewModel.setSearchFilter(filter) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = filter.label,
                            color = if (isSelected) Color.Black else Color(0xFFCCCCCC),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // ── 3. Search Suggestions & Recent Searches vs Search Results ─────────
            Box(modifier = Modifier.weight(1f)) {
                when {
                    ytLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFE53935), modifier = Modifier.size(36.dp))
                        }
                    }

                    // Empty query: Show Recent Searches & Trending Suggestions
                    searchQuery.isBlank() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Recent Searches
                            if (recentSearches.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Recent searches",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Clear all",
                                            color = Color(0xFFE53935),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.clickable { viewModel.clearRecentSearches() }
                                        )
                                    }
                                }
                                items(recentSearches) { query ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                searchQuery = query
                                                viewModel.setYtSearchQuery(query)
                                            }
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Text(
                                            query,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeRecentSearch(query) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color.DarkGray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Made For You / Personalized Taste Shelf
                            if (madeForYouRecommendations.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        madeForYouTitle,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(madeForYouRecommendations.take(12), key = { it.key }) { track ->
                                            Column(
                                                modifier = Modifier
                                                    .width(120.dp)
                                                    .clickable {
                                                        viewModel.playUnified(track, madeForYouRecommendations)
                                                    }
                                            ) {
                                                AsyncImage(
                                                    model = track.thumbnailUrl,
                                                    contentDescription = track.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(120.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color(0xFF242424))
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    track.title,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    track.artist,
                                                    color = Color.Gray,
                                                    fontSize = 10.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Trending searches
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Trending Searches",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val trendingTopics = listOf(
                                "Arijit Singh Hits", "Trending Bollywood 2026", "The Weeknd",
                                "Taylor Swift", "Top English Pop", "Workout Energetic Beats",
                                "Late Night Lo-Fi", "EDM Dance Party"
                            )
                            items(trendingTopics) { topic ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            searchQuery = topic
                                            viewModel.addRecentSearch(topic)
                                            viewModel.setYtSearchQuery(topic)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color(0xFFFF7043),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Text(
                                        topic,
                                        color = Color(0xFFDDDDDD),
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Default.NorthWest,
                                        contentDescription = null,
                                        tint = Color.DarkGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Results list
                    ytResults.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // ── Top Result (Artist) ──────────────────────────────
                            val topArtist = detailedResult?.topArtist
                            if (topArtist != null) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF222222))
                                            .clickable { onOpenArtist(topArtist) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = topArtist.thumbnailUrl,
                                            contentDescription = topArtist.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = topArtist.title,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Artist" + if (topArtist.subscribers.isNotBlank()) " • ${topArtist.subscribers}" else "",
                                                color = Color(0xFFAAAAAA),
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(
                                            "View",
                                            color = Color(0xFFFF0033),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                    }
                                }
                            }

                            // ── Albums shelf ─────────────────────────────────────
                            val albums = detailedResult?.albums.orEmpty()
                            if (albums.isNotEmpty()) {
                                item {
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(
                                            "Albums & Singles",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(albums) { album ->
                                                Column(
                                                    modifier = Modifier
                                                        .width(110.dp)
                                                        .clickable { onOpenAlbum(album) }
                                                ) {
                                                    AsyncImage(
                                                        model = album.thumbnailUrl,
                                                        contentDescription = album.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(110.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                    )
                                                    Spacer(Modifier.height(6.dp))
                                                    Text(
                                                        album.title,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val subtitle = listOfNotNull(album.type, album.year).filter { it.isNotBlank() }.joinToString(" • ")
                                                    if (subtitle.isNotBlank()) {
                                                        Text(
                                                            subtitle,
                                                            color = Color.Gray,
                                                            fontSize = 11.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            items(ytResults, key = { it.videoId }) { result ->
                                val unified = UnifiedTrack.Youtube(result)
                                val isLiked = likedMusic.any { it.key == unified.key }
                                val isDownloaded = downloadedTracks.any { it.key == unified.key }
                                val isDownloading = result.videoId in downloadingIds.value

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF181818))
                                        .clickable {
                                            if (searchQuery.isNotBlank()) {
                                                viewModel.addRecentSearch(searchQuery)
                                            }
                                            viewModel.playYouTubeVideoWithContext(result, ytResults)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF242424))
                                    ) {
                                        AsyncImage(
                                            model = result.thumbnailUrl,
                                            contentDescription = result.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Rounded.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    // Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = result.channelTitle,
                                                color = Color(0xFFAAAAAA),
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (result.duration.isNotBlank()) {
                                                Text(
                                                    text = " • ${result.duration}",
                                                    color = Color(0xFF777777),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(4.dp))

                                    // Like Button
                                    IconButton(
                                        onClick = { viewModel.toggleLikeUnifiedTrack(unified) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = "Like",
                                            tint = if (isLiked) Color(0xFFE53935) else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Add to Playlist Button
                                    IconButton(
                                        onClick = { playlistTrack.value = unified },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PlaylistAdd,
                                            contentDescription = "Add to Playlist",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Download Button
                                    IconButton(
                                        onClick = {
                                            if (!isDownloading && !isDownloaded) {
                                                downloadingIds.value = downloadingIds.value + result.videoId
                                                viewModel.downloadYouTubeAudio(result) { success, _ ->
                                                    downloadingIds.value = downloadingIds.value - result.videoId
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            if (success) "Saved to Downloads: ${result.title}" else "Download failed"
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        if (isDownloading) {
                                            CircularProgressIndicator(
                                                color = Color(0xFFE53935),
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else if (isDownloaded) {
                                            Icon(
                                                Icons.Rounded.CheckCircle,
                                                contentDescription = "Downloaded",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Icon(
                                                Icons.Rounded.Download,
                                                contentDescription = "Download",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ytError != null -> {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                ytError ?: "No results found",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp)
        )

        // Add to Playlist Dialog
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
