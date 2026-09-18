package com.musicdrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.app.ui.theme.DarkBg
import com.musicdrop.app.ui.theme.ElectricLime
import com.musicdrop.app.ui.theme.SurfaceBorder
import com.musicdrop.app.ui.theme.SurfaceDark
import com.musicdrop.app.ui.theme.SurfaceElevated
import com.musicdrop.app.ui.theme.TextMuted
import com.musicdrop.app.ui.theme.TextPrimary
import com.musicdrop.app.ui.theme.TextSecondary
import com.musicdrop.app.ui.theme.VibrantCoral

/**
 * Ultra-clean single filter button that replaces crowded multiple dropdowns on pages.
 * Shows active filter indicator and opens the UnifiedFilterModal on click.
 */
@Composable
fun UnifiedFilterButton(
    onClick: () -> Unit,
    activeTimeFilter: MediaTimeFilter,
    activeFolder: String,
    hideRingtones: Boolean = false,
    accentColor: Color = ElectricLime,
    modifier: Modifier = Modifier
) {
    val isFiltered = activeTimeFilter != MediaTimeFilter.ALL ||
            (activeFolder != "All Folders" && activeFolder != "All Locations") ||
            hideRingtones

    val filterText = when {
        activeTimeFilter != MediaTimeFilter.ALL && (activeFolder != "All Folders" && activeFolder != "All Locations") ->
            "${activeTimeFilter.label} • $activeFolder"
        activeTimeFilter != MediaTimeFilter.ALL ->
            activeTimeFilter.label
        activeFolder != "All Folders" && activeFolder != "All Locations" ->
            activeFolder
        hideRingtones -> "Filtered"
        else -> "Filters"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFiltered) accentColor.copy(alpha = 0.20f) else SurfaceElevated)
            .border(1.dp, if (isFiltered) accentColor else SurfaceBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = "Filter",
                tint = if (isFiltered) accentColor else TextMuted,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = filterText,
                fontSize = 12.sp,
                fontWeight = if (isFiltered) FontWeight.Bold else FontWeight.Medium,
                color = if (isFiltered) accentColor else TextPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = "Open Filter",
                tint = if (isFiltered) accentColor else TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Modern modal bottom sheet popup for filtering all files, photos, videos, and music.
 * Replaces crowded individual dropdown pills with an organized, high-end popup dialog.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UnifiedFilterModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    selectedTimeFilter: MediaTimeFilter,
    onTimeFilterSelect: (MediaTimeFilter) -> Unit,
    availableFolders: List<String>,
    selectedFolder: String,
    onFolderSelect: (String) -> Unit,
    showRingtoneFilter: Boolean = false,
    hideRingtones: Boolean = false,
    onToggleHideRingtones: (() -> Unit)? = null,
    accentColor: Color = ElectricLime,
    title: String = "Filter Library"
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        tonalElevation = 8.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(SurfaceBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            onTimeFilterSelect(MediaTimeFilter.ALL)
                            if (availableFolders.isNotEmpty()) onFolderSelect(availableFolders.first())
                        }
                    ) {
                        Text(
                            text = "Reset",
                            color = VibrantCoral,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. TIME RANGE SECTION
            Text(
                text = "Time Added",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MediaTimeFilter.values().forEach { timeFilter ->
                    val isSelected = selectedTimeFilter == timeFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.22f) else SurfaceElevated)
                            .border(1.dp, if (isSelected) accentColor else SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable { onTimeFilterSelect(timeFilter) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = timeFilter.label,
                                color = if (isSelected) accentColor else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. FOLDERS SECTION
            if (availableFolders.isNotEmpty()) {
                Text(
                    text = "📁 Folders & Locations",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableFolders.forEach { folder ->
                        val isSelected = selectedFolder.equals(folder, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) accentColor.copy(alpha = 0.22f) else SurfaceElevated)
                                .border(1.dp, if (isSelected) accentColor else SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable { onFolderSelect(folder) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (folder == "All Folders" || folder == "All Locations") "📁 $folder" else "📍 $folder",
                                    color = if (isSelected) accentColor else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. RINGTONE & SHORT AUDIO TOGGLE (Optional for Music)
            if (showRingtoneFilter && onToggleHideRingtones != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                        .clickable { onToggleHideRingtones() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pure Songs Only",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Exclude ringtones, notifications, and audio clips under 25s",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = hideRingtones,
                            onCheckedChange = { onToggleHideRingtones() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0D0B1A),
                                checkedTrackColor = accentColor,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color(0xFF2D2644)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Apply Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Apply Filters",
                    color = Color(0xFF0D0B1A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
