package com.io.git.way.data.local

import android.content.Context
import com.io.git.way.domain.model.FileStatus
import com.io.git.way.domain.model.LocalFile
import com.io.git.way.domain.model.ScanIssue
import com.io.git.way.domain.model.ScanReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Runs the full Smart Upload Protection pipeline (PRD "Smart GitHub Upload Protection &
 * Ignore Engine" §2 Upload Workflow — Apply Ignore Rules -> Secret Detection ->
 * Large File Detection -> Generate Upload Summary) over a freshly scanned local project
 * folder. [ScanReport.safeFiles] is the only thing that ever reaches the diff/upload
 * pipeline — ignored, blocked, and secret-flagged files never leave the device.
 */
object ProtectionScanner {

    private const val MAX_CONCURRENT_SECRET_SCANS = 6

    suspend fun scan(context: Context, files: List<LocalFile>): ScanReport = withContext(Dispatchers.Default) {
        // §12 Git Ignore Support: merge the project's own root .gitignore with the
        // internal rules (nested .gitignore files are out of scope for this pass).
        val gitignorePatterns = files.firstOrNull { it.relativePath == ".gitignore" }?.let { gitignoreFile ->
            runCatching { FolderScanner.readBytes(context, gitignoreFile.documentUri).toString(Charsets.UTF_8) }
                .getOrNull()
                ?.let { IgnoreEngine.parseGitignore(it) }
        }.orEmpty()

        val safe = mutableListOf<LocalFile>()
        val ignored = mutableListOf<ScanIssue>()
        val blocked = mutableListOf<ScanIssue>()

        for (file in files) {
            when (val issue = IgnoreEngine.classify(file, gitignorePatterns)) {
                null -> safe += file
                else -> if (issue.status == FileStatus.IGNORED) ignored += issue else blocked += issue
            }
        }

        // §6 Secret Detection Engine: only runs on files that are otherwise safe and look
        // like scannable text, with limited read concurrency since this touches content.
        val semaphore = Semaphore(MAX_CONCURRENT_SECRET_SCANS)
        val secretResults = coroutineScope {
            safe.map { file ->
                async {
                    if (!SecretScanner.isScannable(file.displayName, file.sizeBytes)) {
                        file to null
                    } else {
                        semaphore.withPermit {
                            val label = runCatching {
                                val bytes = FolderScanner.readBytes(context, file.documentUri)
                                SecretScanner.scan(bytes.toString(Charsets.UTF_8))
                            }.getOrNull()
                            file to label
                        }
                    }
                }
            }.map { it.await() }
        }

        val secrets = mutableListOf<ScanIssue>()
        val stillSafe = mutableListOf<LocalFile>()
        for ((file, label) in secretResults) {
            if (label != null) {
                secrets += ScanIssue(file.relativePath, FileStatus.BLOCKED, "Secret detected: $label")
            } else {
                stillSafe += file
            }
        }

        ScanReport(
            totalFiles = files.size,
            safeFiles = stillSafe,
            ignoredFiles = ignored,
            blockedFiles = blocked,
            secretsFound = secrets,
            estimatedUploadBytes = stillSafe.sumOf { it.sizeBytes }
        )
    }
}
