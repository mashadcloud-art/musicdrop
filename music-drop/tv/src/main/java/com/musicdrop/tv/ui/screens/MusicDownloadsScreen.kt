package com.musicdrop.tv.ui.screens

import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.tv.data.model.MediaItem
import com.musicdrop.tv.data.model.MediaType
import com.musicdrop.tv.ui.viewmodel.MainViewModel
import java.io.File

@Composable
fun MusicDownloadsScreen(
    viewModel: MainViewModel
) {
    var downloadedFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(Unit) {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (musicDir.exists() && musicDir.isDirectory) {
            downloadedFiles = musicDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".mp3", true) || file.name.endsWith(".m4a", true))
            }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1565C0))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Music", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
            }
        }

        val allMediaItems = remember(downloadedFiles) {
            downloadedFiles.map { f ->
                MediaItem(
                    id = f.absolutePath.hashCode().toLong(),
                    uri = Uri.fromFile(f),
                    name = f.nameWithoutExtension,
                    size = f.length(),
                    dateAdded = f.lastModified() / 1000,
                    mimeType = "audio/mp3",
                    mediaType = MediaType.AUDIO,
                    durationMs = 0L,
                    albumArtUri = null
                )
            }
        }

        if (downloadedFiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No downloaded tracks yet.\nSearch and download songs from YouTube!",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(downloadedFiles) { file ->
                    val cleanName = file.nameWithoutExtension
                    val item = MediaItem(
                        id = file.absolutePath.hashCode().toLong(),
                        uri = Uri.fromFile(file),
                        name = cleanName,
                        size = file.length(),
                        dateAdded = file.lastModified() / 1000,
                        mimeType = "audio/mp3",
                        mediaType = MediaType.AUDIO,
                        durationMs = 0L,
                        albumArtUri = null
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                viewModel.playbackConnection.playTrack(item, allMediaItems)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF263238)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = "Audio", tint = Color(0xFF64B5F6))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cleanName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${file.length() / (1024 * 1024)} MB • Downloaded",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = {
                            viewModel.playbackConnection.playTrack(item, allMediaItems)
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
