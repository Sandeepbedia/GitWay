package com.io.git.way.domain

/**
 * Files GitHub itself (or a maintainer, via the GitHub web UI) commonly adds straight
 * into a repository — README, LICENSE, `.gitignore`, CI workflows under `.github/`, etc.
 * These describe the *repository*, not the *app*, so a local Android Studio project
 * folder legitimately never contains them.
 *
 * Before this list existed, [ComparisonEngine] treated "exists on GitHub but not in the
 * local folder" as "Removed" for every single path — so every comparison flagged
 * README.md, LICENSE, .gitignore, and the whole `.github/` folder for deletion, even
 * though the person never touched them. That's the exact bug this fixes: removal
 * detection should only ever apply to files that are actually part of the app project.
 */
object RepositoryScaffoldFiles {

    /** Root-level filenames (case-insensitive). Only matched with no folder prefix —
     * a README.md nested inside e.g. `app/src/main/assets/` is still real app content
     * and must still be eligible for removal detection. */
    private val rootFileNames = setOf(
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

    /** Whole-subtree folders that are repository scaffolding, not app source —
     * GitHub Actions workflows, issue/PR templates, funding config, etc. */
    private val ignoredFolderPrefixes = listOf(".github/")

    /** True if [path] is repository scaffolding rather than app project content, and
     * should therefore be excluded from "Removed" detection when it exists on GitHub
     * but not in the local folder. */
    fun isScaffoldFile(path: String): Boolean {
        val normalized = path.trim('/')
        if (ignoredFolderPrefixes.any { normalized.startsWith(it, ignoreCase = true) }) return true
        return !normalized.contains('/') && normalized.lowercase() in rootFileNames
    }
}
