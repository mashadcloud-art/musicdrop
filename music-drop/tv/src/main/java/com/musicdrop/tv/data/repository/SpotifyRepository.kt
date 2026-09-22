package com.musicdrop.tv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Real Spotify metadata fetching — ported directly from the reference MusiX app's
 * decompiled implementation (`com.musixmusicx.utils.update.b`, verified against the
 * actual smali: anonymous token fetch, GraphQL persisted-query bodies + sha256 hashes,
 * and the exact JSON field paths Spotify's own web client uses).
 *
 * Spotify's actual audio is DRM-protected (Widevine) and is NOT obtainable this way —
 * the reference app's own strings.xml says so too: "We do not download songs directly
 * from Spotify, AppleMusic, etc. due to copyright issues. Some songs may not be correct
 * because we could only match them by title and artist." What this DOES get, for real:
 * a track/album/playlist's title, artist, cover art and duration straight from
 * Spotify's own internal API — the caller then matches that to a playable YouTube/
 * JioSaavn result by title+artist search.
 */
object SpotifyRepository {

    data class SpotifyItem(
        val title: String,
        val artist: String,
        val coverUrl: String,
        val durationMs: Long
    )

    private const val GRAPHQL_HOST = "https://api-partner.spotify.com/pathfinder/v2/query"
    private const val TOKEN_URL = "https://open.spotify.com/get_access_token"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private const val HASH_TRACK = "612585ae06ba435ad26369870deaae23b5c8800a256cd8a57e08eddc25a37294"
    private const val HASH_ALBUM = "b9bfabef66ed756e5e13f68a942deb60bd4125ec1f1be8cc42769dc0259b4b10"
    private const val HASH_PLAYLIST = "7982b11e21535cd2594badc40030b745671b61a1fa66766e569d45e6364f3422"

    private var cachedToken: String? = null
    private var cachedTokenExpiryMs: Long = 0L

    enum class LinkType { TRACK, ALBUM, PLAYLIST }

    /** Resolves any open.spotify.com track/album/playlist link into playable-metadata items. */
    suspend fun resolveLink(url: String): List<SpotifyItem>? = withContext(Dispatchers.IO) {
        val (type, id) = parseSpotifyUrl(url) ?: return@withContext null
        if (id.isBlank()) return@withContext null
        val token = getAccessToken() ?: return@withContext null

        try {
            when (type) {
                LinkType.TRACK -> getTrack(token, id)?.let { listOf(it) }
                LinkType.ALBUM -> getAlbum(token, id)
                LinkType.PLAYLIST -> getPlaylist(token, id)
            }
        } catch (e: Exception) {
            android.util.Log.e("SpotifyRepo", "resolveLink failed: ${e.message}")
            null
        }
    }

    /**
     * Same substring approach the reference app uses (getSpotifyId): find "/track/",
     * "/playlist/" or "/album/" anywhere in the URL (works for regional-prefixed links
     * like open.spotify.com/intl-en/track/...), take what follows, strip a "?..." query.
     */
    private fun parseSpotifyUrl(url: String): Pair<LinkType, String>? {
        val markers = listOf("/track/" to LinkType.TRACK, "/playlist/" to LinkType.PLAYLIST, "/album/" to LinkType.ALBUM)
        for ((marker, type) in markers) {
            val idx = url.indexOf(marker)
            if (idx >= 0) {
                var id = url.substring(idx + marker.length)
                val qIdx = id.indexOf("?")
                if (qIdx >= 0) id = id.substring(0, qIdx)
                if (id.isNotBlank()) return type to id
            }
        }
        return null
    }

    // ── Anonymous access token ───────────────────────────────────────────────
    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val cached = cachedToken
        if (cached != null && System.currentTimeMillis() < cachedTokenExpiryMs - 10_000) {
            return@withContext cached
        }
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(TOKEN_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val token = json.optString("accessToken").ifBlank { return@withContext null }
            val expiry = json.optLong("accessTokenExpirationTimestampMs", System.currentTimeMillis() + 60_000)
            cachedToken = token
            cachedTokenExpiryMs = expiry
            token
        } catch (e: Exception) {
            android.util.Log.e("SpotifyRepo", "getAccessToken failed: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun graphqlQuery(token: String, operationName: String, hash: String, body: String): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(GRAPHQL_HOST).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) {
                android.util.Log.w("SpotifyRepo", "$operationName HTTP ${conn.responseCode}")
                return null
            }
            JSONObject(conn.inputStream.bufferedReader().readText())
        } catch (e: Exception) {
            android.util.Log.e("SpotifyRepo", "$operationName failed: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    // ── Track ────────────────────────────────────────────────────────────────
    private fun getTrack(token: String, trackId: String): SpotifyItem? {
        val body = """{"variables":{"uri":"spotify:track:$trackId"},"operationName":"getTrack","extensions":{"persistedQuery":{"version":1,"sha256Hash":"$HASH_TRACK"}}}"""
        val root = graphqlQuery(token, "getTrack", HASH_TRACK, body) ?: return null
        val trackUnion = root.optJSONObject("data")?.optJSONObject("trackUnion") ?: return null
        return trackUnion.toSpotifyItem()
    }

    /** Parses the `trackUnion`/playlist-item "data" shape shared by track/playlist entries. */
    private fun JSONObject.toSpotifyItem(): SpotifyItem? {
        val name = optString("name").ifBlank { return null }
        val durationMs = optJSONObject("duration")?.optLong("totalMilliseconds")
            ?: optJSONObject("trackDuration")?.optLong("totalMilliseconds") ?: 0L
        val artist = optJSONObject("firstArtist")
            ?.optJSONArray("items")?.optJSONObject(0)?.optJSONObject("profile")?.optString("name")
            ?: optJSONObject("artists")
                ?.optJSONArray("items")?.optJSONObject(0)?.optJSONObject("profile")?.optString("name")
            ?: ""
        val cover = optJSONObject("albumOfTrack")
            ?.optJSONObject("coverArt")
            ?.optJSONArray("sources")?.optJSONObject(0)?.optString("url")
            ?: ""
        return SpotifyItem(title = name, artist = artist, coverUrl = cover, durationMs = durationMs)
    }

    // ── Album ────────────────────────────────────────────────────────────────
    private fun getAlbum(token: String, albumId: String): List<SpotifyItem>? {
        val body = """{"variables":{"uri":"spotify:album:$albumId","locale":"","offset":0,"limit":50},"operationName":"getAlbum","extensions":{"persistedQuery":{"version":1,"sha256Hash":"$HASH_ALBUM"}}}"""
        val root = graphqlQuery(token, "getAlbum", HASH_ALBUM, body) ?: return null
        val albumUnion = root.optJSONObject("data")?.optJSONObject("albumUnion") ?: return null
        // Individual track entries under an album don't carry their own cover — the
        // whole album shares one, exactly like the reference app's own album parser.
        val albumCover = albumUnion.optJSONObject("coverArt")
            ?.optJSONArray("sources")?.optJSONObject(0)?.optString("url") ?: ""
        val items = albumUnion.optJSONObject("tracksV2")?.optJSONArray("items") ?: return null
        val results = mutableListOf<SpotifyItem>()
        for (i in 0 until items.length()) {
            val track = items.optJSONObject(i)?.optJSONObject("track") ?: continue
            val parsed = track.toSpotifyItem() ?: continue
            results.add(if (parsed.coverUrl.isBlank()) parsed.copy(coverUrl = albumCover) else parsed)
        }
        return results
    }

    // ── Playlist ─────────────────────────────────────────────────────────────
    private fun getPlaylist(token: String, playlistId: String): List<SpotifyItem>? {
        val body = """{"variables":{"uri":"spotify:playlist:$playlistId"},"operationName":"fetchPlaylistContents","extensions":{"persistedQuery":{"version":1,"sha256Hash":"$HASH_PLAYLIST"}}}"""
        val root = graphqlQuery(token, "fetchPlaylistContents", HASH_PLAYLIST, body) ?: return null
        val items = root.optJSONObject("data")?.optJSONObject("playlistV2")
            ?.optJSONObject("content")?.optJSONArray("items") ?: return null

        val results = mutableListOf<SpotifyItem>()
        for (i in 0 until items.length()) {
            val entry = items.optJSONObject(i) ?: continue
            // Playlist entries are wrapped in "itemV3" (current schema) or "itemV2"
            // (older) — try both, same fallback order the reference app uses.
            val wrapped = entry.optJSONObject("itemV3") ?: entry.optJSONObject("itemV2") ?: continue
            val data = wrapped.optJSONObject("data") ?: continue
            data.toSpotifyItem()?.let { results.add(it) }
        }
        return results
    }
}
