package com.musicdrop.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.model.MediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class DownloadedTrack(
    val key: String,
    val title: String,
    val artist: String,
    val duration: String,
    val coverUrl: String,
    val filePath: String,
    val mimeType: String,
    val downloadedAtMs: Long
) {
    fun toMediaItem(): MediaItem {
        val file = File(filePath)
        val isVideo = mimeType.startsWith("video") || filePath.endsWith(".mp4", ignoreCase = true) || key.startsWith("yt_video:")
        val isVoiceNote = !isVideo && (
            title.matches(Regex("""^20\d{6}_\d{6}.*""")) ||
            title.startsWith("PTT-", ignoreCase = true) ||
            title.startsWith("AUD-", ignoreCase = true) ||
            title.startsWith("REC_", ignoreCase = true) ||
            title.startsWith("Record", ignoreCase = true) ||
            title.startsWith("Voice", ignoreCase = true) ||
            title.startsWith("Call", ignoreCase = true) ||
            (file.exists() && file.length() < 150_000L && !filePath.contains("MusicDrop", ignoreCase = true))
        )
        val effectiveUri = when {
            filePath.startsWith("content://") -> android.net.Uri.parse(filePath)
            file.exists() -> android.net.Uri.fromFile(file)
            filePath.isNotBlank() -> android.net.Uri.parse(filePath)
            else -> android.net.Uri.EMPTY
        }
        return MediaItem(
            id = key.hashCode().toLong(),
            uri = effectiveUri,
            name = title,
            size = if (file.exists()) file.length() else 0L,
            dateAdded = downloadedAtMs / 1000L,
            mimeType = mimeType,
            mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
            durationMs = 0L,
            artist = artist.ifBlank { "Offline Track" },
            album = "Downloads",
            isSong = !isVoiceNote,
            filePath = filePath,
            albumArtUri = if (coverUrl.isNotBlank()) android.net.Uri.parse(coverUrl) else null
        )
    }
}

object DownloadedTracksStore {
    private const val PREFS_NAME = "musicdrop_downloaded_tracks"
    private const val KEY_TRACKS = "tracks_json"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getAll(context: Context): List<DownloadedTrack> {
        val prefs = prefs(context)
        val raw = prefs.getString(KEY_TRACKS, null) ?: return emptyList()
        val list = mutableListOf<DownloadedTrack>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val filePath = obj.optString("filePath")
                val title = obj.optString("title", "")
                val isVoiceNote = title.matches(Regex("""^20\d{6}_\d{6}.*""")) ||
                    title.startsWith("PTT-", ignoreCase = true) ||
                    title.startsWith("AUD-", ignoreCase = true) ||
                    title.startsWith("REC_", ignoreCase = true) ||
                    title.startsWith("Record", ignoreCase = true) ||
                    title.startsWith("Voice", ignoreCase = true) ||
                    title.startsWith("Call", ignoreCase = true) ||
                    (File(filePath).exists() && File(filePath).length() < 150_000L && !filePath.contains("MusicDrop", ignoreCase = true) && !filePath.endsWith(".mp4", ignoreCase = true))

                if (isVoiceNote) continue

                if (filePath.isNotBlank() && File(filePath).exists()) {
                    list.add(
                        DownloadedTrack(
                            key = obj.optString("key"),
                            title = obj.optString("title", "Untitled Track"),
                            artist = obj.optString("artist", "Unknown Artist"),
                            duration = obj.optString("duration", ""),
                            coverUrl = obj.optString("coverUrl", ""),
                            filePath = filePath,
                            mimeType = obj.optString("mimeType", "audio/mp4"),
                            downloadedAtMs = obj.optLong("downloadedAtMs", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.downloadedAtMs }
    }

    @Synchronized
    fun add(context: Context, track: DownloadedTrack) {
        val current = getAll(context).filter { it.key != track.key }.toMutableList()
        current.add(0, track)
        saveAll(context, current)
    }

    @Synchronized
    fun remove(context: Context, key: String) {
        val current = getAll(context).filter { it.key != key }
        saveAll(context, current)
    }

    @Synchronized
    fun deleteFileAndRecord(context: Context, track: DownloadedTrack): Boolean {
        remove(context, track.key)
        val file = File(track.filePath)
        return if (file.exists()) file.delete() else true
    }

    @Synchronized
    fun updateTrack(
        context: Context,
        key: String,
        newTitle: String,
        newArtist: String,
        newCoverUrl: String? = null
    ) {
        val current = getAll(context).map { t ->
            if (t.key == key || t.filePath == key) {
                t.copy(
                    title = newTitle.ifBlank { t.title },
                    artist = newArtist.ifBlank { t.artist },
                    coverUrl = newCoverUrl ?: t.coverUrl
                )
            } else t
        }
        saveAll(context, current)
    }

    private fun saveAll(context: Context, tracks: List<DownloadedTrack>) {
        val array = JSONArray()
        for (t in tracks) {
            val obj = JSONObject().apply {
                put("key", t.key)
                put("title", t.title)
                put("artist", t.artist)
                put("duration", t.duration)
                put("coverUrl", t.coverUrl)
                put("filePath", t.filePath)
                put("mimeType", t.mimeType)
                put("downloadedAtMs", t.downloadedAtMs)
            }
            array.put(obj)
        }
        prefs(context).edit().putString(KEY_TRACKS, array.toString()).apply()
    }
}
