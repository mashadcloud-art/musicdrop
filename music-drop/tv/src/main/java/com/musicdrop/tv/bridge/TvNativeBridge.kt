package com.musicdrop.tv.bridge

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.musicdrop.tv.data.TvVideoItem
import com.musicdrop.tv.download.TvDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TvNativeBridge(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val downloadManager = TvDownloadManager.getInstance(context)
    var currentVideoId: String = ""
    var currentTitle: String = ""

    @JavascriptInterface
    fun onVideoStarted(videoId: String, title: String) {
        currentVideoId = videoId
        currentTitle = title
    }

    @JavascriptInterface
    fun downloadCurrentVideo() {
        if (currentVideoId.isBlank()) {
            Toast.makeText(context, "No video currently playing", Toast.LENGTH_SHORT).show()
            return
        }
        val video = TvVideoItem(
            id = currentVideoId,
            title = currentTitle.ifBlank { "YouTube Video ($currentVideoId)" },
            channelTitle = "YouTube TV",
            thumbnailUrl = "https://img.youtube.com/vi/$currentVideoId/hqdefault.jpg"
        )
        Toast.makeText(context, "⬇ Downloading Video: ${video.title}...", Toast.LENGTH_SHORT).show()
        coroutineScope.launch(Dispatchers.IO) {
            downloadManager.downloadMedia(
                video = video,
                downloadAsVideo = true,
                onComplete = { ok, path ->
                    coroutineScope.launch(Dispatchers.Main) {
                        val msg = if (ok) "✓ Downloaded Video to TV storage!" else "Download failed"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    @JavascriptInterface
    fun downloadCurrentAudio() {
        if (currentVideoId.isBlank()) {
            Toast.makeText(context, "No video currently playing", Toast.LENGTH_SHORT).show()
            return
        }
        val video = TvVideoItem(
            id = currentVideoId,
            title = currentTitle.ifBlank { "YouTube Audio ($currentVideoId)" },
            channelTitle = "YouTube TV",
            thumbnailUrl = "https://img.youtube.com/vi/$currentVideoId/hqdefault.jpg"
        )
        Toast.makeText(context, "🎵 Downloading MP3: ${video.title}...", Toast.LENGTH_SHORT).show()
        coroutineScope.launch(Dispatchers.IO) {
            downloadManager.downloadMedia(
                video = video,
                downloadAsVideo = false,
                onComplete = { ok, path ->
                    coroutineScope.launch(Dispatchers.Main) {
                        val msg = if (ok) "✓ Downloaded MP3 to TV storage!" else "Download failed"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}
