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
            val timestamp = System.currentTimeMillis()
            val url = "$PROXY_BASE/search?q=$encoded&limit=$limit&_t=$timestamp"
            val json = URL(url).readText()
            val obj = JSONObject(json)
            val arr = obj.optJSONArray("songs") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { i ->
                parseSong(arr.getJSONObject(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ─── Get trending songs (blends live 2026 hits & trending charts) ────────
    suspend fun getTrending(force: Boolean = false): List<MediaItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MediaItem>()

        // 1. Fetch live 2026 fresh releases & trending hits directly via search
        try {
            val freshHits = search("latest songs 2026", limit = 15)
            if (freshHits.isNotEmpty()) results.addAll(freshHits)
        } catch (_: Exception) {}

        // 2. Fetch /trending with cache buster
        try {
            val timestamp = System.currentTimeMillis()
            val json = URL("$PROXY_BASE/trending?_t=$timestamp").readText()
            val obj = JSONObject(json)
            val arr = obj.optJSONArray("songs")
            if (arr != null) {
                val trending = (0 until arr.length()).mapNotNull { i ->
                    parseSong(arr.getJSONObject(i))
                }
                results.addAll(trending)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Fallback to trending Bollywood & Punjabi hits if empty
        if (results.isEmpty()) {
            results.addAll(search("trending songs", limit = 20))
        }

        results.distinctBy { it.id }
    }

    // ─── Get new releases directly from JioSaavn ────────────────────────────
    suspend fun getNewReleases(): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        val queries = listOf("new releases 2026", "latest hindi songs 2026", "latest punjabi 2026")
        for (q in queries) {
            try {
                val songs = search(q, limit = 12)
                list.addAll(songs)
            } catch (_: Exception) {}
        }
        list.distinctBy { it.id }
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
