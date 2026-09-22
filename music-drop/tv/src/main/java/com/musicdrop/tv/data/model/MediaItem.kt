package com.musicdrop.tv.data.model

import android.net.Uri

enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val size: Long,
    val dateAdded: Long,
    val mimeType: String,
    val mediaType: MediaType,
    val durationMs: Long = 0L,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val isSong: Boolean = false,
    val filePath: String? = null,
    val bucketName: String? = null,
    val albumArtUri: Uri? = null,
    val isRingtone: Boolean = false
) {
    val formattedSize: String
        get() {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.1f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$size B"
            }
        }

    val formattedDuration: String
        get() {
            if (durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }

    val dateAddedMs: Long
        get() = if (dateAdded > 1_000_000_000_000L) dateAdded else dateAdded * 1000L

    val folderName: String
        get() {
            if (!bucketName.isNullOrBlank()) return bucketName
            if (!filePath.isNullOrEmpty()) {
                val parent = java.io.File(filePath).parentFile?.name
                if (!parent.isNullOrEmpty() && parent != "0" && parent != "emulated") {
                    return parent
                }
            }
            return if (album.isNotBlank() && album != "Unknown Album") album else "Camera"
        }
}
