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

@Composable
fun SkinThemeDialog(
    currentTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit,
    viewModel: MainViewModel? = null
) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current

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
            Toast.makeText(context, "Custom skin wallpaper applied!", Toast.LENGTH_SHORT).show()
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
                // ── HEADER: [< Skin theme] ─────────────────────────────────────────────
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
                    Text(
                        text = "Skin theme",
                        color = appColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // ── SECTION 1: COLOR SWATCHES (MATCHING SCREENSHOT 2) ─────────────
                    item {
                        Text(
                            text = "Color",
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
                                                    width = if (isSelected) 2.5.dp else 1.2.dp,
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
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.75f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    // Filler for incomplete rows
                                    repeat(5 - rowSwatches.size) {
                                        Spacer(modifier = Modifier.size(54.dp))
                                    }
                                }
                            }
                        }
                    }

                    // ── SECTION 2: WALLPAPERS / THEME SKINS ───────────────────────────
                    item {
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
                                            fontSize = 14.sp,
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

                        // Grid of Wallpapers (Custom Card + Image Cards)
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
                                                // "Custom" gallery card matching Image 2
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
                                                // Aesthetic wallpaper card
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(0.60f)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(appColors.surfaceElevated)
                                                        .border(1.dp, (if (appColors.isDark) Color.White.copy(alpha = 0.15f) else appColors.surfaceBorder), RoundedCornerShape(16.dp))
                                                        .clickable {
                                                            Toast.makeText(context, "Applied ${item.title} skin", Toast.LENGTH_SHORT).show()
                                                        }
                                                ) {
                                                    AsyncImage(
                                                        model = item.imageUrl,
                                                        contentDescription = item.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    // Filler for incomplete rows
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
