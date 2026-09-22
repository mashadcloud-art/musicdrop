package com.musicdrop.tv.data.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * YouTube Music search, home shelves, filter categories, and Next Radio Queue
 * via official music.youtube.com InnerTube endpoints.
 */
object YouTubeMusicRepository {

    private const val SEARCH_ENDPOINT = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
    private const val NEXT_ENDPOINT = "https://music.youtube.com/youtubei/v1/next?prettyPrint=false"
    private const val BROWSE_ENDPOINT = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
    private const val API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

    enum class SearchFilter(val label: String, val param: String) {
        ALL("All", "Eg-KAQwIARAAGAAgACgAMABqChAEEAUQAxAKEAk="),
        SONGS("Songs", "EgWKAQIIAWoKEAkQAxAEEAUQEQ=="),
        ARTISTS("Artists", "EgWKAQIgAWoKEAkQAxAEEAUQEQ=="),
        ALBUMS("Albums", "EgWKAQIYAWoKEAkQAxAEEAUQEQ=="),
        PLAYLISTS("Playlists", "EgWKAQIoAWoKEAkQAxAEEAUQEQ=="),
        VIDEOS("Videos", "EgWKAQIQAWoKEAkQAxAEEAUQEQ==")
    }

    /** Search YouTube Music with optional category filter & pagination. */
    suspend fun search(
        query: String,
        ctoken: String? = null,
        filter: SearchFilter = SearchFilter.ALL
    ): YouTubeMusicPage = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext YouTubeMusicPage(emptyList(), null)

