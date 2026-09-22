package com.musicdrop.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object TvYtRepository {

    private const val INNERTUBE_SEARCH = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false"

    suspend fun searchVideos(query: String): List<TvVideoItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        var conn: HttpURLConnection? = null
        try {
            val url = URL(INNERTUBE_SEARCH)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            }

            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("hl", "en")
                        put("gl", "IN")
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240901.00.00")
                    })
                })
                put("query", query)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val resp = stream?.use { s ->
                BufferedReader(InputStreamReader(s)).use { it.readText() }
            }.orEmpty()

            parseSearchResponse(resp)
        } catch (e: Throwable) {
            android.util.Log.e("TvYtRepo", "Search failed: ${e.message}")
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseSearchResponse(jsonStr: String): List<TvVideoItem> {
        val results = mutableListOf<TvVideoItem>()
        if (jsonStr.isBlank()) return results
        try {
            val root = JSONObject(jsonStr)
            val sectionListRenderer = root.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?: return results

            val contents = sectionListRenderer.optJSONArray("contents") ?: return results
            for (i in 0 until contents.length()) {
                val itemSection = contents.optJSONObject(i)?.optJSONObject("itemSectionRenderer") ?: continue
                val items = itemSection.optJSONArray("contents") ?: continue
                for (j in 0 until items.length()) {
                    val videoRenderer = items.optJSONObject(j)?.optJSONObject("videoRenderer") ?: continue
                    val videoId = videoRenderer.optString("videoId")
                    if (videoId.length != 11) continue

                    val titleObj = videoRenderer.optJSONObject("title")
                    val title = titleObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: titleObj?.optString("simpleText")
                        ?: "Untitled Video"

                    val ownerObj = videoRenderer.optJSONObject("ownerText")
                    val channelTitle = ownerObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "YouTube"

                    val lengthObj = videoRenderer.optJSONObject("lengthText")
                    val duration = lengthObj?.optString("simpleText") ?: ""

                    val viewObj = videoRenderer.optJSONObject("viewCountText")
                    val views = viewObj?.optString("simpleText")
                        ?: viewObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: ""

                    val thumbArray = videoRenderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                    val thumbUrl = if (thumbArray != null && thumbArray.length() > 0) {
                        thumbArray.optJSONObject(thumbArray.length() - 1)?.optString("url")
                            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    } else {
                        "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    }

                    results.add(
                        TvVideoItem(
                            id = videoId,
                            title = title,
                            channelTitle = channelTitle,
                            thumbnailUrl = thumbUrl,
                            duration = duration,
                            views = views
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("TvYtRepo", "Parse error: ${e.message}")
        }
        return results
    }
}
