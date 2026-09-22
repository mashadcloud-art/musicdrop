package com.musicdrop.tv.data.youtube

import com.musicdrop.tv.BuildConfig
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

object YouTubeSearchRepository {

    private const val ENDPOINT = "https://www.googleapis.com/youtube/v3/search"
    private const val INNERTUBE_ENDPOINT = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false"

    data class DetailedSearchResult(
        val songs: List<YouTubeSearchResult>,
        val topArtist: com.musicdrop.tv.data.repository.YtMusicApiRepository.YtChartArtist?,
        val albums: List<com.musicdrop.tv.data.repository.YtMusicApiRepository.YtCardItem>
    )

    suspend fun searchDetailed(query: String): DetailedSearchResult = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext DetailedSearchResult(emptyList(), null, emptyList())

        val ytMusicData = try {
            com.musicdrop.tv.data.repository.YtMusicApiRepository.search(query)
        } catch (_: Exception) {
            com.musicdrop.tv.data.repository.YtMusicApiRepository.YtSearchResultData(emptyList(), null, emptyList())
        }

        val publicOutcome = searchPublic(query)
        val publicSongs = if (publicOutcome is YouTubeSearchOutcome.Success) publicOutcome.results else emptyList()

        // Merge and deduplicate by videoId
        val mergedSongs = mutableListOf<YouTubeSearchResult>()
        val seenIds = mutableSetOf<String>()

        for (song in ytMusicData.songs) {
            if (!YouTubeMusicRepository.isLongVideoOrJukebox(song.title, song.duration)) {
                if (seenIds.add(song.videoId)) mergedSongs.add(song)
            }
        }
        for (song in publicSongs) {
            if (!YouTubeMusicRepository.isLongVideoOrJukebox(song.title, song.duration)) {
                if (seenIds.add(song.videoId)) mergedSongs.add(song)
            }
        }

