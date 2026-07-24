package com.io.git.way.data.repository

import android.util.Base64
import com.io.git.way.data.local.TokenManager
import com.io.git.way.data.remote.GitHubApiService
import com.io.git.way.data.remote.dto.CreateBlobRequest
import com.io.git.way.data.remote.dto.CreateCommitRequest
import com.io.git.way.data.remote.dto.CreateRefRequest
import com.io.git.way.data.remote.dto.CreateTreeRequest
import com.io.git.way.data.remote.dto.TreeEntryInput
import com.io.git.way.data.remote.dto.UpdateRefRequest
import com.io.git.way.domain.model.ChangeType
import com.io.git.way.domain.model.FileChange
import com.io.git.way.domain.model.GitRepository as GitRepositoryModel
import com.io.git.way.domain.model.GitUser
import com.io.git.way.domain.model.UploadPhase
import com.io.git.way.domain.repository.GitHubRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Default [GitHubRepository]. Validates a token by calling GET /user; only persists
 * it (via [TokenManager]) once GitHub confirms it's valid, and wipes it again on failure
 * so an invalid token never lingers in encrypted storage.
 *
 * Also implements the Git Data API comparison + sync flow from PRD1/PRD2: [getRepositoryTree]
 * feeds the diff engine, [syncChanges] lands every Added/Modified/Removed path in one commit.
 */
