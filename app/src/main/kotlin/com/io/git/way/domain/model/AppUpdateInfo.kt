package com.io.git.way.domain.model

/** An available update, resolved from the latest GitHub release. */
data class AppUpdateInfo(
    val versionTag: String,
    val releaseTitle: String,
    val releaseNotes: String,
    /** Direct APK download link if the release has an .apk asset attached, otherwise
     * null — the dialog falls back to opening [releasePageUrl] instead. */
    val apkDownloadUrl: String?,
    val releasePageUrl: String
)
