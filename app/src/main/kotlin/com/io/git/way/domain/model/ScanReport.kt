package com.io.git.way.domain.model

/** Outcome of classifying one local file against the ignore/security rules. */
enum class FileStatus { SAFE, IGNORED, BLOCKED }

/** One non-safe file, with a human-readable reason (PRD "Smart Upload Protection" §15
 * "Warning Cards" / §16 "View Ignored/Blocked Files"). */
data class ScanIssue(
    val relativePath: String,
    val status: FileStatus,
    val reason: String
)

/**
 * Summary produced by the Smart Upload Protection scan (PRD §14 "Upload Summary
 * Screen"). [safeFiles] is exactly what feeds the diff/upload pipeline — ignored and
 * blocked files never reach [com.io.git.way.domain.ComparisonEngine] or GitHub.
 */
data class ScanReport(
    val totalFiles: Int,
    val safeFiles: List<LocalFile>,
    val ignoredFiles: List<ScanIssue>,
    val blockedFiles: List<ScanIssue>,
    val secretsFound: List<ScanIssue>,
    val estimatedUploadBytes: Long
) {
    val safeCount get() = safeFiles.size
    val ignoredCount get() = ignoredFiles.size
    val blockedCount get() = blockedFiles.size
    val secretCount get() = secretsFound.size
}
