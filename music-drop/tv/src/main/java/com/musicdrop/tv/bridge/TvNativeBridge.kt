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
        if (videoId.isNotBlank() && videoId != currentVideoId) {
            currentVideoId = videoId
            currentTitle = title
            coroutineScope.launch(Dispatchers.Main) {
                val displayTitle = if (title.isNotBlank()) title else "Video Ready"
                Toast.makeText(context, "▶ Ready to Download: $displayTitle", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JavascriptInterface
    fun downloadCurrentVideo() {
        download(null, null, asVideo = true)
    }

    @JavascriptInterface
    fun downloadCurrentAudio() {
        download(null, null, asVideo = false)
    }

    fun download(videoId: String?, title: String?, asVideo: Boolean) {
        val targetId = (videoId ?: currentVideoId).trim()
        val targetTitle = (title ?: currentTitle).trim()

        if (targetId.isBlank()) {
            coroutineScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "⚠️ Please click and play a video first to download",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        val video = TvVideoItem(
            id = targetId,
            title = if (targetTitle.isNotBlank()) targetTitle else "YouTube Media ($targetId)",
            channelTitle = "YouTube TV",
            thumbnailUrl = "https://img.youtube.com/vi/$targetId/hqdefault.jpg"
        )
        val formatName = if (asVideo) "4K/HD Video" else "MP3 Audio"

        coroutineScope.launch(Dispatchers.Main) {
            Toast.makeText(
                context,
                "⬇ Downloading $formatName: ${video.title}...",
                Toast.LENGTH_LONG
            ).show()
        }

        coroutineScope.launch(Dispatchers.IO) {
            downloadManager.downloadMedia(
                video = video,
                downloadAsVideo = asVideo,
                onComplete = { ok, path ->
                    coroutineScope.launch(Dispatchers.Main) {
                        val msg = if (ok) {
                            "✓ Downloaded $formatName to TV storage!"
                        } else {
                            "❌ Download failed for $targetId"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}

