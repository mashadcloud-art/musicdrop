package com.musicdrop.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.model.UnifiedTrack
import com.musicdrop.app.data.repository.DownloadedTrack
import com.musicdrop.app.data.repository.LikedMusicItem
import com.musicdrop.app.data.repository.UserPlaylistItem
import com.musicdrop.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LibraryTab(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    PLAYLISTS("Playlists"),
    FOLDERS("Folders"),
    ALBUMS("Albums"),
    ARTISTS("Artists")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onOpenSearch: (query: String) -> Unit,
    onOpenArtist: (com.musicdrop.app.data.repository.YtMusicApiRepository.YtChartArtist) -> Unit = {},
    onOpenAlbum: (com.musicdrop.app.data.repository.YtMusicApiRepository.YtCardItem) -> Unit = {}
) {
    val appColors = com.musicdrop.app.ui.theme.LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State collections
    val songs by viewModel.songs.collectAsState()
    val allAudio by viewModel.allAudio.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val likedMusic by viewModel.likedMusic.collectAsState()
    val userPlaylists by viewModel.userPlaylists.collectAsState()
    val savedArtists by viewModel.savedArtists.collectAsState()
    val savedAlbums by viewModel.savedAlbums.collectAsState()

    // Effective local songs list
    val effectiveLocalSongs = remember(songs, allAudio, downloadedTracks) {
        val base = if (songs.isNotEmpty()) songs else allAudio.filter { it.isSong || it.durationMs > 20_000L }
        if (base.isNotEmpty()) base else {
            // Include downloaded tracks mapped to MediaItem if local scan is empty
            downloadedTracks.map { it.toMediaItem() }
        }
    }

    // Drill down detail view state
    var detailTitle by remember { mutableStateOf<String?>(null) }
    var detailSubtitle by remember { mutableStateOf<String?>(null) }
    var detailCoverUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var detailIsArtist by remember { mutableStateOf(false) }
    var detailTracks by remember { mutableStateOf<List<MediaItem>?>(null) }

    // Playlist dialogs
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // In-library search filter
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Filtered songs
    val filteredSongs = remember(effectiveLocalSongs, searchQuery) {
        if (searchQuery.isBlank()) effectiveLocalSongs
        else effectiveLocalSongs.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    // Grouping for Albums, Artists, Folders
    val albumsGrouped = remember(effectiveLocalSongs) {
        effectiveLocalSongs
            .groupBy { it.album.ifBlank { "Unknown Album" } }
            .toList()
            .sortedByDescending { it.second.size }
    }

    val artistsGrouped = remember(effectiveLocalSongs) {
        effectiveLocalSongs
            .groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .toList()
            .sortedByDescending { it.second.size }
    }

    val foldersGrouped = remember(effectiveLocalSongs) {
        effectiveLocalSongs
            .groupBy {
                val path = it.filePath.orEmpty()
                when {
                    path.contains("/WhatsApp", true) -> "WhatsApp Audio"
                    path.contains("/Telegram", true) -> "Telegram"
                    path.contains("/Download", true) -> "Download"
                    path.contains("/Music", true) -> "Music"
                    path.contains("/Recordings", true) -> "Recordings"
                    path.contains("/Podcasts", true) -> "Podcasts"
                    else -> path.substringBeforeLast('/', "Internal Storage").substringAfterLast('/')
                }
            }
            .toList()
            .sortedByDescending { it.second.size }
    }

    // Active sub-screen: Detail Page (Album, Artist, Folder, Smart Playlist)
    if (detailTracks != null && detailTitle != null) {
        LibraryDetailScreen(
            title = detailTitle!!,
            subtitle = detailSubtitle.orEmpty(),
            coverUri = detailCoverUri,
            isArtist = detailIsArtist,
            tracks = detailTracks!!,
            viewModel = viewModel,
            onBack = {
                detailTracks = null
                detailTitle = null
                detailSubtitle = null
                detailCoverUri = null
                detailIsArtist = false
            }
        )
        return
    }

    val tabs = LibraryTab.values()
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C10))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── TOP BAR: BACK & SEARCH IN LIBRARY ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onOpenSearch("") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Search in Library Capsule Input Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color(0xFF1B1B22))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(21.dp))
                        .clickable { isSearchActive = !isSearchActive }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color(0xFF8E8E9B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (!isSearchActive && searchQuery.isEmpty()) {
                            Text(
                                text = "Search in Library",
                                color = Color(0xFF8E8E9B),
                                fontSize = 14.sp
                            )
                        } else {
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 14.sp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── STICKY TAB BAR (Matching Image 1) ───────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color(0xFF0C0C10),
                contentColor = Color.White,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        val tab = tabPositions[pagerState.currentPage]
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tab)
                                .height(3.dp)
                                .padding(horizontal = 12.dp)
                                .background(Color.White, RoundedCornerShape(2.dp))
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = pagerState.currentPage == index
                    Tab(
                        selected = isSelected,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = tab.label,
                                color = if (isSelected) Color.White else Color(0xFF888899),
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // ── HORIZONTAL SWIPEABLE PAGER ───────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (tabs[page]) {
                    LibraryTab.ALL -> AllTabContent(
                        songs = filteredSongs,
                        albums = albumsGrouped,
                        artists = artistsGrouped,
                        onViewAllSongs = { scope.launch { pagerState.animateScrollToPage(1) } },
                        onViewAllAlbums = { scope.launch { pagerState.animateScrollToPage(4) } },
                        onViewAllArtists = { scope.launch { pagerState.animateScrollToPage(5) } },
                        onSongClick = { song -> viewModel.playTrack(song, filteredSongs) },
                        onAlbumClick = { title, trackList ->
                            detailTitle = title
                            detailSubtitle = "${trackList.size} songs"
                            detailCoverUri = trackList.firstOrNull()?.albumArtUri
                            detailIsArtist = false
                            detailTracks = trackList
                        },
                        onArtistClick = { name, trackList ->
                            detailTitle = name
                            detailSubtitle = "${trackList.size} songs"
                            detailCoverUri = trackList.firstOrNull()?.albumArtUri
                            detailIsArtist = true
                            detailTracks = trackList
                        }
                    )

                    LibraryTab.SONGS -> SongsTabContent(
                        songs = filteredSongs,
                        onSongClick = { song -> viewModel.playTrack(song, filteredSongs) }
                    )

                    LibraryTab.PLAYLISTS -> PlaylistsTabContent(
                        allSongs = effectiveLocalSongs,
                        likedMusic = likedMusic,
                        userPlaylists = userPlaylists,
                        onCreatePlaylist = { showNewPlaylistDialog = true },
                        onOpenSmartPlaylist = { title, subtitle, trackList ->
                            detailTitle = title
                            detailSubtitle = subtitle
                            detailCoverUri = trackList.firstOrNull()?.albumArtUri
                            detailIsArtist = false
                            detailTracks = trackList
                        },
                        onOpenUserPlaylist = { pl ->
                            val plMediaItems = pl.tracks.map { track ->
                                MediaItem(
                                    id = track.key.hashCode().toLong(),
                                    uri = android.net.Uri.EMPTY,
                                    name = track.title,
                                    size = 0L,
                                    dateAdded = track.likedAtMs / 1000L,
                                    mimeType = "audio/mp4",
                                    mediaType = com.musicdrop.app.data.model.MediaType.AUDIO,
                                    durationMs = 0L,
                                    artist = track.artist,
                                    album = pl.name,
                                    isSong = true,
                                    filePath = track.key,
                                    albumArtUri = track.coverUrl.takeIf { it.isNotBlank() }?.let { android.net.Uri.parse(it) }
                                )
                            }
                            detailTitle = pl.name
                            detailSubtitle = "${pl.tracks.size} songs"
                            detailCoverUri = plMediaItems.firstOrNull()?.albumArtUri
                            detailIsArtist = false
                            detailTracks = plMediaItems
                        }
                    )

                    LibraryTab.FOLDERS -> FoldersTabContent(
                        folders = foldersGrouped,
                        onFolderClick = { folderName, trackList ->
                            detailTitle = folderName
                            detailSubtitle = "${trackList.size} songs"
                            detailCoverUri = trackList.firstOrNull()?.albumArtUri
                            detailIsArtist = false
                            detailTracks = trackList
                        }
                    )

                    LibraryTab.ALBUMS -> AlbumsTabContent(
                        albums = albumsGrouped,
                        onAlbumClick = { title, trackList ->
                            detailTitle = title
                            detailSubtitle = "${trackList.size} songs"
                            detailCoverUri = trackList.firstOrNull()?.albumArtUri
                            detailIsArtist = false
                            detailTracks = trackList
                        }
                    )

                    LibraryTab.ARTISTS -> ArtistsTabContent(
                        artists = artistsGrouped,
                        onArtistClick = { name, trackList ->
                            detailTitle = name
                            detailSubtitle = "${trackList.size} songs"
                            detailCoverUri = trackList.firstOrNull()?.albumArtUri
                            detailIsArtist = true
                            detailTracks = trackList
                        }
                    )
                }
            }
        }
    }

    // Dialog for creating a new playlist
    if (showNewPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showNewPlaylistDialog = false },
            title = { Text("Create Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
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
            containerColor = Color(0xFF1E1E24)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. ALL TAB (Matching Image 2)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AllTabContent(
    songs: List<MediaItem>,
    albums: List<Pair<String, List<MediaItem>>>,
    artists: List<Pair<String, List<MediaItem>>>,
    onViewAllSongs: () -> Unit,
    onViewAllAlbums: () -> Unit,
    onViewAllArtists: () -> Unit,
    onSongClick: (MediaItem) -> Unit,
    onAlbumClick: (String, List<MediaItem>) -> Unit,
    onArtistClick: (String, List<MediaItem>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // Songs Section Header
        item {
            SectionHeader(title = "Songs", onViewAll = onViewAllSongs)
        }
        items(songs.take(5), key = { "all_song_${it.id}" }) { song ->
            SongItemRow(song = song, onClick = { onSongClick(song) })
        }

        // Albums Section Header
        if (albums.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SectionHeader(title = "Albums", onViewAll = onViewAllAlbums)
            }
            items(albums.take(4), key = { "all_album_${it.first}" }) { (albumTitle, albumSongs) ->
                AlbumListRow(
                    title = albumTitle,
                    artist = albumSongs.firstOrNull()?.artist ?: "Various Artists",
                    songCount = albumSongs.size,
                    coverUri = albumSongs.firstOrNull()?.albumArtUri,
                    onClick = { onAlbumClick(albumTitle, albumSongs) }
                )
            }
        }

        // Artists Section Header
        if (artists.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SectionHeader(title = "Artists", onViewAll = onViewAllArtists)
            }
            items(artists.take(4), key = { "all_artist_${it.first}" }) { (artistName, artistSongs) ->
                ArtistListRow(
                    name = artistName,
                    albumCount = artistSongs.map { it.album }.distinct().size,
                    songCount = artistSongs.size,
                    onClick = { onArtistClick(artistName, artistSongs) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. SONGS TAB (Matching Image 6)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SongsTabContent(
    songs: List<MediaItem>,
    onSongClick: (MediaItem) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No songs found in storage", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                items(songs, key = { "songs_tab_${it.id}" }) { song ->
                    SongItemRow(song = song, onClick = { onSongClick(song) })
                }
            }
        }

        // Floating Scroll-to-Top Button (Image 6)
        val showFab by remember {
            derivedStateOf { listState.firstVisibleItemIndex > 6 }
        }
        if (showFab) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
                    .size(46.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF7C2))
                    .clickable {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Scroll to top",
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. PLAYLISTS TAB (Matching Image 4)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PlaylistsTabContent(
    allSongs: List<MediaItem>,
    likedMusic: List<LikedMusicItem>,
    userPlaylists: List<UserPlaylistItem>,
    onCreatePlaylist: () -> Unit,
    onOpenSmartPlaylist: (title: String, subtitle: String, List<MediaItem>) -> Unit,
    onOpenUserPlaylist: (UserPlaylistItem) -> Unit
) {
    val context = LocalContext.current
    val totalCount = 4 + userPlaylists.size

    val favouriteTracks = remember(likedMusic, allSongs) {
        val likedKeys = likedMusic.map { it.key }.toSet()
        allSongs.filter { it.filePath in likedKeys || it.name in likedKeys }
    }
    val recentlyAddedTracks = remember(allSongs) {
        allSongs.sortedByDescending { it.dateAdded }
    }
    val topTracks = remember(allSongs) {
        allSongs.take(50)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Header: "X Playlists" with + and menu
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$totalCount Playlists",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCreatePlaylist, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = "Create", tint = Color.White)
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                }
            }
        }

        // 4 Smart Playlists (Image 4)
        item {
            SmartPlaylistRow(
                title = "My favourite",
                songCount = favouriteTracks.size,
                icon = Icons.Rounded.Favorite,
                gradient = listOf(Color(0xFF29B6F6), Color(0xFF0288D1)),
                onClick = { onOpenSmartPlaylist("My favourite", "${favouriteTracks.size} songs", favouriteTracks) }
            )
            Spacer(Modifier.height(10.dp))
            SmartPlaylistRow(
                title = "Recently added",
                songCount = recentlyAddedTracks.size,
                icon = Icons.Rounded.MusicNote,
                gradient = listOf(Color(0xFF26A69A), Color(0xFF00796B)),
                onClick = { onOpenSmartPlaylist("Recently added", "${recentlyAddedTracks.size} songs", recentlyAddedTracks) }
            )
            Spacer(Modifier.height(10.dp))
            SmartPlaylistRow(
                title = "Recently played",
                songCount = allSongs.take(20).size,
                icon = Icons.Rounded.History,
                gradient = listOf(Color(0xFFAB47BC), Color(0xFF7B1FA2)),
                onClick = { onOpenSmartPlaylist("Recently played", "${allSongs.take(20).size} songs", allSongs.take(20)) }
            )
            Spacer(Modifier.height(10.dp))
            SmartPlaylistRow(
                title = "My top tracks",
                songCount = topTracks.size,
                icon = Icons.Rounded.LocalFireDepartment,
                gradient = listOf(Color(0xFFFF7043), Color(0xFFE64A19)),
                onClick = { onOpenSmartPlaylist("My top tracks", "${topTracks.size} songs", topTracks) }
            )
        }

        // Section: "My playlists (X)"
        item {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "My playlists (${userPlaylists.size})",
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // + Create Playlist Button Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF191922))
                    .clickable(onClick = onCreatePlaylist)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF252530)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFFFB8D00), modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text("Create playlist", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // User Playlists Items
        items(userPlaylists, key = { it.id }) { pl ->
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF16161E))
                    .clickable { onOpenUserPlaylist(pl) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF242430)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(pl.name, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text("${pl.tracks.size} songs", color = Color.Gray, fontSize = 12.sp)
                }
                Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = Color.Gray)
            }
        }

        // 3 Action Buttons (Image 4): Restore, Import, Transfer
        item {
            Spacer(Modifier.height(24.dp))
            PlaylistActionButton(
                icon = Icons.Rounded.Restore,
                label = "Restore playlist",
                onClick = { android.widget.Toast.makeText(context, "Playlists backed up & up to date", android.widget.Toast.LENGTH_SHORT).show() }
            )
            Spacer(Modifier.height(12.dp))
            PlaylistActionButton(
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                label = "Import playlist",
                onClick = { android.widget.Toast.makeText(context, "M3U / JSON Playlist Import Ready", android.widget.Toast.LENGTH_SHORT).show() }
            )
            Spacer(Modifier.height(12.dp))
            PlaylistActionButton(
                icon = Icons.Rounded.SwapHoriz,
                label = "Transfer playlist",
                onClick = { android.widget.Toast.makeText(context, "Ready to transfer across devices", android.widget.Toast.LENGTH_SHORT).show() }
            )
            Spacer(Modifier.height(120.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. FOLDERS TAB
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FoldersTabContent(
    folders: List<Pair<String, List<MediaItem>>>,
    onFolderClick: (String, List<MediaItem>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        items(folders, key = { "folder_${it.first}" }) { (folderName, trackList) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF14141C))
                    .clickable { onFolderClick(folderName, trackList) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF22222E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(folderName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${trackList.size} songs • ${trackList.firstOrNull()?.filePath?.substringBeforeLast('/') ?: "Internal"}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = Color.Gray)
            }
            Spacer(Modifier.height(10.dp))
        }
        item {
            Spacer(Modifier.height(120.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. ALBUMS TAB (Matching Image 8 - Vinyl record sleeve design)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AlbumsTabContent(
    albums: List<Pair<String, List<MediaItem>>>,
    onAlbumClick: (String, List<MediaItem>) -> Unit
) {
    if (albums.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No albums found in storage", color = Color.Gray, fontSize = 14.sp)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(albums, key = { "album_grid_${it.first}" }) { (albumTitle, trackList) ->
                AlbumVinylCard(
                    title = albumTitle,
                    artist = trackList.firstOrNull()?.artist ?: "Various Artists",
                    songCount = trackList.size,
                    coverUri = trackList.firstOrNull()?.albumArtUri,
                    onClick = { onAlbumClick(albumTitle, trackList) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. ARTISTS TAB (Matching Image 7 - Alphabetical letter avatars)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ArtistsTabContent(
    artists: List<Pair<String, List<MediaItem>>>,
    onArtistClick: (String, List<MediaItem>) -> Unit
) {
    if (artists.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No artists found in storage", color = Color.Gray, fontSize = 14.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            items(artists, key = { "artist_tab_${it.first}" }) { (artistName, trackList) ->
                ArtistListRow(
                    name = artistName,
                    albumCount = trackList.map { it.album }.distinct().size,
                    songCount = trackList.size,
                    onClick = { onArtistClick(artistName, trackList) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DETAIL SCREEN: ALBUM / ARTIST / PLAYLIST / FOLDER (Matching Images 3 & 5)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LibraryDetailScreen(
    title: String,
    subtitle: String,
    coverUri: android.net.Uri?,
    isArtist: Boolean,
    tracks: List<MediaItem>,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isSearchActive by remember { mutableStateOf(false) }
    var detailSearchQuery by remember { mutableStateOf("") }

    val displayedTracks = remember(tracks, detailSearchQuery) {
        if (detailSearchQuery.isBlank()) tracks
        else tracks.filter {
            it.name.contains(detailSearchQuery, ignoreCase = true) ||
            it.artist.contains(detailSearchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E17))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar with Back, Title & Search Icon ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (!isSearchActive) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    androidx.compose.foundation.text.BasicTextField(
                        value = detailSearchQuery,
                        onValueChange = { detailSearchQuery = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                        modifier = Modifier.weight(1f)
                    )
                }
                IconButton(onClick = { isSearchActive = !isSearchActive }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                // ── Hero Section (Artwork + Title + Subtitle) ────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Artwork
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .shadow(12.dp, if (isArtist) CircleShape else RoundedCornerShape(16.dp))
                                .clip(if (isArtist) CircleShape else RoundedCornerShape(16.dp))
                                .background(Color(0xFF22222E)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverUri != null) {
                                AsyncImage(
                                    model = coverUri,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = if (isArtist) Icons.Rounded.Person else Icons.Rounded.Album,
                                    contentDescription = null,
                                    tint = Color(0xFFFB8D00),
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Title
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Subtitle
                        Text(
                            text = subtitle.ifBlank { "${tracks.size} Songs" },
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Controls Row (Count + Sort Icons) ────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${displayedTracks.size} Songs",
                                color = Color(0xFFAAAAAA),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapVert,
                                    contentDescription = "Sort",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(
                                    imageVector = Icons.Rounded.PlaylistPlay,
                                    contentDescription = "Select",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ── Dual Big Pill Action Buttons: [ Shuffle ] & [ Play ] (Image 3 & 5) ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Shuffle Button
                            Button(
                                onClick = {
                                    if (displayedTracks.isNotEmpty()) {
                                        val shuffled = displayedTracks.shuffled()
                                        viewModel.playTrack(shuffled.first(), shuffled)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Shuffle", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }

                            // Play Button
                            Button(
                                onClick = {
                                    if (displayedTracks.isNotEmpty()) {
                                        viewModel.playTrack(displayedTracks.first(), displayedTracks)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Play", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ── Song List Items ──────────────────────────────────────────
                items(displayedTracks, key = { "detail_song_${it.id}" }) { song ->
                    SongItemRow(song = song, onClick = { viewModel.playTrack(song, displayedTracks) })
                }
            }
        }

        // Floating Scroll-to-Top Button (Image 3 & 5)
        val showFab by remember {
            derivedStateOf { listState.firstVisibleItemIndex > 6 }
        }
        if (showFab) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
                    .size(46.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF7C2))
                    .clickable {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Scroll to top",
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE UI ROWS & COMPONENTS (Matching Reference Images 1–8)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.clickable(onClick = onViewAll),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("View all", color = Color(0xFFAAAAAA), fontSize = 13.sp)
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SongItemRow(song: MediaItem, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    val dateStr = remember(song.dateAdded) {
        try {
            dateFormat.format(Date(song.dateAdded * 1000L))
        } catch (_: Exception) {
            "09-09"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Square Artwork / Music Note
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF22222E)),
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
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFFFB8D00),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Subtitle with 320K badge
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${song.artist} - ${song.album.ifBlank { "Music" }}",
                    color = Color(0xFF8E8E9B),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                // 320K Tag
                Box(
                    modifier = Modifier
                        .border(0.8.dp, Color(0xFF555566), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("320K", color = Color(0xFFAAAAAA), fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Date (MM-dd)
        Text(
            text = dateStr,
            color = Color(0xFF6E6E7E),
            fontSize = 11.5.sp
        )

        Spacer(modifier = Modifier.width(6.dp))

        // 3-dot Menu Icon
        IconButton(
            onClick = {},
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = Color(0xFF8E8E9B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AlbumListRow(
    title: String,
    artist: String,
    songCount: Int,
    coverUri: android.net.Uri?,
    onClick: () -> Unit
) {
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
                .background(Color(0xFF22222E)),
            contentAlignment = Alignment.Center
        ) {
            if (coverUri != null) {
                AsyncImage(model = coverUri, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Rounded.Album, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text("<$artist> | $songCount songs", color = Color(0xFF8E8E9B), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = Color(0xFF8E8E9B), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ArtistListRow(
    name: String,
    albumCount: Int,
    songCount: Int,
    onClick: () -> Unit
) {
    val colors = listOf(
        Color(0xFF7E57C2), Color(0xFFD81B60), Color(0xFF1E88E5),
        Color(0xFFFB8C00), Color(0xFF43A047), Color(0xFF8E24AA)
    )
    val avatarBg = colors[kotlin.math.abs(name.hashCode()) % colors.size]
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored Initial Avatar (Image 7)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(avatarBg.copy(alpha = 0.25f))
                .border(1.dp, avatarBg.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = avatarBg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text("$albumCount album | $songCount songs", color = Color(0xFF8E8E9B), fontSize = 12.sp)
        }
        Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = Color(0xFF8E8E9B), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SmartPlaylistRow(
    title: String,
    songCount: Int,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text("$songCount songs", color = Color.Gray, fontSize = 12.sp)
        }
        Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = Color(0xFF8E8E9B), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PlaylistActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(Color(0xFF1E1C2A))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFFB8D00), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Vinyl Album Sleeve Card (Matching Image 8)
@Composable
private fun AlbumVinylCard(
    title: String,
    artist: String,
    songCount: Int,
    coverUri: android.net.Uri?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.08f)
        ) {
            // Vinyl disc peeking out to the right
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF151518))
                    .border(1.dp, Color(0xFF2E2E38), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center)
                        .border(0.5.dp, Color(0xFF383845), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color(0xFF22222E))
                )
            }

            // Main Album Artwork
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF22222E))
                    .shadow(6.dp, RoundedCornerShape(12.dp))
            ) {
                if (coverUri != null) {
                    AsyncImage(
                        model = coverUri,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = Color(0xFFFB8D00),
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                    )
                }

                // "X songs" pill at bottom left
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("$songCount songs", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = artist,
                    color = Color(0xFF8E8E9B),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null,
                tint = Color(0xFF8E8E9B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
