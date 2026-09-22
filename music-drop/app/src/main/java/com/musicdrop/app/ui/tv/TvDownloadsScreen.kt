package com.musicdrop.app.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicdrop.app.data.repository.DownloadedTrack
import com.musicdrop.app.ui.viewmodel.MainViewModel

/**
 * TV Downloads screen: shows all offline downloaded tracks.
 * Allows playing directly with TV remote.
 */
@Composable
fun TvDownloadsScreen(
    viewModel: MainViewModel,
    onPlayTrack: (DownloadedTrack) -> Unit
) {
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Icon(Icons.Filled.Download, null, tint = Color(0xFF7C3AED), modifier = Modifier.size(32.dp))
            Text("Downloads & Offline", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(
                "(${downloadedTracks.size} songs)",
                fontSize = 16.sp,
                color = Color.White.copy(0.5f),
                fontWeight = FontWeight.Medium
            )
        }

        if (downloadedTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = null,
                        tint = Color.White.copy(0.25f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No downloaded songs yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Play any song and click the 📥 Download button in the player to save it offline.",
                        fontSize = 14.sp,
                        color = Color.White.copy(0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(downloadedTracks) { track ->
                    TvDownloadRow(
                        track = track,
                        onPlay = { onPlayTrack(track) },
                        onDelete = { viewModel.deleteDownloadedTrack(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvDownloadRow(
    track: DownloadedTrack,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    TvFocusButton(
        onClick = onPlay,
        cornerRadius = 14.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(0.06f), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(0.1f))
            ) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    fontSize = 12.sp,
                    color = Color.White.copy(0.55f),
                    maxLines = 1
                )
            }

            // Duration & Size
            val file = java.io.File(track.filePath)
            if (file.exists() && file.length() > 0) {
                val mb = file.length().toDouble() / (1024 * 1024)
                Text(
                    String.format("%.1f MB", mb),
                    fontSize = 12.sp,
                    color = Color.White.copy(0.4f)
                )
            }

            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color(0xFF7C3AED),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
