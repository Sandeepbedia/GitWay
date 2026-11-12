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
package com.io.git.way.domain

object RepositoryScaffoldFiles {

    /** Root-level filenames (case-insensitive) that are always repo scaffolding. */
    private val alwaysScaffoldRootFiles = setOf(
        "readme.md", "readme", "readme.txt", "readme.rst",
        "license", "license.md", "license.txt",
        "licence", "licence.md", "licence.txt",
        "notice", "notice.md",
        "changelog.md",
        "code_of_conduct.md",
        "contributing.md",
        "security.md",
        "codeowners",
        "funding.yml",
        ".gitignore",
        ".gitattributes",
        ".gitmodules",
        ".editorconfig"
    )

    private val knownProjectRootFiles = setOf(
        "settings.gradle", "settings.gradle.kts",
        "build.gradle", "build.gradle.kts",
        "gradle.properties", "local.properties",
        "gradlew", "gradlew.bat",
        "keystore.properties", "keystore.properties.example",
        "proguard-rules.pro",
        "version.properties", "versions.properties",
        "gradle.lockfile"
    )

    private val ignoredFolderPrefixes = listOf(
        ".github/",
        "screenshots/",
        "fastlane/",
        "docs/",
        "promo/",
        "store-assets/",
        "storeassets/"
    )

    private val additionsFileRegex = Regex(""".*-additions\.[a-z0-9]+$""", RegexOption.IGNORE_CASE)

    fun isScaffoldFile(path: String): Boolean {
        val normalized = path.trim('/')

        val fileName = normalized.substringAfterLast('/')
        if (fileName.equals(".gitkeep", ignoreCase = true)) return true

        if (ignoredFolderPrefixes.any { normalized.startsWith(it, ignoreCase = true) }) return true

        val isRootLevel = !normalized.contains('/')
        if (!isRootLevel) return false

        val lower = normalized.lowercase()
        if (lower in alwaysScaffoldRootFiles) return true
        if (additionsFileRegex.matches(normalized)) return true

        return lower !in knownProjectRootFiles
    }
}
