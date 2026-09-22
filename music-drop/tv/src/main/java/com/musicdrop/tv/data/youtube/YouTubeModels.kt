package com.musicdrop.tv.data.youtube

/**
 * One row of a YouTube result — just enough to render a result card and hand a
 * video id to the IFrame player. No stream URLs, no audio data: the actual bytes
 * never pass through this app, only official metadata.
 */
data class YouTubeSearchResult(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val duration: String = ""
) {
    /** Returns true if this item is likely a single song (not a 1+ hour continuous DJ mix or podcast). */
    fun isSongDuration(): Boolean {
        if (duration.isBlank()) return true
        val parts = duration.split(":")
        if (parts.size > 2) return false // > 1 hour (e.g. 1:12:30)
        if (parts.size == 2) {
            val minutes = parts[0].toIntOrNull() ?: 0
            if (minutes > 15) return false // > 15 mins
        }
        return true
    }
}

/** Outcome of a search call, including the specific reason for a failure. */
sealed class YouTubeSearchOutcome {
    data class Success(val results: List<YouTubeSearchResult>) : YouTubeSearchOutcome()
    data class Error(val message: String) : YouTubeSearchOutcome()
}

