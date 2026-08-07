package com.io.git.way.domain.model

/** One entry in a repository's commit history (read-only — Git Way still only ever
 * writes new commits via [com.io.git.way.domain.repository.GitHubRepository.syncChanges]). */
data class CommitSummary(
    val sha: String,
    val message: String,
    val authorName: String,
    val date: String,
    val htmlUrl: String
) {
    val shortSha: String get() = sha.take(7)
    /** First line only — commit bodies can be long, the history list just needs the title. */
    val title: String get() = message.substringBefore('\n')
}
