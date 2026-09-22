package com.musicdrop.tv.data

data class TvVideoItem(
    val id: String,
    val title: String,
    val channelTitle: String,
    val channelAvatarUrl: String? = null,
    val thumbnailUrl: String,
    val duration: String = "",
    val views: String = "",
    val isLive: Boolean = false,
    val highQualityThumbnailUrl: String = "https://i.ytimg.com/vi/$id/maxresdefault.jpg"
)

data class TvCategory(
    val id: String,
    val title: String,
    val emoji: String,
    val query: String
)

enum class TvNavScreen {
    HOME,
    SEARCH,
    BROWSE,
    DOWNLOADS,
    SETTINGS
}

data class TvDownloadedMedia(
    val id: String,
    val title: String,
    val channelTitle: String,
    val filePath: String,
    val thumbnailUrl: String,
    val isVideo: Boolean,
    val sizeBytes: Long,
    val durationMs: Long = 0L,
    val dateAddedMs: Long = System.currentTimeMillis()
)