class GitHubRepositoryImpl(
    private val tokenManager: TokenManager,
    private val apiService: GitHubApiService
) : GitHubRepository {

    private companion object {
        const val MAX_CONCURRENT_BLOBS = 6
    }

    override suspend fun validateTokenAndFetchUser(token: String): Result<GitUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                tokenManager.saveToken(token)
                val user = apiService.getAuthenticatedUser()
                GitUser(
                    username = user.login,
                    avatarUrl = user.avatarUrl,
                    displayName = user.name
                )
            }.onFailure {
                tokenManager.clearToken()
            }
        }

    override suspend fun listRepositories(): Result<List<GitRepositoryModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                apiService.listRepositories().map { dto ->
                    GitRepositoryModel(
                        name = dto.name,
                        fullName = dto.fullName,
                        owner = dto.owner.login,
                        isPrivate = dto.isPrivate,
                        lastUpdated = dto.updatedAt.orEmpty(),
                        defaultBranch = dto.defaultBranch ?: "main"
                    )
                }
            }
        }

    override suspend fun getRepositoryTree(repo: GitRepositoryModel): Result<Map<String, String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                try {
                    val treeResponse = githubCall {
                        apiService.getTree(repo.owner, repo.name, repo.defaultBranch, recursive = 1)
                    }
                    treeResponse.tree
                        .filter { it.type == "blob" }
                        .associate { it.path to it.sha.orEmpty() }
                } catch (e: HttpException) {
                    // PRD1 §3.6: empty/new default branch (no commits yet) -> treat as no remote files.
                    if (e.code() == 404 || e.code() == 409) emptyMap() else throw IOException(e.toFriendlyMessage(), e)
                }
            }
        }

    override suspend fun syncChanges(
        repo: GitRepositoryModel,
        changes: List<FileChange>,
        readFileBytes: suspend (relativePath: String) -> ByteArray,
        onProgress: (phase: UploadPhase, completed: Int, total: Int, currentFile: String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val toUpload = changes.filter { it.type != ChangeType.REMOVED }
            val toDelete = changes.filter { it.type == ChangeType.REMOVED }
            val total = changes.size
            val completed = AtomicInteger(0)

            onProgress(UploadPhase.PREPARING, 0, total, "")

            // Phase 1: create a blob for every Added/Modified file (limited concurrency —
            // this dominates total upload time for many small files, PRD2 §3.3).
            val semaphore = Semaphore(MAX_CONCURRENT_BLOBS)
            val blobEntries: List<TreeEntryInput> = coroutineScope {
                toUpload.map { change ->
                    async {
                        semaphore.withPermit {
                            val bytes = readFileBytes(change.filePath)
                            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            val blob = githubCall {
                                apiService.createBlob(repo.owner, repo.name, CreateBlobRequest(content = base64))
                            }
                            val done = completed.incrementAndGet()
                            onProgress(UploadPhase.PREPARING, done, total, change.filePath)
                            TreeEntryInput(path = change.filePath, sha = blob.sha)
                        }
                    }
                }.map { it.await() }
            }

            // Removed paths: null sha in the new tree entries deletes them (PRD2 §3.1 step 4).
            val deleteEntries = toDelete.map { change ->
                val done = completed.incrementAndGet()
                onProgress(UploadPhase.PREPARING, done, total, change.filePath)
                TreeEntryInput(path = change.filePath, sha = null)
            }

            onProgress(UploadPhase.FINALIZING, total, total, "")

            // Phase 2: one tree + commit + ref update lands everything atomically. Nothing is
            // written to the branch until this succeeds (PRD2 §3.4 key safety property).
            val commitSha = finalizeCommit(
                owner = repo.owner,
                repoName = repo.name,
                branch = repo.defaultBranch,
                treeEntries = blobEntries + deleteEntries,
                message = buildCommitMessage(changes)
            )

            onProgress(UploadPhase.DONE, total, total, "")
            commitSha
        }
    }

    /**
     * Runs PRD2 §3.1 steps 1-2 and 5-7. Retries the whole sequence once from step 1 if the
     * ref update hits a 409 (branch moved since we read it) — PRD2 §3.4.
     */
    private suspend fun finalizeCommit(
        owner: String,
        repoName: String,
        branch: String,
        treeEntries: List<TreeEntryInput>,
        message: String
    ): String {
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            val refResult = runCatching { githubCall { apiService.getRef(owner, repoName, branch) } }
            val branchExists = refResult.isSuccess

            val baseTreeSha: String?
            val parents: List<String>
            if (branchExists) {
                val ref = refResult.getOrThrow()
                val commit = githubCall { apiService.getCommit(owner, repoName, ref.objectRef.sha) }
                baseTreeSha = commit.tree.sha
                parents = listOf(ref.objectRef.sha)
            } else {
                baseTreeSha = null
                parents = emptyList()
            }

            val newTree = githubCall {
                apiService.createTree(owner, repoName, CreateTreeRequest(baseTree = baseTreeSha, tree = treeEntries))
            }
            val newCommit = githubCall {
                apiService.createCommit(
                    owner, repoName,
                    CreateCommitRequest(message = message, tree = newTree.sha, parents = parents)
                )
            }

            try {
                if (branchExists) {
                    githubCall { apiService.updateRef(owner, repoName, branch, UpdateRefRequest(sha = newCommit.sha)) }
                } else {
                    githubCall {
                        apiService.createRef(owner, repoName, CreateRefRequest(ref = "refs/heads/$branch", sha = newCommit.sha))
                    }
                }
                return newCommit.sha
            } catch (e: HttpException) {
                if (e.code() == 409 && attempt == 0) {
                    lastError = e
                    return@repeat
                }
                throw IOException(e.toFriendlyMessage(), e)
            }
        }
        throw lastError ?: IOException("Failed to update branch ref after retry.")
    }

    private fun buildCommitMessage(changes: List<FileChange>): String {
        val added = changes.count { it.type == ChangeType.ADDED }
        val modified = changes.count { it.type == ChangeType.MODIFIED }
        val removed = changes.count { it.type == ChangeType.REMOVED }
        return "Git Way sync: $added added, $modified modified, $removed removed"
    }

    /** Wraps a raw API call so 403 rate-limit responses surface a quota-aware message. */
    private suspend fun <T> githubCall(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: HttpException) {
            if (e.code() == 403) throw IOException(e.toFriendlyMessage(), e)
            throw e
        }
    }

    private fun HttpException.toFriendlyMessage(): String {
        if (code() == 403) {
            val headers = response()?.headers()
            val remaining = headers?.get("X-RateLimit-Remaining")
            val reset = headers?.get("X-RateLimit-Reset")
            return if (remaining == "0" && reset != null) {
                "GitHub API rate limit reached. Resets at ${formatResetTime(reset)}."
            } else {
                "GitHub request forbidden (403). Check the token's repo permissions."
            }
        }
        return message() ?: "GitHub API error ${code()}"
    }

    private fun formatResetTime(resetEpochSeconds: String): String = try {
        val millis = resetEpochSeconds.toLong() * 1000
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
    } catch (e: Exception) {
        "later"
    }

    override fun hasToken(): Boolean = tokenManager.hasToken()

    override fun clearToken() = tokenManager.clearToken()
}
