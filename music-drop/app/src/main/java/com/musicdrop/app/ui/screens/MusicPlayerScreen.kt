package com.musicdrop.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.musicdrop.app.data.model.UnifiedTrack
import com.musicdrop.app.ui.components.AddToPlaylistDialog
import com.musicdrop.app.ui.components.EqualizerDialog
import com.musicdrop.app.ui.components.PlayerThemeDialog
import com.musicdrop.app.ui.theme.LocalAppColors
import com.musicdrop.app.ui.theme.PlayerThemeId
import com.musicdrop.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicPlayerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connection = viewModel.playbackConnection
    val currentTrack by connection.currentTrack.collectAsState()
    val isPlaying by connection.isPlaying.collectAsState()
    val positionMs by connection.currentPositionMs.collectAsState()
    val durationMs by connection.durationMs.collectAsState()
    val isShuffle by connection.isShuffle.collectAsState()
    val isRepeat by connection.isRepeat.collectAsState()
    val upNextQueue by viewModel.upNextQueue.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val lyricsLoading by viewModel.lyricsLoading.collectAsState()
    val playerTheme by viewModel.playerTheme.collectAsState()
    val equalizerManager = viewModel.equalizerManager
    val eqState by equalizerManager?.state?.collectAsState() ?: remember { mutableStateOf(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val appColors = LocalAppColors.current

    // 0: Song, 1: Lyrics
    var activeTab by remember { mutableIntStateOf(0) }
    var sliderDragging by remember { mutableFloatStateOf(-1f) }
    var isLiked by remember { mutableStateOf(false) }

    // Dialog toggles
    var showEqualizerModal by remember { mutableStateOf(false) }
    var showThemeModal by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showQueueModal by remember { mutableStateOf(false) }
    var showSleepTimerModal by remember { mutableStateOf(false) }
    var sleepTimerTargetMs by remember { mutableLongStateOf(0L) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Check favorite status
    LaunchedEffect(currentTrack?.id) {
        val cur = currentTrack
        if (cur != null) {
            isLiked = viewModel.isTrackFavorite(cur)
            viewModel.fetchLyrics(cur.name, cur.artist)
        }
    }

    // Sleep Timer countdown
    LaunchedEffect(sleepTimerTargetMs) {
        if (sleepTimerTargetMs > 0L) {
            while (System.currentTimeMillis() < sleepTimerTargetMs) {
                kotlinx.coroutines.delay(1000L)
            }
            connection.pause()
            sleepTimerTargetMs = 0L
            Toast.makeText(context, "Sleep timer: Playback paused", Toast.LENGTH_SHORT).show()
        }
    }

    // Parsed synced lyrics
    val syncedLines = remember(lyrics) {
        lyrics?.parsedSyncedLines().orEmpty()
    }

    // Active lyric index
    val activeLyricIndex = remember(positionMs, syncedLines) {
        if (syncedLines.isEmpty()) -1
        else {
            val idx = syncedLines.indexOfLast { it.first <= positionMs }
            if (idx >= 0) idx else 0
        }
    }

    val lyricsListState = rememberLazyListState()
    LaunchedEffect(activeLyricIndex, activeTab) {
        if (activeTab == 1 && activeLyricIndex in syncedLines.indices) {
            try {
                lyricsListState.animateScrollToItem(maxOf(0, activeLyricIndex - 2))
            } catch (_: Exception) {}
        }
    }

    // Background dynamic ambient gradient
    val dynamicGradient = remember(playerTheme, currentTrack?.albumArtUri) {
        playerTheme.getBackgroundBrush(fallbackAccent = Color(0xFF1E3A5F))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(dynamicGradient)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 30) {
                        onBack()
                    }
                }
            }
    ) {
        // Ambient background image blur layer
        currentTrack?.albumArtUri?.let { artUri ->
            AsyncImage(
                model = artUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.18f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. TOP BAR: DOWN CHEVRON | SONG / LYRICS | THEME & MORE ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Minimize Chevron
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Centered "Song | Lyrics" Header Toggle (Matching Screenshot)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Song",
                        color = if (activeTab == 0) Color.White else Color.White.copy(alpha = 0.45f),
                        fontSize = 15.sp,
                        fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .clickable { activeTab = 0 }
                            .padding(horizontal = 8.dp)
                    )

                    Text(
                        text = "|",
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 14.sp
                    )

                    Text(
                        text = "Lyrics",
                        color = if (activeTab == 1) Color.White else Color.White.copy(alpha = 0.45f),
                        fontSize = 15.sp,
                        fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .clickable { activeTab = 1 }
                            .padding(horizontal = 8.dp)
                    )
                }

                // Right Icons: T-shirt Theme + 3-Dots
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showThemeModal = true }, modifier = Modifier.size(38.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Checkroom,
                            contentDescription = "Themes",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { showOptionsMenu = true }, modifier = Modifier.size(38.dp)) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            modifier = Modifier.background(Color(0xFF1E1B2E))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Equalizer", color = Color.White) },
                                leadingIcon = { Icon(Icons.Rounded.Tune, null, tint = Color(0xFF10B981)) },
                                onClick = {
                                    showOptionsMenu = false
                                    showEqualizerModal = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sleep Timer", color = Color.White) },
                                leadingIcon = { Icon(Icons.Rounded.Schedule, null, tint = Color(0xFF38BDF8)) },
                                onClick = {
                                    showOptionsMenu = false
                                    showSleepTimerModal = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Playlist", color = Color.White) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = Color.White) },
                                onClick = {
                                    showOptionsMenu = false
                                    showAddToPlaylist = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Queue / Up Next", color = Color.White) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = Color.White) },
                                onClick = {
                                    showOptionsMenu = false
                                    showQueueModal = true
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 2. CENTER CONTENT (SONG COVER OR LYRICS VIEW) ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (activeTab == 0) {
                    // ── SONG VIEW: HERO ALBUM COVER ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFF1B1B22),
                            shadowElevation = 18.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .size(285.dp)
                                .shadow(24.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.8f))
                        ) {
                            val artUri = currentTrack?.albumArtUri
                            if (artUri != null) {
                                AsyncImage(
                                    model = artUri,
                                    contentDescription = "Cover Art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.radialGradient(listOf(Color(0xFF2C3E50), Color(0xFF0F2027)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ── LYRICS VIEW: SYNCHRONIZED SCROLLING KARAOKE ──
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            lyricsLoading -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp)
                                    Spacer(Modifier.height(12.dp))
                                    Text("Fetching lyrics...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                }
                            }
                            syncedLines.isNotEmpty() -> {
                                LazyColumn(
                                    state = lyricsListState,
                                    contentPadding = PaddingValues(vertical = 120.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(syncedLines) { index, linePair ->
                                        val isActive = index == activeLyricIndex
                                        val lineTimeMs = linePair.first
                                        val lineText = linePair.second

                                        Text(
                                            text = lineText,
                                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.35f),
                                            fontSize = if (isActive) 22.sp else 16.sp,
                                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                            lineHeight = if (isActive) 30.sp else 24.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    connection.seekTo(lineTimeMs)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            lyrics?.plainLyrics?.isNotBlank() == true -> {
                                LazyColumn(
                                    contentPadding = PaddingValues(vertical = 24.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item {
                                        Text(
                                            text = lyrics?.plainLyrics.orEmpty(),
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 17.sp,
                                            lineHeight = 32.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            else -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lyrics,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        "No lyrics found for this song",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            currentTrack?.let {
                                                viewModel.fetchLyrics(it.name, it.artist)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("Search Lyrics", color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── 3. TRACK TITLE & ARTIST NAME (SINGLE-LINE MARQUEE) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = currentTrack?.name ?: "No Track Playing",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = currentTrack?.artist?.ifBlank { "MusicDrop" } ?: "MusicDrop",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(18.dp))

            // ── 4. UTILITY TOOL ROW: 5 ICONS (MATCHING SCREENSHOT) ──
            // [ 🤍 Like ] [ ➕≣ Add to Playlist ] [ 🎚️ ON Equalizer ] [ ⏱️ Sleep Timer ] [ ≣ Queue ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Favorite / Like Heart
                IconButton(
                    onClick = {
                        val cur = currentTrack
                        if (cur != null) {
                            val newFav = viewModel.toggleFavoriteTrack(cur)
                            isLiked = newFav
                            Toast.makeText(context, if (newFav) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isLiked) Color(0xFFEF4444) else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 2. Add to Playlist
                IconButton(
                    onClick = { showAddToPlaylist = true },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = "Add to Playlist",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(25.dp)
                    )
                }

                // 3. Equalizer Button with "ON" Badge (Per User Screenshot)
                val isEqOn = eqState?.isEnabled == true
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showEqualizerModal = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Equalizer",
                            tint = if (isEqOn) Color(0xFF10B981) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                        if (isEqOn) {
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF10B981))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "ON",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // 4. Sleep Timer Clock Icon
                IconButton(
                    onClick = { showSleepTimerModal = true },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = "Sleep Timer",
                        tint = if (sleepTimerTargetMs > 0L) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(23.dp)
                    )
                }

                // 5. Queue / Current Playlist Icon
                IconButton(
                    onClick = { showQueueModal = true },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── 5. SEEKBAR ROW: [ ⟲ 10 ] [ ────── ( 1:40 / 2:10 ) ────── ] [ ⟳ 10 ] ──
            val effectiveProgress = if (sliderDragging >= 0f) sliderDragging else {
                if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            }
            val displayPositionMs = if (sliderDragging >= 0f) (sliderDragging * durationMs).toLong() else positionMs

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 10 Seconds Button
                IconButton(
                    onClick = {
                        val target = maxOf(0L, positionMs - 10_000L)
                        connection.seekTo(target)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Scrubber Slider with Centered Floating Pill Badge [ 1:40 / 2:10 ]
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Slider(
                        value = effectiveProgress,
                        onValueChange = { sliderDragging = it },
                        onValueChangeFinished = {
                            if (sliderDragging >= 0f && durationMs > 0L) {
                                connection.seekTo((sliderDragging * durationMs).toLong())
                            }
                            sliderDragging = -1f
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Transparent,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Floating Centered Pill: "1:40 / 2:10" (Matches Screenshot)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        shadowElevation = 4.dp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = "${formatMs(displayPositionMs)} / ${formatMs(durationMs)}",
                            color = Color(0xFF14131D),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                // Forward 10 Seconds Button
                IconButton(
                    onClick = {
                        val target = minOf(durationMs, positionMs + 10_000L)
                        connection.seekTo(target)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── 6. PLAYBACK CONTROLS ROW: [ 🔀 ] [ ⏮ ] [ ▶ ] [ ⏭ ] [ 🔁 ] ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(
                    onClick = { connection.toggleShuffle() },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) Color(0xFF10B981) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(23.dp)
                    )
                }

                // Previous Button
                IconButton(
                    onClick = { connection.skipPrevious() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Center Large Solid White Circle Play/Pause Button (Matching Screenshot)
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(66.dp)
                        .clickable { connection.togglePlayPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF14131D),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Next Button
                IconButton(
                    onClick = { connection.skipNext() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat Button (With "1" Badge for Repeat One)
                IconButton(
                    onClick = { connection.toggleRepeat() },
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRepeat) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeat) Color(0xFF10B981) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // ── DIALOGS ──
        if (showEqualizerModal) {
            EqualizerDialog(
                equalizerManager = equalizerManager,
                onDismiss = { showEqualizerModal = false }
            )
        }

        if (showThemeModal) {
            PlayerThemeDialog(
                currentTheme = playerTheme,
                onSelectTheme = { selected ->
                    viewModel.setPlayerTheme(selected)
                    showThemeModal = false
                },
                onDismiss = { showThemeModal = false }
            )
        }

        val curTrack = currentTrack
        if (showAddToPlaylist && curTrack != null) {
            val unified = remember(curTrack.id) {
                UnifiedTrack.Local(
                    key = curTrack.id.toString(),
                    title = curTrack.name,
                    artist = curTrack.artist,
                    thumbnailUrl = curTrack.albumArtUri?.toString().orEmpty(),
                    duration = curTrack.formattedDuration,
                    sourceName = if (curTrack.filePath?.startsWith("yt:") == true) "YouTube" else "Local",
                    filePath = curTrack.filePath.orEmpty(),
                    mediaItem = curTrack
                )
            }
            AddToPlaylistDialog(
                track = unified,
                viewModel = viewModel,
                onDismiss = { showAddToPlaylist = false },
                onAdded = { playlistName ->
                    Toast.makeText(context, "Added to $playlistName", Toast.LENGTH_SHORT).show()
                    showAddToPlaylist = false
                }
            )
        }

        // Queue Dialog
        if (showQueueModal) {
            Dialog(onDismissRequest = { showQueueModal = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF14131D),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Up Next Queue", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showQueueModal = false }) {
                                Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.7f))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (upNextQueue.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No tracks queued up", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(upNextQueue) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .clickable {
                                                viewModel.playTrackFromQueue(item)
                                                showQueueModal = false
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.6f))
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(item.channelTitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sleep Timer Dialog
        if (showSleepTimerModal) {
            Dialog(onDismissRequest = { showSleepTimerModal = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF14131D),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.padding(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Set Sleep Timer", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        val options = listOf(15, 30, 45, 60, 90)
                        options.forEach { minutes ->
                            Text(
                                text = "$minutes minutes",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        sleepTimerTargetMs = System.currentTimeMillis() + minutes * 60 * 1000L
                                        showSleepTimerModal = false
                                        Toast.makeText(context, "Timer set for $minutes minutes", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            )
                        }
                        if (sleepTimerTargetMs > 0L) {
                            Text(
                                text = "Turn off timer",
                                color = Color(0xFFEF4444),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        sleepTimerTargetMs = 0L
                                        showSleepTimerModal = false
                                        Toast.makeText(context, "Timer cancelled", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
