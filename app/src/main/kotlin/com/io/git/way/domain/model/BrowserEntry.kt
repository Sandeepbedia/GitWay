package com.io.git.way.domain.model

/** A single file or folder row in the Repository Browser (file-manager view of a repo). */
data class BrowserEntry(
    val name: String,
    val path: String,
    val isFolder: Boolean
)
