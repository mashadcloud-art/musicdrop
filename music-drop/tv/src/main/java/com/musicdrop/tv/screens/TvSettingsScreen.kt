package com.musicdrop.tv.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.tv.ui.components.TvFocusButton
import com.musicdrop.tv.viewmodel.TvViewModel

private val RESOLUTIONS = listOf("Auto (4K / 1080p)", "1080p Full HD", "720p HD", "Audio Only")

@Composable
fun TvSettingsScreen(viewModel: TvViewModel) {
    var selectedRes by remember { mutableStateOf("Auto (4K / 1080p)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .padding(horizontal = 48.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("⚙️ TV Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)

        HorizontalDivider(color = Color.White.copy(0.12f))

        // Preferred Playback Resolution
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Hd, null, tint = Color(0xFFFF0033), modifier = Modifier.size(24.dp))
                Text("Video Playback Quality", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text("Select preferred resolution for native hardware streaming", color = Color.White.copy(0.5f), fontSize = 13.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RESOLUTIONS.forEach { res ->
                    val isSelected = (res == selectedRes)
                    TvFocusButton(
                        onClick = { selectedRes = res },
                        cornerRadius = 14.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color.White else Color.White.copy(0.08f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                res,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(0.12f))

        // Ad-Bypass Status
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                Text("Ad-Bypass Engine", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "Active — Streams direct hardware video feeds without client-side video ads or popups.",
                color = Color.White.copy(0.6f),
                fontSize = 13.sp
            )
        }

        HorizontalDivider(color = Color.White.copy(0.12f))

        // App Information
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("MusicDrop TV Edition", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Version: 1.0.0 (Dedicated Android TV Build)", color = Color.White.copy(0.5f), fontSize = 12.sp)
            Text("Optimized for Android TV, Google TV, Fire TV & TV boxes with remote D-Pad control.", color = Color.White.copy(0.4f), fontSize = 12.sp)
        }
    }
}
