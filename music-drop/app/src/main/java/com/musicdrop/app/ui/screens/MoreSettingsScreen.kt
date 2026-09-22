package com.musicdrop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.app.ui.components.EqualizerDialog
import com.musicdrop.app.ui.components.SkinThemeDialog
import com.musicdrop.app.ui.theme.*
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
    val cardOpacity by viewModel.cardOpacity.collectAsState()
    val appFontFamily by viewModel.appFontFamily.collectAsState()
    val appFontColor by viewModel.appFontColorOption.collectAsState()
    val currentWallpaperUri by viewModel.themeWallpaperUri.collectAsState()

    var showEqDialog by remember { mutableStateOf(false) }
    var showSkinDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        // ── TOP APP BAR (Clean, Transparent, Edge-to-Edge) ─────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(appColors.surfaceElevated.copy(alpha = (cardOpacity * 0.75f).coerceIn(0.12f, 0.90f)))
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(appColors.surface.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = appColors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Settings",
                        color = appColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Theme, Transparency, Audio & Interface",
                        color = appColors.textSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }

            // Quick Studio Palette Trigger
            IconButton(
                onClick = { showSkinDialog = true },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(appColors.accentPrimary.copy(alpha = 0.18f))
            ) {
                Icon(
                    Icons.Rounded.Palette,
                    contentDescription = "Skin Studio",
                    tint = appColors.accentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── SECTION 1: APPEARANCE & THEME STUDIO HERO ─────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.surfaceElevated),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.surfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(appColors.accentPrimary.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = appColors.accentPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Appearance & Theme Studio",
                                        color = appColors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "${currentTheme.displayName} • ${(cardOpacity * 100).toInt()}% Glass Opacity",
                                        color = appColors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { showSkinDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = appColors.accentPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Full Studio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Theme Palette Horizontal Chips
                        Text(
                            "Instant Themes",
                            color = appColors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val allThemes = AppThemeMode.values()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allThemes.forEach { mode ->
                                val isSelected = mode == currentTheme
                                val swatch = swatchFor(mode)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) appColors.accentPrimary.copy(alpha = 0.22f) else appColors.surface)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.8.dp,
                                            color = if (isSelected) appColors.accentPrimary else appColors.surfaceBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.setAppTheme(mode) }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(swatch.accentPrimary)
                                    )
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        text = mode.displayName,
                                        color = if (isSelected) appColors.accentPrimary else appColors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Glass Translucency / Opacity Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Glass Translucency & Opacity",
                                    color = appColors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    "Adjust transparency of category cards & sections",
                                    color = appColors.textSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(appColors.accentPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "${(cardOpacity * 100).toInt()}%",
                                    color = appColors.accentPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Slider(
                            value = cardOpacity,
                            onValueChange = { viewModel.setCardOpacity(it) },
                            valueRange = 0.10f..1.0f,
                            steps = 17,
                            colors = SliderDefaults.colors(
                                thumbColor = appColors.accentPrimary,
                                activeTrackColor = appColors.accentPrimary,
                                inactiveTrackColor = appColors.surfaceBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 4 Opacity Preset Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Crystal (20%)" to 0.20f,
                                "Balanced (55%)" to 0.55f,
                                "Frosted (75%)" to 0.75f,
                                "Solid (100%)" to 1.0f
                            ).forEach { (label, value) ->
                                val isCurrent = kotlin.math.abs(cardOpacity - value) < 0.08f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCurrent) appColors.accentPrimary else appColors.surface)
                                        .clickable { viewModel.setCardOpacity(value) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isCurrent) Color.White else appColors.textPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // App Font Family Style
                        Text(
                            "App Font Style",
                            color = appColors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "DEFAULT" to ("Default Sans" to FontFamily.Default),
                                "SANS_SERIF" to ("Modern Sans" to FontFamily.SansSerif),
                                "ROUNDED" to ("Rounded" to FontFamily.SansSerif),
                                "SERIF" to ("Serif Elegant" to FontFamily.Serif),
                                "MONOSPACE" to ("Monospace Code" to FontFamily.Monospace)
                            ).forEach { (id, pair) ->
                                val isSelected = appFontFamily.equals(id, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) appColors.accentPrimary.copy(alpha = 0.22f) else appColors.surface)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.8.dp,
                                            color = if (isSelected) appColors.accentPrimary else appColors.surfaceBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.setAppFontFamily(id) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pair.first,
                                        fontFamily = pair.second,
                                        color = if (isSelected) appColors.accentPrimary else appColors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Font Color & Text Contrast
                        Text(
                            "Font Color & Text Contrast",
                            color = appColors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "DEFAULT" to ("Adaptive" to appColors.textPrimary),
                                "PURE_WHITE" to ("Pure White" to Color.White),
                                "WARM_CREAM" to ("Warm Cream" to Color(0xFFFFFBEB)),
                                "GOLD_ACCENT" to ("Vivid Gold" to Color(0xFFFDE047)),
                                "CYAN_ICE" to ("Cyan Ice" to Color(0xFF67E8F9)),
                                "HIGH_CONTRAST" to ("High Contrast" to (if (appColors.isDark) Color.White else Color(0xFF0F172A)))
                            ).forEach { (id, pair) ->
                                val isSelected = appFontColor.equals(id, ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) appColors.accentPrimary.copy(alpha = 0.22f) else appColors.surface)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.8.dp,
                                            color = if (isSelected) appColors.accentPrimary else appColors.surfaceBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.setAppFontColorOption(id) }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(pair.second)
                                            .border(0.5.dp, Color.Gray, CircleShape)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = pair.first,
                                        color = if (isSelected) appColors.accentPrimary else appColors.textPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── SECTION 2: AUDIO QUALITY & PLAYBACK MIX ───────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.surfaceElevated),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.surfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Audio Quality & DJ Transitions",
                                    color = appColors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Hardware equalizer, bass booster & seamless mix",
                                    color = appColors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Audio Equalizer Action Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(appColors.surface)
                                .clickable { showEqDialog = true }
                                .padding(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Hardware Audio Equalizer",
                                        color = appColors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("5-Band + Bass", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Presets: Rock, Bass, Electronic, Acoustic, Flat & Custom sliders",
                                    color = appColors.textSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                            Icon(Icons.Rounded.Tune, contentDescription = "Open", tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Seamless Merge & Crossfade (Zero-Gap)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    "Seamless Merge & Crossfade (Zero-Gap)",
                                    color = appColors.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Gently fade out the ending song and smoothly fade in the next track with zero silent gap or abrupt cuts",
                                    color = appColors.textSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Switch(
                                checked = isDjCrossfadeEnabled,
                                onCheckedChange = { viewModel.setDjCrossfadeEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = appColors.accentPrimary,
                                    uncheckedThumbColor = appColors.textMuted,
                                    uncheckedTrackColor = appColors.surface
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 0.5.dp,
                            color = appColors.surfaceBorder.copy(alpha = 0.4f)
                        )

                        // Smart Silence Trimming
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    "Smart Silence Trimming",
                                    color = appColors.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Automatically eliminate silent intros and trailing dead air between songs",
                                    color = appColors.textSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Switch(
                                checked = isSilenceTrimEnabled,
                                onCheckedChange = { viewModel.setSilenceTrimEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = appColors.accentPrimary,
                                    uncheckedThumbColor = appColors.textMuted,
                                    uncheckedTrackColor = appColors.surface
                                )
                            )
                        }
                    }
                }
            }

            // ── SECTION 3: INTERFACE & NAVIGATION ─────────────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.surfaceElevated),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.surfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Navigation, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Interface & Navigation",
                                    color = appColors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Bottom navigation bar and gesture controls",
                                    color = appColors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    "Show Bottom Navigation Bar",
                                    color = appColors.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Display bottom tabs (Home, Explore, Library). Keep disabled for pure edge-to-edge top swiping.",
                                    color = appColors.textSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Switch(
                                checked = showBottomNav,
                                onCheckedChange = { viewModel.setShowBottomNav(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = appColors.accentPrimary,
                                    uncheckedThumbColor = appColors.textMuted,
                                    uncheckedTrackColor = appColors.surface
                                )
                            )
                        }
                    }
                }
            }

            // ── SECTION 4: ANDROID AUTO & CAR DISPLAY ─────────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.surfaceElevated),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.surfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Android Auto & In-Car Audio",
                                    color = appColors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Car head-unit media browser & YouTube video overlay",
                                    color = appColors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(appColors.surface)
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "MusicDrop Auto Media Service: Active",
                                    color = appColors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Automatically syncs your offline songs and online charts to the dashboard",
                                    color = appColors.textSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val context = androidx.compose.ui.platform.LocalContext.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
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
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Overlay Permission", fontSize = 11.sp, maxLines = 1)
                            }

                            Button(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(context, com.musicdrop.app.auto.VideoOverlayService::class.java).apply {
                                            action = com.musicdrop.app.auto.VideoOverlayService.ACTION_SHOW
                                        }
                                        context.startService(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Overlay: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = appColors.accentPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Test Car Overlay", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // ── SECTION 5: STORAGE & CACHE MANAGEMENT ─────────────────────────
            item {
                var cacheSizeText by remember { mutableStateOf(viewModel.getFormattedCacheSize()) }
                var isClearingCache by remember { mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current

                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.surfaceElevated),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.surfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Cached, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Storage & Stream Cache",
                                    color = appColors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Manage temporary streaming data and cache",
                                    color = appColors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Stream & Media Cache",
                                        color = appColors.textPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFF59E0B).copy(alpha = 0.18f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(cacheSizeText, color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Cached audio streams, search indexes & thumbnails. Your downloaded offline songs are kept safe.",
                                    color = appColors.textSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp
                                )
                            }

                            Button(
                                onClick = {
                                    isClearingCache = true
                                    viewModel.clearAppStreamCache {
                                        cacheSizeText = viewModel.getFormattedCacheSize()
                                        isClearingCache = false
                                        android.widget.Toast.makeText(context, "Stream cache cleared", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isClearingCache,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                    contentColor = Color(0xFFF59E0B)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    if (isClearingCache) "Clearing..." else "Clear Cache",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ── SECTION 6: APP INFO & INSTANT UPDATER ─────────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = appColors.surfaceElevated),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.surfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(appColors.accentPrimary.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = appColors.accentPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "MusicDrop",
                                        color = appColors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "v${com.musicdrop.app.BuildConfig.VERSION_NAME} (Build ${com.musicdrop.app.BuildConfig.VERSION_CODE})",
                                        color = appColors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.checkForAppUpdate(manualToast = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = appColors.accentPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Check Update", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "High-fidelity lossless music streaming, YouTube player, offline downloads, and in-car entertainment.",
                            color = appColors.textMuted,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Dialogs
        if (showEqDialog) {
            EqualizerDialog(
                equalizerManager = viewModel.equalizerManager,
                onDismiss = { showEqDialog = false }
            )
        }

        if (showSkinDialog) {
            SkinThemeDialog(
                currentTheme = currentTheme,
                onSelectTheme = { mode -> viewModel.setAppTheme(mode) },
                onDismiss = { showSkinDialog = false },
                viewModel = viewModel
            )
        }
    }
}

/** Helper swatch color lookup for theme pill badges */
private fun swatchFor(mode: AppThemeMode): AppColors = when (mode) {
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
    AppThemeMode.ROYAL_PLUM -> RoyalPlumAppColors
    AppThemeMode.DEEP_NAVY -> DeepNavyAppColors
    AppThemeMode.ROSE_GOLD -> RoseGoldAppColors
}
