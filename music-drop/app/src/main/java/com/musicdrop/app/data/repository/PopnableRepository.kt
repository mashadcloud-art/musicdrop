package com.musicdrop.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fetches alternative cover artwork from Popnable.
 * URL pattern: https://popnable.com/images/songs/<artist>/<track>.jpg
 * Falls back to a search-scrape if the direct URL returns 404.
 */
object PopnableRepository {

    private const val BASE = "https://popnable.com/images/songs"

    /**
     * Returns a Popnable cover art URL for the given artist + track.
     * Does a HEAD request to verify existence; returns null if not found.
     */
    suspend fun getCoverUrl(artist: String, track: String): String? =
        withContext(Dispatchers.IO) {
            if (artist.isBlank() || track.isBlank()) return@withContext null
            val slug = { s: String ->
                s.lowercase()
                    .replace(Regex("[^a-z0-9]+"), "-")
                    .trim('-')
            }
            val url = "$BASE/${slug(artist)}/${slug(track)}.jpg"
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = 5_000
                    readTimeout = 5_000
                }
                if (conn.responseCode in 200..299) url else null
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Returns a list of candidate cover URLs to try in order (different slug variants).
     * Caller should use the first one that loads successfully in Coil.
     */
    fun candidateUrls(artist: String, track: String): List<String> {
        if (artist.isBlank() || track.isBlank()) return emptyList()
        val slug = { s: String ->
            s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        }
        // Strip feat./ft. suffixes for better matching
        val cleanTrack = track.replace(Regex("""(?i)\s*\(?feat\..*"""), "").trim()
        val cleanArtist = artist.split(",", "&", "feat.", "ft.").first().trim()
        return listOf(
            "$BASE/${slug(cleanArtist)}/${slug(cleanTrack)}.jpg",
            "$BASE/${slug(artist)}/${slug(track)}.jpg",
            "$BASE/${slug(cleanArtist)}/${slug(cleanTrack)}.webp"
        )
    }
}
