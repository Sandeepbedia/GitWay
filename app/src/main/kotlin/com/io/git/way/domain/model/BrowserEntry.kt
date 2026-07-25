package com.io.git.way.domain.model

/** A single file or folder row in the Repository Browser (file-manager view of a repo). */
data class BrowserEntry(
    val name: String,
    val path: String,
    val isFolder: Boolean
)

/** One visible row in the VS Code-style tree explorer: an entry plus how deeply nested
 * it is, so the UI can indent it. Folders only appear expanded (their children included
 * right after them) when their path is in the view model's expanded-folders set. */
data class TreeRow(
    val entry: BrowserEntry,
    val depth: Int,
    val isExpanded: Boolean = false
)
