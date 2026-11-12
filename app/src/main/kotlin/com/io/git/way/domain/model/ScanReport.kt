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

/** Outcome of classifying one local file against the ignore/security rules. */
enum class FileStatus { SAFE, IGNORED, BLOCKED }

data class ScanIssue(
    val file: LocalFile,
    val status: FileStatus,
    val reason: String
) {
    val relativePath get() = file.relativePath
}

data class ScanReport(
    val totalFiles: Int,
    val safeFiles: List<LocalFile>,
    val ignoredFiles: List<ScanIssue>,
    val blockedFiles: List<ScanIssue>,
    val secretsFound: List<ScanIssue>,
    val sanitizedFiles: List<ScanIssue> = emptyList(),
    val contentOverrides: Map<String, ByteArray> = emptyMap(),
    val estimatedUploadBytes: Long
) {
    val safeCount get() = safeFiles.size
    val ignoredCount get() = ignoredFiles.size
    val blockedCount get() = blockedFiles.size
    val secretCount get() = secretsFound.size
}
