package com.musicdrop.app.data.repository

import com.musicdrop.app.data.youtube.YouTubeSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * High-performance, cached client for https://shnwazdev-ytmusicapi.vercel.app/api
 *
 * Endpoints:
 * - trending: /api/trending?country=IN (AE, US, PK, SA, GB)
 * - new_releases: /api/new_releases
 * - recently_added (musicfeed): /api/music_premium/musicfeed
 * - search: /api/search?query=...
 * - song_info: /api/song?id=VIDEO_ID
 * - lyrics: /api/lyrics?id=VIDEO_ID
 * - artist: /api/artist?id=CHANNEL_ID
 * - album: /api/album?id=BROWSE_ID
 * - explore: /api/explore
 * - charts: /api/charts?country=IN (AE, US, PK, SA, GB)
 */
object YtMusicApiRepository {

    private const val BASE_URL = "https://shnwazdev-ytmusicapi.vercel.app/api"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    // ── In-Memory Cache ──────────────────────────────────────────────────────
    private data class CacheEntry(val data: Any, val timestamp: Long)
    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 15 * 60 * 1000L // 15 minutes

    private fun <T> getCached(key: String): T? {
        val entry = memoryCache[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > CACHE_TTL_MS) {
            memoryCache.remove(key)
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return entry.data as? T
    }

    private fun putCache(key: String, data: Any) {
        memoryCache[key] = CacheEntry(data, System.currentTimeMillis())
    }

    data class YtArtistDetails(
        val name: String,
        val channelId: String,
        val subscribers: String?,
        val thumbnailUrl: String?,
        val topSongs: List<YouTubeSearchResult>,
        val albums: List<YtCardItem>,
        val singles: List<YtCardItem>,
        val videos: List<YouTubeSearchResult>
    )

    data class YtAlbumDetails(
        val title: String,
        val artistName: String,
        val year: String?,
        val thumbnailUrl: String?,
        val trackCount: Int,
        val duration: String?,
        val tracks: List<YouTubeSearchResult>
    )

    data class YtCardItem(
        val title: String,
        val browseId: String? = null,
        val audioPlaylistId: String? = null,
        val thumbnailUrl: String? = null,
        val year: String? = null,
        val type: String? = null
    )

    data class YtChartArtist(
        val rank: String = "",
        val title: String,
        val browseId: String,
        val subscribers: String = "",
        val thumbnailUrl: String = "",
        val trend: String = "equal"
    )

    data class YtChartPlaylist(
        val title: String,
        val playlistId: String,
        val thumbnailUrl: String
    )

    data class YtChartsResponse(
        val daily: List<YtChartPlaylist>,
        val weekly: List<YtChartPlaylist>,
        val artists: List<YtChartArtist>,
        val videos: List<YtChartPlaylist> = emptyList(),
        val genres: List<YtChartPlaylist> = emptyList()
    )

    data class YtMusicFeedCategory(
        val title: String,
        val playlists: List<YtChartPlaylist>
    )

    data class YtSearchResultData(
        val songs: List<YouTubeSearchResult>,
        val topResultArtist: YtChartArtist?,
        val albums: List<YtCardItem>
    )

    fun clearCache() {
        memoryCache.clear()
    }

    fun peekArtist(channelId: String): YtArtistDetails? = getCached("artist_$channelId")
    fun peekAlbum(browseId: String): YtAlbumDetails? = getCached("album_$browseId")

    /**
     * 1. Trending Songs across regions (IN, AE, US, PK, SA, GB).
     */
    suspend fun getTrending(country: String = "IN", force: Boolean = false): List<YouTubeSearchResult> = withContext(Dispatchers.IO) {
        val upperCountry = country.uppercase()
        val cacheKey = "trending_$upperCountry"
        if (!force) {
            getCached<List<YouTubeSearchResult>>(cacheKey)?.let { return@withContext it }
        }

        // Special handling for Pakistan: YouTube Music charts API has no official chart table for PK
        // and returns fallback Latin/global songs. Instead, fetch real authentic Pakistani hits & Coke Studio playlists!
        if (upperCountry == "PK") {
            try {
                val pkPlaylists = listOf(
                    "OLAK5uy_n5cdXJeDgQ_8Okr0eN1H0Hbl8M8yMNuTQ", // Coke Studio Season 14 (Pasoori, Kana Yaari, Tu Jhoom, etc.)
                    "PLlYsrzDvIU9Rl3KTGvX2tPr6yBwPPjaH0",        // Best of Coke Studio Pakistan
                    "PLy_wKxVmWb4awLwIktp2HWzj82rJAZqmp"         // Top 100 Songs Pakistan 2026
                )
                val combinedPk = mutableListOf<YouTubeSearchResult>()
                // Put verified headline viral hits first so users immediately see top Pakistani songs
                combinedPk.addAll(getVerifiedPakistaniHits())
                for (pId in pkPlaylists) {
                    try {
                        val tracks = getPlaylistTracks(pId)
                        combinedPk.addAll(tracks)
                    } catch (_: Exception) {}
                }
                val distinctPk = combinedPk.distinctBy { it.videoId }
                if (distinctPk.isNotEmpty()) {
                    putCache(cacheKey, distinctPk)
                    return@withContext distinctPk
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 1. Primary: YouTube Music API
        try {
            val url = "$BASE_URL/trending?country=$upperCountry"
            val jsonStr = httpGet(url)
            if (!jsonStr.isNullOrBlank()) {
                val root = JSONObject(jsonStr)
                val items = root.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val list = mutableListOf<YouTubeSearchResult>()
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val videoId = item.optString("videoId")
                        if (videoId.isNullOrBlank()) continue
                        val title = item.optString("title", "Unknown Title")
                        val artistsText = item.optString("artistsText").takeIf { it.isNotBlank() }
                            ?: item.optJSONArray("artists")?.optJSONObject(0)?.optString("name")
                            ?: "Unknown Artist"
                        val duration = item.optString("duration", "")
                        val thumb = extractThumbnail(item.optJSONArray("thumbnails"))
                            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                        list.add(
                            YouTubeSearchResult(
                                videoId = videoId,
                                title = title,
                                channelTitle = artistsText,
                                thumbnailUrl = thumb,
                                duration = duration
                            )
                        )
                    }
                    if (list.isNotEmpty()) {
                        putCache(cacheKey, list)
                        return@withContext list
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback: Invidious / Piped API (as provided in user spec)
        val fallbackUrls = listOf(
            "https://inv.tux.pizza/api/v1/trending?region=$upperCountry",
            "https://invidious.nerdvpn.de/api/v1/trending?region=$upperCountry",
            "https://pipedapi.kavin.rocks/trending?region=$upperCountry"
        )
        for (fUrl in fallbackUrls) {
            try {
                val jsonStr = httpGet(fUrl) ?: continue
                val items = JSONArray(jsonStr)
                val list = mutableListOf<YouTubeSearchResult>()
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val videoId = item.optString("videoId").takeIf { it.isNotBlank() }
                        ?: item.optString("url").removePrefix("/watch?v=")
                    if (videoId.isBlank()) continue
                    val title = item.optString("title", "Trending Song")
                    val author = item.optString("author").takeIf { it.isNotBlank() }
                        ?: item.optString("uploaderName", "Artist")
                    val thumb = item.optString("thumbnail").takeIf { it.isNotBlank() }
                        ?: extractThumbnail(item.optJSONArray("videoThumbnails"))
                        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    val lengthSec = item.optInt("lengthSeconds", item.optInt("duration", 0))
                    val durationStr = if (lengthSec > 0) String.format("%d:%02d", lengthSec / 60, lengthSec % 60) else ""

                    list.add(
                        YouTubeSearchResult(
                            videoId = videoId,
                            title = title,
                            channelTitle = author,
                            thumbnailUrl = thumb,
                            duration = durationStr
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    putCache(cacheKey, list)
                    return@withContext list
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: Region trending search query
        try {
            val query = when (upperCountry) {
                "IN" -> "bollywood top trending songs 2026"
                "PK" -> "pakistani top trending songs 2026"
                "AE" -> "arabic dubai top trending songs 2026"
                "US" -> "billboard hot 100 trending songs 2026"
                "GB" -> "uk official top trending songs 2026"
                "SA" -> "saudi top trending songs 2026"
                else -> "top trending songs 2026"
            }
            val searchRes = search(query)
            if (searchRes.songs.isNotEmpty()) {
                putCache(cacheKey, searchRes.songs)
                return@withContext searchRes.songs
            }
        } catch (_: Exception) {}

        emptyList()
    }

    /**
     * Fetches tracks from a YouTube Music playlist ID (/api/playlist?id=...).
     */
    suspend fun getPlaylistTracks(playlistId: String): List<YouTubeSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "playlist_$playlistId"
        getCached<List<YouTubeSearchResult>>(cacheKey)?.let { return@withContext it }

        val url = "$BASE_URL/playlist?id=$playlistId"
        val jsonStr = httpGet(url) ?: return@withContext emptyList()
        try {
            val root = JSONObject(jsonStr)
            val tracks = root.optJSONArray("tracks") ?: return@withContext emptyList()
            val list = mutableListOf<YouTubeSearchResult>()
            for (i in 0 until tracks.length()) {
                val item = tracks.optJSONObject(i) ?: continue
                val videoId = item.optString("videoId")
                if (videoId.isNullOrBlank()) continue
                val title = item.optString("title", "Unknown Title")
                val artists = item.optJSONArray("artists")
                val artistName = if (artists != null && artists.length() > 0) {
                    (0 until artists.length()).mapNotNull { idx ->
                        artists.optJSONObject(idx)?.optString("name")
                    }.joinToString(", ")
                } else "Pakistani Music"
                val duration = item.optString("duration", "")
                val thumb = extractThumbnail(item.optJSONArray("thumbnails"))
                    ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                list.add(
                    YouTubeSearchResult(
                        videoId = videoId,
                        title = title,
                        channelTitle = artistName,
                        thumbnailUrl = thumb,
                        duration = duration
                    )
                )
            }
            if (list.isNotEmpty()) putCache(cacheKey, list)
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun getVerifiedPakistaniHits(): List<YouTubeSearchResult> {
        return listOf(
            YouTubeSearchResult("5Eqb_-j3FDA", "Pasoori", "Ali Sethi x Shae Gill • Coke Studio", "https://i.ytimg.com/vi/5Eqb_-j3FDA/hqdefault.jpg", "3:44"),
            YouTubeSearchResult("0F3wO6fS23U", "Tu Jhoom", "Abida Parveen x Naseebo Lal • Coke Studio", "https://i.ytimg.com/vi/0F3wO6fS23U/hqdefault.jpg", "6:54"),
            YouTubeSearchResult("4UGepS0_Vp0", "Kana Yaari", "Kaifi Khalil x Eva B x Wahab Bugti", "https://i.ytimg.com/vi/4UGepS0_Vp0/hqdefault.jpg", "3:39"),
            YouTubeSearchResult("_XBVWlI8TsQ", "Kahani Suno 2.0", "Kaifi Khalil", "https://i.ytimg.com/vi/_XBVWlI8TsQ/hqdefault.jpg", "2:54"),
            YouTubeSearchResult("q48v2gGjW18", "Iraaday", "Abdul Hannan x Rovalio", "https://i.ytimg.com/vi/q48v2gGjW18/hqdefault.jpg", "3:01"),
            YouTubeSearchResult("6d5S3g0H9i4", "Bikhra", "Abdul Hannan x Rovalio", "https://i.ytimg.com/vi/6d5S3g0H9i4/hqdefault.jpg", "3:48"),
            YouTubeSearchResult("bN13qC7d1r0", "Gumaan", "Young Stunners (Talha Anjum, Talhah Yunus)", "https://i.ytimg.com/vi/bN13qC7d1r0/hqdefault.jpg", "4:12"),
            YouTubeSearchResult("ILWdIngbl2A", "Phir Milenge", "Faisal Kapadia x Young Stunners", "https://i.ytimg.com/vi/ILWdIngbl2A/hqdefault.jpg", "5:52"),
            YouTubeSearchResult("uuFY24gHhWY", "Ye Dunya", "Talha Anjum x Karakoram x Faris Shafi", "https://i.ytimg.com/vi/uuFY24gHhWY/hqdefault.jpg", "5:13"),
            YouTubeSearchResult("qMFC-yeYbS4", "Peechay Hutt", "Hasan Raheem x Talal Qureshi x Justin Bibis", "https://i.ytimg.com/vi/qMFC-yeYbS4/hqdefault.jpg", "3:46"),
            YouTubeSearchResult("KHLNSxe5Y8A", "Sammi Meri Waar", "Umair Jaswal x Quratulain Balouch", "https://i.ytimg.com/vi/KHLNSxe5Y8A/hqdefault.jpg", "6:42"),
            YouTubeSearchResult("7SDrjwtfKMk", "Chaap Tilak", "Abida Parveen x Rahat Fateh Ali Khan", "https://i.ytimg.com/vi/7SDrjwtfKMk/hqdefault.jpg", "9:03"),
            YouTubeSearchResult("Uks8psEpmB4", "Rang", "Rahat Fateh Ali Khan x Amjad Sabri", "https://i.ytimg.com/vi/Uks8psEpmB4/hqdefault.jpg", "11:59"),
            YouTubeSearchResult("pba_YmWDAIU", "Ranjish Hi Sahi", "Ali Sethi", "https://i.ytimg.com/vi/pba_YmWDAIU/hqdefault.jpg", "6:11"),
            YouTubeSearchResult("LekqDjknArc", "Aaqa", "Abida Parveen x Ali Sethi", "https://i.ytimg.com/vi/LekqDjknArc/hqdefault.jpg", "8:34"),
            YouTubeSearchResult("cs9ORHI8o38", "Muntazir", "Danyal Zafar x Momina Mustehsan", "https://i.ytimg.com/vi/cs9ORHI8o38/hqdefault.jpg", "6:00"),
            YouTubeSearchResult("Au50TJPu8UU", "Neray Neray Vas", "Soch the Band x Butt Brothers", "https://i.ytimg.com/vi/Au50TJPu8UU/hqdefault.jpg", "3:48"),
            YouTubeSearchResult("LkKSr_MM3z0", "Beparwah", "Momina Mustehsan", "https://i.ytimg.com/vi/LkKSr_MM3z0/hqdefault.jpg", "4:33"),
            YouTubeSearchResult("mIcuxyrZtW4", "Muaziz Saarif", "Faris Shafi x Meesha Shafi", "https://i.ytimg.com/vi/mIcuxyrZtW4/hqdefault.jpg", "4:42"),
            YouTubeSearchResult("TrPvQvbp3Cg", "Paar Chanaa De", "Shilpa Rao x Noori", "https://i.ytimg.com/vi/TrPvQvbp3Cg/hqdefault.jpg", "11:15"),
            YouTubeSearchResult("xhgt47nvZUQ", "Baliye (Laung Gawacha)", "Quratulain Baloch x Haroon Shahid", "https://i.ytimg.com/vi/xhgt47nvZUQ/hqdefault.jpg", "7:42")
        )
    }

    /**
     * 2. Official New Releases (Albums, EPs, Singles).
     */
    suspend fun getNewReleases(force: Boolean = false): List<YouTubeSearchResult> = withContext(Dispatchers.IO) {
        if (!force) {
            getCached<List<YouTubeSearchResult>>("new_releases")?.let { return@withContext it }
        }

        val url = "$BASE_URL/new_releases"
        val jsonStr = httpGet(url) ?: return@withContext emptyList()
        try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONArray("data") ?: return@withContext emptyList()
            val list = mutableListOf<YouTubeSearchResult>()
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val title = item.optString("title", "New Release")
                val artists = item.optJSONArray("artists")
                val artistName = artists?.optJSONObject(0)?.optString("name") ?: "Various Artists"
                val browseId = item.optString("browseId")
                val audioPlaylistId = item.optString("audioPlaylistId")
                val thumb = extractThumbnail(item.optJSONArray("thumbnails")) ?: ""
                val type = item.optString("type", "Single")

                val effectiveId = if (audioPlaylistId.isNotBlank()) audioPlaylistId else browseId
                if (effectiveId.isNotBlank()) {
                    list.add(
                        YouTubeSearchResult(
                            videoId = effectiveId,
                            title = title,
                            channelTitle = "$artistName • $type",
                            thumbnailUrl = thumb,
                            duration = type
                        )
                    )
                }
            }
            if (list.isNotEmpty()) putCache("new_releases", list)
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 3. Music Feed / Recently Added Categories (/api/music_premium/musicfeed).
     */
    suspend fun getMusicFeed(force: Boolean = false): List<YtMusicFeedCategory> = withContext(Dispatchers.IO) {
        if (!force) {
            getCached<List<YtMusicFeedCategory>>("music_feed")?.let { return@withContext it }
        }

        val url = "$BASE_URL/music_premium/musicfeed"
        val jsonStr = httpGet(url) ?: return@withContext emptyList()
        try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONArray("data") ?: return@withContext emptyList()
            val categories = mutableListOf<YtMusicFeedCategory>()
            for (i in 0 until data.length()) {
                val catObj = data.optJSONObject(i) ?: continue
                val catTitle = catObj.optString("title", "Mixes")
                val contents = catObj.optJSONArray("contents") ?: continue
                val playlists = mutableListOf<YtChartPlaylist>()
                for (j in 0 until contents.length()) {
                    val p = contents.optJSONObject(j) ?: continue
                    val pTitle = p.optString("title")
                    val pId = p.optString("playlistId")
                    val thumb = extractThumbnail(p.optJSONArray("thumbnails")) ?: ""
                    if (pId.isNotBlank() && pTitle.isNotBlank()) {
                        playlists.add(YtChartPlaylist(title = pTitle, playlistId = pId, thumbnailUrl = thumb))
                    }
                }
                if (playlists.isNotEmpty()) {
                    categories.add(YtMusicFeedCategory(title = catTitle, playlists = playlists))
                }
            }
            if (categories.isNotEmpty()) putCache("music_feed", categories)
            categories
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 4. Search (/api/search?query=...)
     * Returns categorized songs, top artist, and albums.
     */
    suspend fun search(query: String, force: Boolean = false): YtSearchResultData = withContext(Dispatchers.IO) {
        val cacheKey = "search_${query.trim().lowercase()}"
        if (!force) {
            getCached<YtSearchResultData>(cacheKey)?.let { return@withContext it }
        }

        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$BASE_URL/search?query=$encoded"
        val jsonStr = httpGet(url) ?: return@withContext YtSearchResultData(emptyList(), null, emptyList())
        try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONArray("data") ?: return@withContext YtSearchResultData(emptyList(), null, emptyList())

            val songs = mutableListOf<YouTubeSearchResult>()
            val albums = mutableListOf<YtCardItem>()
            var topArtist: YtChartArtist? = null

            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val resultType = item.optString("resultType")
                val category = item.optString("category")
                val thumb = extractThumbnail(item.optJSONArray("thumbnails")) ?: ""

                if (resultType == "artist" || category.contains("Top result", ignoreCase = true)) {
                    val artistName = item.optJSONArray("artists")?.optJSONObject(0)?.optString("name")
                        ?: item.optString("title", query)
                    val channelId = item.optJSONArray("artists")?.optJSONObject(0)?.optString("id")
                        ?: item.optString("browseId")
                    val subs = item.optString("subscribers")
                    if (topArtist == null && artistName.isNotBlank()) {
                        topArtist = YtChartArtist(
                            rank = "1",
                            title = artistName,
                            browseId = channelId,
                            subscribers = subs,
                            thumbnailUrl = thumb,
                            trend = "up"
                        )
                    }
                }

                if (resultType == "song" || resultType == "video") {
                    val vid = item.optString("videoId")
                    if (vid.isNotBlank()) {
                        val title = item.optString("title")
                        val dur = item.optString("duration", "")
                        if (!com.musicdrop.app.data.youtube.YouTubeMusicRepository.isLongVideoOrJukebox(title, dur)) {
                            val artists = item.optJSONArray("artists")
                            val artistName = if (artists != null && artists.length() > 0) {
                                (0 until artists.length()).mapNotNull { idx ->
                                    artists.optJSONObject(idx)?.optString("name")
                                }.joinToString(", ")
                            } else "YouTube Music"
                            songs.add(
                                YouTubeSearchResult(
                                    videoId = vid,
                                    title = title,
                                    channelTitle = artistName,
                                    thumbnailUrl = thumb,
                                    duration = dur
                                )
                            )
                        }
                    }
                } else if (resultType == "album") {
                    val browseId = item.optString("browseId")
                    val title = item.optString("title")
                    val year = item.optString("year")
                    val type = item.optString("type", "Album")
                    albums.add(
                        YtCardItem(
                            title = title,
                            browseId = browseId,
                            audioPlaylistId = item.optString("audioPlaylistId"),
                            thumbnailUrl = thumb,
                            year = year,
                            type = type
                        )
                    )
                }
            }
            val res = YtSearchResultData(songs = songs, topResultArtist = topArtist, albums = albums)
            if (songs.isNotEmpty() || topArtist != null) putCache(cacheKey, res)
            res
        } catch (e: Exception) {
            e.printStackTrace()
            YtSearchResultData(emptyList(), null, emptyList())
        }
    }

    /**
     * 5. Song Info (/api/song?id=VIDEO_ID)
     */
    suspend fun getSongInfo(videoId: String): JSONObject? = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/song?id=$videoId"
        val jsonStr = httpGet(url) ?: return@withContext null
        try {
            val root = JSONObject(jsonStr)
            root.optJSONObject("data") ?: root
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 6. Lyrics (/api/lyrics?id=VIDEO_ID)
     */
    suspend fun getLyrics(videoId: String): String? = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/lyrics?id=$videoId"
        val jsonStr = httpGet(url) ?: return@withContext null
        try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONObject("data")
            data?.optString("lyrics")?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 7. Artist Discography (/api/artist?id=CHANNEL_ID)
     * Filters out unrelated actor spotlights and focuses strictly on the artist's real songs & albums.
     */
    suspend fun getArtist(channelId: String): YtArtistDetails? = withContext(Dispatchers.IO) {
        val cacheKey = "artist_$channelId"
        getCached<YtArtistDetails>(cacheKey)?.let { return@withContext it }

        val url = "$BASE_URL/artist?id=$channelId"
        val jsonStr = httpGet(url) ?: return@withContext null
        try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONObject("data") ?: return@withContext null
            val name = data.optString("name", "Artist")
            val subs = data.optString("subscribers")
            val thumb = extractThumbnail(data.optJSONArray("thumbnails"))

            val songsList = mutableListOf<YouTubeSearchResult>()
            val songsObj = data.optJSONObject("songs")
            val songResults = songsObj?.optJSONArray("results")
            if (songResults != null) {
                for (i in 0 until songResults.length()) {
                    val s = songResults.optJSONObject(i) ?: continue
                    val vid = s.optString("videoId")
                    if (vid.isBlank()) continue
                    val title = s.optString("title")
                    val artistsArr = s.optJSONArray("artists")
                    val artist = if (artistsArr != null && artistsArr.length() > 0) {
                        (0 until artistsArr.length()).mapNotNull { artistsArr.optJSONObject(it)?.optString("name") }.joinToString(", ")
                    } else name
                    val dur = s.optString("duration", "")
                    val sThumb = extractThumbnail(s.optJSONArray("thumbnails")) ?: thumb ?: ""
                    songsList.add(
                        YouTubeSearchResult(
                            videoId = vid,
                            title = title,
                            channelTitle = artist,
                            thumbnailUrl = sThumb,
                            duration = dur
                        )
                    )
                }
            }

            // Enrich with additional top tracks for large catalog artists if initial list is short
            if (songsList.size < 15 && name.isNotBlank()) {
                try {
                    val searchOutcome = com.musicdrop.app.data.youtube.YouTubeSearchRepository.search("$name songs official", maxResults = 25)
                    if (searchOutcome is com.musicdrop.app.data.youtube.YouTubeSearchOutcome.Success) {
                        val existingIds = songsList.map { it.videoId }.toSet()
                        for (song in searchOutcome.results) {
                            if (song.videoId !in existingIds) {
                                songsList.add(song)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Clean & filter albums and singles to avoid unrelated actor spotlights
            val rawAlbums = parseCardList(data.optJSONObject("albums")?.optJSONArray("results"))
            val albumsList = rawAlbums.filter { album ->
                val lowerTitle = album.title.lowercase()
                val lowerArtist = name.lowercase()
                if (lowerTitle.contains("spotlight") && !lowerTitle.contains(lowerArtist)) {
                    false
                } else {
                    true
                }
            }

            val rawSingles = parseCardList(data.optJSONObject("singles")?.optJSONArray("results"))
            val singlesList = rawSingles.filter { single ->
                val lowerTitle = single.title.lowercase()
                val lowerArtist = name.lowercase()
                if (lowerTitle.contains("spotlight") && !lowerTitle.contains(lowerArtist)) {
                    false
                } else {
                    true
                }
            }

            val details = YtArtistDetails(
                name = name,
                channelId = channelId,
                subscribers = subs,
                thumbnailUrl = thumb,
                topSongs = songsList,
                albums = albumsList,
                singles = singlesList,
                videos = emptyList() // User requested no unrelated music videos
            )
            putCache(cacheKey, details)
            details
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 8. Album Details (/api/album?id=BROWSE_ID)
     */
    suspend fun getAlbum(browseId: String): YtAlbumDetails? = withContext(Dispatchers.IO) {
        val cacheKey = "album_$browseId"
        getCached<YtAlbumDetails>(cacheKey)?.let { return@withContext it }

        val url = "$BASE_URL/album?id=$browseId"
        val jsonStr = httpGet(url) ?: return@withContext null
        try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONObject("data") ?: return@withContext null
            val title = data.optString("title", "Album")
            val artistsArr = data.optJSONArray("artists")
            val artistName = if (artistsArr != null && artistsArr.length() > 0) {
                (0 until artistsArr.length()).mapNotNull { artistsArr.optJSONObject(it)?.optString("name") }.joinToString(", ")
            } else "Various Artists"
            val year = data.optString("year")
            val trackCount = data.optInt("trackCount", 0)
            val duration = data.optString("duration")
            val thumb = extractThumbnail(data.optJSONArray("thumbnails"))

            val tracksList = mutableListOf<YouTubeSearchResult>()
            val tracksArr = data.optJSONArray("tracks")
            if (tracksArr != null) {
                for (i in 0 until tracksArr.length()) {
                    val t = tracksArr.optJSONObject(i) ?: continue
                    val vid = t.optString("videoId")
                    if (vid.isBlank()) continue
                    val tTitle = t.optString("title", "Track ${i + 1}")
                    val tArtistsArr = t.optJSONArray("artists")
                    val tArtist = if (tArtistsArr != null && tArtistsArr.length() > 0) {
                        (0 until tArtistsArr.length()).mapNotNull { tArtistsArr.optJSONObject(it)?.optString("name") }.joinToString(", ")
                    } else artistName
                    val dur = t.optString("duration", "")
                    val tThumb = extractThumbnail(t.optJSONArray("thumbnails")) ?: thumb ?: ""

                    tracksList.add(
                        YouTubeSearchResult(
                            videoId = vid,
                            title = tTitle,
                            channelTitle = tArtist,
                            thumbnailUrl = tThumb,
                            duration = dur
                        )
                    )
                }
            }

            val details = YtAlbumDetails(
                title = title,
                artistName = artistName,
                year = year,
                thumbnailUrl = thumb,
                trackCount = if (trackCount > 0) trackCount else tracksList.size,
                duration = duration,
                tracks = tracksList
            )
            if (tracksList.isNotEmpty()) putCache(cacheKey, details)
            details
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 9. Explore Categories (/api/explore)
     */
    suspend fun getExplore(): JSONObject? = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/explore"
        val jsonStr = httpGet(url) ?: return@withContext null
        try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 10. Official YouTube Music Charts (/api/charts?country=IN/US/AE/PK/SA/GB)
     */
    suspend fun getCharts(country: String = "IN", force: Boolean = false): YtChartsResponse = withContext(Dispatchers.IO) {
        val cacheKey = "charts_${country.uppercase()}"
        if (!force) {
            getCached<YtChartsResponse>(cacheKey)?.let { return@withContext it }
        }

        val url = "$BASE_URL/charts?country=${country.uppercase()}"
        val jsonStr = httpGet(url) ?: return@withContext YtChartsResponse(emptyList(), emptyList(), emptyList())
        try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONObject("data") ?: return@withContext YtChartsResponse(emptyList(), emptyList(), emptyList())

            val daily = mutableListOf<YtChartPlaylist>()
            val dailyArr = data.optJSONArray("daily")
            if (dailyArr != null) {
                for (i in 0 until dailyArr.length()) {
                    val item = dailyArr.optJSONObject(i) ?: continue
                    val title = item.optString("title")
                    val pid = item.optString("playlistId")
                    val thumb = extractThumbnail(item.optJSONArray("thumbnails")) ?: ""
                    if (pid.isNotBlank() && title.isNotBlank()) {
                        daily.add(YtChartPlaylist(title, pid, thumb))
                    }
                }
            }

            val weekly = mutableListOf<YtChartPlaylist>()
            val weeklyArr = data.optJSONArray("weekly")
            if (weeklyArr != null) {
                for (i in 0 until weeklyArr.length()) {
                    val item = weeklyArr.optJSONObject(i) ?: continue
                    val title = item.optString("title")
                    val pid = item.optString("playlistId")
                    val thumb = extractThumbnail(item.optJSONArray("thumbnails")) ?: ""
                    if (pid.isNotBlank() && title.isNotBlank()) {
                        weekly.add(YtChartPlaylist(title, pid, thumb))
                    }
                }
            }

            val videos = mutableListOf<YtChartPlaylist>()
            val videosArr = data.optJSONArray("videos")
            if (videosArr != null) {
                for (i in 0 until videosArr.length()) {
                    val item = videosArr.optJSONObject(i) ?: continue
                    val title = item.optString("title")
                    val pid = item.optString("playlistId")
                    val thumb = extractThumbnail(item.optJSONArray("thumbnails")) ?: ""
                    if (pid.isNotBlank() && title.isNotBlank()) {
                        videos.add(YtChartPlaylist(title, pid, thumb))
                    }
                }
            }

            val genres = mutableListOf<YtChartPlaylist>()
            val genresArr = data.optJSONArray("genres")
            if (genresArr != null) {
                for (i in 0 until genresArr.length()) {
                    val item = genresArr.optJSONObject(i) ?: continue
                    val title = item.optString("title")
                    val pid = item.optString("playlistId")
                    val thumb = extractThumbnail(item.optJSONArray("thumbnails")) ?: ""
                    if (pid.isNotBlank() && title.isNotBlank()) {
                        genres.add(YtChartPlaylist(title, pid, thumb))
                    }
                }
            }

            val artists = mutableListOf<YtChartArtist>()
            val artistsArr = data.optJSONArray("artists")
            if (artistsArr != null) {
                for (i in 0 until artistsArr.length()) {
                    val item = artistsArr.optJSONObject(i) ?: continue
                    val rank = item.optString("rank", "${i + 1}")
                    val title = item.optString("title")
                    val browseId = item.optString("browseId")
                    val subs = item.optString("subscribers")
                    val thumb = extractThumbnail(item.optJSONArray("thumbnails")) ?: ""
                    val trend = item.optString("trend", "equal")
                    if (browseId.isNotBlank() && title.isNotBlank()) {
                        artists.add(
                            YtChartArtist(
                                rank = rank,
                                title = title,
                                browseId = browseId,
                                subscribers = subs,
                                thumbnailUrl = thumb,
                                trend = trend
                            )
                        )
                    }
                }
            }

            val res = YtChartsResponse(daily = daily, weekly = weekly, artists = artists, videos = videos, genres = genres)
            if (artists.isNotEmpty() || daily.isNotEmpty()) putCache(cacheKey, res)
            res
        } catch (e: Exception) {
            e.printStackTrace()
            YtChartsResponse(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        }
    }

    // ── Helper Utilities ─────────────────────────────────────────────────────

    private fun parseCardList(array: JSONArray?): List<YtCardItem> {
        if (array == null) return emptyList()
        val list = mutableListOf<YtCardItem>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val title = item.optString("title", "")
            val browseId = item.optString("browseId")
            val audioPlaylistId = item.optString("audioPlaylistId")
            val thumb = extractThumbnail(item.optJSONArray("thumbnails"))
            val year = item.optString("year")
            val type = item.optString("type")
            list.add(
                YtCardItem(
                    title = title,
                    browseId = browseId,
                    audioPlaylistId = audioPlaylistId,
                    thumbnailUrl = thumb,
                    year = year,
                    type = type
                )
            )
        }
        return list
    }

    private fun extractThumbnail(thumbnails: JSONArray?): String? {
        if (thumbnails == null || thumbnails.length() == 0) return null
        val last = thumbnails.optJSONObject(thumbnails.length() - 1)
        return last?.optString("url")?.takeIf { it.isNotBlank() }
    }

    private fun httpGet(urlStr: String): String? {
        return try {
            val request = Request.Builder()
                .url(urlStr)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
