package com.io.git.way.domain.model

/**
 * A detected Android app package/applicationId, plus which file it came from.
 * Produced by [com.io.git.way.data.local.AppIdentityDetector] for either the local
 * project folder or a GitHub repository's file tree, so the two can be compared before
 * any diff/upload runs (PRD "Repository / Project Match Protection").
 */
data class AppIdentity(
    val packageName: String,
    val sourceFile: String
)
