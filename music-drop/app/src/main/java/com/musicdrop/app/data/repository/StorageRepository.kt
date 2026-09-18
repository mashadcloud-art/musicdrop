package com.musicdrop.app.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.musicdrop.app.data.model.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageRepository(private val context: Context) {

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val path = Environment.getDataDirectory().path
        val stat = StatFs(path)
        val blockSize = stat.blockSizeLong
        val totalBytes = stat.blockCountLong * blockSize
        val availableBytes = stat.availableBlocksLong * blockSize

        var photosBytes = 0L
        var photosCount = 0
        var videosBytes = 0L
        var videosCount = 0
        var audioBytes = 0L
        var audioCount = 0
        var docsBytes = 0L
        var docsCount = 0

        // 1. Photos
        try {
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media.SIZE),
                null,
                null,
                null
            )
            cursor?.use {
                val sizeCol = it.getColumnIndex(MediaStore.Images.Media.SIZE)
                photosCount = it.count
                while (it.moveToNext()) {
                    if (sizeCol != -1) {
                        photosBytes += it.getLong(sizeCol)
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Videos
        try {
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Video.Media.SIZE),
                null,
                null,
                null
            )
            cursor?.use {
                val sizeCol = it.getColumnIndex(MediaStore.Video.Media.SIZE)
                videosCount = it.count
                while (it.moveToNext()) {
                    if (sizeCol != -1) {
                        videosBytes += it.getLong(sizeCol)
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Audio
        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media.SIZE),
                null,
                null,
                null
            )
            cursor?.use {
                val sizeCol = it.getColumnIndex(MediaStore.Audio.Media.SIZE)
                audioCount = it.count
                while (it.moveToNext()) {
                    if (sizeCol != -1) {
                        audioBytes += it.getLong(sizeCol)
                    }
                }
            }
        } catch (_: Exception) {}

        // 4. Documents / Downloads / APKs / Archives
        val scannedPaths = mutableSetOf<String>()
        try {
            val docSelection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE} != ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} " +
                    "AND ${MediaStore.Files.FileColumns.MEDIA_TYPE} != ${MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO} " +
                    "AND ${MediaStore.Files.FileColumns.MEDIA_TYPE} != ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}) " +
                    "AND (${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.pdf' " +
                    "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.doc%' " +
                    "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.xls%' " +
                    "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.ppt%' " +
                    "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.txt' " +
                    "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.rtf' " +
                    "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.csv' " +
                    "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.zip' " +
                    "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.rar' " +
                    "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.7z' " +
                    "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.apk' " +
                    "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.epub' " +
                    "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.json' " +
                    "OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'application/%' " +
                    "OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'text/%')"

            val cursor = context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                arrayOf(MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATA),
                docSelection,
                null,
                null
            )
            cursor?.use {
                val sizeCol = it.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val dataCol = it.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                while (it.moveToNext()) {
                    val pathStr = if (dataCol != -1) it.getString(dataCol) else null
                    if (pathStr != null) scannedPaths.add(pathStr)
                    docsCount++
                    if (sizeCol != -1) {
                        docsBytes += it.getLong(sizeCol)
                    }
                }
            }
        } catch (_: Exception) {}

        // Supplementary scan of Download and Documents public directories
        try {
            val candidateDirs = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            )
            val validExts = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "zip", "rar", "7z", "apk", "epub", "json")
            for (dir in candidateDirs) {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.filter { it.isFile && !it.name.startsWith(".") }?.forEach { file ->
                        val ext = file.extension.lowercase()
                        if (ext in validExts && scannedPaths.add(file.absolutePath)) {
                            docsCount++
                            docsBytes += file.length()
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        StorageStats(
            totalBytes = totalBytes,
            availableBytes = availableBytes,
            photosBytes = photosBytes,
            videosBytes = videosBytes,
            audioBytes = audioBytes,
            docsBytes = docsBytes,
            photosCount = photosCount,
            videosCount = videosCount,
            audioCount = audioCount,
            docsCount = docsCount
        )
    }
}
