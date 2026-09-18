package com.musicdrop.app.data.youtube

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the resolved-stream cache (keyed by YouTube video id) to disk so a track
 * that's already been played once — even in a previous app session — plays instantly
 * instead of re-running the full pWeb/NewPipe extraction race again. Before this, the
 * in-memory `streamCache` in MainViewModel only helped an exact replay within the same
 * process; closing and reopening the app (or just letting the process die in the
 * background, which Android does constantly) meant every single play, even of a song
 * played five minutes ago, paid the full multi-second extraction cost again.
 *
 * Every googlevideo stream URL YouTube hands back carries its own real "expire=<unix
 * seconds>" query param, so a persisted entry is dropped the moment YouTube itself
 * would reject that URL anyway — no separate invalidation logic needed. See
 * [expiryFromUrl]. Capped to the most recent [MAX_ENTRIES] so the SharedPreferences
 * blob never grows unbounded from a long history of different songs played.
 */
object StreamCacheStore {
    private const val PREFS = "stream_cache_prefs"
    private const val KEY = "stream_cache_v1"
    private const val MAX_ENTRIES = 150

    /** Conservative fallback when a URL doesn't carry its own "expire=" param. */
    private const val FALLBACK_TTL_MS = 5 * 60 * 60 * 1000L

    /** Loads every still-fresh cached entry. Safe to call from any thread; best-effort. */
    fun load(context: Context): Map<String, PWebExtractor.StreamInfo> {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY, null) ?: return emptyMap()
            val arr = JSONArray(raw)
            val now = System.currentTimeMillis()
            val out = LinkedHashMap<String, PWebExtractor.StreamInfo>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val expiresAt = o.optLong("expiresAtMs", 0L)
                val videoId = o.optString("videoId")
                if (expiresAt <= now || videoId.isBlank()) continue // don't even load dead entries
                out[videoId] = PWebExtractor.StreamInfo(
                    url = o.optString("url"),
                    mimeType = o.optString("mimeType", "audio/mp4"),
                    title = o.optString("title"),
                    author = o.optString("author"),
                    thumbnailUrl = o.optString("thumbnailUrl"),
                    durationMs = o.optLong("durationMs", 0L),
                    videoId = videoId,
                    expiresAtMs = expiresAt
                )
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Writes the given cache snapshot to disk (only entries that carry a real expiry and
     * aren't already stale — anything else was never meant to be persisted). Called
     * synchronously after every cache write from MainViewModel; SharedPreferences.apply()
     * is itself async, so this doesn't block playback.
     */
    fun save(context: Context, cache: Map<String, PWebExtractor.StreamInfo>) {
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val fresh = cache.values
                .filter { it.expiresAtMs > now }
                .sortedByDescending { it.expiresAtMs }
                .take(MAX_ENTRIES)
            val arr = JSONArray()
            fresh.forEach { info ->
                arr.put(
                    JSONObject().apply {
                        put("videoId", info.videoId)
                        put("url", info.url)
                        put("mimeType", info.mimeType)
                        put("title", info.title)
                        put("author", info.author)
                        put("thumbnailUrl", info.thumbnailUrl)
                        put("durationMs", info.durationMs)
                        put("expiresAtMs", info.expiresAtMs)
                    }
                )
            }
            prefs.edit().putString(KEY, arr.toString()).apply()
        } catch (e: Exception) {
            // Best-effort — a persistence failure should never break playback.
        }
    }

    /**
     * Extracts YouTube's own "expire=<unix seconds>" param from a googlevideo stream URL
     * (this is the real deadline the CDN itself enforces — reusing it means the cache
     * never hands back a URL YouTube would already be rejecting). Falls back to a
     * conservative 5-hour TTL for any URL that doesn't carry one.
     */
    fun expiryFromUrl(url: String): Long {
        val expireSec = Regex("[?&]expire=(\\d+)").find(url)?.groupValues?.get(1)?.toLongOrNull()
        return if (expireSec != null) expireSec * 1000L else System.currentTimeMillis() + FALLBACK_TTL_MS
    }
}
