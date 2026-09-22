package com.musicdrop.tv.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.musicdrop.tv.data.model.MediaItem
import com.musicdrop.tv.data.model.MediaType
import org.json.JSONArray
import org.json.JSONObject

/**
 * One entry in the sent-files history: which file, and who it went to.
 */
data class SentRecord(val item: MediaItem, val peerName: String)

/**
 * On-device history of files this device has successfully sent.
 *
 * Unlike received files — which [com.musicdrop.tv.data.repository.MediaStoreRepository]
 * discovers by re-scanning the Download/FileDrop folder, so no manual bookkeeping is
 * needed — a sent file's source stays wherever it always lived (gallery, Downloads, an
 * installed app's APK, …), leaving no trail of "this was sent" anywhere on disk. This
 * small SharedPreferences-backed log is that trail, written once per transfer right
 * after the receiver acknowledges it (see [com.musicdrop.tv.data.p2p.P2PTransferEngine]'s
 * `finishSend`), so the Recent screen can show a Sent tab at all.
 *
 * Capped at [MAX_ENTRIES] so a device that sends a lot of files doesn't grow this file
 * forever — oldest entries are dropped first.
 */
object SentFilesLog {
    private const val PREFS_NAME = "filedrop_sent_log"
    private const val KEY_ENTRIES = "entries_json"
    private const val MAX_ENTRIES = 400

    /** Records a completed, acknowledged send. */
    @Synchronized
    fun record(context: Context, items: List<MediaItem>, peerName: String) {
        if (items.isEmpty()) return
        val prefs = prefs(context)
        val array = readArray(prefs)
        val sentAt = System.currentTimeMillis()

        for ((offset, item) in items.withIndex()) {
            val obj = JSONObject()
            obj.put("name", item.name)
            obj.put("size", item.size)
            obj.put("mimeType", item.mimeType)
            obj.put("mediaType", item.mediaType.name)
            obj.put("filePath", item.filePath ?: "")
            obj.put("uri", item.uri.toString())
            obj.put("peerName", peerName)
            // Offset each item in the same batch by a millisecond so a stable sort by
            // sentAtMs keeps the original send order instead of collapsing to one tie.
            obj.put("sentAtMs", sentAt + offset)
            array.put(obj)
        }

        while (array.length() > MAX_ENTRIES) {
            array.remove(0)
        }

        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    /** Most recently sent items first, paired with who they were sent to. */
    fun getAllRecords(context: Context): List<SentRecord> {
        val array = readArray(prefs(context))
        val result = mutableListOf<SentRecord>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            try {
                val sentAtMs = obj.optLong("sentAtMs", 0L)
                val mediaType = try {
                    MediaType.valueOf(obj.optString("mediaType", "DOCUMENT"))
                } catch (_: Exception) {
                    MediaType.DOCUMENT
                }
                val item = MediaItem(
                    id = sentAtMs,
                    uri = Uri.parse(obj.optString("uri", "")),
                    name = obj.optString("name", "File"),
                    size = obj.optLong("size", 0L),
                    dateAdded = sentAtMs,
                    mimeType = obj.optString("mimeType", "*/*"),
                    mediaType = mediaType,
                    filePath = obj.optString("filePath", "").ifBlank { null }
                )
                result.add(SentRecord(item, obj.optString("peerName", "device")))
            } catch (_: Exception) {
                // Skip a corrupted entry rather than losing the whole log.
            }
        }
        return result.sortedByDescending { it.item.dateAdded }
    }

    /** Convenience for call sites that only need the plain file list. */
    fun getAll(context: Context): List<MediaItem> = getAllRecords(context).map { it.item }

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
