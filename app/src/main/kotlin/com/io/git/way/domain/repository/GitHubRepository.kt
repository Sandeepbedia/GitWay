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
     * Re-checks [repo] directly against GitHub right before upload: still exists, token
     * still has push permission, not archived/disabled, and has a resolvable default
     * branch (422-fix PRD §2 "Validate Repository Before Upload"). Returns the fresh
     * default branch name on success — never the possibly-stale one cached on [repo].
     */
    suspend fun validateRepositoryForUpload(repo: GitRepository): Result<String>

    /**
     * Uploads every [changes] entry to GitHub as a single commit using the Git Data API
     * (PRD2 §3.1 approach B): blobs are created (with limited concurrency) for every
     * ADDED/MODIFIED path, then one tree + commit + ref update lands the whole change set
     * atomically. Nothing is written to the branch until the final ref update, so a failure
     * before then leaves the repo untouched. Returns the new commit sha on success.
     * [commitMessage] is always the final, resolved text for this commit (the caller has
     * already substituted its own default if the user left the field blank) — this
     * function never invents or alters it, so what the user sees on the Confirmation
     * screen is exactly what lands on GitHub.
     */
    suspend fun syncChanges(
        repo: GitRepository,
        changes: List<FileChange>,
        commitMessage: String,
        readFileBytes: suspend (relativePath: String) -> ByteArray,
        onProgress: (phase: UploadPhase, completed: Int, total: Int, currentFile: String) -> Unit
    ): Result<String>

    fun hasToken(): Boolean

    /** Removes the stored token (PRD "Security Requirements > Clear token option"). */
    fun clearToken()

    /**
     * Reads a file's raw bytes from GitHub by its blob sha (already known from the cached
     * repo tree — see [getRepositoryTree]). Powers the Repository Browser's read/edit view.
     */
    suspend fun getFileContent(repo: GitRepository, blobSha: String): Result<ByteArray>
}
