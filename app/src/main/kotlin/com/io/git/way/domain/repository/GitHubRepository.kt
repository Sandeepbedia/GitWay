/*
 * GitWay — an Android client for GitHub.
 *
 * This file is part of GitWay. GitWay is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * GitWay is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GitWay. If not, see <https://www.gnu.org/licenses/>.
 */
package com.io.git.way.domain.repository

import com.io.git.way.domain.model.AppUpdateInfo
import com.io.git.way.domain.model.ApiRateLimit
import com.io.git.way.domain.model.ArtifactInfo
import com.io.git.way.domain.model.CodeSearchResult
import com.io.git.way.domain.model.CommitDiffFile
import com.io.git.way.domain.model.CommitSummary
import com.io.git.way.domain.model.FileChange
import com.io.git.way.domain.model.GitHubWorkflow
import com.io.git.way.domain.model.GitRelease
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.domain.model.GitUser
import com.io.git.way.domain.model.Issue
import com.io.git.way.domain.model.IssueComment
import com.io.git.way.domain.model.PullRequest
import com.io.git.way.domain.model.PullRequestFile
import com.io.git.way.domain.model.ReleaseAsset
import com.io.git.way.domain.model.RemoteTreeEntry
import com.io.git.way.domain.model.TokenValidationResult
import com.io.git.way.domain.model.UploadPhase
import com.io.git.way.domain.model.WorkflowRun
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Abstraction over GitHub authentication + repository access. */
interface GitHubRepository {

    /** Emits immediately whenever the active GitHub account changes. */
    val activeAccountFlow: StateFlow<String?>

    /** Emits after a successful remote write so list/browser screens can refresh without restart. */
    val remoteDataChanged: SharedFlow<Unit>

    suspend fun validateTokenAndFetchUser(token: String): Result<TokenValidationResult>

    /** Lists all repositories accessible to the currently stored token. */
    suspend fun listRepositories(): Result<List<GitRepository>>

    suspend fun getRepositoryTree(repo: GitRepository, branch: String? = null): Result<Map<String, String>>

    suspend fun getRepositoryTreeDetailed(repo: GitRepository, branch: String? = null): Result<Map<String, RemoteTreeEntry>>

    suspend fun validateRepositoryForUpload(repo: GitRepository): Result<String>

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

    suspend fun getFileContent(repo: GitRepository, blobSha: String): Result<ByteArray>

    suspend fun checkForUpdate(
        owner: String,
        repo: String,
        currentVersionName: String
    ): Result<AppUpdateInfo?>

    // ===== Repository Management =====

    /** Every branch's name, most-recently-pushed first (GitHub's own default order). */
    suspend fun listBranches(repo: GitRepository): Result<List<String>>

    suspend fun createBranch(repo: GitRepository, newBranchName: String, fromBranch: String): Result<Unit>

    suspend fun getCommitHistory(repo: GitRepository, branch: String? = null): Result<List<CommitSummary>>

    /** Creates a brand-new GitHub repository for the authenticated user. */
    suspend fun createRepository(name: String, description: String, isPrivate: Boolean): Result<GitRepository>

    suspend fun updateRepository(
        repo: GitRepository,
        newName: String? = null,
        newDescription: String? = null,
        newIsPrivate: Boolean? = null
    ): Result<GitRepository>

    suspend fun deleteRepository(repo: GitRepository): Result<Unit>

    suspend fun getCurrentUser(): Result<GitUser>

    suspend fun getApiRateLimit(): Result<ApiRateLimit>

    // ===== GitHub Actions (workflow scope) =====

    /** Every workflow file registered in [repo], for the Actions screen. */
    suspend fun listWorkflows(repo: GitRepository): Result<List<GitHubWorkflow>>

    /** Recent workflow runs, most recent first. [workflowId]/[branch] filter when set. */
    suspend fun listWorkflowRuns(
        repo: GitRepository,
        workflowId: Long? = null,
        branch: String? = null,
        status: String? = null
    ): Result<List<WorkflowRun>>

    /** Manually triggers a workflow that declares a `workflow_dispatch` trigger. */
    suspend fun triggerWorkflow(repo: GitRepository, workflowId: Long, ref: String): Result<Unit>

