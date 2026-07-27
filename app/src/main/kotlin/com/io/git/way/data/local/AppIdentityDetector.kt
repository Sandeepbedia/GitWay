package com.io.git.way.data.local

import android.content.Context
import com.io.git.way.domain.model.AppIdentity
import com.io.git.way.domain.model.LocalFile

/**
 * Detects which app/project a folder or GitHub repository belongs to, per PRD
 * "Repository Project Validation & Safe Upload System": Package Name / Application ID
 * (the primary signal — every supported project type has one) plus, when resolvable,
 * a human-readable App Name.
 *
 * Package/Application ID comes from whichever of these a project has: an Android
 * module's build.gradle(.kts) `applicationId`/`namespace` (or the older `package`
 * attribute on AndroidManifest.xml), a Flutter project's pubspec.yaml `name:`, or a
 * Node/JS project's package.json `"name"`.
 *
 * App Name comes from Android's `android:label` on AndroidManifest.xml's
 * `<application>` tag — resolved through res/values/strings.xml when it's a
 * `@string/...` reference rather than a literal string. Non-Android projects don't have
 * a meaningfully separate "app name" field, so it stays null there and only the
 * package/project name is compared for them.
 *
 * This exists purely to catch "wrong repo selected" or "wrong local folder selected"
 * before Git Way diffs or uploads anything. It is intentionally best-effort: if a
 * project exposes no recognisable package/name at all, detection returns null and no
 * mismatch is reported — Git Way only blocks when it can actually read an identifier
 * from *both* sides and they disagree.
 */
object AppIdentityDetector {

