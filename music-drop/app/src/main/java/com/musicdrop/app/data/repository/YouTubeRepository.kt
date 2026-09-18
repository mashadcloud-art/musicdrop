package com.musicdrop.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class YouTubeVideoItem(
    val id: String,
    val title: String,
    val channel: String,
    val views: String,
    val duration: String,
    val thumbnailUrl: String
)

object YouTubeRepository {
    private const val PROXY_BASE = "https://filedrop-saavn.fly.dev"

    val FALLBACK_TRENDING = listOf(
        YouTubeVideoItem(
            id = "BddP6PYo2gs",
            title = "Kesariya - Brahmāstra | Ranbir Kapoor, Alia Bhatt | Pritam | Arijit Singh",
            channel = "Sony Music India",
            views = "620M views",
            duration = "2:53",
            thumbnailUrl = "https://i.ytimg.com/vi/BddP6PYo2gs/hqdefault.jpg"
        ),
        YouTubeVideoItem(
            id = "7wtfhZwyrcc",
            title = "Imagine Dragons - Believer (Official Music Video)",
            channel = "ImagineDragons",
            views = "2.6B views",
            duration = "3:36",
            thumbnailUrl = "https://i.ytimg.com/vi/7wtfhZwyrcc/hqdefault.jpg"
        ),
        YouTubeVideoItem(
            id = "Umqb9KENgmk",
            title = "Tum Hi Ho | Aashiqui 2 | Arijit Singh | Aditya Roy Kapur, Shraddha Kapoor",
            channel = "T-Series",
            views = "840M views",
            duration = "4:22",
            thumbnailUrl = "https://i.ytimg.com/vi/Umqb9KENgmk/hqdefault.jpg"
        ),
        YouTubeVideoItem(
            id = "ElZfdU54Cp8",
            title = "Apna Bana Le - Bhediya | Varun Dhawan, Kriti Sanon | Sachin-Jigar, Arijit Singh",
            channel = "Zee Music Company",
            views = "490M views",
            duration = "3:24",
            thumbnailUrl = "https://i.ytimg.com/vi/ElZfdU54Cp8/hqdefault.jpg"
        ),
        YouTubeVideoItem(
            id = "kJQP7kiw5Fk",
            title = "Luis Fonsi - Despacito ft. Daddy Yankee",
            channel = "Luis Fonsi",
            views = "8.4B views",
            duration = "4:41",
            thumbnailUrl = "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg"
        ),
        YouTubeVideoItem(
            id = "fTKqtvXjkvo",
            title = "Top Hits 2026 ~ Trending Songs 2026 ~ Top Songs 2026 Top Music",
            channel = "Revive Music",
            views = "54M views",
            duration = "3:45",
            thumbnailUrl = "https://i.ytimg.com/vi/fTKqtvXjkvo/hqdefault.jpg"
        ),
        YouTubeVideoItem(
            id = "OPf0YbXqDm0",
            title = "Mark Ronson - Uptown Funk (Official Video) ft. Bruno Mars",
            channel = "Mark Ronson",
            views = "5.1B views",
            duration = "4:30",
            thumbnailUrl = "https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg"
        ),
        YouTubeVideoItem(
            id = "JGwWNGJdvx8",
            title = "Ed Sheeran - Shape of You (Official Music Video)",
            channel = "Ed Sheeran",
            views = "6.2B views",
            duration = "4:23",
            thumbnailUrl = "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg"
        ),
        YouTubeVideoItem(
            id = "2Vv-BfVoq4g",
            title = "Ed Sheeran - Perfect (Official Music Video)",
            channel = "Ed Sheeran",
            views = "3.8B views",
            duration = "4:39",
            thumbnailUrl = "https://i.ytimg.com/vi/2Vv-BfVoq4g/hqdefault.jpg"
        ),
        YouTubeVideoItem(
            id = "hT_nvWreIhg",
            title = "OneRepublic - Counting Stars (Official Music Video)",
            channel = "OneRepublic",
            views = "4.0B views",
            duration = "4:44",
            thumbnailUrl = "https://i.ytimg.com/vi/hT_nvWreIhg/hqdefault.jpg"
        )
    )

    suspend fun getTrending(query: String = "Top Trending Music Songs 2026", limit: Int = 25): List<YouTubeVideoItem> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "$PROXY_BASE/youtube/trending?q=$encoded&limit=$limit"
            val conn = URL(url).openConnection()
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            val json = conn.getInputStream().bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("videos")
            val list = (0 until arr.length()).mapNotNull { i ->
                parseVideo(arr.getJSONObject(i))
            }
            if (list.isNotEmpty()) list else FALLBACK_TRENDING
        } catch (e: Exception) {
            e.printStackTrace()
            FALLBACK_TRENDING
        }
    }

    suspend fun search(query: String, limit: Int = 25): List<YouTubeVideoItem> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "$PROXY_BASE/youtube/search?q=$encoded&limit=$limit"
            val conn = URL(url).openConnection()
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            val json = conn.getInputStream().bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("videos")
            (0 until arr.length()).mapNotNull { i ->
                parseVideo(arr.getJSONObject(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseVideo(obj: JSONObject): YouTubeVideoItem? {
        val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return null
        return YouTubeVideoItem(
            id = id,
            title = obj.optString("title", "YouTube Video"),
            channel = obj.optString("channel", ""),
            views = obj.optString("views", ""),
            duration = obj.optString("duration", ""),
            thumbnailUrl = obj.optString("thumbnailUrl", "https://i.ytimg.com/vi/$id/hqdefault.jpg")
        )
    }
}
