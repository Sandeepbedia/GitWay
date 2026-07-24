package com.io.git.way.domain.model

import android.net.Uri

/**
 * A single file discovered while walking the user-selected local project folder
 * (PRD1 "Folder Selection Screen"). [relativePath] is forward-slash separated and
 * relative to the selected root, matching GitHub's path format (e.g. "app/src/Main.kt").
 */
data class LocalFile(
    val relativePath: String,
    val displayName: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val documentUri: Uri
)
