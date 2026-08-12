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
package com.io.git.way.data.local

import com.io.git.way.domain.model.FileStatus
import com.io.git.way.domain.model.LocalFile
import com.io.git.way.domain.model.ScanIssue

/**
 * Smart GitHub Upload Protection & Ignore Engine (see the matching PRD). Classifies every
 * locally scanned file as SAFE, IGNORED (build/tooling noise — silently skipped), or
 * BLOCKED (security-sensitive or dangerous-to-commit — always surfaced to the user with a
 * reason, §19 "No override for these by default").
 *
 * Priority order (PRD §12): internal security rules -> generated/binary rules -> hidden
 * files -> project .gitignore. Whichever rule matches first wins and short-circuits the
 * rest, mirroring how a real ignore engine would short-circuit on the first match.
 */
object IgnoreEngine {

    /** PRD §10 — 50 MB default large-file limit. */
    const val DEFAULT_LARGE_FILE_BYTES = 50L * 1024 * 1024

    /** PRD §4/§22 — generated/tooling folders, matched against any path segment
     * (case-insensitive) so "Android/Build/" and "build/" both match. */
    private val IGNORED_DIR_NAMES = setOf(
        ".git", ".github", "build", ".gradle", ".idea", ".vscode",
        ".dart_tool", ".flutter-plugins", ".flutter-plugins-dependencies", ".pub",
        "captures", ".cxx", ".externalnativebuild", "node_modules",
        "dist", "target", "bin", "obj", "coverage", "tmp", "temp", ".cache",
        "logs", "out", "release", "debug", "generated", "pods", "deriveddata",
        "vendor", "__pycache__", ".terraform", ".next", ".nuxt", ".svelte-kit"
    )

    /** PRD §5 minus the security-critical subset below — archives, compiled artifacts,
     * editor/OS noise. Ignored quietly, not flagged as a security warning. */
    private val IGNORED_FILE_GLOBS = listOf(
        "*.iml", "*.zip", "*.rar", "*.7z", "*.tar", "*.gz",
        "*.class", "*.dex", "*.o", "*.obj", "*.tmp", "*.bak", "*.log",
        "Thumbs.db", ".DS_Store", "desktop.ini"
    )

    /** PRD §6/§19 — secrets, signing material, credentials. Always BLOCKED, no override. */
    private val SECURITY_BLOCKED_GLOBS = listOf(
        "*.keystore", "*.jks", "*.pem", "*.key", "*.p12", "*.cer",
        ".env", ".env.*", "google-services.json", "firebase-adminsdk.json",
        "key.properties", "secrets.properties", "credentials.json", "config.secret",
        "local.properties"
    )

    /** PRD §7 — generated app binaries, never committed. */
    private val APK_BLOCKED_GLOBS = listOf("*.apk", "*.aab", "*.apks", "*.xapk", "*.ipa")

    /** PRD §11 — hidden files are ignored by default except these. */
    private val ALLOWED_HIDDEN_FILES = setOf(".gitignore", ".gitattributes", ".editorconfig")

    /** Returns the [ScanIssue] for [file] if it's IGNORED or BLOCKED, or null if it's SAFE. */
    fun classify(
        file: LocalFile,
        gitignorePatterns: List<String>,
        largeFileThresholdBytes: Long = DEFAULT_LARGE_FILE_BYTES
    ): ScanIssue? {
        val segments = file.relativePath.split("/")
        val name = file.displayName

        // .github itself is an auto-ignored folder (repo metadata), but CI workflow
        // definitions are real project files developers expect to push — carve them out.
        val isGithubWorkflow = segments.size >= 2 && segments[0] == ".github" && segments[1] == "workflows"

        if (!isGithubWorkflow && segments.dropLast(1).any { it.lowercase() in IGNORED_DIR_NAMES }) {
            return ScanIssue(file, FileStatus.IGNORED, "Inside an auto-ignored folder")
        }

        matchAny(name, SECURITY_BLOCKED_GLOBS)?.let { glob ->
            return ScanIssue(file, FileStatus.BLOCKED, "Security-sensitive file ($glob) — never uploaded")
        }

        matchAny(name, APK_BLOCKED_GLOBS)?.let { glob ->
            return ScanIssue(file, FileStatus.BLOCKED, "Generated app binary ($glob) should not be committed")
        }

        if (name.startsWith(".") && name !in ALLOWED_HIDDEN_FILES) {
            return ScanIssue(file, FileStatus.IGNORED, "Hidden file")
        }

        matchAny(name, IGNORED_FILE_GLOBS)?.let { glob ->
            return ScanIssue(file, FileStatus.IGNORED, "Matches internal ignore rule ($glob)")
        }

        if (gitignorePatterns.any { pattern -> matches(file.relativePath, pattern) || matches(name, pattern) }) {
            return ScanIssue(file, FileStatus.IGNORED, "Matches .gitignore")
        }

        if (file.sizeBytes > largeFileThresholdBytes) {
            val mb = file.sizeBytes / (1024.0 * 1024.0)
            val limitMb = largeFileThresholdBytes / (1024 * 1024)
            return ScanIssue(
                file,
                FileStatus.BLOCKED,
                "Large file (${"%.1f".format(mb)} MB) — over the $limitMb MB limit. Consider Git LFS."
            )
        }

        return null
    }

    /** Parses a .gitignore's content into simple glob patterns. This is a pragmatic subset
     * of real git pathspec semantics (no negation, no double-star) — comments/blank lines
     * dropped, leading/trailing slashes trimmed since matching runs per path segment. */
    fun parseGitignore(content: String): List<String> =
        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("!") }
            .map { it.removePrefix("/").removeSuffix("/") }
            .filter { it.isNotEmpty() }
            .toList()

    private fun matchAny(name: String, globs: List<String>): String? =
        globs.firstOrNull { glob -> matches(name, glob) }

    private fun matches(text: String, glob: String): Boolean = globToRegex(glob).matches(text)

    private fun globToRegex(glob: String): Regex {
        val sb = StringBuilder("^")
        for (c in glob) {
            when (c) {
                '*' -> sb.append(".*")
                '.', '(', ')', '+', '[', ']', '^', '$', '{', '}', '|', '\\' -> sb.append('\\').append(c)
                else -> sb.append(c)
            }
        }
        sb.append("$")
        return Regex(sb.toString(), RegexOption.IGNORE_CASE)
    }
}
