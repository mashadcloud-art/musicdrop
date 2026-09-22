package com.musicdrop.tv.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import com.musicdrop.tv.data.model.InstalledAppInfo
import com.musicdrop.tv.data.model.MediaItem
import com.musicdrop.tv.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Enumerates user-installed apps for the "Clone Apps" feature and stages their APKs
 * for the P2P transfer engine.
 *
 * Split-APK apps (installed via a Play Store app bundle — config splits for density/ABI/
 * language) are flagged via [InstalledAppInfo.hasSplitApks] so the UI can warn that only
 * the base APK will transfer. Sideloading just the base APK of a split app can still work
 * (many bundles put everything essential in base.apk) but may crash or be missing assets —
 * the picker should let the user see and skip those.
 */
class InstalledAppsRepository(private val context: Context) {

    suspend fun getInstalledUserApps(includeSystemApps: Boolean = true): List<InstalledAppInfo> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val ownPackage = context.packageName
            val apps = mutableListOf<InstalledAppInfo>()

            @Suppress("DEPRECATION")
            val installed = try {
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            } catch (_: Exception) {
                emptyList()
            }

            for (appInfo in installed) {
                if (appInfo.packageName == ownPackage) continue

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                if (isSystem && !isUpdatedSystem && !includeSystemApps) continue

                val sourcePath = appInfo.sourceDir ?: continue
                val sourceFile = File(sourcePath)
                if (!sourceFile.exists() || !sourceFile.canRead()) continue

                val versionName = try {
                    pm.getPackageInfo(appInfo.packageName, 0).versionName
                } catch (_: Exception) {
                    null
                }

                val hasSplits = !appInfo.splitSourceDirs.isNullOrEmpty()

                apps.add(
                    InstalledAppInfo(
                        packageName = appInfo.packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        versionName = versionName,
                        apkSourcePath = sourceFile.absolutePath,
                        apkSizeBytes = sourceFile.length(),
                        isSystemApp = isSystem,
                        hasSplitApks = hasSplits
                    )
                )
            }

            apps.sortedBy { it.appName.lowercase() }
        }

    /**
     * Copies each selected app's base APK into the app cache dir — the original
     * /data/app/... path isn't guaranteed to stay valid for the whole transfer, and
     * copying gives the P2P engine a plain file it can stream with zero changes —
     * and wraps it as a MediaItem the existing P2PTransferManager already knows how to send.
     */
    suspend fun prepareAppsForTransfer(apps: List<InstalledAppInfo>): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val stagingDir = File(context.cacheDir, "clone_apks").apply { mkdirs() }
            val items = mutableListOf<MediaItem>()

            for (app in apps) {
                try {
                    val source = File(app.apkSourcePath)
                    if (!source.exists()) continue

                    val safeName = app.appName
                        .replace(Regex("[^A-Za-z0-9 _-]"), "")
                        .ifBlank { app.packageName }
                    val versionSuffix = app.versionName?.let { "_$it" } ?: ""
                    val destFile = File(stagingDir, "$safeName$versionSuffix.apk")

                    source.copyTo(destFile, overwrite = true)

                    items.add(
                        MediaItem(
                            id = destFile.hashCode().toLong(),
                            uri = Uri.fromFile(destFile),
                            name = destFile.name,
                            size = destFile.length(),
                            dateAdded = System.currentTimeMillis() / 1000,
                            mimeType = "application/vnd.android.package-archive",
                            mediaType = MediaType.DOCUMENT,
                            filePath = destFile.absolutePath,
                            bucketName = "Phone Clone"
                        )
                    )
                } catch (_: Exception) {
                    // Some OEMs restrict reading another app's APK path even though it's
                    // normally world-readable — skip silently, the UI reports the final count.
                }
            }

            items
        }

    /** Clears any previously staged APK copies once a transfer completes or is cancelled. */
    fun clearStagedApks() {
        try {
            File(context.cacheDir, "clone_apks").deleteRecursively()
        } catch (_: Exception) {}
    }
}
