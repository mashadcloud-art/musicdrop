package com.musicdrop.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.musicdrop.app.ui.theme.AppThemeMode
import com.musicdrop.app.ui.theme.LocalAppColors
import com.musicdrop.app.ui.viewmodel.MainViewModel

data class ThemeSwatchData(
    val mode: AppThemeMode,
    val name: String,
    val brush: Brush,
    val isLight: Boolean = false
)

data class SkinWallpaper(
    val id: String,
    val title: String,
    val category: String,
    val imageUrl: String
)

data class FontOption(
    val id: String,
    val label: String,
    val family: FontFamily
)

data class FontColorOption(
    val id: String,
    val label: String,
    val previewColor: Color
)

@Composable
fun SkinThemeDialog(
    currentTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit,
    viewModel: MainViewModel? = null
) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current

    val cardOpacity by (viewModel?.cardOpacity?.collectAsState() ?: remember { mutableFloatStateOf(0.55f) })
    val appFontFamily by (viewModel?.appFontFamily?.collectAsState() ?: remember { mutableStateOf("DEFAULT") })
    val appFontColor by (viewModel?.appFontColorOption?.collectAsState() ?: remember { mutableStateOf("DEFAULT") })
    val currentWallpaperUri by (viewModel?.themeWallpaperUri?.collectAsState() ?: remember { mutableStateOf<String?>(null) })

    val themeSwatches = remember {
        listOf(
            // Row 1
            ThemeSwatchData(AppThemeMode.ROYAL_PLUM, "Royal Plum", Brush.linearGradient(listOf(Color(0xFF3B0D28), Color(0xFFE11D48)))),
            ThemeSwatchData(AppThemeMode.DEEP_NAVY, "Deep Navy", Brush.linearGradient(listOf(Color(0xFF0A1E3F), Color(0xFF1E40AF)))),
            ThemeSwatchData(AppThemeMode.YOUTUBE_MUSIC, "YouTube Music", Brush.linearGradient(listOf(Color(0xFF180303), Color(0xFFDC2626)))),
            ThemeSwatchData(AppThemeMode.OLED_BLACK, "OLED Black", Brush.linearGradient(listOf(Color(0xFF000000), Color(0xFF10B981)))),
            ThemeSwatchData(AppThemeMode.SUNSET_NEBULA, "Sunset Nebula", Brush.linearGradient(listOf(Color(0xFF2E1065), Color(0xFF7C3AED)))),

            // Row 2
            ThemeSwatchData(AppThemeMode.MUSIC_PULSE, "Pulse", Brush.linearGradient(listOf(Color(0xFF172554), Color(0xFF8E7CFF)))),
            ThemeSwatchData(AppThemeMode.ROSE_GOLD, "Rose Gold", Brush.linearGradient(listOf(Color(0xFFFFF1F2), Color(0xFFFB7185))), isLight = true),
            ThemeSwatchData(AppThemeMode.CYBER_DARK, "Cyber Dark", Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF334155)))),
            ThemeSwatchData(AppThemeMode.MUSIC_ORBIT, "Orbit", Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF047857)))),
            ThemeSwatchData(AppThemeMode.TURBO_CONNECT, "Turbo Connect", Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF2563EB)))),

            // Row 3
            ThemeSwatchData(AppThemeMode.RETRO, "Retro", Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C)))),
            ThemeSwatchData(AppThemeMode.MUSIC_GREENROOM, "Greenroom", Brush.linearGradient(listOf(Color(0xFF84CC16), Color(0xFF65A30D)))),
            ThemeSwatchData(AppThemeMode.GLASSMORPHISM, "Glassmorphism", Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8)))),
            ThemeSwatchData(AppThemeMode.CLEAN_LIGHT, "Clean Light", Brush.linearGradient(listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))), isLight = true),
            ThemeSwatchData(AppThemeMode.IOS_LIGHT, "iOS Light", Brush.linearGradient(listOf(Color(0xFFF2F2F7), Color(0xFF007AFF))), isLight = true)
        )
    }

    val fontOptions = remember {
        listOf(
            FontOption("DEFAULT", "Default Sans", FontFamily.Default),
            FontOption("SANS_SERIF", "Modern Sans", FontFamily.SansSerif),
            FontOption("ROUNDED", "Rounded", FontFamily.SansSerif),
            FontOption("SERIF", "Serif Elegant", FontFamily.Serif),
            FontOption("MONOSPACE", "Monospace", FontFamily.Monospace)
        )
    }

    val fontColorOptions = remember {
        listOf(
            FontColorOption("DEFAULT", "Adaptive", appColors.textPrimary),
            FontColorOption("PURE_WHITE", "Pure White", Color.White),
            FontColorOption("WARM_CREAM", "Warm Cream", Color(0xFFFFFBEB)),
            FontColorOption("GOLD_ACCENT", "Vivid Gold", Color(0xFFFDE047)),
            FontColorOption("CYAN_ICE", "Cyan Ice", Color(0xFF67E8F9)),
            FontColorOption("HIGH_CONTRAST", "High Contrast", if (appColors.isDark) Color.White else Color(0xFF0F172A))
        )
    }

    val wallpaperCategories = listOf("All images", "Nature", "Space", "Anime", "Others")
    var selectedCategory by remember { mutableStateOf("All images") }

    val wallpapers = remember {
        listOf(
            SkinWallpaper("wp_1", "Earth Orbit", "Space", "https://images.unsplash.com/photo-1614728894747-a83421e2b9c9?w=600&auto=format&fit=crop&q=80"),
            SkinWallpaper("wp_2", "Cyber Anime", "Anime", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80"),
            SkinWallpaper("wp_3", "Supercar", "Others", "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=600&auto=format&fit=crop&q=80"),
            SkinWallpaper("wp_4", "Cosmic Nebula", "Space", "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80"),
            SkinWallpaper("wp_5", "Snowy Horizon", "Nature", "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=600&auto=format&fit=crop&q=80"),
            SkinWallpaper("wp_6", "Sunset Ocean", "Nature", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&auto=format&fit=crop&q=80")
        )
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel?.setThemeWallpaper(uri.toString())
            Toast.makeText(context, "Custom wallpaper applied!", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
            color = appColors.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── HEADER: [< Skin & Appearance Studio] ───────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = appColors.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Skin & Appearance",
                            color = appColors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Themes, Translucency, Typography & Wallpapers",
                            color = appColors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 36.dp)
                ) {
                    // ── SECTION 1: THEME COLOR PALETTE ────────────────────────────────
                    item {
                        Text(
                            text = "Theme Palette",
                            color = appColors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 5-Column Circular Swatch Grid
                        val chunkedSwatches = themeSwatches.chunked(5)
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            chunkedSwatches.forEach { rowSwatches ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowSwatches.forEach { swatch ->
                                        val isSelected = currentTheme == swatch.mode

                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(swatch.brush)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.2.dp,
                                                    color = if (isSelected) appColors.accentPrimary else (if (appColors.isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f)),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    onSelectTheme(swatch.mode)
                                                    Toast.makeText(context, "${swatch.name} Theme Applied", Toast.LENGTH_SHORT).show()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.75f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    repeat(5 - rowSwatches.size) {
                                        Spacer(modifier = Modifier.size(54.dp))
                                    }
                                }
                            }
                        }
                    }

                    // ── SECTION 2: CARD & SECTION GLASS OPACITY ───────────────────────
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = appColors.surfaceElevated),
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, appColors.surfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Card & Section Translucency",
                                            color = appColors.textPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Glass transparency across categories & cards",
                                            color = appColors.textSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(appColors.accentPrimary.copy(alpha = 0.18f))
                                            .border(1.dp, appColors.accentPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${(cardOpacity * 100).toInt()}% Opacity",
                                            color = appColors.accentPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                Slider(
                                    value = cardOpacity,
                                    onValueChange = { viewModel?.setCardOpacity(it) },
                                    valueRange = 0.10f..1.0f,
                                    steps = 17,
                                    colors = SliderDefaults.colors(
                                        thumbColor = appColors.accentPrimary,
                                        activeTrackColor = appColors.accentPrimary,
                                        inactiveTrackColor = appColors.surfaceBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(8.dp))

                                // Quick presets
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "Glass 20%" to 0.20f,
                                        "Balanced 55%" to 0.55f,
                                        "Frosted 75%" to 0.75f,
                                        "Solid 100%" to 1.0f
                                    ).forEach { (label, value) ->
                                        val isCurrent = kotlin.math.abs(cardOpacity - value) < 0.08f
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isCurrent) appColors.accentPrimary else appColors.surface)
                                                .clickable { viewModel?.setCardOpacity(value) }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isCurrent) Color.White else appColors.textPrimary,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── SECTION 3: APP FONT STYLE ─────────────────────────────────────
                    item {
                        Column {
                            Text(
                                text = "App Font Style",
                                color = appColors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                fontOptions.take(3).forEach { opt ->
                                    val isSelected = appFontFamily.equals(opt.id, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) appColors.accentPrimary.copy(alpha = 0.2f) else appColors.surfaceElevated)
                                            .border(1.2.dp, if (isSelected) appColors.accentPrimary else appColors.surfaceBorder, RoundedCornerShape(12.dp))
                                            .clickable { viewModel?.setAppFontFamily(opt.id) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = opt.label,
                                            fontFamily = opt.family,
                                            color = if (isSelected) appColors.accentPrimary else appColors.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                fontOptions.drop(3).forEach { opt ->
                                    val isSelected = appFontFamily.equals(opt.id, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) appColors.accentPrimary.copy(alpha = 0.2f) else appColors.surfaceElevated)
                                            .border(1.2.dp, if (isSelected) appColors.accentPrimary else appColors.surfaceBorder, RoundedCornerShape(12.dp))
                                            .clickable { viewModel?.setAppFontFamily(opt.id) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = opt.label,
                                            fontFamily = opt.family,
                                            color = if (isSelected) appColors.accentPrimary else appColors.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── SECTION 4: FONT COLOR & TEXT CONTRAST ─────────────────────────
                    item {
                        Column {
                            Text(
                                text = "Font Color & Contrast",
                                color = appColors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val colorChunks = fontColorOptions.chunked(3)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                colorChunks.forEach { rowColors ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowColors.forEach { opt ->
                                            val isSelected = appFontColor.equals(opt.id, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) appColors.accentPrimary.copy(alpha = 0.2f) else appColors.surfaceElevated)
                                                    .border(1.2.dp, if (isSelected) appColors.accentPrimary else appColors.surfaceBorder, RoundedCornerShape(12.dp))
                                                .clickable { viewModel?.setAppFontColorOption(opt.id) }
                                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(opt.previewColor)
                                                            .border(0.5.dp, Color.Gray, CircleShape)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = opt.label,
                                                        color = if (isSelected) appColors.accentPrimary else appColors.textPrimary,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                        repeat(3 - rowColors.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── SECTION 5: WALLPAPERS & SKINS ─────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Wallpaper Skin",
                                color = appColors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (currentWallpaperUri != null) {
                                TextButton(onClick = {
                                    viewModel?.setThemeWallpaper(null)
                                    Toast.makeText(context, "Wallpaper cleared", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text("Reset to Solid", color = appColors.accentPrimary, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Category Tabs (All images, Nature, Space, Anime, Others)
                        ScrollableTabRow(
                            selectedTabIndex = wallpaperCategories.indexOf(selectedCategory).coerceAtLeast(0),
                            containerColor = Color.Transparent,
                            divider = {},
                            edgePadding = 0.dp,
                            indicator = { tabPositions ->
                                val idx = wallpaperCategories.indexOf(selectedCategory).coerceAtLeast(0)
                                if (idx < tabPositions.size) {
                                    Box(
                                        Modifier
                                            .tabIndicatorOffset(tabPositions[idx])
                                            .height(3.dp)
                                            .padding(horizontal = 12.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(appColors.textPrimary)
                                    )
                                }
                            }
                        ) {
                            wallpaperCategories.forEach { cat ->
                                val isSelected = cat == selectedCategory
                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    text = {
                                        Text(
                                            text = cat,
                                            color = if (isSelected) appColors.textPrimary else appColors.textSecondary,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        val filteredWallpapers = if (selectedCategory == "All images") {
                            wallpapers
                        } else {
                            wallpapers.filter { it.category.equals(selectedCategory, ignoreCase = true) }
                        }

                        // Grid of Wallpapers (Custom Card + Preset Cards)
                        val allCards = listOf<SkinWallpaper?>(null) + filteredWallpapers
                        val rows = allCards.chunked(3)

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            rows.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (item == null) {
                                                // "Custom" gallery card
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(0.60f)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(
                                                            Brush.verticalGradient(
                                                                listOf(Color(0xFF2A1C6A), Color(0xFF130E38))
                                                            )
                                                        )
                                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                                        .clickable { galleryPickerLauncher.launch("image/*") },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(44.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.White.copy(alpha = 0.15f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                                                contentDescription = "Custom Image",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(
                                                            text = "Custom",
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            } else {
                                                val isApplied = currentWallpaperUri == item.imageUrl
                                                // Aesthetic wallpaper card
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(0.60f)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(appColors.surfaceElevated)
                                                        .border(
                                                            width = if (isApplied) 2.5.dp else 1.dp,
                                                            color = if (isApplied) appColors.accentPrimary else (if (appColors.isDark) Color.White.copy(alpha = 0.15f) else appColors.surfaceBorder),
                                                            shape = RoundedCornerShape(16.dp)
                                                        )
                                                        .clickable {
                                                            viewModel?.setThemeWallpaper(item.imageUrl)
                                                            Toast.makeText(context, "Applied ${item.title} wallpaper", Toast.LENGTH_SHORT).show()
                                                        }
                                                ) {
                                                    AsyncImage(
                                                        model = item.imageUrl,
                                                        contentDescription = item.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    if (isApplied) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(6.dp)
                                                                .size(22.dp)
                                                                .clip(CircleShape)
                                                                .background(appColors.accentPrimary),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
