package com.io.git.way.domain.model

/** One file's change inside a commit, including its unified diff text. */
data class CommitDiffFile(
    val filename: String,
    val status: String,
    val additions: Int,
    val deletions: Int,
    val changes: Int,
    val patch: String?
)

/** Result of a GitHub code search hit. */
data class CodeSearchResult(
    val name: String,
    val path: String,
    val htmlUrl: String?,
    val repositoryFullName: String
)
