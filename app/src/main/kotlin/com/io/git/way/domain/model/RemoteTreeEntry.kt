package com.io.git.way.domain.model

/** One blob in a GitHub repository's file tree — sha (for content/diff lookups) plus
 * its size in bytes, when GitHub reports one. See [com.io.git.way.domain.repository.GitHubRepository.getRepositoryTreeDetailed]. */
data class RemoteTreeEntry(
    val sha: String,
    val size: Long?
)
