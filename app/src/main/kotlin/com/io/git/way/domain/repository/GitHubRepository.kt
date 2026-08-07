/*
 * Git Way
 * Copyright (C) 2026 Sandeep Bedia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.io.git.way.domain.repository

import com.io.git.way.domain.model.AppUpdateInfo
import com.io.git.way.domain.model.ApiRateLimit
import com.io.git.way.domain.model.CommitSummary
import com.io.git.way.domain.model.FileChange
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.domain.model.GitUser
import com.io.git.way.domain.model.RemoteTreeEntry
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
    suspend fun getRepositoryTree(repo: GitRepository, branch: String? = null): Result<Map<String, String>>

    /** Same tree as [getRepositoryTree] but also carries each blob's size — used by the
     * Explorer's file-metadata display (PRD "Repository Explorer" §7). A thin wrapper
     * over the same underlying call so callers that only need [getRepositoryTree]'s
     * plain sha map don't pay for anything extra. */
    suspend fun getRepositoryTreeDetailed(repo: GitRepository, branch: String? = null): Result<Map<String, RemoteTreeEntry>>

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
     * [targetBranch] pushes to that branch instead of the repo's default — null (the
     * default) preserves the original always-push-to-default behavior.
     */
    suspend fun syncChanges(
        repo: GitRepository,
        changes: List<FileChange>,
        commitMessage: String,
        targetBranch: String? = null,
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

    /**
     * Checks GitHub's latest release against [currentVersionName] and returns update
     * info if a newer tagged release exists, or `null` if the app is already current
     * (or the repo has no releases yet). Never throws for "no releases" (404) — that's
     * treated as "no update", not a failure; only genuine network/parse errors surface
     * as [Result.failure].
     */
    suspend fun checkForUpdate(
        owner: String,
        repo: String,
        currentVersionName: String
    ): Result<AppUpdateInfo?>

    // ===== Repository Management =====

    /** Every branch's name, most-recently-pushed first (GitHub's own default order). */
    suspend fun listBranches(repo: GitRepository): Result<List<String>>

    /** Creates [newBranchName] pointing at whatever [fromBranch] currently resolves to —
     * a real "fork this branch", not an empty new ref. */
    suspend fun createBranch(repo: GitRepository, newBranchName: String, fromBranch: String): Result<Unit>

    /** Read-only commit log for [branch] (or the repo's default branch if null), most
     * recent first. Git Way still never rewrites or amends history — this is purely
     * "what's already there". */
    suspend fun getCommitHistory(repo: GitRepository, branch: String? = null): Result<List<CommitSummary>>

    /** Creates a brand-new GitHub repository for the authenticated user. */
    suspend fun createRepository(name: String, description: String, isPrivate: Boolean): Result<GitRepository>

    /** Renames/redescribes/re-visibilities [repo] — any null parameter leaves that field
     * unchanged on GitHub. */
    suspend fun updateRepository(
        repo: GitRepository,
        newName: String? = null,
        newDescription: String? = null,
        newIsPrivate: Boolean? = null
    ): Result<GitRepository>

    /** Permanently deletes [repo] from GitHub. Irreversible — the caller is responsible
     * for making the user confirm before calling this. */
    suspend fun deleteRepository(repo: GitRepository): Result<Unit>

    /** Re-fetches the authenticated account's full profile — same endpoint
     * [validateTokenAndFetchUser] uses, but callable any time a screen (Overview,
     * Profile) needs it without re-validating/re-saving the token. */
    suspend fun getCurrentUser(): Result<GitUser>

    /** Current GitHub API core rate limit for this token — powers the Overview
     * dashboard's usage meter. */
    suspend fun getApiRateLimit(): Result<ApiRateLimit>
}
