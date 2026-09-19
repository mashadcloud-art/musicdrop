package com.musicdrop.app.data.updater

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.musicdrop.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

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

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun checkForUpdate(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val currentCode = BuildConfig.VERSION_CODE
        val endpoints = listOf(GITHUB_VERSION_URL, BACKUP_CONFIG_URL)

        for (endpoint in endpoints) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .header("User-Agent", "MusicDrop/${BuildConfig.VERSION_NAME}")
                    .header("Cache-Control", "no-cache")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: continue
                    val json = JSONObject(raw)
                    val remoteCode = json.optInt("versionCode", json.optInt("min_version_code", 0))
                    val remoteName = json.optString("versionName", json.optString("version", ""))
                    val downloadUrl = json.optString("downloadUrl", json.optString("update_url", ""))
                    val title = json.optString("title", "MusicDrop $remoteName Available")
                    val changelog = json.optString(
                        "changelog",
                        "• Fresh Trending Mix\n• Multi-Page Swipeable Speed Dial\n• Modern Transparent Background\n• Mobile Layout & Responsiveness Fixes"
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

            val request = Request.Builder()
                .url(updateInfo.downloadUrl)
                .header("User-Agent", "MusicDrop/${BuildConfig.VERSION_NAME}")
                .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    onError("Download failed: HTTP ${response.code}")
                }
                return@withContext
            }

            val body = response.body
            if (body == null) {
                withContext(Dispatchers.Main) { onError("Empty response from server") }
                return@withContext
            }

            val totalBytes = body.contentLength().coerceAtLeast(1)
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 1) {
                            val progress = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                    }
                    output.flush()
                }
            }

            // APK Integrity Verification
            val packageInfo = activity.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            if (packageInfo == null || apkFile.length() < 2_000_000) {
                apkFile.delete()
                withContext(Dispatchers.Main) {
                    onError("Downloaded package is incomplete or corrupted (${apkFile.length()} bytes). Please try again.")
                }
                return@withContext
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

            // Grant read permission to all matching activities (essential for custom ROMs & Android 12+)
            val resInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                activity.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                activity.grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
