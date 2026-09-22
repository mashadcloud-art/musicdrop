package com.musicdrop.tv.data.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AudioCacheManager {

    private const val TAG = "AudioCacheManager"
    private const val MAX_CACHE_AGE_MS = 30 * 60 * 1000L // 30 minutes
    private const val MAX_TOTAL_CACHE_BYTES = 200 * 1024 * 1024L // 200 MB

    suspend fun pruneOldAudioCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            val cacheDirs = listOfNotNull(
                File(context.cacheDir, "media_cache"),
                File(context.cacheDir, "exoplayer"),
                File(context.cacheDir, "stream_cache"),
                context.externalCacheDir?.let { File(it, "media_cache") }
            )

            val now = System.currentTimeMillis()

            for (dir in cacheDirs) {
                if (!dir.exists() || !dir.isDirectory) continue

                val files = dir.listFiles() ?: continue
                var totalSize = 0L

                for (file in files) {
                    if (file.isDirectory) continue
                    totalSize += file.length()

                    // Delete files older than 30 minutes
                    val age = now - file.lastModified()
                    if (age > MAX_CACHE_AGE_MS) {
                        Log.d(TAG, "Pruning expired cache file (${age / 60000}m old): ${file.name}")
                        file.delete()
                    }
                }

                // If total size still exceeds 200MB, delete oldest files until under limit
                if (totalSize > MAX_TOTAL_CACHE_BYTES) {
                    val remainingFiles = (dir.listFiles() ?: emptyArray())
                        .filter { it.isFile }
                        .sortedBy { it.lastModified() }

                    var currentSize = totalSize
                    for (file in remainingFiles) {
                        if (currentSize <= MAX_TOTAL_CACHE_BYTES) break
                        currentSize -= file.length()
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cache pruning encountered an issue: ${e.message}")
        }
    }
}
