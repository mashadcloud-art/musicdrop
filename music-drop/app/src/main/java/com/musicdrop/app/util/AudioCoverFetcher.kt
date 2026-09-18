package com.musicdrop.app.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer

/**
 * Custom Coil Fetcher that extracts embedded album artwork from audio files
 * (MP3 ID3 tags, M4A/AAC, FLAC metadata) on Android 10+ without relying on deprecated albumart provider.
 */
class AudioCoverFetcher(
    private val context: Context,
    private val data: Uri
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, data)
            val picture = retriever.embeddedPicture
            if (picture != null) {
                val buffer = Buffer().write(picture)
                return@withContext SourceResult(
                    source = ImageSource(buffer, context),
                    mimeType = "image/jpeg",
                    dataSource = DataSource.DISK
                )
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
        null
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val str = data.toString()
            if (str.contains("audio") ||
                str.endsWith(".mp3", true) ||
                str.endsWith(".m4a", true) ||
                str.endsWith(".flac", true) ||
                str.endsWith(".wav", true) ||
                str.endsWith(".aac", true) ||
                str.endsWith(".ogg", true)
            ) {
                return AudioCoverFetcher(context, data)
            }
            return null
        }
    }
}
