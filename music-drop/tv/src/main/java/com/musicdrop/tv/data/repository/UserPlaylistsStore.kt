package com.musicdrop.tv.data.repository

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class UserPlaylistItem(
    val id: String,
    val name: String,
    val description: String,
    val createdAtMs: Long,
    val tracks: List<LikedMusicItem>
)

object UserPlaylistsStore {
    private const val PREFS_NAME = "musicdrop_user_playlists"
    private const val KEY_PLAYLISTS = "playlists_json"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getAll(context: Context): List<UserPlaylistItem> {
        val prefs = prefs(context)
        val raw = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        val list = mutableListOf<UserPlaylistItem>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val trackArray = obj.optJSONArray("tracks") ?: JSONArray()
                val trackList = mutableListOf<LikedMusicItem>()
                for (j in 0 until trackArray.length()) {
                    val t = trackArray.optJSONObject(j) ?: continue
                    trackList.add(
                        LikedMusicItem(
                            key = t.optString("key"),
                            title = t.optString("title"),
                            artist = t.optString("artist"),
                            coverUrl = t.optString("coverUrl"),
                            duration = t.optString("duration"),
                            sourceName = t.optString("sourceName", "YouTube"),
                            likedAtMs = t.optLong("likedAtMs", 0L)
                        )
                    )
                }
                list.add(
                    UserPlaylistItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Untitled Playlist"),
                        description = obj.optString("description", ""),
                        createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis()),
                        tracks = trackList
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.createdAtMs }
    }

    @Synchronized
    fun create(context: Context, name: String, description: String = ""): UserPlaylistItem {
        val newItem = UserPlaylistItem(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "My Playlist" },
            description = description,
            createdAtMs = System.currentTimeMillis(),
            tracks = emptyList()
        )
        val current = getAll(context).toMutableList()
        current.add(0, newItem)
        saveAll(context, current)
        return newItem
    }

    @Synchronized
    fun delete(context: Context, playlistId: String) {
        val current = getAll(context).filter { it.id != playlistId }
        saveAll(context, current)
    }

    @Synchronized
    fun addTrack(context: Context, playlistId: String, track: LikedMusicItem) {
        val current = getAll(context).toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val pl = current[index]
            if (pl.tracks.none { it.key == track.key }) {
                val updatedTracks = listOf(track) + pl.tracks
                current[index] = pl.copy(tracks = updatedTracks)
                saveAll(context, current)
            }
        }
    }

    @Synchronized
    fun removeTrack(context: Context, playlistId: String, trackKey: String) {
        val current = getAll(context).toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val pl = current[index]
            val updatedTracks = pl.tracks.filter { it.key != trackKey }
            current[index] = pl.copy(tracks = updatedTracks)
            saveAll(context, current)
        }
    }

    private fun saveAll(context: Context, playlists: List<UserPlaylistItem>) {
        val array = JSONArray()
        for (pl in playlists) {
            val obj = JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                put("description", pl.description)
                put("createdAtMs", pl.createdAtMs)
                val trackArray = JSONArray()
                for (t in pl.tracks) {
                    trackArray.put(
                        JSONObject().apply {
                            put("key", t.key)
                            put("title", t.title)
                            put("artist", t.artist)
                            put("coverUrl", t.coverUrl)
                            put("duration", t.duration)
                            put("sourceName", t.sourceName)
                            put("likedAtMs", t.likedAtMs)
                        }
                    )
                }
                put("tracks", trackArray)
            }
            array.put(obj)
        }
        prefs(context).edit().putString(KEY_PLAYLISTS, array.toString()).apply()
    }
}
