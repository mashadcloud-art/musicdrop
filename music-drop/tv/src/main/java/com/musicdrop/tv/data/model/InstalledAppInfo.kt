package com.musicdrop.tv.data.model

/**
 * A lightweight snapshot of one installed app, used by the Phone Clone feature's
 * app picker. Kept separate from [MediaItem] because the UI needs app-specific fields
 * (package name, split-APK warning) before the app is turned into a MediaItem for transfer.
 */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val apkSourcePath: String,
    val apkSizeBytes: Long,
    val isSystemApp: Boolean,
    val hasSplitApks: Boolean
) {
    val formattedSize: String
        get() {
            val mb = apkSizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) String.format("%.1f MB", mb)
            else String.format("%.0f KB", apkSizeBytes / 1024.0)
        }
}