        var connection: HttpURLConnection? = null
        try {
            val urlStr = buildString {
                append(SEARCH_ENDPOINT)
                append("&key=$API_KEY")
                if (ctoken != null) {
                    append("&ctoken=${URLEncoder.encode(ctoken, "UTF-8")}")
                    append("&continuation=${URLEncoder.encode(ctoken, "UTF-8")}")
                    append("&type=next")
                }
            }

            connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                setRequestProperty("Origin", "https://music.youtube.com")
                setRequestProperty("Referer", "https://music.youtube.com/")
                setRequestProperty("X-YouTube-Client-Name", "67")
                setRequestProperty("X-YouTube-Client-Version", "1.20250317.01.00")
            }

            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("hl", "en")
                        put("gl", "US")
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20250317.01.00")
                        put("platform", "DESKTOP")
                    })
                })
                put("query", query)
                if (ctoken == null) {
                    put("params", filter.param)
                } else {
                    put("ctoken", ctoken)
                }
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use {
                it.write(body.toString())
                it.flush()
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.use { s ->
                BufferedReader(InputStreamReader(s, "UTF-8")).readText()
            }.orEmpty()

            if (status !in 200..299) {
                return@withContext YouTubeMusicPage(emptyList(), null)
            }

            parseResponse(JSONObject(responseText), isContinuation = ctoken != null)
        } catch (e: Exception) {
            e.printStackTrace()
            YouTubeMusicPage(emptyList(), null)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Fetch YouTube Music's live Up-Next / Radio recommendations for a song.
     * This powers continuous endless auto-play and the UP NEXT queue drawer.
     */
    suspend fun getUpNextRadioQueue(videoId: String, fallbackQuery: String = ""): List<YouTubeSearchResult> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext emptyList()

        val rawQueue = fetchNextApiQueue(videoId)
        var results = rawQueue.filter { it.videoId != videoId }

        if (results.size < 6 && fallbackQuery.isNotBlank()) {
            val queryToUse = if (fallbackQuery.contains("song", ignoreCase = true)) fallbackQuery else "$fallbackQuery songs"
            val searchHits = search(queryToUse).results.filter { it.videoId != videoId }
            results = (results + searchHits).distinctBy { it.videoId }
        }
        results
    }

    private fun fetchNextApiQueue(videoId: String): List<YouTubeSearchResult> {
        var connection: HttpURLConnection? = null
        return try {
            val urlStr = "$NEXT_ENDPOINT&key=$API_KEY"
            connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 12_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                setRequestProperty("Origin", "https://music.youtube.com")
                setRequestProperty("Referer", "https://music.youtube.com/")
                setRequestProperty("X-YouTube-Client-Name", "67")
                setRequestProperty("X-YouTube-Client-Version", "1.20250317.01.00")
            }

            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("hl", "en")
                        put("gl", "US")
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20250317.01.00")
                        put("platform", "DESKTOP")
                    })
                })
                put("videoId", videoId)
                put("isAudioOnly", true)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use {
                it.write(body.toString())
                it.flush()
            }

            if (connection.responseCode !in 200..299) return emptyList()

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            parseNextQueue(json)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }


    private fun parseNextQueue(json: JSONObject): List<YouTubeSearchResult> {
        val list = mutableListOf<YouTubeSearchResult>()
        try {
            val tab = json.optJSONObject("contents")
                ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                ?.optJSONObject("tabbedRenderer")
                ?.optJSONObject("watchNextTabbedResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicQueueRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("playlistPanelRenderer")

            val items = tab?.optJSONArray("contents") ?: return emptyList()

            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i)?.optJSONObject("playlistPanelVideoRenderer") ?: continue
                val vId = item.optString("videoId").takeIf { it.isNotBlank() } ?: continue
                val title = item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
                val artist = item.optJSONObject("longBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
                val duration = item.optJSONObject("lengthText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
                val thumb = "https://i.ytimg.com/vi_webp/$vId/hqdefault.webp"

                if (title.isNotBlank()) {
                    list.add(
                        YouTubeSearchResult(
                            videoId = vId,
                            title = title,
                            channelTitle = artist,
                            thumbnailUrl = thumb,
                            duration = duration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /** Fetch a default set of trending/popular music for the empty-state gallery. */
    suspend fun getTrending(): YouTubeMusicPage {
        val trendingItems = com.musicdrop.tv.data.repository.YtMusicApiRepository.getTrending("IN")
        if (trendingItems.isNotEmpty()) {
            return YouTubeMusicPage(results = trendingItems, nextCtoken = null)
        }
        return search("top hits trending songs 2026")
    }

    // ── Response parsing ────────────────────────────────────────────────────

    private fun parseResponse(json: JSONObject, isContinuation: Boolean): YouTubeMusicPage {
        val results = mutableListOf<YouTubeSearchResult>()
        var nextCtoken: String? = null

        if (isContinuation) {
            val shelf = json.optJSONObject("continuationContents")?.optJSONObject("musicShelfContinuation")
            val items = shelf?.optJSONArray("contents")
            if (items != null) {
                for (i in 0 until items.length()) {
                    val renderer = items.optJSONObject(i)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                    parseItem(renderer)?.let { results.add(it) }
                }
            }
            nextCtoken = shelf?.optJSONArray("continuations")
                ?.optJSONObject(0)
                ?.optJSONObject("nextContinuationData")
                ?.optString("continuation")
                ?.takeIf { it.isNotBlank() }
        } else {
            val tabs = json.optJSONObject("contents")
                ?.optJSONObject("tabbedSearchResultsRenderer")
                ?.optJSONArray("tabs")
            val contents = tabs?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")

            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val section = contents.optJSONObject(i) ?: continue

                    // 1. Regular musicShelfRenderer (Songs, Videos, Filtered results)
                    val shelf = section.optJSONObject("musicShelfRenderer")
                    if (shelf != null) {
                        val items = shelf.optJSONArray("contents")
                        if (items != null) {
                            for (j in 0 until items.length()) {
                                val renderer = items.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                                parseItem(renderer)?.let { results.add(it) }
                            }
                        }
                        if (nextCtoken == null) {
                            nextCtoken = shelf.optJSONArray("continuations")
                                ?.optJSONObject(0)
                                ?.optJSONObject("nextContinuationData")
                                ?.optString("continuation")
                                ?.takeIf { it.isNotBlank() }
                        }
                    }

                    // 2. Card shelf renderer (Top Result / Artist / Album hero card)
                    val cardShelf = section.optJSONObject("musicCardShelfRenderer")
                    if (cardShelf != null) {
                        val title = cardShelf.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
                        val subtitle = cardShelf.optJSONObject("subtitle")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
                        val thumb = cardShelf.optJSONObject("thumbnail")
                            ?.optJSONObject("musicThumbnailRenderer")
                            ?.optJSONObject("thumbnail")
                            ?.optJSONArray("thumbnails")
                            ?.let { thumbs -> thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") }
                            .orEmpty()
                        val playEndpoint = cardShelf.optJSONObject("onTap")?.optJSONObject("watchEndpoint")
                            ?: cardShelf.optJSONObject("buttons")?.optJSONArray("runs")?.optJSONObject(0)
                                ?.optJSONObject("buttonRenderer")?.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
                        val vId = playEndpoint?.optString("videoId").takeIf { !it.isNullOrBlank() }

                        if (vId != null && title.isNotBlank()) {
                            results.add(
                                YouTubeSearchResult(
                                    videoId = vId,
                                    title = title,
                                    channelTitle = subtitle,
                                    thumbnailUrl = thumb.ifBlank { "https://i.ytimg.com/vi_webp/$vId/hqdefault.webp" }
                                )
                            )
                        }
                    }
                }
            }
        }

        return YouTubeMusicPage(results.distinctBy { it.videoId }, nextCtoken)
    }

    private fun parseItem(renderer: JSONObject): YouTubeSearchResult? {
        val videoId = renderer.optJSONObject("playlistItemData")
            ?.optString("videoId")
            ?.takeIf { it.isNotBlank() }
            ?: renderer.optJSONObject("doubleTapCommand")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
            ?: return null

        val cols = renderer.optJSONArray("flexColumns") ?: return null

        val title = cols.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            .orEmpty()

        val artist = cols.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            .orEmpty()

        val col1Runs = cols.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
        val duration = if (col1Runs != null && col1Runs.length() > 0) {
            col1Runs.optJSONObject(col1Runs.length() - 1)?.optString("text").orEmpty()
        } else ""

        val views = cols.optJSONObject(2)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            .orEmpty()

        val thumbUrl = "https://i.ytimg.com/vi_webp/$videoId/hqdefault.webp"

        if (title.isBlank()) return null
        if (isLongVideoOrJukebox(title, duration)) return null

        return YouTubeSearchResult(
            videoId = videoId,
            title = title,
            channelTitle = artist.ifBlank { views },
            thumbnailUrl = thumbUrl,
            duration = duration
        )
    }

    fun isLongVideoOrJukebox(title: String, duration: String): Boolean {
        val lower = title.lowercase()
        val blacklistedKeywords = listOf(
            "full album", "jukebox", "1 hour", "2 hour", "3 hour", "4 hour", "5 hour", "6 hour", "7 hour", "8 hour", "9 hour", "10 hour", "12 hour",
            "1 hr", "2 hr", "3 hr", "4 hr", "5 hr", "1hr", "2hr", "3hr", "4hr", "5hr",
            "podcast", "audiobook", "all songs nonstop", "mega mix", "megamix",
            "mashup non stop", "non stop songs", "nonstop", "non-stop", "full movie",
            "compilation", "greatest hits full", "discography", "soundtrack full",
            "24/7", "live stream", "radio stream"
        )
        if (blacklistedKeywords.any { lower.contains(it) }) {
            return true
        }
        if (duration.isNotBlank()) {
            val cleanDur = duration.trim()
            val parts = cleanDur.split(":").mapNotNull { it.trim().toIntOrNull() }
            if (parts.size >= 3) {
                // H:MM:SS format -> Any video with hours (1 hr+) or > 10 minutes
                val totalSeconds = parts[0] * 3600 + parts[1] * 60 + parts[2]
                if (totalSeconds > 600) return true
            } else if (parts.size == 2) {
                // MM:SS format -> If > 10 minutes (600 seconds)
                val totalSeconds = parts[0] * 60 + parts[1]
                if (totalSeconds > 600) return true
            }
        }
        return false
    }
}

/** One page of YouTube Music results with an optional continuation token. */
data class YouTubeMusicPage(
    val results: List<YouTubeSearchResult>,
    val nextCtoken: String?
)
