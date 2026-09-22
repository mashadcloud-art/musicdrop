package com.musicdrop.tv.data.repository

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists hidden/deleted track keys so users can remove unwanted songs from category feeds,
 * recommendations, or shelves. Stored in SharedPreferences as a StringSet.
 */
object HiddenTracksStore {
    private const val PREFS_NAME = "musicdrop_hidden_tracks"
    private const val KEY_HIDDEN_KEYS = "hidden_keys"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHiddenKeys(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_HIDDEN_KEYS, emptySet()) ?: emptySet()
    }

    @Synchronized
    fun hideTrack(context: Context, key: String) {
        if (key.isBlank()) return
        val current = getHiddenKeys(context).toMutableSet()
        current.add(key)
        prefs(context).edit().putStringSet(KEY_HIDDEN_KEYS, current).apply()
    }

    @Synchronized
    fun unhideTrack(context: Context, key: String) {
        if (key.isBlank()) return
        val current = getHiddenKeys(context).toMutableSet()
        current.remove(key)
        prefs(context).edit().putStringSet(KEY_HIDDEN_KEYS, current).apply()
    }

    fun isHidden(context: Context, key: String): Boolean {
        return getHiddenKeys(context).contains(key)
    }

    @Synchronized
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_HIDDEN_KEYS).apply()
    }
}
