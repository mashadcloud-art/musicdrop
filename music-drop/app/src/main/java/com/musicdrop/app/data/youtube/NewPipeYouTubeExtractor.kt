package com.musicdrop.app.data.youtube

import android.content.Context
import android.net.Uri
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import com.musicdrop.app.data.youtube.potoken.MusicDropPoTokenProvider

/**
 * Real YouTube audio extraction via NewPipeExtractor.
 *
 * This is what the original MusiX app (found decompiled in the reference APK under
 * `com.musixmusicx.extractor` / `com.musixmusicx.ytparse`) actually used under the hood —
 * NewPipeExtractor downloads the YouTube player's own JavaScript and runs it through a
 * bundled Rhino JS engine to properly solve `signatureCipher` and the n-parameter
 * throttling function. That's a fundamentally more capable approach than the old
 * [YouTubeStreamExtractor], which only accepted formats that already came back
 * unencrypted from a handful of legacy client identities — which is exactly why it kept
 * failing as YouTube ciphers more and more formats.
 *
 * Note: this does NOT yet register a PoTokenProvider with NewPipeExtractor (that requires
 * implementing four separate BotGuard-backed methods for different InnerTube clients).
 * NewPipeExtractor works without one — just with somewhat reduced reliability on some
 * videos — so this is a large step up on its own, with PoToken wiring as a possible
 * follow-up if extraction still misses certain videos.
 */
class NewPipeYouTubeExtractor private constructor() {

    data class StreamResult(
        val url: String,
        val mimeType: String,
        val durationSec: Long
    ) {
        fun toMediaItem(
            videoId: String,
            knownTitle: String,
            knownAuthor: String,
            knownThumb: String
        ): MediaItem = MediaItem(
            id          = videoId.hashCode().toLong(),
            uri         = Uri.parse(url),
            name        = knownTitle.ifBlank { "YouTube Audio" },
            size        = 0L,
            dateAdded   = System.currentTimeMillis() / 1000,
            mimeType    = mimeType.ifBlank { "audio/mp4" },
            mediaType   = MediaType.AUDIO,
            durationMs  = durationSec * 1000L,
            artist      = knownAuthor.ifBlank { "YouTube" },
            album       = "YouTube Music",
            isSong      = true,
            filePath    = url,
            bucketName  = "YouTube Stream",
            albumArtUri = knownThumb.takeIf { it.isNotBlank() }
                ?.let { Uri.parse(it) }
                ?: Uri.parse("https://i.ytimg.com/vi/$videoId/hqdefault.jpg")
        )
    }

    data class VideoStreamResult(
        val url: String,
        val mimeType: String,
        val durationSec: Long,
        val resolution: String
    )

    /**
     * Fetches a progressive (muxed audio+video in one file) stream — the kind ExoPlayer
     * can play directly with no separate audio/video merging needed. YouTube only serves
     * a handful of these per video, usually capped somewhere around 360p-720p — that's
     * a deliberate trade for "just works, one URL" simplicity over building out a full
     * adaptive/DASH (separate audio+video tracks merged at playback time) pipeline.
     */
    suspend fun extractVideo(videoId: String): VideoStreamResult? = withContext(Dispatchers.IO) {
        try {
            val watchUrl = "https://www.youtube.com/watch?v=$videoId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, watchUrl)

            val progressive: List<VideoStream> = info.videoStreams ?: emptyList()
            val usable = progressive.filter { it.isUrl && it.content.isNotBlank() }
            if (usable.isEmpty()) {
                android.util.Log.w("NewPipeYT", "No progressive (muxed) video streams for $videoId")
                return@withContext null
            }

            // Prefer mp4 at the highest resolution available (best ExoPlayer compatibility).
            val sorted = usable.sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }
            val best = sorted.firstOrNull { it.format?.mimeType?.contains("mp4") == true } ?: sorted.first()

            android.util.Log.i(
                "NewPipeYT",
                "✅ NewPipeExtractor video success for $videoId — res=${best.resolution} format=${best.format}"
            )

            VideoStreamResult(
                url         = best.content,
                mimeType    = best.format?.mimeType ?: "video/mp4",
                durationSec = info.duration,
                resolution  = best.resolution ?: ""
            )
        } catch (e: Exception) {
            android.util.Log.e("NewPipeYT", "NewPipeExtractor video failed for $videoId: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    suspend fun extract(videoId: String): StreamResult? = withContext(Dispatchers.IO) {
        try {
            val watchUrl = "https://www.youtube.com/watch?v=$videoId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, watchUrl)

            val audioStreams: List<AudioStream> = info.audioStreams ?: emptyList()
            if (audioStreams.isEmpty()) {
                android.util.Log.w("NewPipeYT", "No audio streams for $videoId (errors=${info.errors})")
                return@withContext null
            }

            val usable = audioStreams.filter { it.isUrl && it.content.isNotBlank() }
            if (usable.isEmpty()) {
                android.util.Log.w("NewPipeYT", "${audioStreams.size} audio streams but none had a direct URL for $videoId")
                return@withContext null
            }

            // Prefer M4A/AAC (best ExoPlayer/media3 compatibility), otherwise highest bitrate.
            val sorted = usable.sortedByDescending { it.averageBitrate }
            val best = sorted.firstOrNull { it.format?.mimeType?.contains("mp4") == true }
                ?: sorted.first()

            android.util.Log.i(
                "NewPipeYT",
                "✅ NewPipeExtractor success for $videoId — bitrate=${best.averageBitrate} format=${best.format}"
            )

            StreamResult(
                url         = best.content,
                mimeType    = best.format?.mimeType ?: "audio/mp4",
                durationSec = info.duration
            )
        } catch (e: Exception) {
            // NewPipeExtractor throws various ExtractionException subtypes (age-restricted,
            // geo-blocked, private, etc.) plus plain IOException on network failure — we treat
            // all of them the same way here: log and fall back to the older extractors.
            android.util.Log.e("NewPipeYT", "NewPipeExtractor failed for $videoId: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    companion object {
        @Volatile private var initialized = false

        private fun ensureInitialized() {
            if (initialized) return
            synchronized(this) {
                if (!initialized) {
                    NewPipe.init(NewPipeDownloader.getInstance())
                    // Register the BotGuard poToken provider (ANDROID/IOS client poTokens).
                    YoutubeStreamExtractor.setPoTokenProvider(MusicDropPoTokenProvider)
                    initialized = true
                }
            }
        }

        @Volatile private var instance: NewPipeYouTubeExtractor? = null

        /** [context] is accepted for API symmetry with the other extractors; not currently used. */
        fun getInstance(context: Context): NewPipeYouTubeExtractor {
            MusicDropPoTokenProvider.attach(context.applicationContext)
            ensureInitialized()
            return instance ?: synchronized(this) {
                instance ?: NewPipeYouTubeExtractor().also { instance = it }
            }
        }
    }
}
