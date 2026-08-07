/*
 * Git Way
 * Copyright (C) 2026 Sandeep Bedia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.io.git.way.domain.model

/** Outcome of classifying one local file against the ignore/security rules. */
enum class FileStatus { SAFE, IGNORED, BLOCKED }

/** One non-safe file, with a human-readable reason (PRD "Smart Upload Protection" §15
 * "Warning Cards" / §16 "View Ignored/Blocked Files"). [file] is kept so the user can
 * override the decision and still include it — Smart Upload Protection flags files, it
 * doesn't get the final say (§16 "user selects which files to upload"). */
data class ScanIssue(
    val file: LocalFile,
    val status: FileStatus,
    val reason: String
) {
    val relativePath get() = file.relativePath
}

/**
 * Summary produced by the Smart Upload Protection scan (PRD §14 "Upload Summary
 * Screen"). [safeFiles] is exactly what feeds the diff/upload pipeline — ignored and
 * blocked files never reach [com.io.git.way.domain.ComparisonEngine] or GitHub.
 * [safeFiles] also includes any auto-redacted credential files and the generated
 * jks_config.txt guide — [sanitizedFiles] lists those separately just for display, and
 * [contentOverrides] carries the redacted bytes actually used instead of the file's raw
 * on-disk content (keyed by relativePath; a path with no override reads straight from disk).
 */
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
