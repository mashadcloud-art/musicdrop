package com.musicdrop.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device list of favorited YouTube tracks (videoId + title + channel + thumbnail
 * only — never any audio/video file). Mirrors [SentFilesLog]'s SharedPreferences+JSON
 * pattern rather than pulling in Room for one small list, matching how the rest of
 * this app handles small local stores. Favoriting just means "remember this videoId
 * so it can be searched-and-replayed quickly" — nothing is downloaded or cached.
 */
object MusicFavoritesStore {
    private const val PREFS_NAME = "filedrop_music_favorites"
    private const val KEY_ENTRIES = "entries_json"

    fun isFavorite(context: Context, videoId: String): Boolean =
        getAll(context).any { it.videoId == videoId }

    /** Adds if not already present, or removes if it is. Returns the new favorited state. */
    @Synchronized
    fun toggle(context: Context, result: YouTubeSearchResult): Boolean {
        val prefs = prefs(context)
        val array = readArray(prefs)
        val existingIndex = (0 until array.length()).firstOrNull { i ->
            array.optJSONObject(i)?.optString("videoId") == result.videoId
        }

        if (existingIndex != null) {
            val rebuilt = JSONArray()
            for (i in 0 until array.length()) if (i != existingIndex) rebuilt.put(array.get(i))
            prefs.edit().putString(KEY_ENTRIES, rebuilt.toString()).apply()
            return false
        }

        val obj = JSONObject()
        obj.put("videoId", result.videoId)
        obj.put("title", result.title)
        obj.put("channelTitle", result.channelTitle)
        obj.put("thumbnailUrl", result.thumbnailUrl)
        obj.put("savedAtMs", System.currentTimeMillis())
        array.put(obj)
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
        return true
    }

    /** Most recently favorited first. */
    fun getAll(context: Context): List<YouTubeSearchResult> {
        val array = readArray(prefs(context))
        val result = mutableListOf<Pair<Long, YouTubeSearchResult>>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val videoId = obj.optString("videoId")
            if (videoId.isBlank()) continue
            result.add(
                obj.optLong("savedAtMs", 0L) to YouTubeSearchResult(
                    videoId = videoId,
                    title = obj.optString("title", "Untitled"),
                    channelTitle = obj.optString("channelTitle", ""),
                    thumbnailUrl = obj.optString("thumbnailUrl", "")
                )
            )
        }
        return result.sortedByDescending { it.first }.map { it.second }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readArray(prefs: SharedPreferences): JSONArray {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }
}