        DetailedSearchResult(
            songs = mergedSongs.take(30),
            topArtist = ytMusicData.topResultArtist,
            albums = ytMusicData.albums
        )
    }

    suspend fun search(query: String, maxResults: Int = 25): YouTubeSearchOutcome =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                return@withContext YouTubeSearchOutcome.Success(emptyList())
            }

            val detailed = searchDetailed(query)
            if (detailed.songs.isNotEmpty()) {
                return@withContext YouTubeSearchOutcome.Success(detailed.songs)
            }

            val apiKey = BuildConfig.YOUTUBE_API_KEY
            if (apiKey.isNotBlank()) {
                val officialResult = searchOfficial(query, apiKey, maxResults)
                if (officialResult is YouTubeSearchOutcome.Success && officialResult.results.isNotEmpty()) {
                    val filtered = officialResult.results.filter { !YouTubeMusicRepository.isLongVideoOrJukebox(it.title, it.duration) }
                    return@withContext YouTubeSearchOutcome.Success(filtered)
                }
            }

            // Seamless public fallback
            searchPublic(query)
        }

    suspend fun getTrendingMusic(): YouTubeSearchOutcome =
        withContext(Dispatchers.IO) {
            search("Trending Songs Music Hits 2026", maxResults = 15)
        }

    private fun searchOfficial(query: String, apiKey: String, maxResults: Int): YouTubeSearchOutcome {
        var connection: HttpURLConnection? = null
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL(
                "$ENDPOINT?part=snippet&type=video&maxResults=$maxResults" +
                    "&q=$encodedQuery&key=$apiKey"
            )
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 12_000
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { s ->
                BufferedReader(InputStreamReader(s)).use { it.readText() }
            }.orEmpty()

            if (status !in 200..299) {
                return YouTubeSearchOutcome.Error("Official API HTTP $status")
            }

            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return YouTubeSearchOutcome.Success(emptyList())
            val results = mutableListOf<YouTubeSearchResult>()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val videoId = item.optJSONObject("id")?.optString("videoId").orEmpty()
                if (videoId.isBlank()) continue
                val snippet = item.optJSONObject("snippet") ?: continue
                val title = snippet.optString("title", "Untitled")
                if (YouTubeMusicRepository.isLongVideoOrJukebox(title, "")) continue
                val thumbnails = snippet.optJSONObject("thumbnails")
                val thumbUrl = thumbnails?.optJSONObject("medium")?.optString("url")
                    ?: thumbnails?.optJSONObject("default")?.optString("url")
                    ?: ""
                results.add(
                    YouTubeSearchResult(
                        videoId = videoId,
                        title = title,
                        channelTitle = snippet.optString("channelTitle", ""),
                        thumbnailUrl = thumbUrl
                    )
                )
            }
            YouTubeSearchOutcome.Success(results)
        } catch (e: Exception) {
            YouTubeSearchOutcome.Error(e.localizedMessage ?: "Network error")
        } finally {
            connection?.disconnect()
        }
    }

    private fun searchPublic(query: String): YouTubeSearchOutcome {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(INNERTUBE_ENDPOINT)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }

            val requestBody = JSONObject().apply {
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

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { s ->
                BufferedReader(InputStreamReader(s)).use { it.readText() }
            }.orEmpty()

            if (status !in 200..299) {
                return YouTubeSearchOutcome.Error("Search request failed (HTTP $status)")
            }

            val json = JSONObject(body)
            val results = mutableListOf<YouTubeSearchResult>()
            extractVideosRecursive(json, results)

            val filtered = results
                .filter { !YouTubeMusicRepository.isLongVideoOrJukebox(it.title, it.duration) }
                .distinctBy { it.videoId }
                .take(30)

            YouTubeSearchOutcome.Success(filtered)
        } catch (e: Exception) {
            YouTubeSearchOutcome.Error(e.localizedMessage ?: "Couldn't reach search service. Check internet connection.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun extractVideosRecursive(obj: Any?, out: MutableList<YouTubeSearchResult>) {
        when (obj) {
            is JSONObject -> {
                if (obj.has("videoRenderer")) {
                    val vr = obj.optJSONObject("videoRenderer")
                    if (vr != null) {
                        val videoId = vr.optString("videoId")
                        val titleRuns = vr.optJSONObject("title")?.optJSONArray("runs")
                        val title = if (titleRuns != null && titleRuns.length() > 0) {
                            val sb = StringBuilder()
                            for (i in 0 until titleRuns.length()) {
                                sb.append(titleRuns.optJSONObject(i)?.optString("text").orEmpty())
                            }
                            sb.toString()
                        } else {
                            vr.optJSONObject("title")?.optString("simpleText").orEmpty()
                        }

                        val ownerRuns = vr.optJSONObject("ownerText")?.optJSONArray("runs")
                        val channel = if (ownerRuns != null && ownerRuns.length() > 0) {
                            ownerRuns.optJSONObject(0)?.optString("text").orEmpty()
                        } else ""

                        val thumbs = vr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                        val thumbUrl = if (thumbs != null && thumbs.length() > 0) {
                            thumbs.optJSONObject(thumbs.length() - 1)?.optString("url").orEmpty()
                        } else ""

                        val duration = vr.optJSONObject("lengthText")?.optString("simpleText").orEmpty()

                        if (videoId.isNotBlank() && title.isNotBlank() && !YouTubeMusicRepository.isLongVideoOrJukebox(title, duration)) {
                            out.add(
                                YouTubeSearchResult(
                                    videoId = videoId,
                                    title = title,
                                    channelTitle = channel,
                                    thumbnailUrl = thumbUrl,
                                    duration = duration
                                )
                            )
                        }
                    }
                } else {
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        extractVideosRecursive(obj.opt(keys.next()), out)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until obj.length()) {
                    extractVideosRecursive(obj.opt(i), out)
                }
            }
        }
    }
}
