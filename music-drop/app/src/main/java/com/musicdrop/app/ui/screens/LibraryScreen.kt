package com.musicdrop.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.core.content.ContextCompat
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.model.UnifiedTrack
import com.musicdrop.app.data.repository.DownloadedTrack
import com.musicdrop.app.data.repository.LikedMusicItem
import com.musicdrop.app.data.repository.UserPlaylistItem
import com.musicdrop.app.ui.viewmodel.MainViewModel

enum class LibraryMainSection(val label: String, val icon: ImageVector) {
    DEVICE_STORAGE("Device Storage", Icons.Rounded.PhoneAndroid),
    DOWNLOADS("Downloads", Icons.Rounded.Download),
    ONLINE_LIBRARY("Playlists & Liked", Icons.Rounded.LibraryMusic)
}

enum class DeviceSubCategory(val label: String) {
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    FOLDERS("Folders")
}

enum class DownloadSubCategory(val label: String) {
    SONGS("Music"),
    VIDEOS("Videos"),
    ARTISTS("Artists"),
    ALBUMS("Albums")
}

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onOpenSearch: (query: String) -> Unit,
    onOpenArtist: (com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist) -> Unit = {},
    onOpenAlbum: (com.musicdrop.app.data.repository.YtMusicApiRepository.YtCardItem) -> Unit = {}
) {
    var activeSection by remember { mutableStateOf(LibraryMainSection.DEVICE_STORAGE) }
    var deviceCategory by remember { mutableStateOf(DeviceSubCategory.SONGS) }
    var downloadCategory by remember { mutableStateOf(DownloadSubCategory.SONGS) }

    // State collections
    val songs by viewModel.songs.collectAsState()
    val allAudio by viewModel.allAudio.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val likedMusic by viewModel.likedMusic.collectAsState()
    val userPlaylists by viewModel.userPlaylists.collectAsState()
    val savedArtists by viewModel.savedArtists.collectAsState()
    val savedAlbums by viewModel.savedAlbums.collectAsState()
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current

    // Search query for local filtering
    var filterQuery by remember { mutableStateOf("") }

    // Drill down navigation for local categories
    var drillDownTitle by remember { mutableStateOf<String?>(null) }
    var drillDownSubtitle by remember { mutableStateOf<String?>(null) }
    var drillDownTracks by remember { mutableStateOf<List<MediaItem>?>(null) }
    var drillDownDownloadedTracks by remember { mutableStateOf<List<DownloadedTrack>?>(null) }

    // Sub-screen navigation: "liked", "episodes", or playlist ID
    var activeSubView by remember { mutableStateOf<String?>(null) }

    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // Effective local songs list (filtered if user types search query)
    val effectiveLocalSongs = remember(songs, allAudio) {
        if (songs.isNotEmpty()) songs else allAudio.filter { it.isSong || it.durationMs > 25_000L }
    }

    if (activeSubView != null) {
        when (activeSubView) {
            "liked" -> LikedMusicDetailView(
                viewModel = viewModel,
                likedList = likedMusic,
                onBack = { activeSubView = null }
            )
            "episodes" -> EpisodesDetailView(
                onBack = { activeSubView = null }
            )
            else -> {
                val pl = userPlaylists.firstOrNull { it.id == activeSubView }
                if (pl != null) {
                    UserPlaylistDetailView(
                        viewModel = viewModel,
                        playlist = pl,
                        onBack = { activeSubView = null }
                    )
                } else {
                    activeSubView = null
                }
            }
        }
        return
    }

    // Drill down list view for local artist/album/folder
    if (drillDownTracks != null && drillDownTitle != null) {
        LocalDrillDownView(
            title = drillDownTitle!!,
            subtitle = drillDownSubtitle.orEmpty(),
            tracks = drillDownTracks!!,
            viewModel = viewModel,
            onBack = {
                drillDownTracks = null
                drillDownTitle = null
                drillDownSubtitle = null
            }
        )
        return
    }

    // Drill down list view for downloaded artist/album
    if (drillDownDownloadedTracks != null && drillDownTitle != null) {
        DownloadedDrillDownView(
            title = drillDownTitle!!,
            subtitle = drillDownSubtitle.orEmpty(),
            tracks = drillDownDownloadedTracks!!,
            viewModel = viewModel,
            onBack = {
                drillDownDownloadedTracks = null
                drillDownTitle = null
                drillDownSubtitle = null
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── TOP HEADER BAR ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Library",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onOpenSearch("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
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
                            .background(Color(0xFFD81B60)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "O",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── 1. PRIMARY SECTION SEGREGATION TABS ───────────────────────────────────
            // Explicitly separate Local Device Storage from Downloads so they are never mixed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LibraryMainSection.values().forEach { section ->
                    val isSelected = activeSection == section
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) appColors.accentPrimary else Color.Transparent)
                            .clickable {
                                activeSection = section
                                filterQuery = ""
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = section.label,
                            tint = if (isSelected) Color.Black else Color(0xFFAAAAAA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = section.label,
                            color = if (isSelected) Color.Black else Color(0xFFAAAAAA),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ── 2. SECTION-SPECIFIC CONTENT ──────────────────────────────────────────
            when (activeSection) {
                LibraryMainSection.DEVICE_STORAGE -> {
                    DeviceStorageView(
                        category = deviceCategory,
                        onSelectCategory = { deviceCategory = it },
                        songs = effectiveLocalSongs,
                        filterQuery = filterQuery,
                        onFilterChange = { filterQuery = it },
                        viewModel = viewModel,
                        onDrillDown = { title, sub, tracks ->
                            drillDownTitle = title
                            drillDownSubtitle = sub
                            drillDownTracks = tracks
                        }
                    )
                }
                LibraryMainSection.DOWNLOADS -> {
                    DownloadsView(
                        category = downloadCategory,
                        onSelectCategory = { downloadCategory = it },
                        downloads = downloadedTracks,
                        filterQuery = filterQuery,
                        onFilterChange = { filterQuery = it },
                        viewModel = viewModel,
                        onDrillDown = { title, sub, tracks ->
                            drillDownTitle = title
                            drillDownSubtitle = sub
                            drillDownDownloadedTracks = tracks
                        }
                    )
                }
                LibraryMainSection.ONLINE_LIBRARY -> {
                    OnlineLibraryView(
                        likedMusic = likedMusic,
                        userPlaylists = userPlaylists,
                        savedArtists = savedArtists,
                        savedAlbums = savedAlbums,
                        onOpenLiked = { activeSubView = "liked" },
                        onOpenPlaylist = { activeSubView = it.id },
                        onOpenEpisodes = { activeSubView = "episodes" },
                        onOpenArtist = onOpenArtist,
                        onOpenAlbum = onOpenAlbum,
                        onCreatePlaylist = { showNewPlaylistDialog = true }
                    )
                }
            }
        }
    }

    // Dialog for creating user playlist
    if (showNewPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showNewPlaylistDialog = false },
            title = { Text("New Playlist", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist Name", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = appColors.accentPrimary,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                            showNewPlaylistDialog = false
                        }
                    }
                ) {
                    Text("Create", color = appColors.accentPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPlaylistDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. DEVICE STORAGE TABBED VIEW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DeviceStorageView(
    category: DeviceSubCategory,
    onSelectCategory: (DeviceSubCategory) -> Unit,
    songs: List<MediaItem>,
    filterQuery: String,
    onFilterChange: (String) -> Unit,
    viewModel: MainViewModel,
    onDrillDown: (title: String, subtitle: String, tracks: List<MediaItem>) -> Unit
) {
    val context = LocalContext.current
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                audioPermission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.loadData()
            android.widget.Toast.makeText(context, "Scanning local device songs...", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Sub-Category Tabs (Songs / Artists / Albums / Folders)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DeviceSubCategory.values().forEach { subCat ->
            val isSelected = category == subCat
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Color.White else Color(0xFF242424))
                    .clickable { onSelectCategory(subCat) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = subCat.label,
                    color = if (isSelected) Color.Black else Color(0xFFCCCCCC),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }

    // Instant filter / search box
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = filterQuery,
            onValueChange = onFilterChange,
            placeholder = { Text("Search local device ${category.label.lowercase()}...", color = Color.Gray, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
    }

    val filteredSongs = remember(songs, filterQuery) {
        if (filterQuery.isBlank()) songs
        else songs.filter {
            it.name.contains(filterQuery, ignoreCase = true) ||
            it.artist.contains(filterQuery, ignoreCase = true) ||
            it.album.contains(filterQuery, ignoreCase = true)
        }
    }

    when (category) {
        DeviceSubCategory.SONGS -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                // Header action row (Count & Shuffle All)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${filteredSongs.size} device songs",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rescan device storage button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF262626))
                                    .clickable {
                                        if (!hasAudioPermission) {
                                            audioPermissionLauncher.launch(audioPermission)
                                        } else {
                                            viewModel.loadData()
                                            android.widget.Toast.makeText(context, "Rescanning storage...", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Rescan", tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Rescan", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }

                            if (filteredSongs.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF262626))
                                        .clickable {
                                            val shuffled = filteredSongs.shuffled()
                                            viewModel.playTrack(shuffled.first(), shuffled)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = appColors.accentPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Shuffle All", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                if (filteredSongs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (!hasAudioPermission) Icons.Rounded.FolderShared else Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = Color(0xFFFF0000),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = if (!hasAudioPermission) "Allow Access to Local Songs" else "No Local Songs Found",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (!hasAudioPermission)
                                        "Grant storage permission so MusicDrop can scan and play music files stored on your device."
                                    else if (filterQuery.isNotBlank())
                                        "No songs matched \"$filterQuery\"."
                                    else
                                        "No audio files found in Music or Downloads folder. Add songs to your device or tap Rescan.",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(18.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (!hasAudioPermission) {
                                        Button(
                                            onClick = { audioPermissionLauncher.launch(audioPermission) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text("Grant Permission", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.loadData()
                                            android.widget.Toast.makeText(context, "Rescanning device storage...", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, Color(0xFF555555))
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Rescan Storage", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredSongs, key = { "local_song_${it.id}" }) { song ->
                        LocalSongRow(
                            song = song,
                            onClick = { viewModel.playTrack(song, filteredSongs) }
                        )
                    }
                }
            }
        }

        DeviceSubCategory.ARTISTS -> {
            val artistsMap = remember(songs, filterQuery) {
                songs
                    .groupBy { it.artist.ifBlank { "Unknown Artist" } }
                    .filter { if (filterQuery.isBlank()) true else it.key.contains(filterQuery, ignoreCase = true) }
                    .toList()
                    .sortedByDescending { it.second.size }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                item {
                    Text(
                        text = "${artistsMap.size} artists on device",
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(artistsMap, key = { "local_artist_${it.first}" }) { (artistName, trackList) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDrillDown(artistName, "${trackList.size} device songs", trackList) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF262626)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = appColors.accentPrimary, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(artistName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text("${trackList.size} songs", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.playTrack(trackList.first(), trackList) }) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = "Play Artist", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }

        DeviceSubCategory.ALBUMS -> {
            val albumsMap = remember(songs, filterQuery) {
                songs
                    .groupBy { it.album.ifBlank { "Unknown Album" } }
                    .filter { if (filterQuery.isBlank()) true else it.key.contains(filterQuery, ignoreCase = true) }
                    .toList()
                    .sortedByDescending { it.second.size }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                item {
                    Text(
                        text = "${albumsMap.size} albums on device",
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(albumsMap, key = { "local_album_${it.first}" }) { (albumName, trackList) ->
                    val firstTrack = trackList.firstOrNull()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDrillDown(albumName, "${firstTrack?.artist.orEmpty()} • ${trackList.size} songs", trackList) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222222)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (firstTrack?.albumArtUri != null) {
                                AsyncImage(
                                    model = firstTrack.albumArtUri,
                                    contentDescription = albumName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Rounded.Album, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(albumName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text("${firstTrack?.artist.orEmpty()} • ${trackList.size} songs", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.playTrack(trackList.first(), trackList) }) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = "Play Album", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }

        DeviceSubCategory.FOLDERS -> {
            val foldersMap = remember(songs, filterQuery) {
                songs
                    .groupBy { it.folderName }
                    .filter { if (filterQuery.isBlank()) true else it.key.contains(filterQuery, ignoreCase = true) }
                    .toList()
                    .sortedByDescending { it.second.size }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                item {
                    Text(
                        text = "${foldersMap.size} folders with audio",
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(foldersMap, key = { "local_folder_${it.first}" }) { (folderName, trackList) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDrillDown(folderName, "${trackList.size} device songs", trackList) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF262626)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Folder, contentDescription = null, tint = appColors.accentPrimary, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folderName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text("${trackList.size} audio files", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.playTrack(trackList.first(), trackList) }) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = "Play Folder", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. DOWNLOADS TABBED VIEW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DownloadsView(
    category: DownloadSubCategory,
    onSelectCategory: (DownloadSubCategory) -> Unit,
    downloads: List<DownloadedTrack>,
    filterQuery: String,
    onFilterChange: (String) -> Unit,
    viewModel: MainViewModel,
    onDrillDown: (title: String, subtitle: String, tracks: List<DownloadedTrack>) -> Unit
) {
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current

    // Sub-Category Tabs (Songs / Artists / Albums / Playlists)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DownloadSubCategory.values().forEach { subCat ->
            val isSelected = category == subCat
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Color.White else Color(0xFF242424))
                    .clickable { onSelectCategory(subCat) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = subCat.label,
                    color = if (isSelected) Color.Black else Color(0xFFCCCCCC),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }

    // Instant filter / search box
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = filterQuery,
            onValueChange = onFilterChange,
            placeholder = { Text("Search downloaded ${category.label.lowercase()}...", color = Color.Gray, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
    }

    val filteredDownloads = remember(downloads, filterQuery) {
        if (filterQuery.isBlank()) downloads
        else downloads.filter {
            it.title.contains(filterQuery, ignoreCase = true) ||
            it.artist.contains(filterQuery, ignoreCase = true)
        }
    }

    val audioDownloads = remember(downloads, filterQuery) {
        downloads.filter {
            !it.mimeType.startsWith("video") &&
            !it.filePath.endsWith(".mp4", ignoreCase = true) &&
            !it.key.startsWith("yt_video:")
        }.filter {
            if (filterQuery.isBlank()) true
            else it.title.contains(filterQuery, ignoreCase = true) || it.artist.contains(filterQuery, ignoreCase = true)
        }
    }

    val videoDownloads = remember(downloads, filterQuery) {
        downloads.filter {
            it.mimeType.startsWith("video") ||
            it.filePath.endsWith(".mp4", ignoreCase = true) ||
            it.key.startsWith("yt_video:")
        }.filter {
            if (filterQuery.isBlank()) true
            else it.title.contains(filterQuery, ignoreCase = true) || it.artist.contains(filterQuery, ignoreCase = true)
        }
    }

    when (category) {
        DownloadSubCategory.SONGS -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${audioDownloads.size} offline audio songs",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )
                        if (audioDownloads.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF262626))
                                    .clickable {
                                        val shuffled = audioDownloads.shuffled()
                                        viewModel.playDownloadedTrack(shuffled.first(), shuffled)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = appColors.accentPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Shuffle All", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                if (audioDownloads.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (filterQuery.isNotBlank()) "No downloaded music matching \"$filterQuery\""
                                else "No downloaded music yet.\nTap the download icon on any song to listen offline!",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(audioDownloads, key = { it.key }) { track ->
                        DownloadedSongRow(
                            track = track,
                            onPlay = { viewModel.playDownloadedTrack(track, audioDownloads) },
                            onDelete = { viewModel.deleteDownloadedTrack(track) }
                        )
                    }
                }
            }
        }

        DownloadSubCategory.VIDEOS -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${videoDownloads.size} offline video songs",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )
                        if (videoDownloads.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF262626))
                                    .clickable {
                                        viewModel.playDownloadedTrack(videoDownloads.first(), videoDownloads)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Play All Videos", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                if (videoDownloads.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (filterQuery.isNotBlank()) "No downloaded videos matching \"$filterQuery\""
                                else "No downloaded video songs yet.\nTap the download icon and choose \"Download Video (MP4)\" to watch offline videos!",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(videoDownloads, key = { it.key }) { track ->
                        DownloadedSongRow(
                            track = track,
                            onPlay = { viewModel.playDownloadedTrack(track, videoDownloads) },
                            onDelete = { viewModel.deleteDownloadedTrack(track) }
                        )
                    }
                }
            }
        }

        DownloadSubCategory.ARTISTS -> {
            val artistsMap = remember(downloads, filterQuery) {
                downloads
                    .groupBy { it.artist.ifBlank { "Unknown Artist" } }
                    .filter { if (filterQuery.isBlank()) true else it.key.contains(filterQuery, ignoreCase = true) }
                    .toList()
                    .sortedByDescending { it.second.size }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                item {
                    Text(
                        text = "${artistsMap.size} downloaded artists",
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(artistsMap, key = { "dl_artist_${it.first}" }) { (artistName, trackList) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDrillDown(artistName, "${trackList.size} offline tracks", trackList) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF262626)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = appColors.accentPrimary, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(artistName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text("${trackList.size} offline songs", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.playDownloadedTrack(trackList.first(), trackList) }) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = "Play Artist", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }

        DownloadSubCategory.ALBUMS -> {
            val albumsMap = remember(downloads, filterQuery) {
                downloads
                    .groupBy { "Downloaded Tracks" }
                    .filter { if (filterQuery.isBlank()) true else it.key.contains(filterQuery, ignoreCase = true) }
                    .toList()
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                items(albumsMap) { (albumName, trackList) ->
                    val firstTrack = trackList.firstOrNull()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDrillDown(albumName, "${trackList.size} offline songs", trackList) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222222)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!firstTrack?.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = firstTrack?.coverUrl,
                                    contentDescription = albumName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Rounded.Album, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(albumName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text("${trackList.size} offline songs", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.playDownloadedTrack(trackList.first(), trackList) }) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = "Play Album", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }


    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. ONLINE LIBRARY & PLAYLISTS VIEW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OnlineLibraryView(
    likedMusic: List<LikedMusicItem>,
    userPlaylists: List<UserPlaylistItem>,
    savedArtists: List<com.musicdrop.app.data.repository.SavedArtistItem>,
    savedAlbums: List<com.musicdrop.app.data.repository.SavedAlbumItem>,
    onOpenLiked: () -> Unit,
    onOpenPlaylist: (UserPlaylistItem) -> Unit,
    onOpenEpisodes: () -> Unit,
    onOpenArtist: (com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist) -> Unit,
    onOpenAlbum: (com.musicdrop.app.data.repository.YtMusicApiRepository.YtCardItem) -> Unit,
    onCreatePlaylist: () -> Unit
) {
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
    ) {
        // Liked Music Pinned Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenLiked)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF7B1FA2), Color(0xFFE91E63)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ThumbUp, contentDescription = "Liked Music", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Liked Music", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text("📌 Auto playlist • ${likedMusic.size} songs", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                }
            }
        }

        // Episodes for Later
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenEpisodes)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFF00897B)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.BookmarkBorder, contentDescription = "Episodes", tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Episodes for later", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text("📌 Auto playlist • Saved for later", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                }
            }
        }

        // New Playlist Action
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreatePlaylist)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF242424)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "New Playlist", tint = appColors.accentPrimary, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text("New playlist", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // User Custom Playlists
        items(userPlaylists, key = { it.id }) { pl ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPlaylist(pl) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF262626)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = appColors.accentPrimary, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(pl.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text("${pl.tracks.size} songs • Custom playlist", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                }
            }
        }

        // Saved Artists Section
        if (savedArtists.isNotEmpty()) {
            item {
                Text(
                    text = "Saved Artists",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            items(savedArtists, key = { "saved_artist_${it.browseId}" }) { artist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenArtist(
                                com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist(
                                    title = artist.name,
                                    browseId = artist.browseId,
                                    subscribers = artist.subscribers,
                                    thumbnailUrl = artist.thumbnailUrl
                                )
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF262626)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artist.thumbnailUrl.isNotBlank()) {
                            AsyncImage(model = artist.thumbnailUrl, contentDescription = artist.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artist.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(artist.subscribers.ifBlank { "Artist" }, color = Color(0xFFAAAAAA), fontSize = 12.sp)
                    }
                }
            }
        }

        // Saved Albums Section
        if (savedAlbums.isNotEmpty()) {
            item {
                Text(
                    text = "Saved Albums",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            items(savedAlbums, key = { "saved_album_${it.browseId}" }) { album ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenAlbum(
                                com.musicdrop.app.data.repository.YtMusicApiRepository.YtCardItem(
                                    title = album.title,
                                    browseId = album.browseId,
                                    thumbnailUrl = album.thumbnailUrl,
                                    type = album.type,
                                    year = album.year
                                )
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF222222)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (album.thumbnailUrl.isNotBlank()) {
                            AsyncImage(model = album.thumbnailUrl, contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Rounded.Album, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(album.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text("${album.type.orEmpty()} • ${album.year.orEmpty()}", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE ROW & DRILL-DOWN COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LocalSongRow(
    song: MediaItem,
    onClick: () -> Unit
) {
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF222222)),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = song.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = appColors.accentPrimary, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${song.artist} • ${song.formattedDuration.ifBlank { song.formattedSize }}",
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.Gray, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun DownloadedSongRow(
    track: DownloadedTrack,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF222222)),
            contentAlignment = Alignment.Center
        ) {
            val isVideoTrack = track.mimeType.startsWith("video") || track.filePath.endsWith(".mp4", ignoreCase = true) || track.key.startsWith("yt_video:")
            if (track.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(if (isVideoTrack) Icons.Rounded.Videocam else Icons.Rounded.MusicNote, contentDescription = null, tint = Color.Gray)
            }
            if (isVideoTrack) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Red.copy(alpha = 0.85f))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text("VIDEO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${track.artist} • ${track.duration}",
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun LocalDrillDownView(
    title: String,
    subtitle: String,
    tracks: List<MediaItem>,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        // Action Buttons Row (Play All, Shuffle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { if (tracks.isNotEmpty()) viewModel.playTrack(tracks.first(), tracks) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    if (tracks.isNotEmpty()) {
                        val shuffled = tracks.shuffled()
                        viewModel.playTrack(shuffled.first(), shuffled)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = appColors.accentPrimary)
                Spacer(Modifier.width(6.dp))
                Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(tracks, key = { "drill_${it.id}" }) { song ->
                LocalSongRow(song = song, onClick = { viewModel.playTrack(song, tracks) })
            }
        }
    }
}

@Composable
private fun DownloadedDrillDownView(
    title: String,
    subtitle: String,
    tracks: List<DownloadedTrack>,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { if (tracks.isNotEmpty()) viewModel.playDownloadedTrack(tracks.first(), tracks) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    if (tracks.isNotEmpty()) {
                        val shuffled = tracks.shuffled()
                        viewModel.playDownloadedTrack(shuffled.first(), shuffled)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = appColors.accentPrimary)
                Spacer(Modifier.width(6.dp))
                Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(tracks, key = { "dl_drill_${it.key}" }) { track ->
                DownloadedSongRow(
                    track = track,
                    onPlay = { viewModel.playDownloadedTrack(track, tracks) },
                    onDelete = { viewModel.deleteDownloadedTrack(track) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DETAIL VIEWS (Liked Music, Episodes, Custom Playlists)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LikedMusicDetailView(
    viewModel: MainViewModel,
    likedList: List<LikedMusicItem>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("Liked Music", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF7B1FA2), Color(0xFFE91E63)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ThumbUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Liked Music", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Auto playlist • ${likedList.size} songs", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.playAllLikedMusic() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        if (likedList.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No liked songs yet.\nTap the heart icon on any song to save it here!", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
                items(likedList, key = { it.key }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playLikedItem(item, likedList) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222222))
                        ) {
                            if (item.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.coverUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text(item.artist, color = Color(0xFFAAAAAA), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { viewModel.toggleLikeUnifiedTrack(item.toUnifiedTrack()) }) {
                            Icon(Icons.Rounded.Favorite, contentDescription = "Unlike", tint = Color(0xFFFF3B30), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodesDetailView(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("Episodes for later", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.BookmarkBorder, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No saved episodes yet",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Save episodes from Podcasts to listen later",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun UserPlaylistDetailView(
    viewModel: MainViewModel,
    playlist: UserPlaylistItem,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(playlist.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1565C0), Color(0xFF00ACC1)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(playlist.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("${playlist.tracks.size} songs • Playlist", color = Color.Gray, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.playUserPlaylist(playlist) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(4.dp))
                    Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (playlist.tracks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Playlist is empty.\nTap the + or ... menu on any song to add it here!", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
                items(playlist.tracks, key = { it.key }) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playLikedItem(track, playlist.tracks) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222222))
                        ) {
                            if (track.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = track.coverUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text(track.artist, color = Color(0xFFAAAAAA), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { viewModel.removeTrackFromPlaylist(playlist.id, track.key) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
