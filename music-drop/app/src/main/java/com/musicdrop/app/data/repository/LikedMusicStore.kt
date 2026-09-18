package com.musicdrop.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.musicdrop.app.data.model.UnifiedTrack
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import org.json.JSONArray
import org.json.JSONObject

data class LikedMusicItem(
    val key: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val duration: String,
    val sourceName: String,
    val likedAtMs: Long
) {
    fun toUnifiedTrack(): UnifiedTrack {
        return UnifiedTrack.Youtube(
            YouTubeSearchResult(
                videoId = key.removePrefix("yt:").removePrefix("sv:").removePrefix("vm:"),
                title = title,
                channelTitle = artist,
                thumbnailUrl = coverUrl,
                duration = duration
            )
        )
    }
}

object LikedMusicStore {
    private const val PREFS_NAME = "musicdrop_liked_music"
    private const val KEY_LIKED = "liked_json"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getAll(context: Context): List<LikedMusicItem> {
        val prefs = prefs(context)
        val raw = prefs.getString(KEY_LIKED, null) ?: return emptyList()
        val list = mutableListOf<LikedMusicItem>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                list.add(
                    LikedMusicItem(
                        key = obj.optString("key"),
                        title = obj.optString("title", "Untitled Track"),
                        artist = obj.optString("artist", "Unknown Artist"),
                        coverUrl = obj.optString("coverUrl", ""),
                        duration = obj.optString("duration", ""),
                        sourceName = obj.optString("sourceName", "YouTube"),
                        likedAtMs = obj.optLong("likedAtMs", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.likedAtMs }
    }

    @Synchronized
    fun isLiked(context: Context, key: String): Boolean {
        return getAll(context).any { it.key == key }
    }

    @Synchronized
    fun toggleLike(context: Context, item: LikedMusicItem): Boolean {
        val current = getAll(context).toMutableList()
        val existingIndex = current.indexOfFirst { it.key == item.key }
        val isNowLiked = if (existingIndex >= 0) {
            current.removeAt(existingIndex)
            false
        } else {
            current.add(0, item)
            true
        }
        saveAll(context, current)
        return isNowLiked
    }

    @Synchronized
    fun remove(context: Context, key: String) {
        val current = getAll(context).filter { it.key != key }
        saveAll(context, current)
    }

    private fun saveAll(context: Context, items: List<LikedMusicItem>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("key", item.key)
                put("title", item.title)
                put("artist", item.artist)
                put("coverUrl", item.coverUrl)
                put("duration", item.duration)
                put("sourceName", item.sourceName)
                put("likedAtMs", item.likedAtMs)
            }
            array.put(obj)
        }
        prefs(context).edit().putString(KEY_LIKED, array.toString()).apply()
    }
}
