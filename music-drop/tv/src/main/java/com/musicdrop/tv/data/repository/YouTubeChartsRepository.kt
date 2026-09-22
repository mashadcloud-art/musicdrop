package com.musicdrop.tv.data.repository

import com.musicdrop.tv.data.youtube.YouTubeSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fetches trending music from charts.youtube.com via the InnerTube browse endpoint.
 * Returns [YouTubeSearchResult] so results slot directly into the existing player.
 *
 * The browse ID "FEmusic_charts" is the public YouTube Music Charts shelf — no API
 * key required, same auth model as the music.youtube.com InnerTube calls.
 */
object YouTubeChartsRepository {

    private const val ENDPOINT =
        "https://charts.youtube.com/youtubei/v1/browse?prettyPrint=false"

    private val REGIONS = listOf("US", "IN", "GB", "AE", "SA", "EG")

    data class ChartsPage(
        val topSongs: List<YouTubeSearchResult>,
        val trending: List<YouTubeSearchResult>
    )

    suspend fun getCharts(region: String = "US"): ChartsPage = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                setRequestProperty("Origin", "https://charts.youtube.com")
                setRequestProperty("Referer", "https://charts.youtube.com/")
                setRequestProperty("X-YouTube-Client-Name", "62")
                setRequestProperty("X-YouTube-Client-Version", "1.20250317.01.00")
            }

            val body = JSONObject().apply {
                put("browseId", "FEmusic_charts")
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("hl", "en")
                        put("gl", region.uppercase())
                        put("clientName", "WEB_MUSIC_ANALYTICS")
                        put("clientVersion", "2.0")
                    })
                })
            }

            conn.outputStream.bufferedWriter().use { it.write(body.toString()) }

            val status = conn.responseCode
            if (status !in 200..299) return@withContext ChartsPage(emptyList(), emptyList())

            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            parseCharts(json)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: use InnerTube search for "top charts ${region}"
            ChartsPage(
                topSongs = searchFallback("top music charts $region songs"),
                trending = searchFallback("trending music $region 2026")
            )
        } finally {
            conn?.disconnect()
        }
    }

    /** All available regions for the charts selector. */
    fun availableRegions(): List<String> = REGIONS

    // ── Parsing ─────────────────────────────────────────────────────────────

    private fun parseCharts(json: JSONObject): ChartsPage {
        val topSongs = mutableListOf<YouTubeSearchResult>()
        val trending = mutableListOf<YouTubeSearchResult>()

        try {
            val contents = json
                .optJSONObject("contents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return ChartsPage(emptyList(), emptyList())

            for (i in 0 until contents.length()) {
                val section = contents.optJSONObject(i) ?: continue
                val shelf = section.optJSONObject("musicCarouselShelfRenderer")
                    ?: section.optJSONObject("musicShelfRenderer")
                    ?: continue

                val headerTitle = shelf
                    .optJSONObject("header")
                    ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                    ?.optJSONObject("title")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.lowercase()
                    ?: ""

                val items = shelf.optJSONArray("contents") ?: continue
                val parsed = (0 until items.length()).mapNotNull { j ->
                    parseItem(items.optJSONObject(j) ?: return@mapNotNull null)
                }

                when {
                    headerTitle.contains("top song") || headerTitle.contains("top 100") ->
                        topSongs.addAll(parsed)
                    headerTitle.contains("trend") ->
                        trending.addAll(parsed)
                    else ->
                        topSongs.addAll(parsed) // fallback bucket
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ChartsPage(topSongs, trending)
    }

    private fun parseItem(obj: JSONObject): YouTubeSearchResult? {
        // charts.youtube.com uses musicTwoRowItemRenderer inside carousels
        val renderer = obj.optJSONObject("musicTwoRowItemRenderer")
            ?: obj.optJSONObject("musicResponsiveListItemRenderer")
            ?: return null

        val videoId = renderer
            .optJSONObject("navigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId")
            ?.takeIf { it.isNotBlank() }
            ?: renderer.optJSONObject("playlistItemData")
                ?.optString("videoId")
                ?.takeIf { it.isNotBlank() }
            ?: return null

        val title = renderer.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            ?: renderer.optJSONArray("flexColumns")
                ?.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optString("text")
            ?: return null

        val artist = renderer.optJSONObject("subtitle")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text")
            .orEmpty()

        val thumb = renderer
            .optJSONObject("thumbnailRenderer")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { thumbs ->
                // Pick the largest available thumbnail
                (0 until thumbs.length()).map { thumbs.getJSONObject(it) }
                    .maxByOrNull { it.optInt("width", 0) }
                    ?.optString("url")
            }
            ?: "https://i.ytimg.com/vi_webp/$videoId/default.webp"

        return YouTubeSearchResult(
            videoId = videoId,
            title = title,
            channelTitle = artist,
            thumbnailUrl = thumb,
            duration = ""
        )
    }

    // Fallback when the charts endpoint fails — plain InnerTube keyword search
    private fun searchFallback(query: String): List<YouTubeSearchResult> {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("hl", "en")
                        put("gl", "US")
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240101.00.00")
                    })
                })
                put("query", query)
            }
            conn.outputStream.bufferedWriter().use { it.write(body.toString()) }
            if (conn.responseCode !in 200..299) return emptyList()
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val results = mutableListOf<YouTubeSearchResult>()
            extractVideosRecursive(json, results)
            results.distinctBy { it.videoId }.take(20)
        } catch (e: Exception) {
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }

    private fun extractVideosRecursive(obj: Any?, out: MutableList<YouTubeSearchResult>) {
        when (obj) {
            is JSONObject -> {
                if (obj.has("videoRenderer")) {
                    val vr = obj.optJSONObject("videoRenderer") ?: return
                    val videoId = vr.optString("videoId").takeIf { it.isNotBlank() } ?: return
                    val titleRuns = vr.optJSONObject("title")?.optJSONArray("runs")
                    val title = buildString {
                        if (titleRuns != null) for (i in 0 until titleRuns.length())
                            append(titleRuns.optJSONObject(i)?.optString("text").orEmpty())
                    }.ifBlank { vr.optJSONObject("title")?.optString("simpleText").orEmpty() }
                    val channel = vr.optJSONObject("ownerText")
                        ?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
                    val thumb = vr.optJSONObject("thumbnail")
                        ?.optJSONArray("thumbnails")
                        ?.let { t -> (0 until t.length()).map { t.getJSONObject(it) }
                            .maxByOrNull { it.optInt("width", 0) }?.optString("url").orEmpty() }
                        ?: ""
                    val dur = vr.optJSONObject("lengthText")?.optString("simpleText").orEmpty()
                    if (title.isNotBlank())
                        out.add(YouTubeSearchResult(videoId, title, channel, thumb, dur))
                } else {
                    obj.keys().forEach { extractVideosRecursive(obj.opt(it), out) }
                }
            }
            is org.json.JSONArray -> {
                for (i in 0 until obj.length()) extractVideosRecursive(obj.opt(i), out)
            }
        }
    }
}
