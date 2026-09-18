package com.musicdrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.app.ui.theme.SurfaceBorder
import com.musicdrop.app.ui.theme.SurfaceDark
import com.musicdrop.app.ui.theme.TextPrimary
import com.musicdrop.app.ui.theme.TextSecondary

private val AccentBlue = Color(0xFF3B82F6)

/**
 * Universal, theme-aware sticky category navigation strip across all pages.
 * activeIndex: 0 = Apps, 1 = Photos, 2 = Videos, 3 = Music, 4 = Files, 5 = Docs
 */
@Composable
fun StickyCategoryNavBar(
    activeIndex: Int = 0,
    onSelectCategory: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("Apps", "Photos", "Videos", "Music", "Files", "Docs")
    val clampedIndex = activeIndex.coerceIn(0, categories.size - 1)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .border(width = 0.5.dp, color = SurfaceBorder)
            .statusBarsPadding()
            .padding(vertical = 4.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = clampedIndex,
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (clampedIndex < tabPositions.size) {
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[clampedIndex]),
                        color = AccentBlue,
                        height = 3.dp
                    )
                }
            },
            divider = {}
        ) {
            categories.forEachIndexed { index, title ->
                val isSelected = clampedIndex == index
                Tab(
                    selected = isSelected,
                    onClick = { onSelectCategory(index) },
                    text = {
                        Text(
                            text = title,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }
    }
}
