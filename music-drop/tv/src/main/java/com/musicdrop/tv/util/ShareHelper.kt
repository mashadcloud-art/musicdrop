package com.musicdrop.tv.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.musicdrop.tv.data.model.MediaItem

object ShareHelper {

    fun shareSingle(context: Context, item: MediaItem) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share ${item.name} via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareMultiple(context: Context, items: List<MediaItem>) {
        if (items.isEmpty()) return
        if (items.size == 1) {
            shareSingle(context, items.first())
            return
        }

        val uris = ArrayList<Uri>(items.map { it.uri })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share ${items.size} files via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
