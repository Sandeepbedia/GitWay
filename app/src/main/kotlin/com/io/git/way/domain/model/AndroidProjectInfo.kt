package com.io.git.way.domain.model

/**
 * Android Project Intelligence (PRD "Repository Explorer" §16): what Git Way could
 * detect about the repository from its build files, shown as a summary card at the
 * root of the Explorer. Every field besides [packageName] is best-effort — a project
 * using version catalogs or a non-standard build setup may leave some of them null.
 */
data class AndroidProjectInfo(
    val packageName: String,
    val minSdk: String? = null,
    val targetSdk: String? = null,
    val compileSdk: String? = null,
    val language: String = "Kotlin",
    val buildSystem: String = "Gradle"
)
