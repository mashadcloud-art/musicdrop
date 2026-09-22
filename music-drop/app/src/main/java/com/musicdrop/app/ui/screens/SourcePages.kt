package com.musicdrop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.Search
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
import com.musicdrop.app.data.repository.AppleMusicRepository
import com.musicdrop.app.data.repository.SpotifyRepository
import com.musicdrop.app.ui.viewmodel.MainViewModel

/**
 * Every backend gets its own dedicated page for browsing/searching that source only —
 * separate from the blended "Popular"/"Recently Played" feed on Discover, which stays
 * as a quick cross-source shortcut. Reuses [UnifiedMusicCard] (same look as Discover's
 * cards, badge included) wherever the source's results fit [UnifiedTrack]; Apple Music
 * and Spotify get their own simple card layouts since they don't play through the
 * normal on-device playlist path.
 */
enum class MusicSource(
    val label: String,
    val badgeText: String,
    val badgeColor: Color,
    // A real icon glyph reads as far more "premium" than a 2-letter monogram — used
    // instead of [badgeText] everywhere a badge is drawn. Generic Material glyphs
    // rather than each service's actual logo mark (which is trademarked artwork).
    val badgeIcon: ImageVector
) {
    YOUTUBE("YouTube", "YT", Color(0xFFFF3B30), Icons.Filled.PlayArrow),
    JIOSAAVN("JioSaavn", "JS", Color(0xFF2ED8A7), Icons.Filled.MusicNote),
    VIMEO("Vimeo", "VM", Color(0xFF17C3E6), Icons.Filled.Videocam),
    APPLE_MUSIC("Apple Music", "AM", Color(0xFFFA5F91), Icons.Filled.Album),
    SPOTIFY("Spotify", "SP", Color(0xFF1ED760), Icons.Filled.GraphicEq)
}

@Composable
fun MusicSourcePage(source: MusicSource, viewModel: MainViewModel, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        when (source) {
            MusicSource.YOUTUBE -> YouTubeSourceScreen(viewModel, onBack)
            MusicSource.JIOSAAVN -> JioSaavnSourceScreen(viewModel, onBack)
            MusicSource.VIMEO -> VimeoSourceScreen(viewModel, onBack)
            MusicSource.APPLE_MUSIC -> AppleMusicSourceScreen(viewModel, onBack)
            MusicSource.SPOTIFY -> SpotifySourceScreen(viewModel, onBack)
        }
    }
}

@Composable
private fun EmptyOrLoading(loading: Boolean, empty: Boolean, emptyText: String, accentColor: Color = Color(0xFF1976D2)) {
    if (loading) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accentColor)
        }
    } else if (empty) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(emptyText, color = Color(0xFF666666), fontSize = 13.sp)
        }
    }
}

