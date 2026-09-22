package com.musicdrop.tv.screens

import android.widget.Toast
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicdrop.tv.data.TvDownloadedMedia
import com.musicdrop.tv.data.TvVideoItem
import com.musicdrop.tv.ui.components.TvFocusButton
import com.musicdrop.tv.viewmodel.TvViewModel

@Composable
fun TvDownloadsScreen(
    viewModel: TvViewModel,
    onPlayVideo: (TvVideoItem) -> Unit
) {
    val context = LocalContext.current
    val downloads by viewModel.downloadManager.downloads.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .padding(horizontal = 36.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Downloads", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "${downloads.size} offline items available",
                    fontSize = 13.sp,
                    color = Color.White.copy(0.5f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Download,
                        null,
                        tint = Color.White.copy(0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "No downloads yet",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Play any video and tap 'Download 4K/HD' to watch completely offline without internet.",
                        color = Color.White.copy(0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 36.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(downloads, key = { it.id }) { item ->
                    TvFocusButton(
                        onClick = {
                            // Play downloaded video
                            val videoItem = TvVideoItem(
                                id = item.id,
                                title = item.title,
                                channelTitle = item.channelTitle,
                                thumbnailUrl = item.thumbnailUrl
                            )
                            onPlayVideo(videoItem)
                        },
                        cornerRadius = 14.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1F1F27))
                            ) {
                                AsyncImage(
                                    model = item.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.Black.copy(0.85f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (item.isVideo) "VIDEO" else "AUDIO",
                                        color = if (item.isVideo) Color(0xFFFF0033) else Color(0xFFFFD600),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${item.channelTitle} • ${formatFileSize(item.sizeBytes)}",
                                    color = Color.White.copy(0.6f),
                                    fontSize = 12.sp
                                )
                            }

                            // Play action icon
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )

                            // Delete button
                            TvFocusButton(
                                onClick = {
                                    viewModel.downloadManager.deleteDownload(item.id)
                                    Toast.makeText(context, "Deleted from TV storage", Toast.LENGTH_SHORT).show()
                                },
                                cornerRadius = 24.dp,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.White.copy(0.12f), RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        "%.2f GB".format(mb / 1024.0)
    } else {
        "%.1f MB".format(mb)
    }
}
