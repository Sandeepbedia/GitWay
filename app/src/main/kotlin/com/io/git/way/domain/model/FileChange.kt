package com.io.git.way.domain.model

enum class ChangeType { ADDED, MODIFIED, REMOVED }

/** A single file-level diff between the local project and the GitHub repository. */
data class FileChange(
    val fileName: String,
    val filePath: String,
    val type: ChangeType
)
