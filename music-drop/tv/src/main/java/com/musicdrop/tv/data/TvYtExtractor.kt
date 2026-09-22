package com.musicdrop.tv.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream

class TvYtExtractor private constructor() {

    data class ExtractedMedia(
        val videoUrl: String?,
        val audioUrl: String?,
        val title: String,
        val channelTitle: String,
        val durationSec: Long,
        val resolution: String,
        val mimeType: String
    )

    suspend fun extractMedia(videoId: String): ExtractedMedia? = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            val watchUrl = "https://www.youtube.com/watch?v=$videoId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, watchUrl)

            // 1. Progressive video streams (Muxed video+audio for instant ExoPlayer playback)
            val progressive: List<VideoStream> = info.videoStreams ?: emptyList()
            val usableProgressive = progressive.filter { it.isUrl && it.content.isNotBlank() }

            // 2. Video only streams (including 1080p, 1440p, 4K 2160p)
            val videoOnly: List<VideoStream> = info.videoOnlyStreams ?: emptyList()
            val usableVideoOnly = videoOnly.filter { it.isUrl && it.content.isNotBlank() }

            // 3. Audio streams (best bitrate audio)
            val audioStreams: List<AudioStream> = info.audioStreams ?: emptyList()
            val bestAudio = audioStreams
                .filter { it.isUrl && it.content.isNotBlank() }
                .maxByOrNull { it.averageBitrate }

            // Sort video candidate streams by resolution descending (e.g. 2160p, 1440p, 1080p, 720p)
            val allCandidates = (usableProgressive + usableVideoOnly)
            val sorted = allCandidates.sortedByDescending { stream ->
                stream.resolution?.replace("p", "")?.toIntOrNull() ?: 0
            }

            // Prefer MP4 container if available for maximum hardware decoding compatibility
            val bestVideo = sorted.firstOrNull { it.format?.mimeType?.contains("mp4") == true } ?: sorted.firstOrNull()

            // If we have a progressive stream, videoUrl has both audio and video
            val videoUrl = bestVideo?.content ?: usableProgressive.firstOrNull()?.content
            val audioUrl = bestAudio?.content ?: videoUrl

            if (videoUrl == null && audioUrl == null) {
                Log.w("TvYtExtractor", "No media streams found for $videoId")
                return@withContext null
            }

            ExtractedMedia(
                videoUrl = videoUrl,
                audioUrl = audioUrl,
                title = info.name.orEmpty(),
                channelTitle = info.uploaderName.orEmpty(),
                durationSec = info.duration,
                resolution = bestVideo?.resolution ?: "720p",
                mimeType = bestVideo?.format?.mimeType ?: "video/mp4"
            )
        } catch (e: Throwable) {
            Log.e("TvYtExtractor", "Error extracting $videoId: ${e.message}", e)
            null
        }
    }

    companion object {
        private var initialized = false
        private var instance: TvYtExtractor? = null

        fun getInstance(): TvYtExtractor {
            return instance ?: synchronized(this) {
                instance ?: TvYtExtractor().also { instance = it }
            }
        }

        private fun ensureInitialized() {
            if (!initialized) {
                synchronized(this) {
                    if (!initialized) {
                        NewPipe.init(NewPipeDownloader.getInstance())
                        initialized = true
                    }
                }
            }
        }
    }
}
