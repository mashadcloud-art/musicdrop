package com.musicdrop.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.model.MediaType
import com.musicdrop.app.data.model.UnifiedTrack
import com.musicdrop.app.data.youtube.YouTubeSearchResult
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device "recently played" history across every music source at once — same
 * SharedPreferences+JSON pattern as [MusicFavoritesStore], but source-agnostic:
 * each entry is tagged with a hidden "type" field used only to reconstruct the
 * right [UnifiedTrack] subtype, never surfaced in the UI.
 */
object RecentPlaysStore {
    private const val PREFS_NAME = "musicdrop_recent_plays"
    private const val KEY_ENTRIES = "entries_json"
    private const val MAX_ENTRIES = 40

    /** Adds/moves [track] to the front of recent history (de-duped by key). */
    @Synchronized
    fun record(context: Context, track: UnifiedTrack) {
        val prefs = prefs(context)
        val array = readArray(prefs)
        val rebuilt = JSONArray()
        // Drop any existing entry for this key so it moves to the front instead of duplicating.
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (obj.optString("key") != track.key) rebuilt.put(obj)
        }

        val entry = JSONObject().apply {
            put("key", track.key)
            put("playedAtMs", System.currentTimeMillis())
            when (track) {
                is UnifiedTrack.Youtube -> {
                    put("type", "yt")
                    put("videoId", track.result.videoId)
                    put("title", track.result.title)
                    put("channelTitle", track.result.channelTitle)
                    put("thumbnailUrl", track.result.thumbnailUrl)
                    put("duration", track.result.duration)
                }
                is UnifiedTrack.Saavn -> {
                    put("type", "sv")
                    put("id", track.item.id)
                    put("uri", track.item.uri.toString())
                    put("name", track.item.name)
                    put("artist", track.item.artist)
                    put("mimeType", track.item.mimeType)
                    put("albumArtUri", track.item.albumArtUri?.toString().orEmpty())
                }
                is UnifiedTrack.Vimeo -> {
                    put("type", "vm")
                    put("id", track.video.id)
                    put("title", track.video.title)
                    put("authorName", track.video.authorName)
                    put("thumbnailUrl", track.video.thumbnailUrl)
                    put("duration", track.video.duration)
                }
                is UnifiedTrack.Local -> {
                    put("type", "local")
                    put("title", track.title)
                    put("artist", track.artist)
                    put("thumbnailUrl", track.thumbnailUrl)
                    put("duration", track.duration)
                    put("filePath", track.filePath)
                }
            }
        }

        // Newest first; prepend then cap.
        val finalArray = JSONArray()
        finalArray.put(entry)
        for (i in 0 until rebuilt.length()) finalArray.put(rebuilt.get(i))
        while (finalArray.length() > MAX_ENTRIES) finalArray.remove(finalArray.length() - 1)

        prefs.edit().putString(KEY_ENTRIES, finalArray.toString()).apply()
    }

    /** Most recently played first. */
    fun getAll(context: Context): List<UnifiedTrack> {
        val array = readArray(prefs(context))
        val result = mutableListOf<UnifiedTrack>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val track = obj.toUnifiedTrackOrNull() ?: continue
            result.add(track)
        }
        return result
    }

    private fun JSONObject.toUnifiedTrackOrNull(): UnifiedTrack? = try {
        when (optString("type")) {
            "yt" -> UnifiedTrack.Youtube(
                YouTubeSearchResult(
                    videoId      = optString("videoId"),
                    title        = optString("title", "Untitled"),
                    channelTitle = optString("channelTitle", ""),
                    thumbnailUrl = optString("thumbnailUrl", ""),
                    duration     = optString("duration", "")
                )
            ).takeIf { it.result.videoId.isNotBlank() }

            "sv" -> UnifiedTrack.Saavn(
                MediaItem(
                    id          = optLong("id"),
                    uri         = Uri.parse(optString("uri")),
                    name        = optString("name", "Untitled"),
                    size        = 0L,
                    dateAdded   = System.currentTimeMillis() / 1000,
                    mimeType    = optString("mimeType", "audio/mpeg"),
                    mediaType   = MediaType.AUDIO,
                    artist      = optString("artist", ""),
                    isSong      = true,
                    bucketName  = "JioSaavn Online",
                    albumArtUri = optString("albumArtUri").takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                )
            ).takeIf { optString("uri").isNotBlank() }

            "vm" -> UnifiedTrack.Vimeo(
                VimeoRepository.VimeoVideo(
                    id           = optString("id"),
                    title        = optString("title", "Untitled"),
                    description  = "",
                    authorName   = optString("authorName", ""),
                    thumbnailUrl = optString("thumbnailUrl", ""),
                    duration     = optString("duration", ""),
                    viewCount    = ""
                )
            ).takeIf { optString("id").isNotBlank() }

            "local" -> UnifiedTrack.Local(
                key = optString("key"),
                title = optString("title", "Untitled"),
                artist = optString("artist", "Unknown Artist"),
                thumbnailUrl = optString("thumbnailUrl", ""),
                duration = optString("duration", ""),
                filePath = optString("filePath", "")
            )

            else -> null
        }
    } catch (e: Exception) {
        null
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
