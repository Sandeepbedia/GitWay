/*
 * GitWay — an Android client for GitHub.
 *
 * This file is part of GitWay. GitWay is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * GitWay is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GitWay. If not, see <https://www.gnu.org/licenses/>.
 */
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
