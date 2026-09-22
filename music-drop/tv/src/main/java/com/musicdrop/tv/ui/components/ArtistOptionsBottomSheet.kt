package com.musicdrop.tv.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
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
import com.musicdrop.tv.data.model.MediaItem
import com.musicdrop.tv.data.model.UnifiedTrack
import com.musicdrop.tv.data.repository.ArtistCoverRepository
import com.musicdrop.tv.ui.theme.LocalAppColors
import com.musicdrop.tv.ui.viewmodel.MainViewModel

/**
 * Artist Options Bottom Sheet matching Screenshot 1 with rich theme adaptation:
 * - Header: Artist photo / avatar, Artist Name, X albums | Y songs
 * - Actions: Play, Play next, Add to queue, Add to playlist, Edit tags (rename)
 * - Rounded CLOSE button at the bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistOptionsBottomSheet(
    artistName: String,
    tracks: List<MediaItem>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current

    var artistCoverUrl by remember(artistName) { mutableStateOf<String?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newArtistName by remember { mutableStateOf(artistName) }

    LaunchedEffect(artistName) {
        val resolved = ArtistCoverRepository.getArtistCover(context, artistName, tracks.firstOrNull())
        if (resolved != null) {
            artistCoverUrl = resolved
        }
    }

    val albumCount = remember(tracks) { tracks.map { it.album }.distinct().size }
    val songCount = tracks.size

    val colors = listOf(
        Color(0xFF7E57C2), Color(0xFFD81B60), Color(0xFF1E88E5),
        Color(0xFFFB8C00), Color(0xFF43A047), Color(0xFF8E24AA)
    )
    val avatarBg = colors[kotlin.math.abs(artistName.hashCode()) % colors.size]
    val initial = artistName.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "A"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16151E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── 1. ARTIST HEADER ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Square Artwork or Colored Initial
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(avatarBg.copy(alpha = 0.25f))
                        .border(1.dp, avatarBg.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!artistCoverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = artistCoverUrl,
                            contentDescription = artistName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = initial,
                            color = avatarBg,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artistName,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$albumCount albums | $songCount songs",
                        color = Color(0xFF9E9EA8),
                        fontSize = 13.sp
                    )
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // ── 2. ACTION LIST (Matching Screenshot 1) ───────────────────
            ArtistActionItem(
                icon = Icons.Rounded.PlayCircleOutline,
                title = "Play",
                onClick = {
                    if (tracks.isNotEmpty()) {
                        viewModel.playTrack(tracks.first(), tracks)
                        Toast.makeText(context, "Playing $artistName", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }
            )

            ArtistActionItem(
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                title = "Play next",
                onClick = {
                    tracks.asReversed().forEach { viewModel.playNext(it) }
                    Toast.makeText(context, "Added $artistName to play next", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            ArtistActionItem(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                title = "Add to queue",
                onClick = {
                    tracks.forEach { viewModel.addToQueue(it) }
                    Toast.makeText(context, "Added $songCount songs to queue", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            ArtistActionItem(
                icon = Icons.Rounded.PlaylistAdd,
                title = "Add to playlist",
                onClick = {
                    showAddToPlaylistDialog = true
                }
            )

            ArtistActionItem(
                icon = Icons.Rounded.Label,
                title = "Edit tags",
                onClick = {
                    newArtistName = artistName
                    showRenameDialog = true
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── 3. CLOSE BUTTON (Matching Screenshot 1) ──────────────────
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C2825),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "CLOSE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }

    // Add To Playlist Dialog
    val playlists by viewModel.userPlaylists.collectAsState()
    if (showAddToPlaylistDialog && tracks.isNotEmpty()) {
        val sampleTrack = tracks.first()
        AddToPlaylistDialog(
            track = UnifiedTrack.Local(
                key = sampleTrack.filePath.orEmpty().ifBlank { "track:${sampleTrack.id}" },
                title = sampleTrack.name,
                artist = artistName,
                thumbnailUrl = artistCoverUrl ?: sampleTrack.albumArtUri?.toString().orEmpty(),
                duration = sampleTrack.formattedDuration,
                filePath = sampleTrack.filePath.orEmpty(),
                mediaItem = sampleTrack
            ),
            viewModel = viewModel,
            onDismiss = { showAddToPlaylistDialog = false },
            onAdded = { playlistName ->
                // Add remaining tracks as well
                val targetPlaylist = playlists.find { it.name.equals(playlistName, ignoreCase = true) }
                if (targetPlaylist != null) {
                    tracks.drop(1).forEach { song ->
                        viewModel.addUnifiedTrackToPlaylist(
                            playlistId = targetPlaylist.id,
                            track = UnifiedTrack.Local(
                                key = song.filePath.orEmpty().ifBlank { "track:${song.id}" },
                                title = song.name,
                                artist = artistName,
                                thumbnailUrl = artistCoverUrl ?: song.albumArtUri?.toString().orEmpty(),
                                duration = song.formattedDuration,
                                filePath = song.filePath.orEmpty(),
                                mediaItem = song
                            )
                        )
                    }
                }
                showAddToPlaylistDialog = false
                Toast.makeText(context, "Added $songCount songs to $playlistName", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        )
    }

    // Rename Artist Dialog ("Edit tags")
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = Color(0xFF232029),
            title = {
                Text("Edit Artist Name", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = newArtistName,
                    onValueChange = { newArtistName = it },
                    label = { Text("Artist Name") },
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
                Button(
                    onClick = {
                        val trimmed = newArtistName.trim()
                        if (trimmed.isNotBlank() && trimmed != artistName) {
                            tracks.forEach { song ->
                                viewModel.editSongDetails(
                                    track = song,
                                    newTitle = song.name,
                                    newArtist = trimmed,
                                    newAlbum = song.album
                                )
                            }
                            Toast.makeText(context, "Renamed artist to $trimmed", Toast.LENGTH_SHORT).show()
                        }
                        showRenameDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.accentPrimary)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color(0xFF9E9EA8))
                }
            }
        )
    }
}

@Composable
private fun ArtistActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
