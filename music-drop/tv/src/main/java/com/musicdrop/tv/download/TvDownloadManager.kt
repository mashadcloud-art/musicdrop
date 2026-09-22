package com.musicdrop.tv.download

import android.content.Context
import android.os.Environment
import com.musicdrop.tv.data.TvDownloadedMedia
import com.musicdrop.tv.data.TvVideoItem
import com.musicdrop.tv.data.TvYtExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class TvDownloadManager private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("tv_downloads_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _downloads = MutableStateFlow<List<TvDownloadedMedia>>(emptyList())
    val downloads: StateFlow<List<TvDownloadedMedia>> = _downloads.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    init {
        loadSavedDownloads()
    }

    private fun loadSavedDownloads() {
        val raw = prefs.getString("downloads_list", "[]") ?: "[]"
        val list = mutableListOf<TvDownloadedMedia>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val path = obj.getString("filePath")
                val f = File(path)
                if (f.exists() && f.length() > 0) {
                    list.add(
                        TvDownloadedMedia(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            channelTitle = obj.getString("channelTitle"),
                            filePath = path,
                            thumbnailUrl = obj.getString("thumbnailUrl"),
                            isVideo = obj.optBoolean("isVideo", true),
                            sizeBytes = f.length(),
                            durationMs = obj.optLong("durationMs", 0L),
                            dateAddedMs = obj.optLong("dateAddedMs", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (_: Throwable) {}
        _downloads.value = list.sortedByDescending { it.dateAddedMs }
    }

    private fun saveDownloads() {
        val arr = JSONArray()
        for (item in _downloads.value) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("channelTitle", item.channelTitle)
                put("filePath", item.filePath)
                put("thumbnailUrl", item.thumbnailUrl)
                put("isVideo", item.isVideo)
                put("sizeBytes", item.sizeBytes)
                put("durationMs", item.durationMs)
                put("dateAddedMs", item.dateAddedMs)
            }
            arr.put(obj)
        }
        prefs.edit().putString("downloads_list", arr.toString()).apply()
    }

    suspend fun downloadMedia(
        video: TvVideoItem,
        downloadAsVideo: Boolean = true,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val videoId = video.id
        try {
            _downloadProgress.value = _downloadProgress.value + (videoId to 0.05f)

            // 1. Extract stream URL
            val media = TvYtExtractor.getInstance().extractMedia(videoId)
            if (media == null) {
                _downloadProgress.value = _downloadProgress.value - videoId
                onComplete(false, "Extraction failed")
                return@withContext
            }

            val targetUrl = if (downloadAsVideo) (media.videoUrl ?: media.audioUrl) else (media.audioUrl ?: media.videoUrl)
            if (targetUrl == null) {
                _downloadProgress.value = _downloadProgress.value - videoId
                onComplete(false, "No stream URL found")
                return@withContext
            }

            val extension = if (downloadAsVideo) "mp4" else "m4a"
            val sanitizedTitle = video.title.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(50)
            val dir = if (downloadAsVideo) {
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            } else {
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            }
            if (!dir.exists()) dir.mkdirs()

            val targetFile = File(dir, "${sanitizedTitle}_${videoId}.$extension")

            // 2. Download with OkHttp stream
            val request = Request.Builder().url(targetUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    _downloadProgress.value = _downloadProgress.value - videoId
                    onComplete(false, "HTTP ${response.code}")
                    return@withContext
                }

                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                        _downloadProgress.value = _downloadProgress.value + (videoId to progress)
                    }
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()
            }

            // 3. Register downloaded media
            val item = TvDownloadedMedia(
                id = videoId,
                title = video.title,
                channelTitle = video.channelTitle,
                filePath = targetFile.absolutePath,
                thumbnailUrl = video.thumbnailUrl,
                isVideo = downloadAsVideo,
                sizeBytes = targetFile.length(),
                durationMs = media.durationSec * 1000L,
                dateAddedMs = System.currentTimeMillis()
            )

            _downloads.value = (_downloads.value.filter { it.id != videoId } + item).sortedByDescending { it.dateAddedMs }
            saveDownloads()
            _downloadProgress.value = _downloadProgress.value - videoId
            onComplete(true, targetFile.absolutePath)
        } catch (e: Throwable) {
            _downloadProgress.value = _downloadProgress.value - videoId
            android.util.Log.e("TvDownloader", "Download failed for $videoId: ${e.message}", e)
            onComplete(false, e.message ?: "Download failed")
        }
    }

    fun deleteDownload(id: String) {
        val item = _downloads.value.firstOrNull { it.id == id } ?: return
        try {
            val f = File(item.filePath)
            if (f.exists()) f.delete()
        } catch (_: Throwable) {}
        _downloads.value = _downloads.value.filter { it.id != id }
        saveDownloads()
    }

    companion object {
        private var instance: TvDownloadManager? = null

        fun getInstance(context: Context): TvDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: TvDownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
