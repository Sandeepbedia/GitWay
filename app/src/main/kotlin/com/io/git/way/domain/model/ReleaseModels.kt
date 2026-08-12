package com.io.git.way.domain.model

/** A published (or draft/prerelease) GitHub release with its downloadable assets. */
data class GitRelease(
    val id: Long,
    val tagName: String,
    val name: String?,
    val body: String?,
    val draft: Boolean,
    val prerelease: Boolean,
    val createdAt: String,
    val publishedAt: String?,
    val htmlUrl: String?,
    val assets: List<ReleaseAsset>
) {
    val apkAsset: ReleaseAsset? get() = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
}

data class ReleaseAsset(
    val id: Long,
    val name: String,
    val size: Long,
    val browserDownloadUrl: String?,
    val contentType: String?,
    val createdAt: String? = null
)
