package com.musicdrop.tv.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.musicdrop.tv.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves and caches high-quality cover images and logos for artists
 * (e.g. T-Series, Sony Music, Ritviz, Shubh, A.R. Rahman, etc.)
 * using Deezer & iTunes public APIs with fast local persistent caching.
 */
object ArtistCoverRepository {

    private const val PREFS_NAME = "artist_covers_cache"
    private val memoryCache = ConcurrentHashMap<String, String>()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs?.all?.forEach { (key, value) ->
                if (value is String && value.isNotBlank()) {
                    memoryCache[key] = value
                }
            }
        }
    }

    /**
     * Resolves the artist cover URL.
     * 1. Checks memory cache
     * 2. Checks SharedPreferences cache
     * 3. Checks sampleTrack albumArtUri if available
     * 4. Queries Deezer / iTunes public artist search
     */
    suspend fun getArtistCover(
        context: Context,
        artistName: String,
        sampleTrack: MediaItem? = null
    ): String? = withContext(Dispatchers.IO) {
        val cleanName = artistName.trim().removePrefix("<").removeSuffix(">").trim()
        if (cleanName.isBlank() || cleanName.equals("Unknown Artist", ignoreCase = true) || cleanName.equals("Unknown", ignoreCase = true)) {
            return@withContext null
        }

        init(context)

        // 1. Memory Cache
        memoryCache[cleanName.lowercase()]?.let { return@withContext it }

        // 2. Track Album Art Fallback (instant)
        val trackArt = sampleTrack?.albumArtUri?.toString()
        if (!trackArt.isNullOrBlank()) {
            memoryCache[cleanName.lowercase()] = trackArt
            prefs?.edit()?.putString(cleanName.lowercase(), trackArt)?.apply()
            return@withContext trackArt
        }

        // 3. Query Deezer Public Artist API (Returns authentic artist profile image / logo)
        try {
            val encoded = URLEncoder.encode(cleanName, "UTF-8")
            val deezerUrl = "https://api.deezer.com/search/artist?q=$encoded&limit=1"
            val conn = (URL(deezerUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; MusicDrop)")
            }

            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(jsonStr)
                val data = json.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val first = data.getJSONObject(0)
                    val pic = first.optString("picture_big", first.optString("picture_medium", ""))
                    if (pic.isNotBlank() && pic.startsWith("http")) {
                        memoryCache[cleanName.lowercase()] = pic
                        prefs?.edit()?.putString(cleanName.lowercase(), pic)?.apply()
                        return@withContext pic
                    }
                }
            } else {
                conn.disconnect()
            }
        } catch (_: Exception) {}

        // 4. Fallback to iTunes Search API
        try {
            val encoded = URLEncoder.encode(cleanName, "UTF-8")
            val itunesUrl = "https://itunes.apple.com/search?term=$encoded&entity=album&limit=1"
            val conn = (URL(itunesUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; MusicDrop)")
            }

            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(jsonStr)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val first = results.getJSONObject(0)
                    val art = first.optString("artworkUrl100", "")
                        .replace("100x100bb", "600x600bb")
                    if (art.isNotBlank() && art.startsWith("http")) {
                        memoryCache[cleanName.lowercase()] = art
                        prefs?.edit()?.putString(cleanName.lowercase(), art)?.apply()
                        return@withContext art
                    }
                }
            } else {
                conn.disconnect()
            }
        } catch (_: Exception) {}

        null
    }
}
