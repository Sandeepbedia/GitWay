package com.io.git.way.domain.model

/**
 * A detected app identity — package/applicationId (the primary, always-present signal)
 * plus, when it could be resolved, the human-readable app name (Android's
 * `android:label`, resolved through strings.xml if it's a `@string/...` reference).
 * Produced by [com.io.git.way.data.local.AppIdentityDetector] for either the local
 * project folder or a GitHub repository's file tree, so the two can be compared before
 * any diff/upload runs (PRD "Repository Project Validation & Safe Upload System").
 */
data class AppIdentity(
    val packageName: String,
    val appName: String? = null,
    val sourceFile: String
)
