package com.io.git.way.domain.model

/** A GitHub pull request in the selected repository. */
data class PullRequest(
    val number: Int,
    val title: String,
    val state: String,
    val body: String?,
    val htmlUrl: String?,
    val createdAt: String,
    val updatedAt: String?,
    val mergedAt: String?,
    val headRef: String,
    val baseRef: String,
    val headLabel: String?,
    val author: String,
    val mergeable: Boolean?,
    val changedFiles: Int,
    val additions: Int,
    val deletions: Int,
    val commentsCount: Int = 0
) {
    val isOpen: Boolean get() = state == "open"
    val isMerged: Boolean get() = mergedAt != null
}

/** A single file changed by a pull request, with its unified diff patch. */
data class PullRequestFile(
    val filename: String,
    val status: String,
    val additions: Int,
    val deletions: Int,
    val patch: String?
)