// ── YouTube ──────────────────────────────────────────────────────────────────
@Composable
private fun YouTubeSourceScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val trending by viewModel.ytMusicResults.collectAsState()
    val loading by viewModel.ytMusicLoading.collectAsState()
    val preparingKey by viewModel.preparingKey.collectAsState()
    val selectedFilter by viewModel.selectedSearchFilter.collectAsState()
    var downloadingKeys by remember { mutableStateOf(setOf<String>()) }
    var downloadedKeys by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) { viewModel.loadYtMusicTrending() }

    val tracks = remember(trending) { trending.map { UnifiedTrack.Youtube(it) } }

    fun onPlayYt(track: UnifiedTrack) {
        (track as? UnifiedTrack.Youtube)?.let { viewModel.playYtMusicTrack(it.result) }
    }
    fun onDownloadYt(track: UnifiedTrack) {
        val yt = track as? UnifiedTrack.Youtube ?: return
        downloadingKeys = downloadingKeys + track.key
        viewModel.downloadYouTubeAudio(yt.result) { success, _ ->
            downloadingKeys = downloadingKeys - track.key
            if (success) downloadedKeys = downloadedKeys + track.key
        }
    }

    Column(Modifier.fillMaxSize()) {
        // ── TOP SINGLE ROW: [ < Back ] [ Red Icon ] [ Search Bar ] ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MusicSource.YOUTUBE.badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = MusicSource.YOUTUBE.badgeIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.isNotBlank()) viewModel.searchYtMusic(it)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                placeholder = { Text("Search songs, artists, albums...", color = Color(0xFF888888), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = {
                            query = ""
                            viewModel.loadYtMusicTrending()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF888888), modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MusicSource.YOUTUBE.badgeColor,
                    unfocusedBorderColor = Color(0xFF2E2E2E),
                    focusedContainerColor = Color(0xFF161616),
                    unfocusedContainerColor = Color(0xFF161616),
                    cursorColor = Color.White
                )
            )
        }

        // ── CATEGORY FILTER CHIPS ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(com.musicdrop.app.data.youtube.YouTubeMusicRepository.SearchFilter.values().toList()) { filter ->
                val selected = filter == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) MusicSource.YOUTUBE.badgeColor else Color(0xFF222222))
                        .clickable { viewModel.setSearchFilter(filter) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        filter.label,
                        color = if (selected) Color.White else Color(0xFFCCCCCC),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        EmptyOrLoading(loading, tracks.isEmpty(), "No YouTube results yet", accentColor = MusicSource.YOUTUBE.badgeColor)

        if (query.isBlank()) {
            // Speed dial & Quick picks
            LazyColumn(Modifier.fillMaxSize()) {
                if (tracks.isNotEmpty()) {
                    item {
                        Text(
                            "Speed dial",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 10.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(tracks.take(10), key = { "sd_${it.key}" }) { track ->
                                Column(
                                    modifier = Modifier
                                        .width(104.dp)
                                        .clickable { onPlayYt(track) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(104.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1A1A1A))
                                    ) {
                                        AsyncImage(
                                            model = track.thumbnailUrl,
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (track.key == preparingKey) {
                                            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(22.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        track.title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            "Quick picks",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(tracks, key = { "qp_${it.key}" }) { track ->
                        YtMusicListRow(
                            track = track,
                            isDownloading = track.key in downloadingKeys,
                            isDownloaded = track.key in downloadedKeys,
                            isPreparing = track.key == preparingKey,
                            onPlay = { onPlayYt(track) },
                            onDownload = { onDownloadYt(track) }
                        )
                    }
                    item { Spacer(Modifier.height(84.dp)) }
                }
            }
        } else {
            // ── 2 SYMMETRICAL COLUMNS FOR SEARCH RESULTS ──
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tracks, key = { it.key }) { track ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayYt(track) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A1A1A))
                        ) {
                            AsyncImage(
                                model = track.thumbnailUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (track.key == preparingKey) {
                                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayCircleFilled,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            // Download button in corner
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .clickable(enabled = track.key !in downloadingKeys && track.key !in downloadedKeys) {
                                        onDownloadYt(track)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    track.key in downloadingKeys -> CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 1.5.dp,
                                        color = Color.White
                                    )
                                    track.key in downloadedKeys -> Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Downloaded",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    else -> Icon(
                                        Icons.Rounded.Download,
                                        contentDescription = "Download",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = track.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Spacer(Modifier.height(84.dp))
                }
            }
        }
    }
}

/** A single "Quick picks"-style row — thumbnail, title/artist, download — matching
 *  YouTube Music's own list rows (as opposed to [UnifiedMusicCard]'s square grid tile). */
@Composable
private fun YtMusicListRow(
    track: UnifiedTrack,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    isPreparing: Boolean,
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
                .background(Color(0xFF1A1A1A))
        ) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isPreparing) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(track.artist, color = Color(0xFF888888), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A))
                .clickable(enabled = !isDownloading && !isDownloaded) { onDownload() },
            contentAlignment = Alignment.Center
        ) {
            when {
                isDownloading -> CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 1.5.dp, color = Color.White)
                isDownloaded -> Icon(Icons.Rounded.CheckCircle, contentDescription = "Downloaded", tint = Color(0xFF4CAF50), modifier = Modifier.size(17.dp))
                else -> Icon(Icons.Rounded.Download, contentDescription = "Download", tint = Color(0xFF888888), modifier = Modifier.size(17.dp))
            }
        }
    }
}

