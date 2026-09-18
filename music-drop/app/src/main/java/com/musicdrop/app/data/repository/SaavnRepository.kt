package com.musicdrop.app.data.repository

import android.net.Uri
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Fetches songs from JioSaavn via our Fly.io proxy hosted in Singapore.
 * The proxy is unblocked in UAE and relays requests to Saavn on our behalf.
 */
object SaavnRepository {

    private const val PROXY_BASE = "https://filedrop-saavn.fly.dev"

    // ─── Search songs ───────────────────────────────────────────────────────
    suspend fun search(query: String, limit: Int = 20): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "$PROXY_BASE/search?q=$encoded&limit=$limit"
            val json = URL(url).readText()
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("songs")
            (0 until arr.length()).mapNotNull { i ->
                parseSong(arr.getJSONObject(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ─── Get trending songs ─────────────────────────────────────────────────
    suspend fun getTrending(): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val json = URL("$PROXY_BASE/trending").readText()
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("songs")
            (0 until arr.length()).mapNotNull { i ->
                parseSong(arr.getJSONObject(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ─── Get song by ID (with stream URL) ───────────────────────────────────
    suspend fun getSongById(id: String): MediaItem? = withContext(Dispatchers.IO) {
        try {
            val json = URL("$PROXY_BASE/song/$id").readText()
            parseSong(JSONObject(json))
        } catch (e: Exception) {
            null
        }
    }

    // ─── Parse a JSON song object into a MediaItem ──────────────────────────
    private fun parseSong(obj: JSONObject): MediaItem? {
        return try {
            val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return null
            val mediaUrl = obj.optString("mediaUrl").takeIf { it.isNotBlank() } ?: return null
            val title = obj.optString("title", "Unknown Song")
            val artist = obj.optString("artist", "Unknown Artist")
            val album = obj.optString("album", "")
            val image = obj.optString("image", "")
            val durationSec = obj.optInt("duration", 0)

            MediaItem(
                id = id.hashCode().toLong(),
                uri = Uri.parse(mediaUrl),       // ExoPlayer plays this directly
                name = title,
                size = 0L,
                dateAdded = System.currentTimeMillis() / 1000,
                mimeType = "audio/mpeg",
                mediaType = MediaType.AUDIO,
                durationMs = durationSec * 1000L,
                artist = artist,
                album = album.ifBlank { "JioSaavn" },
                isSong = true,
                filePath = mediaUrl,             // used as stream URL by player
                bucketName = "JioSaavn Online",  // shows 🌐 Live 320kbps badge
                albumArtUri = image.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            )
        } catch (e: Exception) {
            null
        }
    }
}
