package com.musicdrop.tv.data.youtube

data class YouTubeExtractedFormat(
    val url: String,
    val mimeType: String,
    val qualityLabel: String,
    val averageBitrate: Long,
    val contentLength: Long
)

data class YouTubeExtractionResult(
    val videoId: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String,
    val formats: List<YouTubeExtractedFormat>
)