    /** Cancels an in-progress run. No-op (still success) if it already finished. */
    suspend fun cancelWorkflowRun(repo: GitRepository, runId: Long): Result<Unit>

    /** Re-runs only the failed jobs of a completed run. */
    suspend fun rerunFailedJobs(repo: GitRepository, runId: Long): Result<Unit>

    /** Build artifacts (APKs etc.) produced by Actions runs. */
    suspend fun listArtifacts(repo: GitRepository): Result<List<ArtifactInfo>>

    /** Downloads an artifact's zip archive as raw bytes. */
    suspend fun downloadArtifactZip(repo: GitRepository, artifactId: Long): Result<ByteArray>

    /** Downloads a run's log zip archive as raw bytes. */
    suspend fun downloadRunLogs(repo: GitRepository, runId: Long): Result<ByteArray>

    // ===== Pull requests =====

    suspend fun listPullRequests(repo: GitRepository, state: String = "open"): Result<List<PullRequest>>

    suspend fun createPullRequest(
        repo: GitRepository,
        title: String,
        head: String,
        base: String,
        body: String?
    ): Result<PullRequest>

    /** Closes (state = "closed") or reopens (state = "open") a PR. */
    suspend fun updatePullRequestState(repo: GitRepository, number: Int, state: String): Result<PullRequest>

    suspend fun mergePullRequest(repo: GitRepository, number: Int): Result<Unit>

    suspend fun listPullRequestFiles(repo: GitRepository, number: Int): Result<List<PullRequestFile>>

    // ===== Issues =====

    suspend fun listIssues(repo: GitRepository, state: String = "open"): Result<List<Issue>>

    suspend fun createIssue(repo: GitRepository, title: String, body: String?): Result<Issue>

    suspend fun updateIssueState(repo: GitRepository, number: Int, state: String): Result<Issue>

    suspend fun listIssueComments(repo: GitRepository, number: Int): Result<List<IssueComment>>

    suspend fun createIssueComment(repo: GitRepository, number: Int, body: String): Result<IssueComment>

    // ===== Star / unstar / fork =====

    /** Full-name (owner/repo) set of every repo the token user has starred. */
    suspend fun listStarredRepositories(): Result<Set<String>>

    suspend fun starRepository(repo: GitRepository): Result<Unit>

    suspend fun unstarRepository(repo: GitRepository): Result<Unit>

    /** Creates a fork of [repo] under the authenticated user. Returns the new repo. */
    suspend fun forkRepository(repo: GitRepository): Result<GitRepository>

    // ===== Releases (full management) =====

    suspend fun listReleases(repo: GitRepository): Result<List<GitRelease>>

    suspend fun createRelease(
        repo: GitRepository,
        tagName: String,
        name: String?,
        body: String?,
        draft: Boolean,
        prerelease: Boolean,
        targetCommitish: String?
    ): Result<GitRelease>

    suspend fun deleteRelease(repo: GitRepository, releaseId: Long): Result<Unit>

    suspend fun listReleaseAssets(repo: GitRepository, releaseId: Long): Result<List<ReleaseAsset>>

    /** Uploads [bytes] as an asset ([fileName]) of an existing release. */
    suspend fun uploadReleaseAsset(
        repo: GitRepository,
        releaseId: Long,
        fileName: String,
        bytes: ByteArray
    ): Result<ReleaseAsset>

    // ===== Search + commit diff + archive =====

    suspend fun searchRepositories(query: String): Result<List<GitRepository>>

    suspend fun searchCode(query: String, owner: String, repo: String): Result<List<CodeSearchResult>>

    /** Full detail of one commit, including per-file diffs. */
    suspend fun getCommitDiff(repo: GitRepository, sha: String): Result<List<CommitDiffFile>>

    /** Downloads the repo's source archive (zipball) for [ref] as raw bytes. */
    suspend fun downloadRepoZip(repo: GitRepository, ref: String? = null): Result<ByteArray>

    /** Downloads a release asset's raw bytes by its GitHub asset id. */
    suspend fun downloadReleaseAsset(repo: GitRepository, assetId: Long): Result<ByteArray>
}
