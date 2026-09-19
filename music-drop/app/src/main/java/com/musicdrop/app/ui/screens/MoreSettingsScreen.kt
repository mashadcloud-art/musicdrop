package com.musicdrop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.app.ui.theme.AppThemeMode
import com.musicdrop.app.ui.theme.CleanLightAppColors
import com.musicdrop.app.ui.theme.CyberDarkAppColors
import com.musicdrop.app.ui.theme.GlassmorphismAppColors
import com.musicdrop.app.ui.theme.IosLightAppColors
import com.musicdrop.app.ui.theme.LocalAppColors
import com.musicdrop.app.ui.theme.MusicGreenroomAppColors
import com.musicdrop.app.ui.theme.MusicOrbitAppColors
import com.musicdrop.app.ui.theme.MusicPulseAppColors
import com.musicdrop.app.ui.theme.NearbyShareAppColors
import com.musicdrop.app.ui.theme.OledBlackAppColors
import com.musicdrop.app.ui.theme.RetroAppColors
import com.musicdrop.app.ui.theme.SunsetNebulaAppColors
import com.musicdrop.app.ui.theme.TurboConnectAppColors
import com.musicdrop.app.ui.theme.YouTubeMusicAppColors
import com.musicdrop.app.ui.viewmodel.MainViewModel

