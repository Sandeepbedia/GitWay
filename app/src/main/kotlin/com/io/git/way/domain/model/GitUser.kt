package com.io.git.way.domain.model

/** The authenticated GitHub account, resolved after token validation. */
data class GitUser(
    val username: String,
    val avatarUrl: String?,
    val displayName: String?
)
