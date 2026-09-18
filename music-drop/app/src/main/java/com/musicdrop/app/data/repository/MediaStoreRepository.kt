package com.musicdrop.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepository(private val context: Context) {

    suspend fun getRecentMedia(limit: Int = 50): List<MediaItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MediaItem>()
        result.addAll(getPhotos(limit = limit / 2))
        result.addAll(getVideos(limit = limit / 4))
        result.addAll(getAudioTracks(limit = limit / 4))
        result.sortedByDescending { it.dateAdded }.take(limit)
    }

    suspend fun getPhotos(limit: Int = 50, offset: Int = 0): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val args = Bundle().apply {
                    putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
                    putInt(android.content.ContentResolver.QUERY_ARG_OFFSET, offset)
                    putStringArray(android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_ADDED))
                    putInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION, android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                }
                context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, args, null)
            } else {
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    "$sortOrder LIMIT $limit OFFSET $offset"
                )
            }

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val dataCol = it.getColumnIndex(MediaStore.Images.Media.DATA)
                val bucketCol = it.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val filePath = if (dataCol != -1) it.getString(dataCol) else null
                    val bucketName = if (bucketCol != -1) it.getString(bucketCol) else null
                    list.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = it.getString(nameCol) ?: "Photo",
                            size = it.getLong(sizeCol),
                            dateAdded = it.getLong(dateCol),
                            mimeType = it.getString(mimeCol) ?: "image/jpeg",
                            mediaType = MediaType.IMAGE,
                            filePath = filePath,
                            bucketName = bucketName
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun getVideos(limit: Int = 50, offset: Int = 0): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        try {
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val args = Bundle().apply {
                    putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
                    putInt(android.content.ContentResolver.QUERY_ARG_OFFSET, offset)
                    putStringArray(android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Video.Media.DATE_ADDED))
                    putInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION, android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                }
                context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, args, null)
            } else {
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${MediaStore.Video.Media.DATE_ADDED} DESC LIMIT $limit OFFSET $offset"
                )
            }

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val durCol = it.getColumnIndex(MediaStore.Video.Media.DURATION)
                val dataCol = it.getColumnIndex(MediaStore.Video.Media.DATA)
                val bucketCol = it.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val duration = if (durCol != -1) it.getLong(durCol) else 0L
                    val filePath = if (dataCol != -1) it.getString(dataCol) else null
                    val bucketName = if (bucketCol != -1) it.getString(bucketCol) else null
                    list.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = it.getString(nameCol) ?: "Video",
                            size = it.getLong(sizeCol),
                            dateAdded = it.getLong(dateCol),
                            mimeType = it.getString(mimeCol) ?: "video/mp4",
                            mediaType = MediaType.VIDEO,
                            durationMs = duration,
                            filePath = filePath,
                            bucketName = bucketName
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun getAudioTracks(limit: Int = 100, offset: Int = 0): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_NOTIFICATION,
            MediaStore.Audio.Media.IS_ALARM,
            MediaStore.Audio.Media.DATA
        )

        try {
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val args = Bundle().apply {
                    putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
                    putInt(android.content.ContentResolver.QUERY_ARG_OFFSET, offset)
                    putStringArray(android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Audio.Media.DATE_ADDED))
                    putInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION, android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                }
                context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, args, null)
            } else {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${MediaStore.Audio.Media.DATE_ADDED} DESC LIMIT $limit OFFSET $offset"
                )
            }

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val durCol = it.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val artistCol = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val isMusicCol = it.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)
                val isRingtoneCol = it.getColumnIndex(MediaStore.Audio.Media.IS_RINGTONE)
                val isNotifCol = it.getColumnIndex(MediaStore.Audio.Media.IS_NOTIFICATION)
                val isAlarmCol = it.getColumnIndex(MediaStore.Audio.Media.IS_ALARM)
                val dataCol = it.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val name = it.getString(nameCol) ?: "Audio Track"
                    val duration = if (durCol != -1) it.getLong(durCol) else 0L
                    val artist = if (artistCol != -1) it.getString(artistCol) ?: "Unknown Artist" else "Unknown Artist"
                    val album = if (albumCol != -1) it.getString(albumCol) ?: "Unknown Album" else "Unknown Album"
                    val albumId = if (albumIdCol != -1) it.getLong(albumIdCol) else -1L
                    val isMusicFlag = if (isMusicCol != -1) it.getInt(isMusicCol) == 1 else true
                    val filePath = if (dataCol != -1) it.getString(dataCol) else null

                    // Album Art URI (direct media content URI resolved via AudioCoverFetcher)
                    val albumArtUri = contentUri

                    // Detect Ringtones / Alarms / Notifications
                    val isRingtone = (if (isRingtoneCol != -1) it.getInt(isRingtoneCol) == 1 else false) ||
                            (if (isNotifCol != -1) it.getInt(isNotifCol) == 1 else false) ||
                            (if (isAlarmCol != -1) it.getInt(isAlarmCol) == 1 else false) ||
                            (filePath?.contains("/Ringtones/", ignoreCase = true) == true) ||
                            (filePath?.contains("/Notifications/", ignoreCase = true) == true) ||
                            (filePath?.contains("/Alarms/", ignoreCase = true) == true) ||
                            (filePath?.contains("/sound/", ignoreCase = true) == true) ||
                            (duration in 1..24999L && !name.contains("song", ignoreCase = true) && !name.contains("track", ignoreCase = true))

                    // SMART AUDIO SEGREGATION:
                    // Telegram and Snaptube audio are explicitly treated as true music songs
                    val isTelegramOrSnaptube = filePath?.contains("Telegram", ignoreCase = true) == true ||
                            filePath?.contains("Snaptube", ignoreCase = true) == true
                    val isVoiceNotePattern = name.startsWith("PTT-", ignoreCase = true) ||
                            name.startsWith("AUD-", ignoreCase = true) ||
                            name.startsWith("REC_", ignoreCase = true) ||
                            (name.startsWith("Voice", ignoreCase = true) && duration < 60000L) ||
                            (name.contains("WhatsApp", ignoreCase = true) && duration < 45000L)

                    val isTrueSong = !isRingtone && !isVoiceNotePattern && (isMusicFlag || isTelegramOrSnaptube || duration >= 30000L)

                    list.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            size = it.getLong(sizeCol),
                            dateAdded = it.getLong(dateCol),
                            mimeType = it.getString(mimeCol) ?: "audio/mpeg",
                            mediaType = MediaType.AUDIO,
                            durationMs = duration,
                            artist = artist,
                            album = album,
                            isSong = isTrueSong,
                            filePath = filePath,
                            albumArtUri = albumArtUri,
                            isRingtone = isRingtone
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // Supplementary scan: Direct folder search for Telegram and Snaptube Audio
        try {
            val extStorage = android.os.Environment.getExternalStorageDirectory()
            val candidateDirs = listOf(
                java.io.File(extStorage, "Telegram/Telegram Audio"),
                java.io.File(extStorage, "Android/media/org.telegram.messenger/Telegram/Telegram Audio"),
                java.io.File(extStorage, "snaptube/download/Snaptube Audio"),
                java.io.File(extStorage, "Snaptube/Audio"),
                java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "Telegram"),
                java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "Snaptube"),
                java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC), "Snaptube")
            )
            val audioExts = setOf("mp3", "m4a", "wav", "aac", "flac", "opus", "ogg")
            val seenPaths = list.mapNotNull { it.filePath?.lowercase() }.toMutableSet()

            for (dir in candidateDirs) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles()?.filter { it.isFile && !it.name.startsWith(".") } ?: emptyList()
                    for (file in files) {
                        val ext = file.extension.lowercase()
                        if (ext in audioExts && seenPaths.add(file.absolutePath.lowercase())) {
                            val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "audio/mpeg"
                            var parsedArtist = "Unknown Artist"
                            var parsedTitle = file.nameWithoutExtension
                            var parsedDur = 0L

                            try {
                                val mmr = android.media.MediaMetadataRetriever()
                                mmr.setDataSource(file.absolutePath)
                                mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { parsedArtist = it }
                                mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { parsedTitle = it }
                                mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let { parsedDur = it }
                                mmr.release()
                            } catch (_: Exception) {}

                            list.add(
                                MediaItem(
                                    id = file.hashCode().toLong(),
                                    uri = Uri.fromFile(file),
                                    name = if (parsedTitle.isNotBlank()) parsedTitle else file.name,
                                    size = file.length(),
                                    dateAdded = file.lastModified() / 1000L,
                                    mimeType = mime,
                                    mediaType = MediaType.AUDIO,
                                    durationMs = parsedDur,
                                    artist = parsedArtist,
                                    album = dir.name,
                                    isSong = true,
                                    filePath = file.absolutePath,
                                    bucketName = dir.name,
                                    isRingtone = false
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        list.sortedByDescending { it.dateAdded }
    }

    suspend fun getDocuments(limit: Int = 300, offset: Int = 0): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        val seenKeys = mutableSetOf<String>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA
        )

        // Use the reliable legacy selection string — QUERY_ARG_SQL_SELECTION inside a Bundle
        // is NOT supported on many OEMs running Android O-Q, so we always use the classic API.
        val docSelection = "(" +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.pdf' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.doc' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.docx' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.xls' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.xlsx' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.ppt' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.pptx' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.txt' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.rtf' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.csv' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.zip' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.rar' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.7z' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.tar' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.gz' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.apk' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.epub' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.json' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.xml' " +
                "OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.vcf' " +
                "OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'application/%' " +
                "OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'text/%' " +
                ")"

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC LIMIT $limit OFFSET $offset"

        try {
            // Always use the classic query() so the WHERE clause is honoured on all OEM firmwares
            val cursor = context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                docSelection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dataCol = it.getColumnIndex(MediaStore.Files.FileColumns.DATA)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                    val filePath = if (dataCol != -1) it.getString(dataCol) else null
                    val name = it.getString(nameCol) ?: "Document"
                    // Skip zero-byte entries and hidden dot-files
                    val size = it.getLong(sizeCol)
                    if (size == 0L || name.startsWith(".")) continue
                    val key = (filePath ?: name).lowercase()
                    if (seenKeys.add(key)) {
                        val rawMime = it.getString(mimeCol) ?: ""
                        // Derive a proper mime from extension when the DB has a blank/null mime
                        val ext = name.substringAfterLast('.', "").lowercase()
                        val mime = rawMime.ifBlank {
                            android.webkit.MimeTypeMap.getSingleton()
                                .getMimeTypeFromExtension(ext) ?: "application/octet-stream"
                        }
                        list.add(
                            MediaItem(
                                id = id,
                                uri = contentUri,
                                name = name,
                                size = size,
                                dateAdded = it.getLong(dateCol),
                                mimeType = mime,
                                mediaType = MediaType.DOCUMENT,
                                filePath = filePath
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // Supplementary scan: direct file system scan in Download, Documents, and messaging app directories
        try {
            val extRoot = android.os.Environment.getExternalStorageDirectory()
            val candidateDirs = mutableListOf<java.io.File>()
            
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)?.let { candidateDirs.add(it) }
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)?.let { candidateDirs.add(it) }
            candidateDirs.add(java.io.File(extRoot, "Download"))
            candidateDirs.add(java.io.File(extRoot, "Documents"))
            candidateDirs.add(java.io.File(extRoot, "WhatsApp/Media/WhatsApp Documents"))
            candidateDirs.add(java.io.File(extRoot, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents"))
            candidateDirs.add(java.io.File(extRoot, "Telegram/Telegram Documents"))
            candidateDirs.add(java.io.File(extRoot, "Android/media/org.telegram.messenger/Telegram/Telegram Documents"))
            candidateDirs.add(java.io.File(extRoot, "GBWhatsApp/Media/GBWhatsApp Documents"))
            candidateDirs.add(java.io.File(extRoot, "ShareIT/file"))
            candidateDirs.add(java.io.File(extRoot, "Xender/file"))
            candidateDirs.add(java.io.File(extRoot, "bluetooth"))

            val validExts = setOf(
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                "txt", "rtf", "csv", "zip", "rar", "7z", "tar", "gz",
                "apk", "epub", "json", "xml", "vcf"
            )

            fun scanDirRecursively(dir: java.io.File, depth: Int = 0) {
                if (depth > 2 || !dir.exists() || !dir.isDirectory || dir.name.startsWith(".")) return
                val entries = dir.listFiles() ?: return
                for (file in entries) {
                    if (file.name.startsWith(".")) continue
                    if (file.isDirectory) {
                        scanDirRecursively(file, depth + 1)
                    } else if (file.isFile && file.length() > 0) {
                        val ext = file.extension.lowercase()
                        if (ext in validExts) {
                            val key = file.absolutePath.lowercase()
                            if (seenKeys.add(key)) {
                                val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                                    ?: "application/octet-stream"
                                list.add(
                                    MediaItem(
                                        id = file.hashCode().toLong(),
                                        uri = Uri.fromFile(file),
                                        name = file.name,
                                        size = file.length(),
                                        dateAdded = file.lastModified() / 1000L,
                                        mimeType = mime,
                                        mediaType = MediaType.DOCUMENT,
                                        filePath = file.absolutePath,
                                        bucketName = dir.name
                                    )
                                )
                            }
                        }
                    }
                }
            }

            for (dir in candidateDirs) {
                scanDirRecursively(dir, 0)
            }
        } catch (_: Exception) {}

        list.sortedByDescending { it.dateAdded }
    }

    suspend fun getReceivedFiles(): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        val downloadDir = java.io.File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            "FileDrop"
        )
        if (!downloadDir.exists() || !downloadDir.isDirectory) {
            downloadDir.mkdirs()
            return@withContext emptyList()
        }

        val files = downloadDir.listFiles()?.filter { it.isFile && !it.name.startsWith(".") } ?: emptyList()

        for (file in files.sortedByDescending { it.lastModified() }) {
            val name = file.name
            val ext = name.substringAfterLast('.', "").lowercase()
            val (mediaType, mimeType) = when (ext) {
                "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic" -> Pair(MediaType.IMAGE, "image/*")
                "mp4", "mkv", "mov", "avi", "3gp", "webm", "flv" -> Pair(MediaType.VIDEO, "video/*")
                "mp3", "m4a", "wav", "aac", "flac", "ogg", "opus" -> Pair(MediaType.AUDIO, "audio/*")
                "pdf" -> Pair(MediaType.DOCUMENT, "application/pdf")
                "doc", "docx" -> Pair(MediaType.DOCUMENT, "application/msword")
                "xls", "xlsx" -> Pair(MediaType.DOCUMENT, "application/vnd.ms-excel")
                "ppt", "pptx" -> Pair(MediaType.DOCUMENT, "application/vnd.ms-powerpoint")
                "txt" -> Pair(MediaType.DOCUMENT, "text/plain")
                "zip", "rar", "7z", "tar", "gz" -> Pair(MediaType.DOCUMENT, "application/zip")
                "apk" -> Pair(MediaType.DOCUMENT, "application/vnd.android.package-archive")
                "vcf" -> Pair(MediaType.DOCUMENT, "text/x-vcard")
                else -> Pair(MediaType.DOCUMENT, "application/octet-stream")
            }

            // Create file URI with FileProvider if needed or standard file Uri
            val fileUri = Uri.fromFile(file)

            list.add(
                MediaItem(
                    id = file.hashCode().toLong(),
                    uri = fileUri,
                    name = name,
                    size = file.length(),
                    dateAdded = file.lastModified() / 1000,
                    mimeType = mimeType,
                    mediaType = mediaType,
                    isSong = mediaType == MediaType.AUDIO,
                    filePath = file.absolutePath,
                    bucketName = "FileDrop"
                )
            )
        }
        list
    }

    fun getDeleteIntentSender(items: List<MediaItem>): android.content.IntentSender? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = items.map { it.uri }.filter { it.scheme == "content" }
            if (uris.isNotEmpty()) {
                return try {
                    android.provider.MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }

    suspend fun deleteMediaItems(items: List<MediaItem>): Boolean = withContext(Dispatchers.IO) {
        var anySuccess = false
        for (item in items) {
            try {
                var fileDeleted = false
                if (!item.filePath.isNullOrEmpty()) {
                    val f = java.io.File(item.filePath)
                    if (f.exists()) {
                        fileDeleted = f.delete()
                    }
                }
                var resolverDeleted = false
                if (item.uri.scheme == "content") {
                    try {
                        val count = context.contentResolver.delete(item.uri, null, null)
                        resolverDeleted = count > 0
                    } catch (_: Exception) {}
                }
                if (fileDeleted || resolverDeleted) {
                    anySuccess = true
                    if (!item.filePath.isNullOrEmpty()) {
                        try {
                            android.media.MediaScannerConnection.scanFile(
                                context,
                                arrayOf(item.filePath),
                                null,
                                null
                            )
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }
        anySuccess
    }
}
