package com.musicdrop.tv.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.musicdrop.tv.data.youtube.YouTubeExtractedFormat
import com.musicdrop.tv.ui.theme.SurfaceElevated
import com.musicdrop.tv.ui.theme.TextMuted
import com.musicdrop.tv.ui.theme.TextPrimary
import com.musicdrop.tv.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeDownloadBottomSheet(
    viewModel: MainViewModel
) {
    val showSheet by viewModel.showYtBottomSheet.collectAsState()
    val extractionResult by viewModel.ytExtractionResult.collectAsState()
    
    if (showSheet && extractionResult != null) {
        val result = extractionResult!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        // Group formats by Music and Video
        val audioFormats = result.formats.filter { !it.mimeType.contains("video") }.sortedByDescending { it.averageBitrate }
        val videoFormats = result.formats.filter { it.mimeType.contains("video") }.sortedByDescending { it.averageBitrate }
        
        var selectedUrl by remember { mutableStateOf(result.formats.firstOrNull()?.url ?: "") }
        
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeYtBottomSheet() },
            sheetState = sheetState,
            containerColor = SurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Header (Thumbnail, Title, Author)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = result.thumbnailUrl,
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = result.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.author,
                            color = TextMuted,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Formats List
                if (audioFormats.isNotEmpty()) {
                    Text("Music", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    audioFormats.take(2).forEachIndexed { index, format ->
                        FormatRow(
                            format = format,
                            label = if (index == 0) "Fast (48K)" else "Standard (128K)",
                            isSelected = selectedUrl == format.url,
                            onSelect = { selectedUrl = format.url }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                if (videoFormats.isNotEmpty()) {
                    Text("Video", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    videoFormats.take(2).forEachIndexed { index, format ->
                        FormatRow(
                            format = format,
                            label = if (index == 0) "Fast (360P)" else "Standard (720P)",
                            isSelected = selectedUrl == format.url,
                            onSelect = { selectedUrl = format.url }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Download Button
                Button(
                    onClick = {
                        if (selectedUrl.isNotBlank()) {
                            val ytResult = com.musicdrop.tv.data.youtube.YouTubeSearchResult(
                                videoId = result.videoId,
                                title = result.title,
                                channelTitle = result.author,
                                thumbnailUrl = result.thumbnailUrl
                            )
                            viewModel.playYouTubeDirectStream(ytResult, selectedUrl)
                            viewModel.closeYtBottomSheet()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Download", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun FormatRow(
    format: YouTubeExtractedFormat,
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextPrimary, fontSize = 15.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val mb = format.contentLength / (1024.0 * 1024.0)
            val sizeStr = if (format.contentLength > 0) String.format("%.2f MB", mb) else ""
            Text(sizeStr, color = TextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF1976D2),
                    unselectedColor = TextMuted
                )
            )
        }
    }
}