// ── JioSaavn ─────────────────────────────────────────────────────────────────
@Composable
private fun JioSaavnSourceScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val trending by viewModel.saavnTrending.collectAsState()
    val searchResults by viewModel.saavnResults.collectAsState()
    val loading by viewModel.saavnLoading.collectAsState()
    val preparingKey by viewModel.preparingKey.collectAsState()
    var downloadingKeys by remember { mutableStateOf(setOf<String>()) }
    var downloadedKeys by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) { viewModel.loadSaavnTrending() }

    val source = if (query.isBlank()) trending else searchResults
    val tracks = remember(source) { source.map { UnifiedTrack.Saavn(it) } }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(MusicSource.JIOSAAVN.badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(MusicSource.JIOSAAVN.badgeIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.searchSaavn(it)
                },
                modifier = Modifier.weight(1f).height(48.dp),
                placeholder = { Text("Search JioSaavn 320kbps...", color = Color(0xFF888888), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MusicSource.JIOSAAVN.badgeColor,
                    unfocusedBorderColor = Color(0xFF2E2E2E),
                    focusedContainerColor = Color(0xFF161616),
                    unfocusedContainerColor = Color(0xFF161616),
                    cursorColor = Color.White
                )
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = { viewModel.refreshSaavn() },
                enabled = !loading,
                modifier = Modifier.size(38.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MusicSource.JIOSAAVN.badgeColor
                    )
                } else {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Refresh New Releases",
                        tint = MusicSource.JIOSAAVN.badgeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        EmptyOrLoading(loading, tracks.isEmpty(), "No JioSaavn results yet", accentColor = MusicSource.JIOSAAVN.badgeColor)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tracks, key = { it.key }) { track ->
                Column(
                    modifier = Modifier.fillMaxWidth().clickable {
                        (track as? UnifiedTrack.Saavn)?.let { sv -> viewModel.playSaavnTrack(sv.item) }
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A))
                    ) {
                        AsyncImage(model = track.thumbnailUrl, contentDescription = track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(
                            Modifier.align(Alignment.Center).size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.PlayCircleFilled, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.65f))
                                .clickable(enabled = track.key !in downloadingKeys && track.key !in downloadedKeys) {
                                    val sv = track as? UnifiedTrack.Saavn ?: return@clickable
                                    downloadingKeys = downloadingKeys + track.key
                                    viewModel.downloadSaavnTrack(sv.item) { success, _ ->
                                        downloadingKeys = downloadingKeys - track.key
                                        if (success) downloadedKeys = downloadedKeys + track.key
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                track.key in downloadingKeys -> CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color.White)
                                track.key in downloadedKeys -> Icon(Icons.Rounded.CheckCircle, contentDescription = "Downloaded", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                else -> Icon(Icons.Rounded.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(track.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(track.artist, color = Color(0xFFAAAAAA), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Spacer(Modifier.height(84.dp))
            }
        }
    }
}

// ── Vimeo ────────────────────────────────────────────────────────────────────
@Composable
private fun VimeoSourceScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var linkText by remember { mutableStateOf("") }
    val results by viewModel.vimeoImportResults.collectAsState()
    val loading by viewModel.vimeoImportLoading.collectAsState()
    val error by viewModel.vimeoImportError.collectAsState()
    val preparingKey by viewModel.preparingKey.collectAsState()
    var downloadingKeys by remember { mutableStateOf(setOf<String>()) }
    var downloadedKeys by remember { mutableStateOf(setOf<String>()) }

    val tracks = remember(results) { results.map { UnifiedTrack.Vimeo(it) } }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(MusicSource.VIMEO.badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(MusicSource.VIMEO.badgeIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("Vimeo Video Import", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = linkText,
                onValueChange = { linkText = it },
                modifier = Modifier.weight(1f).height(48.dp),
                placeholder = { Text("https://vimeo.com/...", color = Color(0xFF888888), fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF17C3E6),
                    unfocusedBorderColor = Color(0xFF333333),
                    cursorColor = Color.White
                )
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { if (linkText.isNotBlank()) viewModel.importVimeoLink(linkText) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF17C3E6))
            ) { Text("Import", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF17C3E6))
            }
        }
        error?.let {
            Text(it, color = Color(0xFFEF5350), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        EmptyOrLoading(false, tracks.isEmpty() && !loading, "No videos imported yet — paste a link above", accentColor = MusicSource.VIMEO.badgeColor)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tracks, key = { it.key }) { track ->
                Column(
                    modifier = Modifier.fillMaxWidth().clickable {
                        (track as? UnifiedTrack.Vimeo)?.let { vm -> viewModel.playVimeoVideo(vm.video) }
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A))
                    ) {
                        AsyncImage(model = track.thumbnailUrl, contentDescription = track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        if (track.key == preparingKey) {
                            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                            }
                        } else {
                            Box(
                                Modifier.align(Alignment.Center).size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.PlayCircleFilled, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.65f))
                                .clickable(enabled = track.key !in downloadingKeys && track.key !in downloadedKeys) {
                                    val vm = track as? UnifiedTrack.Vimeo ?: return@clickable
                                    downloadingKeys = downloadingKeys + track.key
                                    viewModel.downloadVimeoVideo(vm.video) { success, _ ->
                                        downloadingKeys = downloadingKeys - track.key
                                        if (success) downloadedKeys = downloadedKeys + track.key
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                track.key in downloadingKeys -> CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color.White)
                                track.key in downloadedKeys -> Icon(Icons.Rounded.CheckCircle, contentDescription = "Downloaded", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                else -> Icon(Icons.Rounded.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(track.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(track.artist, color = Color(0xFFAAAAAA), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Spacer(Modifier.height(84.dp))
            }
        }
    }
}

// ── Apple Music ──────────────────────────────────────────────────────────────
@Composable
private fun AppleMusicSourceScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.appleTracks.collectAsState()
    val loading by viewModel.appleLoading.collectAsState()
    var downloadingKeys by remember { mutableStateOf(setOf<Long>()) }
    var downloadedKeys by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(Unit) {
        query = "top hits"
        viewModel.searchAppleMusic("top hits")
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(MusicSource.APPLE_MUSIC.badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(MusicSource.APPLE_MUSIC.badgeIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.searchAppleMusic(it)
                },
                modifier = Modifier.weight(1f).height(48.dp),
                placeholder = { Text("Search Apple Music...", color = Color(0xFF888888), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MusicSource.APPLE_MUSIC.badgeColor,
                    unfocusedBorderColor = Color(0xFF2E2E2E),
                    focusedContainerColor = Color(0xFF161616),
                    unfocusedContainerColor = Color(0xFF161616),
                    cursorColor = Color.White
                )
            )
        }
        EmptyOrLoading(loading, results.isEmpty(), "No Apple Music results yet", accentColor = MusicSource.APPLE_MUSIC.badgeColor)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(results, key = { it.trackId }) { track ->
                Column(modifier = Modifier.fillMaxWidth().clickable { viewModel.playAppleTrackPreview(track) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1A))
                    ) {
                        AsyncImage(
                            model = track.hdArtwork,
                            contentDescription = track.trackName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            Modifier.align(Alignment.Center).size(36.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.PlayCircleFilled, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable(enabled = track.trackId !in downloadingKeys && track.trackId !in downloadedKeys) {
                                    downloadingKeys = downloadingKeys + track.trackId
                                    viewModel.downloadAppleTrackPreview(track) { success, _ ->
                                        downloadingKeys = downloadingKeys - track.trackId
                                        if (success) downloadedKeys = downloadedKeys + track.trackId
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                track.trackId in downloadingKeys -> CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color.White)
                                track.trackId in downloadedKeys -> Icon(Icons.Rounded.CheckCircle, contentDescription = "Downloaded", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                else -> Icon(Icons.Rounded.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        track.trackName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(track.artistName, color = Color(0xFFAAAAAA), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Spacer(Modifier.height(84.dp))
            }
        }
    }
}

// ── Spotify ──────────────────────────────────────────────────────────────────
@Composable
private fun SpotifySourceScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var linkText by remember { mutableStateOf("") }
    val results by viewModel.spotifyImportResults.collectAsState()
    val loading by viewModel.spotifyImportLoading.collectAsState()
    val error by viewModel.spotifyImportError.collectAsState()
    var matchingIndex by remember { mutableStateOf(-1) }
    var noMatchIndex by remember { mutableStateOf(-1) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(MusicSource.SPOTIFY.badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(MusicSource.SPOTIFY.badgeIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("Spotify Importer", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = linkText,
                onValueChange = { linkText = it },
                modifier = Modifier.weight(1f).height(48.dp),
                placeholder = { Text("https://open.spotify.com/...", color = Color(0xFF888888), fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF1ED760),
                    unfocusedBorderColor = Color(0xFF333333),
                    cursorColor = Color.White
                )
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { if (linkText.isNotBlank()) viewModel.importSpotifyLink(linkText) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760))
            ) { Text("Import", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1ED760))
            }
        }
        error?.let {
            Text(it, color = Color(0xFFEF5350), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(results.size) { index ->
                val item = results[index]
                Column(
                    modifier = Modifier.fillMaxWidth().clickable {
                        matchingIndex = index
                        noMatchIndex = -1
                        viewModel.matchAndPlaySpotifyItem(item, onNoMatch = {
                            matchingIndex = -1
                            noMatchIndex = index
                        })
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1A))
                    ) {
                        AsyncImage(
                            model = item.coverUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            Modifier.align(Alignment.TopStart).padding(6.dp).clip(CircleShape)
                                .background(Color(0xFF1ED760).copy(alpha = 0.9f)).padding(4.dp)
                        ) {
                            Icon(MusicSource.SPOTIFY.badgeIcon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                        }
                        Box(
                            Modifier.align(Alignment.Center).size(36.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (matchingIndex == index) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Rounded.PlayCircleFilled, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (noMatchIndex == index) "No playable match found" else item.artist,
                        color = if (noMatchIndex == index) Color(0xFFEF5350) else Color(0xFFAAAAAA),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Spacer(Modifier.height(84.dp))
            }
        }
    }
}
