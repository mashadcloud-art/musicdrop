package com.musicdrop.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.musicdrop.app.data.model.MediaItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SongDetailsEditDialog(
    song: MediaItem,
    onDismiss: () -> Unit,
    onSave: (newTitle: String, newArtist: String, newAlbum: String, newCoverUri: String?) -> Unit
) {
    var title by remember(song) { mutableStateOf(song.name) }
    var artist by remember(song) { mutableStateOf(song.artist) }
    var album by remember(song) { mutableStateOf(song.album) }
    var coverUri by remember(song) { mutableStateOf<Uri?>(song.albumArtUri) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coverUri = uri
        }
    }

    val fileSizeText = remember(song.size, song.filePath) {
        val bytes = if (song.size > 0L) song.size else {
            val f = song.filePath?.let { File(it) }
            if (f != null && f.exists()) f.length() else 0L
        }
        if (bytes > 0L) {
            String.format(Locale.getDefault(), "%.2f MB", bytes / (1024f * 1024f))
        } else "Unknown"
    }

    val dateAddedText = remember(song.dateAdded) {
        if (song.dateAdded > 0L) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(song.dateAdded * 1000L))
        } else "Unknown"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1C24),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Song Details & Tags",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cover Artwork with Edit Badge
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2A2832))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (coverUri != null) {
                        AsyncImage(
                            model = coverUri,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // Change Cover Badge Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Change Cover",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Editable Fields
                Text("TITLE", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF97316),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color(0xFF141318),
                        unfocusedContainerColor = Color(0xFF141318)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("ARTIST", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF97316),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color(0xFF141318),
                        unfocusedContainerColor = Color(0xFF141318)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("ALBUM", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF97316),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color(0xFF141318),
                        unfocusedContainerColor = Color(0xFF141318)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Technical Metadata Panel
                Text(
                    text = "FILE INFORMATION",
                    color = Color(0xFFAAAAAA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF15141B))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetadataRow("Duration", song.formattedDuration.ifBlank { "Unknown" })
                    MetadataRow("Format", song.mimeType.ifBlank { "audio/mp3" })
                    MetadataRow("Bitrate", "320 kbps (High Quality)")
                    MetadataRow("Size", fileSizeText)
                    MetadataRow("Date Added", dateAddedText)
                    if (!song.filePath.isNullOrBlank()) {
                        MetadataRow("Location", song.filePath.orEmpty())
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Cancel and Save Changes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onSave(title, artist, album, coverUri?.toString())
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF97316),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF888899), fontSize = 12.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
