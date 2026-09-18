package com.musicdrop.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fetches synchronized (LRC) and plain lyrics from LRCLIB — a free, open-source
 * lyrics engine with no API key required.
 *
 * Docs: https://lrclib.net/docs
 */
object LrcLibRepository {

    private const val BASE = "https://lrclib.net/api"

    data class LrcLibResult(
        val trackName: String,
        val artistName: String,
        val albumName: String,
        val durationSec: Int,
        /** Synchronized LRC lyrics with timestamps e.g. "[00:12.50] Some line" */
        val syncedLyrics: String?,
        /** Plain text lyrics without timestamps */
        val plainLyrics: String?
    ) {
        /** Returns synced lyrics if available, falls back to plain. */
        val bestLyrics: String? get() = syncedLyrics?.takeIf { it.isNotBlank() } ?: plainLyrics

        /** Parses syncedLyrics into a list of (timestampMs, line) pairs for karaoke display. */
        fun parsedSyncedLines(): List<Pair<Long, String>> {
            val raw = syncedLyrics?.takeIf { it.isNotBlank() } ?: return emptyList()
            val regex = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})\]\s*(.*)$""")
            return raw.lines().mapNotNull { line ->
                val match = regex.matchEntire(line.trim()) ?: return@mapNotNull null
                val (min, sec, cs, text) = match.destructured
                val ms = min.toLong() * 60_000 + sec.toLong() * 1_000 +
                    if (cs.length == 2) cs.toLong() * 10 else cs.toLong()
                ms to text
            }.sortedBy { it.first }
        }
    }

    /**
     * Search for lyrics by track name + artist. Returns the best match or null.
     * Pass [durationSec] for a more precise match (optional, 0 = skip).
     */
    suspend fun search(
        trackName: String,
        artistName: String,
        albumName: String = "",
        durationSec: Int = 0
    ): LrcLibResult? = withContext(Dispatchers.IO) {
        if (trackName.isBlank()) return@withContext null
        var conn: HttpURLConnection? = null
        try {
            val q = buildString {
                append("$BASE/search?")
                append("track_name=${URLEncoder.encode(trackName, "UTF-8")}")
                if (artistName.isNotBlank())
                    append("&artist_name=${URLEncoder.encode(artistName, "UTF-8")}")
                if (albumName.isNotBlank())
                    append("&album_name=${URLEncoder.encode(albumName, "UTF-8")}")
            }
            conn = (URL(q).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "FileDrop/2.6 (Android)")
            }
            val status = conn.responseCode
            if (status !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader().readText()
            val arr = JSONArray(body)
            if (arr.length() == 0) return@withContext null

            // Pick best match: prefer exact duration match if provided
            val candidates = (0 until arr.length()).map { arr.getJSONObject(it) }
            val best = if (durationSec > 0) {
                candidates.minByOrNull { obj ->
                    kotlin.math.abs(obj.optInt("duration", 0) - durationSec)
                }
            } else candidates.firstOrNull()

            best?.toResult()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Direct lookup by LRCLIB track id (fastest, used for caching).
     */
    suspend fun getById(id: Int): LrcLibResult? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL("$BASE/get/$id").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "FileDrop/2.6 (Android)")
            }
            if (conn.responseCode !in 200..299) return@withContext null
            conn.inputStream.bufferedReader().readText()
                .let { JSONObject(it).toResult() }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun JSONObject.toResult() = LrcLibResult(
        trackName  = optString("trackName", ""),
        artistName = optString("artistName", ""),
        albumName  = optString("albumName", ""),
        durationSec = optInt("duration", 0),
        syncedLyrics = optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" },
        plainLyrics  = optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
    )
}
