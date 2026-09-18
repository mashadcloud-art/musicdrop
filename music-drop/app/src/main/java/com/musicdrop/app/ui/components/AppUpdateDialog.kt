package com.musicdrop.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.musicdrop.app.data.updater.AppUpdateInfo
import com.musicdrop.app.ui.theme.LocalAppColors

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    progress: Float?,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val appColors = LocalAppColors.current
    val isDownloading = progress != null

    Dialog(
        onDismissRequest = {
            if (!updateInfo.forceUpdate && !isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.forceUpdate && !isDownloading,
            dismissOnClickOutside = !updateInfo.forceUpdate && !isDownloading
        )
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = appColors.surfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, appColors.accentPrimary.copy(alpha = 0.45f)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(appColors.accentPrimary, appColors.accentSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDownloading) Icons.Rounded.Download else Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title & Version Badge
                Text(
                    text = updateInfo.title,
                    color = appColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = appColors.accentPrimary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.accentPrimary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "Version ${updateInfo.latestVersionName}",
                        color = appColors.accentPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Changelog Box
                if (updateInfo.changelog.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = appColors.background.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.surfaceBorder.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NewReleases,
                                    contentDescription = null,
                                    tint = appColors.accentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "What's New:",
                                    color = appColors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = updateInfo.changelog,
                                color = appColors.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Download Progress
                AnimatedVisibility(visible = isDownloading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val pct = ((progress ?: 0f) * 100).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (pct >= 100) "Opening Installer..." else "Downloading...",
                                color = appColors.textSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "$pct%",
                                color = appColors.accentPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress ?: 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = appColors.accentPrimary,
                            trackColor = appColors.surfaceBorder
                        )
                    }
                }

                // Action Buttons
                Button(
                    onClick = onUpdate,
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appColors.accentPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (isDownloading) "Downloading..." else "Update & Install Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (!updateInfo.forceUpdate && !isDownloading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Later",
                            color = appColors.textMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
