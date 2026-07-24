package com.io.git.way.domain.model

/** A GitHub repository accessible by the authenticated user's token. */
data class GitRepository(
    val name: String,
    val fullName: String,
    val owner: String,
    val isPrivate: Boolean,
    val lastUpdated: String,
    val defaultBranch: String = "main",
    val language: String? = null,
    val createdAt: String = ""
)
