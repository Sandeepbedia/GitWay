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
