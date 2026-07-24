package com.io.git.way.domain.repository

import com.io.git.way.domain.model.FileChange
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.domain.model.GitUser
import com.io.git.way.domain.model.UploadPhase

/** Abstraction over GitHub authentication + repository access. */
interface GitHubRepository {

    /** Validates [token] against the GitHub API and, on success, persists it securely. */
    suspend fun validateTokenAndFetchUser(token: String): Result<GitUser>

    /** Lists all repositories accessible to the currently stored token. */
    suspend fun listRepositories(): Result<List<GitRepository>>

    /**
     * Fetches the full recursive file tree of [repo]'s default branch as a map of
     * relativePath -> blob sha (PRD1 §3.2). Returns an empty map (not a failure) when the
     * default branch has no commits yet (PRD1 §3.6 "empty/new default branch").
     */
    suspend fun getRepositoryTree(repo: GitRepository): Result<Map<String, String>>

    /**
     * Uploads every [changes] entry to GitHub as a single commit using the Git Data API
     * (PRD2 §3.1 approach B): blobs are created (with limited concurrency) for every
     * ADDED/MODIFIED path, then one tree + commit + ref update lands the whole change set
     * atomically. Nothing is written to the branch until the final ref update, so a failure
     * before then leaves the repo untouched. Returns the new commit sha on success.
     */
    suspend fun syncChanges(
        repo: GitRepository,
        changes: List<FileChange>,
        readFileBytes: suspend (relativePath: String) -> ByteArray,
        onProgress: (phase: UploadPhase, completed: Int, total: Int, currentFile: String) -> Unit
    ): Result<String>

    fun hasToken(): Boolean

    /** Removes the stored token (PRD "Security Requirements > Clear token option"). */
    fun clearToken()
}
