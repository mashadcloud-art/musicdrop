package com.musicdrop.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

object RecentSearchStore {
    private const val PREFS_NAME = "musicdrop_recent_searches"
    private const val KEY_SEARCHES = "recent_queries"
    private const val MAX_HISTORY = 20

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSearches(context: Context): List<String> {
        val json = getPrefs(context).getString(KEY_SEARCHES, null) ?: return defaultSuggestions()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val item = array.optString(i)
                if (item.isNotBlank()) list.add(item)
            }
            if (list.isEmpty()) defaultSuggestions() else list
        } catch (e: Exception) {
            defaultSuggestions()
        }
    }

    fun addSearch(context: Context, query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = getSearches(context).toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val trimmedList = current.take(MAX_HISTORY)
        val array = JSONArray()
        trimmedList.forEach { array.put(it) }
        getPrefs(context).edit().putString(KEY_SEARCHES, array.toString()).apply()
    }

    fun removeSearch(context: Context, query: String) {
        val current = getSearches(context).toMutableList()
        current.remove(query)
        val array = JSONArray()
        current.forEach { array.put(it) }
        getPrefs(context).edit().putString(KEY_SEARCHES, array.toString()).apply()
    }

    fun clearAll(context: Context) {
        getPrefs(context).edit().remove(KEY_SEARCHES).apply()
    }

    private fun defaultSuggestions(): List<String> = listOf(
        "Arijit Singh",
        "The Weeknd",
        "Taylor Swift",
        "Ed Sheeran",
        "Trending Hits 2026",
        "Lo-Fi Chill Beats"
    )
}
