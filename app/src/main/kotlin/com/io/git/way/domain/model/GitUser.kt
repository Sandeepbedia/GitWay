package com.io.git.way.domain.model

/** The authenticated GitHub account — resolved after token validation, and re-fetchable
 * any time via [com.io.git.way.domain.repository.GitHubRepository.getCurrentUser] for
 * screens (Overview, Profile) that need it without re-validating the token. */
data class GitUser(
    val username: String,
    val avatarUrl: String?,
    val displayName: String?,
    val bio: String? = null,
    val company: String? = null,
    val location: String? = null,
    val htmlUrl: String = "",
    val publicRepos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val createdAt: String = ""
)

/** GitHub API core rate limit — surfaced on the Overview dashboard so a heavy user can
 * see when they're approaching the hourly cap instead of hitting an opaque 403. */
data class ApiRateLimit(
    val limit: Int,
    val remaining: Int,
    /** Epoch seconds when the limit resets. */
    val resetEpochSeconds: Long
)