@Composable
fun MoreSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {}
) {
    val appColors = LocalAppColors.current
    val currentTheme by viewModel.appTheme.collectAsState()
    val isDjCrossfadeEnabled by viewModel.isDjCrossfadeEnabled.collectAsState()
    val isSilenceTrimEnabled by viewModel.isSilenceTrimEnabled.collectAsState()
    val showBottomNav by viewModel.showBottomNav.collectAsState()
    var showEqDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(appColors.surface)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = appColors.textPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Theme & Settings", color = appColors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = appColors.textPrimary)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = "App", tint = appColors.accentPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Music Drop", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Version 9.9", color = appColors.textMuted, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "High quality background & screen-off music player with direct YouTube streaming, downloading, and Android Auto car support.",
                            color = appColors.textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Android Auto & Car Display Settings ──
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Menu, contentDescription = "Car", tint = appColors.accentPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Android Auto & Car Display", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("In-car media browser & YouTube video overlay", color = appColors.textMuted, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Status Row 1: Media Browser
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("MusicDrop Auto Service: Ready", color = appColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Appears automatically in your car head unit's music browser. Browse YouTube trending, JioSaavn, and offline files.",
                            color = appColors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Row 2: Video Overlay
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2196F3))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Car Video Overlay (CarStream style)", color = appColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Projects a full YouTube WebView over Android Auto. Requires granting Accessibility Service permission on your phone.",
                            color = appColors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val context = androidx.compose.ui.platform.LocalContext.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(
                                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = appColors.accentPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Overlay Permission", fontSize = 12.sp, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(context, com.musicdrop.app.auto.VideoOverlayService::class.java).apply {
                                            action = com.musicdrop.app.auto.VideoOverlayService.ACTION_SHOW
                                        }
                                        context.startService(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Test Overlay", fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "💡 Tip: In phone Android Auto settings, tap 'Version' 10 times to enable Developer settings, then enable 'Unknown sources' for full sideload support.",
                            color = appColors.textMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Playback & Seamless DJ Mix ──
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, contentDescription = "DJ Mix", tint = appColors.accentPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Playback & Seamless DJ Mix", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Song transitions and gapless playback", color = appColors.textMuted, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Toggle 1: DJ Overlap Crossfade
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    "DJ Overlap Crossfade (Seamless Mix)",
                                    color = appColors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Start next song before the current track ends for an uninterrupted DJ mashup blend.",
                                    color = appColors.textSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            Switch(
                                checked = isDjCrossfadeEnabled,
                                onCheckedChange = { viewModel.setDjCrossfadeEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = appColors.accentPrimary,
                                    uncheckedThumbColor = appColors.textMuted,
                                    uncheckedTrackColor = appColors.surfaceElevated
                                )
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 0.5.dp,
                            color = appColors.surfaceBorder.copy(alpha = 0.3f)
                        )

                        // Toggle 2: Silence & Dead-Space Trimming
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    "Smart Silence Trimming",
                                    color = appColors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Automatically eliminate silent lead-ins and trailing dead air between songs.",
                                    color = appColors.textSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            Switch(
                                checked = isSilenceTrimEnabled,
                                onCheckedChange = { viewModel.setSilenceTrimEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = appColors.accentPrimary,
                                    uncheckedThumbColor = appColors.textMuted,
                                    uncheckedTrackColor = appColors.surfaceElevated
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 0.5.dp,
                            color = appColors.surfaceBorder.copy(alpha = 0.3f)
                        )

                        // Action 3: Open Audio Equalizer
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEqDialog = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    "Audio Equalizer & Bass Boost",
                                    color = appColors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "5-band hardware equalizer, bass booster, 3D surround sound, and genre presets.",
                                    color = appColors.textSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Equalizer",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 0.5.dp,
                            color = appColors.surfaceBorder.copy(alpha = 0.3f)
                        )

                        // Toggle 4: Show Bottom Navigation Bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    "Show Bottom Navigation Bar",
                                    color = appColors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Display bottom tabs (Home, Explore, Library). Off by default for clean edge-to-edge top swiping.",
                                    color = appColors.textSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            Switch(
                                checked = showBottomNav,
                                onCheckedChange = { viewModel.setShowBottomNav(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = appColors.accentPrimary,
                                    uncheckedThumbColor = appColors.textMuted,
                                    uncheckedTrackColor = appColors.surfaceElevated
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = appColors.accentPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Theme", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            items(AppThemeMode.values().toList()) { mode ->
                ThemeOptionRow(
                    mode = mode,
                    isSelected = mode == currentTheme,
                    onSelect = { viewModel.setAppTheme(mode) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = appColors.accentPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("App Updates & Info", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = appColors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.surfaceBorder.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.checkForAppUpdate(manualToast = true) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("MusicDrop Version", color = appColors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("v${com.musicdrop.app.BuildConfig.VERSION_NAME} (Build ${com.musicdrop.app.BuildConfig.VERSION_CODE})", color = appColors.textMuted, fontSize = 12.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = appColors.accentPrimary.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, appColors.accentPrimary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "Check Updates",
                                color = appColors.accentPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        if (showEqDialog) {
            com.musicdrop.app.ui.components.EqualizerDialog(
                equalizerManager = viewModel.equalizerManager,
                onDismiss = { showEqDialog = false }
            )
        }
    }
}

/** The dark/light preview swatch for each [AppThemeMode] — same values Theme.kt uses. */
private fun swatchFor(mode: AppThemeMode): com.musicdrop.app.ui.theme.AppColors = when (mode) {
    AppThemeMode.YOUTUBE_MUSIC -> YouTubeMusicAppColors
    AppThemeMode.MUSIC_PULSE -> MusicPulseAppColors
    AppThemeMode.MUSIC_ORBIT -> MusicOrbitAppColors
    AppThemeMode.MUSIC_GREENROOM -> MusicGreenroomAppColors
    AppThemeMode.CYBER_DARK -> CyberDarkAppColors
    AppThemeMode.CLEAN_LIGHT -> CleanLightAppColors
    AppThemeMode.OLED_BLACK -> OledBlackAppColors
    AppThemeMode.SUNSET_NEBULA -> SunsetNebulaAppColors
    AppThemeMode.IOS_LIGHT -> IosLightAppColors
    AppThemeMode.NEARBY_SHARE -> NearbyShareAppColors
    AppThemeMode.TURBO_CONNECT -> TurboConnectAppColors
    AppThemeMode.RETRO -> RetroAppColors
    AppThemeMode.GLASSMORPHISM -> GlassmorphismAppColors
}

@Composable
private fun ThemeOptionRow(mode: AppThemeMode, isSelected: Boolean, onSelect: () -> Unit) {
    val appColors = LocalAppColors.current
    val swatch = swatchFor(mode)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) appColors.surfaceElevated else appColors.surface)
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // A little two-tone swatch (background + accent) previews the palette
        // without switching to it first.
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(swatch.background),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(swatch.accentPrimary)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(mode.displayName, color = appColors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(mode.subtitle, color = appColors.textMuted, fontSize = 11.sp)
        }
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = appColors.accentPrimary, modifier = Modifier.size(20.dp))
        }
    }
}
