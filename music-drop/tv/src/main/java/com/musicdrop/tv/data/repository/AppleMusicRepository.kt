package com.musicdrop.tv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fetches track metadata and album art from the Apple Music / iTunes Search API.
 * Uses the public iTunes Search API — no API key required.
 *
 * Docs: https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/
 */
object AppleMusicRepository {

    private const val ITUNES_SEARCH = "https://itunes.apple.com/search"
    private const val ITUNES_LOOKUP = "https://itunes.apple.com/lookup"

    data class AppleTrack(
        val trackId: Long,
        val trackName: String,
        val artistName: String,
        val albumName: String,
        val artworkUrl: String,   // 100x100 — replace "100x100" with "600x600" for HD
        val previewUrl: String,   // 30-second AAC preview clip
        val releaseDate: String,
        val trackTimeMs: Long,
        val genre: String,
        val country: String
    ) {
        /** High-resolution artwork (600x600). */
        val hdArtwork: String get() =
            artworkUrl.replace("100x100bb", "600x600bb")
    }

    /**
     * Search for tracks by name + optional artist.
     * Returns up to [limit] results (max 200).
     */
    suspend fun searchTracks(
        query: String,
        artist: String = "",
        limit: Int = 20
    ): List<AppleTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        var conn: HttpURLConnection? = null
        try {
            val term = URLEncoder.encode(
                if (artist.isNotBlank()) "$query $artist" else query,
                "UTF-8"
            )
            val url = "$ITUNES_SEARCH?term=$term&media=music&entity=song&limit=$limit&lang=en_us"
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "FileDrop/2.6 (Android)")
            }
            if (conn.responseCode !in 200..299) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return@withContext emptyList()
            (0 until results.length()).mapNotNull { i ->
                results.optJSONObject(i)?.toAppleTrack()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Look up an artist's top tracks by iTunes artist ID.
     */
    suspend fun getArtistTopTracks(artistId: Long, limit: Int = 10): List<AppleTrack> =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = "$ITUNES_LOOKUP?id=$artistId&entity=song&limit=$limit"
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 10_000
                    setRequestProperty("User-Agent", "FileDrop/2.6 (Android)")
                }
                if (conn.responseCode !in 200..299) return@withContext emptyList()
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val results = json.optJSONArray("results") ?: return@withContext emptyList()
                (0 until results.length()).mapNotNull { i ->
                    results.optJSONObject(i)
                        ?.takeIf { it.optString("wrapperType") == "track" }
                        ?.toAppleTrack()
                }
            } catch (e: Exception) {
                emptyList()
            } finally {
                conn?.disconnect()
            }
        }

    /**
     * Fetch HD artwork URL for a track (artist + track name match).
     * Returns null if not found.
     */
    suspend fun getArtwork(trackName: String, artistName: String): String? {
        val results = searchTracks(trackName, artistName, limit = 1)
        return results.firstOrNull()?.hdArtwork
    }

    private fun JSONObject.toAppleTrack(): AppleTrack? {
        val trackId = optLong("trackId").takeIf { it > 0 } ?: return null
        return AppleTrack(
            trackId     = trackId,
            trackName   = optString("trackName", ""),
            artistName  = optString("artistName", ""),
            albumName   = optString("collectionName", ""),
            artworkUrl  = optString("artworkUrl100", ""),
            previewUrl  = optString("previewUrl", ""),
            releaseDate = optString("releaseDate", "").take(10),
            trackTimeMs = optLong("trackTimeMillis", 0L),
            genre       = optString("primaryGenreName", ""),
            country     = optString("country", "")
        )
    }
}
