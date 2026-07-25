package com.io.git.way.data.local

import android.content.Context
import android.net.Uri
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
 * pipeline by default — ignored, blocked, and secret-flagged files never leave the
 * device unless the user explicitly overrides that (see [GitWaySessionViewModel]).
 */
object ProtectionScanner {

    private const val MAX_CONCURRENT_SECRET_SCANS = 6
    private const val CONFIG_GUIDE_PATH = "jks_config.txt"

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

        // Credential-shaped text files (key.properties, secrets.properties, .env*) get a
        // chance to be redacted instead of hard-blocked — real values swapped for
        // YOUR_..._HERE placeholders. Binary keystores (.jks/.keystore/.p12/.pem/.cer)
        // are never touched here and stay in [blocked].
        val contentOverrides = mutableMapOf<String, ByteArray>()
        val sanitized = mutableListOf<ScanIssue>()
        val stillBlocked = mutableListOf<ScanIssue>()
        for (issue in blocked) {
            val file = issue.file
            if (!KeystoreSanitizer.isSanitizable(file.displayName)) {
                stillBlocked += issue
                continue
            }
            val original = runCatching { FolderScanner.readBytes(context, file.documentUri).toString(Charsets.UTF_8) }.getOrNull()
            val redacted = original?.let { KeystoreSanitizer.sanitize(it) }
            if (redacted != null) {
                contentOverrides[file.relativePath] = redacted.toByteArray(Charsets.UTF_8)
                safe += file
                sanitized += ScanIssue(file, FileStatus.SAFE, "Real credentials redacted before upload — see $CONFIG_GUIDE_PATH")
            } else {
                stillBlocked += issue
            }
        }

        // §6 Secret Detection Engine: only runs on files that are otherwise safe and look
        // like scannable text, with limited read concurrency since this touches content.
        val semaphore = Semaphore(MAX_CONCURRENT_SECRET_SCANS)
        val secretResults = coroutineScope {
            safe.map { file ->
                async {
                    val override = contentOverrides[file.relativePath]
                    if (override == null && !SecretScanner.isScannable(file.displayName, file.sizeBytes)) {
                        file to null
                    } else {
                        semaphore.withPermit {
                            val label = runCatching {
                                val bytes = override ?: FolderScanner.readBytes(context, file.documentUri)
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
            // A file we just redacted is never re-flagged as a secret for its own
            // placeholder text (e.g. matching "password=" as a bare word).
            if (label != null && file.relativePath !in contentOverrides) {
                secrets += ScanIssue(file, FileStatus.BLOCKED, "Secret detected: $label")
            } else {
                stillSafe += file
            }
        }

        // If anything was redacted, generate a root-level setup guide so any developer
        // knows what was swapped out and how to plug their own values back in — unless
        // the project already ships its own jks_config.txt.
        if (sanitized.isNotEmpty() && files.none { it.relativePath == CONFIG_GUIDE_PATH }) {
            val guideContent = buildConfigGuide(sanitized.map { it.relativePath })
            val guideBytes = guideContent.toByteArray(Charsets.UTF_8)
            val guideFile = LocalFile(
                relativePath = CONFIG_GUIDE_PATH,
                displayName = CONFIG_GUIDE_PATH,
                sizeBytes = guideBytes.size.toLong(),
                lastModified = System.currentTimeMillis(),
                documentUri = Uri.EMPTY // generated, not read from disk — contentOverrides always wins for this path
            )
            contentOverrides[CONFIG_GUIDE_PATH] = guideBytes
            stillSafe += guideFile
        }

        ScanReport(
            totalFiles = files.size,
            safeFiles = stillSafe,
            ignoredFiles = ignored,
            blockedFiles = stillBlocked,
            secretsFound = secrets,
            sanitizedFiles = sanitized,
            contentOverrides = contentOverrides,
            estimatedUploadBytes = stillSafe.sumOf { it.sizeBytes }
        )
    }

    private fun buildConfigGuide(redactedPaths: List<String>): String = buildString {
        appendLine("Git Way — Keystore / Secrets Setup")
        appendLine("===================================")
        appendLine()
        appendLine("Smart Upload Protection found real credentials in this project and replaced")
        appendLine("them with placeholders before uploading, so nothing sensitive ever reached")
        appendLine("GitHub. Files touched:")
        appendLine()
        redactedPaths.forEach { appendLine(" - $it") }
        appendLine()
        appendLine("To build a signed release locally:")
        appendLine()
        appendLine("1. Generate or locate your own upload keystore (.jks/.keystore). Never commit")
        appendLine("   this file — keep it outside the repo, or add its path to .gitignore.")
        appendLine("2. Open the file(s) listed above and replace every YOUR_..._HERE placeholder")
        appendLine("   with your own real values (store password, key password, key alias, etc.).")
        appendLine("3. Re-run the build — Gradle reads the real credentials from your local copy;")
        appendLine("   the placeholders only ever exist in the copy that was uploaded to GitHub.")
        appendLine()
        appendLine("Never commit a filled-in key.properties, .env, or keystore file — Smart Upload")
        appendLine("Protection will keep redacting or blocking them automatically on future uploads.")
    }
}