    private val APPLICATION_ID_REGEX = Regex("""applicationId\s*=?\s*["']([\w.]+)["']""")
    private val NAMESPACE_REGEX = Regex("""namespace\s*=?\s*["']([\w.]+)["']""")
    private val MANIFEST_PACKAGE_REGEX = Regex("""package\s*=\s*["']([\w.]+)["']""")
    private val PUBSPEC_NAME_REGEX = Regex("""(?m)^name:\s*(\S+)""")
    private val PACKAGE_JSON_NAME_REGEX = Regex(""""name"\s*:\s*"([^"]+)"""")
    private val MANIFEST_LABEL_REGEX = Regex("""android:label\s*=\s*["']([^"']+)["']""")

    private fun isGradleFile(path: String) =
        path.endsWith("build.gradle") || path.endsWith("build.gradle.kts")

    private fun isManifestFile(path: String) =
        path.endsWith("AndroidManifest.xml")

    private fun isPubspecFile(path: String) =
        path.endsWith("pubspec.yaml") || path.endsWith("pubspec.yml")

    private fun isPackageJsonFile(path: String) =
        path == "package.json" || path.endsWith("/package.json")

    private fun isStringsXmlFile(path: String) =
        path.endsWith("res/values/strings.xml")

    /** Shallower paths first — an app-module's own build.gradle.kts (or a project's root
     * pubspec.yaml/package.json) is a much more reliable signal than one nested inside a
     * random subfolder, sample module, or node_modules-adjacent package. */
    private fun byDepth(path: String) = path.count { it == '/' }

    private fun extractFromGradleText(text: String): String? =
        APPLICATION_ID_REGEX.find(text)?.groupValues?.get(1)
            ?: NAMESPACE_REGEX.find(text)?.groupValues?.get(1)

    private fun extractFromManifestText(text: String): String? =
        MANIFEST_PACKAGE_REGEX.find(text)?.groupValues?.get(1)

    private fun extractFromPubspecText(text: String): String? =
        PUBSPEC_NAME_REGEX.find(text)?.groupValues?.get(1)?.trim()

    private fun extractFromPackageJsonText(text: String): String? =
        PACKAGE_JSON_NAME_REGEX.find(text)?.groupValues?.get(1)?.trim()

    private fun extractRawAppLabel(text: String): String? =
        MANIFEST_LABEL_REGEX.find(text)?.groupValues?.get(1)

    private fun resolveStringResource(name: String, stringsXmlText: String): String? =
        Regex("""<string\s+name\s*=\s*["']${Regex.escape(name)}["'][^>]*>([^<]*)</string>""")
            .find(stringsXmlText)?.groupValues?.get(1)?.trim()

    /** Finds the first candidate file matching [matches], reads it, and runs [extract] —
     * returns the extracted value paired with which file it came from. Closest-to-root
     * candidate wins when several match. */
    private suspend fun <T> firstMatch(
        candidates: List<T>,
        pathOf: (T) -> String,
        read: suspend (T) -> String?,
        matches: (String) -> Boolean,
        extract: (String) -> String?
    ): Pair<String, String>? {
        val sorted = candidates.filter { matches(pathOf(it)) }.sortedBy { byDepth(pathOf(it)) }
        for (candidate in sorted) {
            val text = read(candidate) ?: continue
            extract(text)?.let { return it to pathOf(candidate) }
        }
        return null
    }

    private suspend fun <T> detectPackage(
        candidates: List<T>,
        pathOf: (T) -> String,
        read: suspend (T) -> String?
    ): Pair<String, String>? =
        firstMatch(candidates, pathOf, read, ::isGradleFile, ::extractFromGradleText)
            ?: firstMatch(candidates, pathOf, read, ::isManifestFile, ::extractFromManifestText)
            ?: firstMatch(candidates, pathOf, read, ::isPubspecFile, ::extractFromPubspecText)
            ?: firstMatch(candidates, pathOf, read, ::isPackageJsonFile, ::extractFromPackageJsonText)

    /** Android only: reads `android:label` off AndroidManifest.xml, resolving a
     * `@string/...` reference against res/values/strings.xml when needed. */
    private suspend fun <T> detectAppName(
        candidates: List<T>,
        pathOf: (T) -> String,
        read: suspend (T) -> String?
    ): String? {
        val (rawLabel, _) = firstMatch(candidates, pathOf, read, ::isManifestFile, ::extractRawAppLabel) ?: return null
        if (!rawLabel.startsWith("@string/")) return rawLabel

        val resourceName = rawLabel.removePrefix("@string/")
        val stringsCandidates = candidates.filter { isStringsXmlFile(pathOf(it)) }.sortedBy { byDepth(pathOf(it)) }
        for (candidate in stringsCandidates) {
            val text = read(candidate) ?: continue
            resolveStringResource(resourceName, text)?.let { return it }
        }
        return null
    }

    /** Reads the already-scanned local folder's own files looking for its package/app
     * name. Only reads the small number of candidate files (gradle build scripts,
     * manifests, strings.xml, pubspec.yaml, package.json) — never the whole project. */
    suspend fun detectLocal(context: Context, files: List<LocalFile>): AppIdentity? {
        val pathOf: (LocalFile) -> String = { it.relativePath }
        val read: suspend (LocalFile) -> String? = { file ->
            runCatching { FolderScanner.readBytes(context, file.documentUri).toString(Charsets.UTF_8) }.getOrNull()
        }
        val (packageName, sourceFile) = detectPackage(files, pathOf, read) ?: return null
        val appName = detectAppName(files, pathOf, read)
        return AppIdentity(packageName = packageName, appName = appName, sourceFile = sourceFile)
    }

    /** Same detection against a GitHub repository's remote file tree (path -> blob sha,
     * as returned by [com.io.git.way.domain.repository.GitHubRepository.getRepositoryTree]).
     * [readRemoteFile] fetches one blob's bytes by sha — only called for the handful of
     * candidate paths, not the whole tree. */
    suspend fun detectRemote(
        remoteTree: Map<String, String>,
        readRemoteFile: suspend (blobSha: String) -> ByteArray?
    ): AppIdentity? {
        val paths = remoteTree.keys.toList()
        val pathOf: (String) -> String = { it }
        val read: suspend (String) -> String? = { path ->
            val sha = remoteTree.getValue(path)
            readRemoteFile(sha)?.let { bytes -> runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull() }
        }
        val (packageName, sourceFile) = detectPackage(paths, pathOf, read) ?: return null
        val appName = detectAppName(paths, pathOf, read)
        return AppIdentity(packageName = packageName, appName = appName, sourceFile = sourceFile)
    }
}
