package com.musicdrop.tv.data.repository

import com.musicdrop.tv.data.youtube.YouTubeSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * MusiX private backend — trending tags, feature flags, curated playlists and
 * app-update metadata from the app's own server infrastructure.
 *
 * Endpoints:
 *   api.mxf-95274725.com  — primary
 *   mp3-box.com           — secondary / CDN mirror
 */
object MusiXServerRepository {

    private val HOSTS = listOf(
        "https://api.mxf-95274725.com",
        "https://mp3-box.com/api"
    )

    // ── Response models ──────────────────────────────────────────────────────

    data class TrendingTag(
        val tag: String,
        val query: String,   // search query to use when the tag is tapped
        val count: Int       // approximate track count
    )

    data class FeatureFlags(
        val lyricsEnabled: Boolean    = true,
        val vimeoEnabled: Boolean     = true,
        val chartsEnabled: Boolean    = true,
        val appleMusicEnabled: Boolean = true,
        val popnableEnabled: Boolean  = true,
        val saavnEnabled: Boolean     = true,
        val minAppVersionCode: Int    = 0,
        val updateUrl: String         = ""
    )

    data class CuratedPlaylist(
        val id: String,
        val title: String,
        val description: String,
        val coverUrl: String,
        val tracks: List<YouTubeSearchResult>
    )

    // ── Public API ───────────────────────────────────────────────────────────

    /** Fetch trending tags / genre shortcuts from the MusiX backend. */
    suspend fun getTrendingTags(): List<TrendingTag> = withContext(Dispatchers.IO) {
        tryHosts { host ->
            val conn = openGet("$host/trending/tags")
            if (conn.responseCode !in 200..299) return@tryHosts null
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val arr = json.optJSONArray("tags") ?: return@tryHosts null
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                TrendingTag(
                    tag   = obj.optString("tag", ""),
                    query = obj.optString("query", ""),
                    count = obj.optInt("count", 0)
                )
            }
        } ?: defaultTags()
    }

    /** Fetch remote feature-flag configuration. */
    suspend fun getFeatureFlags(): FeatureFlags = withContext(Dispatchers.IO) {
        tryHosts { host ->
            val conn = openGet("$host/config/flags")
            if (conn.responseCode !in 200..299) return@tryHosts null
            val j = JSONObject(conn.inputStream.bufferedReader().readText())
            FeatureFlags(
                lyricsEnabled      = j.optBoolean("lyrics_enabled", true),
                vimeoEnabled       = j.optBoolean("vimeo_enabled", true),
                chartsEnabled      = j.optBoolean("charts_enabled", true),
                appleMusicEnabled  = j.optBoolean("apple_music_enabled", true),
                popnableEnabled    = j.optBoolean("popnable_enabled", true),
                saavnEnabled       = j.optBoolean("saavn_enabled", true),
                minAppVersionCode  = j.optInt("min_version_code", 0),
                updateUrl          = j.optString("update_url", "")
            )
        } ?: FeatureFlags()
    }

    /** Fetch a curated playlist by ID. */
    suspend fun getPlaylist(playlistId: String): CuratedPlaylist? = withContext(Dispatchers.IO) {
        tryHosts { host ->
            val conn = openGet("$host/playlists/${URLEncoder.encode(playlistId, "UTF-8")}")
            if (conn.responseCode !in 200..299) return@tryHosts null
            val j = JSONObject(conn.inputStream.bufferedReader().readText())
            parsePlaylist(j)
        }
    }

    /** Fetch all featured / curated playlists. */
    suspend fun getFeaturedPlaylists(): List<CuratedPlaylist> = withContext(Dispatchers.IO) {
        tryHosts { host ->
            val conn = openGet("$host/playlists/featured")
            if (conn.responseCode !in 200..299) return@tryHosts null
            val j = JSONObject(conn.inputStream.bufferedReader().readText())
            val arr = j.optJSONArray("playlists") ?: return@tryHosts null
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { parsePlaylist(it) }
            }
        } ?: emptyList()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun openGet(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "FileDrop/2.6 (Android)")
            setRequestProperty("Accept", "application/json")
        }

    /** Try each host in order, returning the first non-null result. */
    private inline fun <T> tryHosts(block: (host: String) -> T?): T? {
        for (host in HOSTS) {
            try {
                val result = block(host)
                if (result != null) return result
            } catch (_: Exception) {}
        }
        return null
    }

    private fun parsePlaylist(j: JSONObject): CuratedPlaylist? {
        val id = j.optString("id").takeIf { it.isNotBlank() } ?: return null
        val tracksArr = j.optJSONArray("tracks") ?: return null
        val tracks = (0 until tracksArr.length()).mapNotNull { i ->
            val t = tracksArr.optJSONObject(i) ?: return@mapNotNull null
            val vid = t.optString("videoId").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            YouTubeSearchResult(
                videoId      = vid,
                title        = t.optString("title", ""),
                channelTitle = t.optString("artist", ""),
                thumbnailUrl = t.optString("thumbnail",
                    "https://i.ytimg.com/vi_webp/$vid/default.webp"),
                duration     = t.optString("duration", "")
            )
        }
        return CuratedPlaylist(
            id          = id,
            title       = j.optString("title", "Playlist"),
            description = j.optString("description", ""),
            coverUrl    = j.optString("cover", ""),
            tracks      = tracks
        )
    }

    private fun defaultTags() = listOf(
        TrendingTag("🔥 Trending",  "trending music 2026", 0),
        TrendingTag("💿 Hindi Hits","hindi songs hits 2026", 0),
        TrendingTag("🎸 Pop",       "top pop songs 2026", 0),
        TrendingTag("🎤 Hip-Hop",   "hip hop rap 2026", 0),
        TrendingTag("🎻 Classical", "classical music best", 0),
        TrendingTag("🌙 Chill",     "chill lo-fi music", 0),
        TrendingTag("💪 Workout",   "workout gym music 2026", 0),
        TrendingTag("🕺 Party",     "party music hits 2026", 0)
    )
}
