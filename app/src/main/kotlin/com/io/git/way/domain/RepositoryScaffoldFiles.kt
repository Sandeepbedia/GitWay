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

/**
 * Files GitHub itself (or a maintainer, via the GitHub web UI) commonly adds straight
 * into a repository — README, LICENSE, `.gitignore`, CI workflows under `.github/`,
 * Play Store screenshots, promo banners, etc. These describe the *repository*, not the
 * *app*, so a local Android Studio project folder legitimately never contains them.
 *
 * Before this list existed, [ComparisonEngine] treated "exists on GitHub but not in the
 * local folder" as "Removed" for every single path — so every comparison flagged
 * README.md, LICENSE, .gitignore, and the whole `.github/` folder for deletion, even
 * though the person never touched them. That's the exact bug this fixes: removal
 * detection should only ever apply to files that are actually part of the app project.
 *
 * Real-world repos keep drifting past a fixed filename list (`banner.png`,
 * `SIGNING.md`, `README-additions.md`, `.gitkeep` placeholders in native-lib folders,
 * a `screenshots/` folder…) so on top of the explicit lists below, any *root-level*
 * file that isn't a recognized Gradle/Android project file is treated as repo
 * decoration too — an Android app's real source never lives loosely at the repo root.
 */
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

    /** Root-level filenames that ARE real project files and must never be excluded,
     * even though nothing else at the root is trusted by default. */
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

    /** Whole-subtree folders that are repository scaffolding, not app source —
     * GitHub Actions workflows, issue/PR templates, Play Store screenshots, promo art. */
    private val ignoredFolderPrefixes = listOf(
        ".github/",
        "screenshots/",
        "fastlane/",
        "docs/",
        "promo/",
        "store-assets/",
        "storeassets/"
    )

    /** Maintainer convention seen in the wild for "content to be manually merged in" —
     * e.g. README-additions.md, gitignore-additions.txt. */
    private val additionsFileRegex = Regex(""".*-additions\.[a-z0-9]+$""", RegexOption.IGNORE_CASE)

    /** True if [path] is repository scaffolding rather than app project content, and
     * should therefore be excluded from "Removed" detection when it exists on GitHub
     * but not in the local folder. */
    fun isScaffoldFile(path: String): Boolean {
        val normalized = path.trim('/')

        // .gitkeep is purely a "keep this empty folder in git" marker — never real app
        // content, wherever it lives (e.g. app/src/main/jniLibs/arm64-v8a/.gitkeep).
        val fileName = normalized.substringAfterLast('/')
        if (fileName.equals(".gitkeep", ignoreCase = true)) return true

        if (ignoredFolderPrefixes.any { normalized.startsWith(it, ignoreCase = true) }) return true

        val isRootLevel = !normalized.contains('/')
        if (!isRootLevel) return false

        val lower = normalized.lowercase()
        if (lower in alwaysScaffoldRootFiles) return true
        if (additionsFileRegex.matches(normalized)) return true

        // Anything else at the repo root that isn't a recognized Gradle/Android project
        // file is treated as repo decoration by default (banner.png, SIGNING.md, ...) —
        // real Android source always lives inside a module folder like app/.
        return lower !in knownProjectRootFiles
    }
}
