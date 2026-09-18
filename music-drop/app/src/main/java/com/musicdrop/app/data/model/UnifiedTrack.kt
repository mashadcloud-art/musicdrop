package com.musicdrop.app.data.model

import com.musicdrop.app.data.repository.VimeoRepository
import com.musicdrop.app.data.youtube.YouTubeSearchResult

/**
 * A single, source-agnostic "music card" the UI can render without ever revealing
 * which backend a track actually came from — mirrors the pattern found in the
 * reference MusiX app's decompiled code (`WebDownloadInfo` + a single dispatcher
 * that branches internally on a hidden `site` field while every card/button in the
 * UI looks and behaves identically). [MainViewModel.playUnified] and
 * [MainViewModel.downloadUnified] are the single play/download entry points that
 * branch on the sealed subtype the same way.
 */
sealed class UnifiedTrack {
    /** Stable key for list diffing/dedup — unique across all sources. */
    abstract val key: String
    abstract val title: String
    abstract val artist: String
    abstract val thumbnailUrl: String
    abstract val duration: String

    /**
     * Which backend this card actually plays from. The blended feed still looks like
     * one unified library — this is only surfaced as a small badge on the card (see
     * DiscoverScreen's UnifiedMusicCard), not a big label, per the user's request to
     * be able to tell sources apart at a glance without breaking the "one library" feel.
     */
    abstract val sourceName: String

    data class Youtube(val result: YouTubeSearchResult) : UnifiedTrack() {
        override val key get() = "yt:${result.videoId}"
        override val title get() = result.title
        override val artist get() = result.channelTitle
        override val thumbnailUrl get() = result.thumbnailUrl
        override val duration get() = result.duration
        override val sourceName get() = "YouTube"
    }

    data class Saavn(val item: MediaItem) : UnifiedTrack() {
        override val key get() = "sv:${item.id}"
        override val title get() = item.name
        override val artist get() = item.artist
        override val thumbnailUrl get() = item.albumArtUri?.toString().orEmpty()
        override val duration get() = item.formattedDuration
        override val sourceName get() = "JioSaavn"
    }

    data class Vimeo(val video: VimeoRepository.VimeoVideo) : UnifiedTrack() {
        override val key get() = "vm:${video.id}"
        override val title get() = video.title
        override val artist get() = video.authorName
        override val thumbnailUrl get() = video.thumbnailUrl
        override val duration get() = video.duration
        override val sourceName get() = "Vimeo"
    }

    data class Local(
        override val key: String,
        override val title: String,
        override val artist: String,
        override val thumbnailUrl: String,
        override val duration: String = "",
        override val sourceName: String = "Downloads",
        val filePath: String = "",
        val mediaItem: MediaItem? = null
    ) : UnifiedTrack()
}
