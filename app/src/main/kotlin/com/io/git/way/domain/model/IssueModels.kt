package com.io.git.way.domain.model

/** A GitHub issue in the selected repository. */
data class Issue(
    val number: Int,
    val title: String,
    val state: String,
    val body: String?,
    val htmlUrl: String?,
    val createdAt: String,
    val updatedAt: String?,
    val author: String,
    val commentsCount: Int,
    val isPullRequest: Boolean
) {
    val isOpen: Boolean get() = state == "open"
}

/** A comment on an issue. */
data class IssueComment(
    val id: Long,
    val body: String,
    val htmlUrl: String?,
    val createdAt: String,
    val author: String
)
