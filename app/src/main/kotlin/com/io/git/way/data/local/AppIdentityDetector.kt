package com.io.git.way.data.local

import android.content.Context
import com.io.git.way.domain.model.AppIdentity
import com.io.git.way.domain.model.LocalFile

/**
 * Detects which Android app a project folder or GitHub repository belongs to, by
 * reading the same files every Android project has one of: a module's
 * build.gradle(.kts) `applicationId`/`namespace`, or (older projects) the `package`
 * attribute on AndroidManifest.xml.
 *
 * This exists purely to catch "wrong repo selected" or "wrong local folder selected"
 * before Git Way diffs or uploads anything — see PRD "Repository / Project Match
 * Protection". It is intentionally best-effort: if neither project exposes a
 * recognisable package name (non-Android repos, unusual build setups), detection
 * returns null and no mismatch is reported — Git Way only blocks when it can actually
 * read a package name from *both* sides and they disagree.
 */
object AppIdentityDetector {

    private val APPLICATION_ID_REGEX = Regex("""applicationId\s*=?\s*["']([\w.]+)["']""")
    private val NAMESPACE_REGEX = Regex("""namespace\s*=?\s*["']([\w.]+)["']""")
    private val MANIFEST_PACKAGE_REGEX = Regex("""package\s*=\s*["']([\w.]+)["']""")

    private fun isGradleFile(path: String) =
        path.endsWith("build.gradle") || path.endsWith("build.gradle.kts")

    private fun isManifestFile(path: String) =
        path.endsWith("AndroidManifest.xml")

    /** Shallower paths first — an app-module's own build.gradle.kts is a much more
     * reliable signal than one nested inside a random subfolder or sample module. */
    private fun byDepth(path: String) = path.count { it == '/' }

    private fun extractFromGradleText(text: String): String? =
        APPLICATION_ID_REGEX.find(text)?.groupValues?.get(1)
            ?: NAMESPACE_REGEX.find(text)?.groupValues?.get(1)

    private fun extractFromManifestText(text: String): String? =
        MANIFEST_PACKAGE_REGEX.find(text)?.groupValues?.get(1)

    /** Reads the already-scanned local folder's own files looking for its
     * package/applicationId. Only reads the small number of candidate files (gradle
     * build scripts, manifests) — never the whole project. */
    suspend fun detectLocal(context: Context, files: List<LocalFile>): AppIdentity? {
        val gradleCandidates = files.filter { isGradleFile(it.relativePath) }
            .sortedBy { byDepth(it.relativePath) }
        for (file in gradleCandidates) {
            val text = runCatching {
                FolderScanner.readBytes(context, file.documentUri).toString(Charsets.UTF_8)
            }.getOrNull() ?: continue
            extractFromGradleText(text)?.let { return AppIdentity(it, file.relativePath) }
        }

        val manifestCandidates = files.filter { isManifestFile(it.relativePath) }
            .sortedBy { byDepth(it.relativePath) }
        for (file in manifestCandidates) {
            val text = runCatching {
                FolderScanner.readBytes(context, file.documentUri).toString(Charsets.UTF_8)
            }.getOrNull() ?: continue
            extractFromManifestText(text)?.let { return AppIdentity(it, file.relativePath) }
        }
        return null
    }

    /** Same detection against a GitHub repository's remote file tree (path -> blob sha,
     * as returned by [com.io.git.way.domain.repository.GitHubRepository.getRepositoryTree]).
     * [readRemoteFile] fetches one blob's bytes by sha — only called for the handful of
     * candidate paths, not the whole tree. */
    suspend fun detectRemote(
        remoteTree: Map<String, String>,
        readRemoteFile: suspend (blobSha: String) -> ByteArray?
    ): AppIdentity? {
        val gradlePaths = remoteTree.keys.filter { isGradleFile(it) }.sortedBy { byDepth(it) }
        for (path in gradlePaths) {
            val sha = remoteTree.getValue(path)
            val bytes = readRemoteFile(sha) ?: continue
            val text = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull() ?: continue
            extractFromGradleText(text)?.let { return AppIdentity(it, path) }
        }

        val manifestPaths = remoteTree.keys.filter { isManifestFile(it) }.sortedBy { byDepth(it) }
        for (path in manifestPaths) {
            val sha = remoteTree.getValue(path)
            val bytes = readRemoteFile(sha) ?: continue
            val text = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull() ?: continue
            extractFromManifestText(text)?.let { return AppIdentity(it, path) }
        }
        return null
    }
}
