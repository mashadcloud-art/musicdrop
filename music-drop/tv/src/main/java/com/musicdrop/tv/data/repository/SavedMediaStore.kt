package com.musicdrop.tv.data.repository

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class SavedArtistItem(
    val browseId: String,
    val name: String,
    val thumbnailUrl: String,
    val subscribers: String = "",
    val savedAtMs: Long = System.currentTimeMillis()
)

data class SavedAlbumItem(
    val browseId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val year: String = "",
    val type: String = "Album",
    val savedAtMs: Long = System.currentTimeMillis()
)

object SavedMediaStore {
    private const val PREFS_NAME = "musicdrop_saved_media"
    private const val KEY_ARTISTS = "saved_artists_json"
    private const val KEY_ALBUMS = "saved_albums_json"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── ARTISTS ─────────────────────────────────────────────────────────────
    @Synchronized
    fun getSavedArtists(context: Context): List<SavedArtistItem> {
        val raw = prefs(context).getString(KEY_ARTISTS, null) ?: return emptyList()
        val list = mutableListOf<SavedArtistItem>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("browseId")
                if (id.isNotBlank()) {
                    list.add(
                        SavedArtistItem(
                            browseId = id,
                            name = obj.optString("name"),
                            thumbnailUrl = obj.optString("thumbnailUrl"),
                            subscribers = obj.optString("subscribers"),
                            savedAtMs = obj.optLong("savedAtMs", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.savedAtMs }
    }

    @Synchronized
    fun isArtistSaved(context: Context, browseId: String): Boolean {
        return getSavedArtists(context).any { it.browseId == browseId }
    }

    @Synchronized
    fun toggleSaveArtist(context: Context, item: SavedArtistItem): Boolean {
        val current = getSavedArtists(context).toMutableList()
        val idx = current.indexOfFirst { it.browseId == item.browseId }
        val isSaved = if (idx >= 0) {
            current.removeAt(idx)
            false
        } else {
            current.add(0, item)
            true
        }
        val array = JSONArray()
        for (a in current) {
            array.put(
                JSONObject().apply {
                    put("browseId", a.browseId)
                    put("name", a.name)
                    put("thumbnailUrl", a.thumbnailUrl)
                    put("subscribers", a.subscribers)
                    put("savedAtMs", a.savedAtMs)
                }
            )
        }
        prefs(context).edit().putString(KEY_ARTISTS, array.toString()).apply()
        return isSaved
    }

    // ── ALBUMS ──────────────────────────────────────────────────────────────
    @Synchronized
    fun getSavedAlbums(context: Context): List<SavedAlbumItem> {
        val raw = prefs(context).getString(KEY_ALBUMS, null) ?: return emptyList()
        val list = mutableListOf<SavedAlbumItem>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("browseId")
                if (id.isNotBlank()) {
                    list.add(
                        SavedAlbumItem(
                            browseId = id,
                            title = obj.optString("title"),
                            artist = obj.optString("artist"),
                            thumbnailUrl = obj.optString("thumbnailUrl"),
                            year = obj.optString("year"),
                            type = obj.optString("type", "Album"),
                            savedAtMs = obj.optLong("savedAtMs", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.savedAtMs }
    }

    @Synchronized
    fun isAlbumSaved(context: Context, browseId: String): Boolean {
        return getSavedAlbums(context).any { it.browseId == browseId }
    }

    @Synchronized
    fun toggleSaveAlbum(context: Context, item: SavedAlbumItem): Boolean {
        val current = getSavedAlbums(context).toMutableList()
        val idx = current.indexOfFirst { it.browseId == item.browseId }
        val isSaved = if (idx >= 0) {
            current.removeAt(idx)
            false
        } else {
            current.add(0, item)
            true
        }
        val array = JSONArray()
        for (al in current) {
            array.put(
                JSONObject().apply {
                    put("browseId", al.browseId)
                    put("title", al.title)
                    put("artist", al.artist)
                    put("thumbnailUrl", al.thumbnailUrl)
                    put("year", al.year)
                    put("type", al.type)
                    put("savedAtMs", al.savedAtMs)
                }
            )
        }
        prefs(context).edit().putString(KEY_ALBUMS, array.toString()).apply()
        return isSaved
    }
}
