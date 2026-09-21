package com.musicdrop.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.model.UnifiedTrack
import com.musicdrop.app.ui.viewmodel.MainViewModel

/**
 * Bottom Sheet modal matching reference Screenshot 1:
 * - Header with Song Art, Title, "Artist | Duration | Bitrate", (i) Info, and Share button
 * - 2x3 Grid with "Set as ringtone", "Change cover", "Edit tags", "Hide song", "Delete from device"
 * - Action List with "Play next", "Add to queue", "Add to playlist"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: MediaItem,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.changeSongCover(song, uri.toString())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF26211E),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.28f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            // ── Track Header Row (Artwork, Title, Subtitle, Info, Share) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF332B27)),
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
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    val subtitle = buildString {
                        append(song.artist.ifBlank { "Unknown Artist" })
                        if (song.formattedDuration.isNotBlank()) {
                            append(" | ${song.formattedDuration}")
                        }
                        append(" | 320kbps")
                    }
                    Text(
                        text = subtitle,
                        color = Color(0xFFA59D98),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Info Icon Button (i) -> Opens Details & Tags dialog
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Song Info",
                        tint = Color(0xFFC7BEB8),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Share Icon Button
                IconButton(
                    onClick = {
                        viewModel.shareMediaFile(
                            context = context,
                            filePath = song.filePath.orEmpty(),
                            mimeType = song.mimeType.ifBlank { "audio/*" },
                            title = song.name
                        )
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share",
                        tint = Color(0xFFC7BEB8),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Action Grid (Rounded cards matching Screenshot 1) ──
            val cardBg = Color(0xFF38312D)

            // Grid Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Rounded.Notifications,
                    title = "Set as ringtone",
                    backgroundColor = cardBg,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.setSongAsRingtone(context, song)
                        onDismiss()
                    }
                )

                QuickActionCard(
                    icon = Icons.Rounded.Image,
                    title = "Change cover",
                    backgroundColor = cardBg,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    }
                )

                QuickActionCard(
                    icon = Icons.Rounded.Label,
                    title = "Edit tags",
                    backgroundColor = cardBg,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showEditDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Rounded.VisibilityOff,
                    title = "Hide song",
                    backgroundColor = cardBg,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.hideSong(song)
                        onDismiss()
                    }
                )

                QuickActionCard(
                    icon = Icons.Rounded.Delete,
                    title = "Delete from device",
                    backgroundColor = cardBg,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showDeleteConfirmDialog = true
                    }
                )

                // Placeholder space for symmetrical 3-column look
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // ── Action List (Play next, Add to queue, Add to playlist) ──
            ActionListItem(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                title = "Play next",
                onClick = {
                    viewModel.playNext(song)
                    onDismiss()
                }
            )

            ActionListItem(
                icon = Icons.Rounded.AddToQueue,
                title = "Add to queue",
                onClick = {
                    viewModel.addToQueue(song)
                    onDismiss()
                }
            )

            ActionListItem(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                title = "Add to playlist",
                onClick = {
                    showAddToPlaylistDialog = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Song Details & Tag Editor Modal
    if (showEditDialog) {
        SongDetailsEditDialog(
            song = song,
            onDismiss = { showEditDialog = false },
            onSave = { newTitle, newArtist, newAlbum, newCover ->
                viewModel.editSongDetails(song, newTitle, newArtist, newAlbum, newCover)
                showEditDialog = false
                onDismiss()
            }
        )
    }

    // Add to Playlist Dialog
    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            track = UnifiedTrack.Local(
                key = song.filePath.orEmpty().ifBlank { "track:${song.id}" },
                title = song.name,
                artist = song.artist,
                thumbnailUrl = song.albumArtUri?.toString().orEmpty(),
                duration = song.formattedDuration,
                filePath = song.filePath.orEmpty(),
                mediaItem = song
            ),
            viewModel = viewModel,
            onDismiss = { showAddToPlaylistDialog = false },
            onAdded = { _ ->
                showAddToPlaylistDialog = false
                onDismiss()
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Song", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${song.name}\" from your device?", color = Color(0xFFCCCCCC)) },
            containerColor = Color(0xFF26211E),
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteSong(song)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFFE8E2DD),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = Color(0xFFD4CDC8),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionListItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFFD8D0CB),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
