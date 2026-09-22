package com.musicdrop.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicdrop.tv.data.model.MediaItem
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.musicdrop.tv.ui.theme.PastelLilac
import com.musicdrop.tv.ui.theme.SurfaceBorder
import com.musicdrop.tv.ui.theme.SurfaceElevated
import com.musicdrop.tv.ui.theme.TextMuted
import com.musicdrop.tv.ui.theme.TextPrimary
import com.musicdrop.tv.ui.theme.TextSecondary

enum class MediaTimeFilter(val label: String) {
    ALL("All Time"),
    LAST_HOUR("Last Hour"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week")
}

typealias GalleryTimeFilter = MediaTimeFilter

data class MediaTimeGroup(
    val title: String,
    val items: List<MediaItem>
)

fun groupMediaItems(
    items: List<MediaItem>,
    now: Long = System.currentTimeMillis(),
    startOfToday: Long = 0L,
    startOfYesterday: Long = 0L,
    oneHourAgo: Long = 0L
): List<MediaTimeGroup> {
    if (items.isEmpty()) return emptyList()

    val actualNow = if (now > 0) now else System.currentTimeMillis()
    val actualToday = if (startOfToday > 0) startOfToday else {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val actualYesterday = if (startOfYesterday > 0) startOfYesterday else (actualToday - 86400000L)
    val actualHourAgo = if (oneHourAgo > 0) oneHourAgo else (actualNow - 3600000L)

    val lastHour = mutableListOf<MediaItem>()
    val today = mutableListOf<MediaItem>()
    val yesterday = mutableListOf<MediaItem>()
    val thisWeek = mutableListOf<MediaItem>()
    val older = mutableListOf<MediaItem>()

    for (item in items) {
        val ms = item.dateAddedMs
        when {
            ms >= actualHourAgo -> lastHour.add(item)
            ms >= actualToday -> today.add(item)
            ms >= actualYesterday -> yesterday.add(item)
            ms >= (actualNow - 7 * 86400000L) -> thisWeek.add(item)
            else -> older.add(item)
        }
    }

    val groups = mutableListOf<MediaTimeGroup>()
    if (lastHour.isNotEmpty()) groups.add(MediaTimeGroup("Last Hour", lastHour))
    if (today.isNotEmpty()) groups.add(MediaTimeGroup("Today", today))
    if (yesterday.isNotEmpty()) groups.add(MediaTimeGroup("Yesterday", yesterday))
    if (thisWeek.isNotEmpty()) groups.add(MediaTimeGroup("This Week", thisWeek))
    if (older.isNotEmpty()) {
        if (groups.isEmpty()) {
            groups.add(MediaTimeGroup("Latest Files", older))
        } else {
            groups.add(MediaTimeGroup("Earlier", older))
        }
    }
    return groups
}

@Composable
fun CompactStreamlinedFilterRow(
    selectedTimeFilter: MediaTimeFilter,
    onTimeFilterSelect: (MediaTimeFilter) -> Unit,
    availableFolders: List<String>,
    selectedFolder: String,
    onFolderSelect: (String) -> Unit,
    accentColor: Color = PastelLilac,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        // Time filters
        items(MediaTimeFilter.values()) { timeFilter ->
            val isSelected = selectedTimeFilter == timeFilter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) accentColor.copy(alpha = 0.25f) else Color(0xFF1E1A24))
                    .border(1.dp, if (isSelected) accentColor else SurfaceBorder, RoundedCornerShape(10.dp))
                    .clickable { onTimeFilterSelect(timeFilter) }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(
                    text = timeFilter.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextSecondary
                )
            }
        }

        // Folder filters (if more than 1 available)
        if (availableFolders.size > 1) {
            item {
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .width(1.dp)
                        .background(SurfaceBorder)
                )
            }

            items(availableFolders) { folder ->
                val isSelected = selectedFolder.equals(folder, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) accentColor.copy(alpha = 0.20f) else Color(0xFF16131D))
                        .border(1.dp, if (isSelected) accentColor.copy(alpha = 0.8f) else SurfaceBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable { onFolderSelect(folder) }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (folder == "All Folders" || folder == "All Locations") "📁 $folder" else "📍 $folder",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) accentColor else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun CompactDropdownFilterRow(
    selectedTimeFilter: MediaTimeFilter,
    onTimeFilterSelect: (MediaTimeFilter) -> Unit,
    availableFolders: List<String>,
    selectedFolder: String,
    onFolderSelect: (String) -> Unit,
    showRingtoneFilter: Boolean = false,
    hideRingtones: Boolean = true,
    onToggleHideRingtones: (() -> Unit)? = null,
    accentColor: Color = PastelLilac,
    modifier: Modifier = Modifier
) {
    var timeMenuExpanded by remember { mutableStateOf(false) }
    var folderMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Time Filter Dropdown Pill
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedTimeFilter != MediaTimeFilter.ALL) accentColor.copy(alpha = 0.22f) else Color(0xFF1E1A26))
                    .border(1.dp, if (selectedTimeFilter != MediaTimeFilter.ALL) accentColor else SurfaceBorder, RoundedCornerShape(12.dp))
                    .clickable { timeMenuExpanded = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedTimeFilter.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTimeFilter != MediaTimeFilter.ALL) accentColor else TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = "Dropdown",
                    tint = if (selectedTimeFilter != MediaTimeFilter.ALL) accentColor else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = timeMenuExpanded,
                onDismissRequest = { timeMenuExpanded = false },
                modifier = Modifier
                    .background(SurfaceElevated)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                MediaTimeFilter.values().forEach { filter ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = filter.label,
                                color = if (selectedTimeFilter == filter) accentColor else TextPrimary,
                                fontWeight = if (selectedTimeFilter == filter) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        onClick = {
                            onTimeFilterSelect(filter)
                            timeMenuExpanded = false
                        }
                    )
                }
            }
        }

        // 2. Folder Filter Dropdown Pill
        if (availableFolders.size > 1) {
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedFolder != "All Folders" && selectedFolder != "All Locations") accentColor.copy(alpha = 0.22f) else Color(0xFF1E1A26))
                        .border(1.dp, if (selectedFolder != "All Folders" && selectedFolder != "All Locations") accentColor else SurfaceBorder, RoundedCornerShape(12.dp))
                        .clickable { folderMenuExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayFolder = if (selectedFolder.length > 14) selectedFolder.take(12) + "…" else selectedFolder
                    Text(
                        text = if (displayFolder == "All Folders" || displayFolder == "All Locations") "📁 $displayFolder" else "📍 $displayFolder",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedFolder != "All Folders" && selectedFolder != "All Locations") accentColor else TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = if (selectedFolder != "All Folders" && selectedFolder != "All Locations") accentColor else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = folderMenuExpanded,
                    onDismissRequest = { folderMenuExpanded = false },
                    modifier = Modifier
                        .background(SurfaceElevated)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                ) {
                    availableFolders.forEach { folder ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (folder == "All Folders" || folder == "All Locations") "📁 $folder" else "📍 $folder",
                                    color = if (selectedFolder.equals(folder, ignoreCase = true)) accentColor else TextPrimary,
                                    fontWeight = if (selectedFolder.equals(folder, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                onFolderSelect(folder)
                                folderMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // 3. Optional Pure Songs vs Ringtone filter
        if (showRingtoneFilter && onToggleHideRingtones != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (hideRingtones) accentColor.copy(alpha = 0.20f) else Color(0xFF1E1A26))
                    .border(1.dp, if (hideRingtones) accentColor else SurfaceBorder, RoundedCornerShape(12.dp))
                    .clickable { onToggleHideRingtones() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hideRingtones) "🎵 Songs Only" else "🔔 + Ringtones",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hideRingtones) accentColor else TextMuted
                )
            }
        }
    }
}
