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

import android.content.Context
import com.io.git.way.domain.model.AppIdentity
import com.io.git.way.domain.model.LocalFile

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

    suspend fun detectLocal(context: Context, files: List<LocalFile>): AppIdentity? {
        val pathOf: (LocalFile) -> String = { it.relativePath }
        val read: suspend (LocalFile) -> String? = { file ->
            runCatching { FolderScanner.readBytes(context, file.documentUri).toString(Charsets.UTF_8) }.getOrNull()
        }
        val (packageName, sourceFile) = detectPackage(files, pathOf, read) ?: return null
        val appName = detectAppName(files, pathOf, read)
        return AppIdentity(packageName = packageName, appName = appName, sourceFile = sourceFile)
    }

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
