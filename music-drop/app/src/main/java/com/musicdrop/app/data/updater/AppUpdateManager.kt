package com.musicdrop.app.data.updater

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.musicdrop.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val title: String = "New Update Available",
    val changelog: String = "",
    val downloadUrl: String = "",
    val forceUpdate: Boolean = false
)

object AppUpdateManager {

    private const val GITHUB_VERSION_URL = "https://raw.githubusercontent.com/mashadcloud-art/musicdrop/main/version.json"
    private const val BACKUP_CONFIG_URL = "https://api.mxf-95274725.com/config/flags"

    suspend fun checkForUpdate(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val currentCode = BuildConfig.VERSION_CODE
        val endpoints = listOf(GITHUB_VERSION_URL, BACKUP_CONFIG_URL)

        for (endpoint in endpoints) {
            try {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "MusicDrop/${BuildConfig.VERSION_NAME}")
                }
                if (conn.responseCode in 200..299) {
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(raw)
                    val remoteCode = json.optInt("versionCode", json.optInt("min_version_code", 0))
                    val remoteName = json.optString("versionName", json.optString("version", ""))
                    val downloadUrl = json.optString("downloadUrl", json.optString("update_url", ""))
                    val title = json.optString("title", "MusicDrop $remoteName Available")
                    val changelog = json.optString(
                        "changelog",
                        "• Full Screen Edge-to-Edge Top Video player\n• Persistent Video Mode across song changes\n• YouTube Music Brand Badge\n• Live in-app update & install"
                    )
                    val force = json.optBoolean("forceUpdate", false)

                    if (remoteCode > currentCode && downloadUrl.isNotBlank()) {
                        return@withContext AppUpdateInfo(
                            latestVersionCode = remoteCode,
                            latestVersionName = remoteName.ifBlank { "v$remoteCode" },
                            title = title,
                            changelog = changelog,
                            downloadUrl = downloadUrl,
                            forceUpdate = force
                        )
                    }
                }
            } catch (_: Exception) {
                // Try next endpoint
            }
        }
        null
    }

    suspend fun downloadAndInstallApk(
        activity: Activity,
        updateInfo: AppUpdateInfo,
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            if (updateInfo.downloadUrl.isBlank()) {
                withContext(Dispatchers.Main) { onError("Invalid download link") }
                return@withContext
            }

            val downloadsDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: activity.cacheDir
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val apkFile = File(downloadsDir, "MusicDrop-v${updateInfo.latestVersionName}.apk")
            if (apkFile.exists()) apkFile.delete()

            val conn = (URL(updateInfo.downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 15000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MusicDrop/${BuildConfig.VERSION_NAME}")
            }

            val totalBytes = conn.contentLength.coerceAtLeast(1)
            var downloadedBytes = 0L

            conn.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                        withContext(Dispatchers.Main) { onProgress(progress) }
                    }
                    output.flush()
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(1f)
                installApk(activity, apkFile)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "Download failed")
            }
        }
    }

    fun installApk(activity: Activity, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(intent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                activity,
                "Unable to start installer: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
}
