package com.io.git.way.data.repository

import android.util.Base64
import android.util.Log
import com.io.git.way.data.local.PathNormalizer
import com.io.git.way.data.local.TokenManager
import com.io.git.way.data.remote.GitHubApiService
import com.io.git.way.data.remote.dto.CreateBlobRequest
import com.io.git.way.data.remote.dto.CreateCommitRequest
import com.io.git.way.data.remote.dto.CreateRefRequest
import com.io.git.way.data.remote.dto.CreateTreeRequest
import com.io.git.way.data.remote.dto.GitHubErrorResponseDto
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Default [GitHubRepository]. Validates a token by calling GET /user; only persists
 * it (via [TokenManager]) once GitHub confirms it's valid, and wipes it again on failure
 * so an invalid token never lingers in encrypted storage.
 *
 * Also implements the Git Data API comparison + sync flow: [getRepositoryTree] feeds the
 * diff engine, [syncChanges] lands every Added/Modified/Removed path in one commit.
 *
 * Reworked under the "GitHub Push HTTP 422 Fix" PRD. The old version's only defense
 * against a 422 was a single hard-coded guess message and a fast-forward retry inside
 * the final ref update — any 422 from createBlob/createTree/createCommit (bad path,
 * stale base_tree, no push permission, archived repo, etc.) fell straight through to
 * the UI as Retrofit's raw "HTTP 422" with no explanation. Every call below now goes
 * through [githubCall]/[githubCallWithRetry], which parse GitHub's actual JSON error body.
 */
class GitHubRepositoryImpl(
    private val tokenManager: TokenManager,
    private val apiService: GitHubApiService
) : GitHubRepository {

    private companion object {
        const val TAG = "GitWayUpload"
        const val MAX_CONCURRENT_BLOBS = 6
        const val MAX_TRANSIENT_RETRIES = 3
        const val MAX_RACE_RETRIES = 4
    }

    private val errorJson = Json { ignoreUnknownKeys = true }

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
                        defaultBranch = dto.defaultBranch ?: "main",
                        language = dto.language,
                        createdAt = dto.createdAt.orEmpty()
                    )
                }
            }
        }

    // ===== §2 Validate Repository Before Upload =====

    override suspend fun validateRepositoryForUpload(repo: GitRepositoryModel): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!tokenManager.hasToken()) {
                    throw IOException("No GitHub token saved. Please sign in again.")
                }

                Log.d(TAG, "Validating ${repo.owner}/${repo.name} before upload")

                val fresh = try {
                    apiService.getRepository(repo.owner, repo.name)
                } catch (e: HttpException) {
                    val friendly = when (e.code()) {
                        404 -> "Repository \"${repo.owner}/${repo.name}\" no longer exists or the token can no longer see it."
                        401 -> "GitHub token is invalid or expired. Please sign in again."
                        else -> e.toFriendlyMessage()
                    }
                    throw IOException(friendly, e)
                }

                if (fresh.disabled) {
                    throw IOException("Repository \"${fresh.fullName}\" is disabled on GitHub and cannot accept pushes.")
                }
                if (fresh.archived) {
                    throw IOException("Repository \"${fresh.fullName}\" is archived (read-only). Unarchive it on GitHub before pushing.")
                }
                val perms = fresh.permissions
                if (perms != null && !perms.push) {
                    throw IOException("This token does not have push permission on \"${fresh.fullName}\".")
                }

                val branch = fresh.defaultBranch
                if (branch.isNullOrBlank()) {
                    throw IOException("Repository \"${fresh.fullName}\" has no resolvable default branch.")
                }

                Log.d(TAG, "Validation OK — branch=$branch push=${perms?.push} archived=${fresh.archived}")
                branch
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
                    // Empty/new default branch (no commits yet) -> treat as no remote files.
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
            onProgress(UploadPhase.VALIDATING, 0, changes.size, "")
            val branch = validateRepositoryForUpload(repo).getOrThrow()

            // §7 Normalize File Paths: fail fast, before any blob is created, rather than
            // letting GitHub bounce a bad path back as an opaque 422 mid-upload. The
            // original path is kept alongside the normalized one so file reads still
            // resolve correctly even if normalization altered the path.
            data class NormalizedChange(val original: FileChange, val cleanPath: String)
            val normalized = changes.map { change ->
                val cleanPath = try {
                    PathNormalizer.normalize(change.filePath)
                } catch (e: PathNormalizer.InvalidPathException) {
                    throw IOException("Invalid file path, cannot upload: \"${change.filePath}\"", e)
                }
                NormalizedChange(change, cleanPath)
            }

            val toUpload = normalized.filter { it.original.type != ChangeType.REMOVED }
            val toDelete = normalized.filter { it.original.type == ChangeType.REMOVED }
            val total = normalized.size
            val completed = AtomicInteger(0)

            onProgress(UploadPhase.CREATING_BLOBS, 0, total, "")
            Log.d(TAG, "Sync start: repo=${repo.owner}/${repo.name} branch=$branch total=$total (${toUpload.size} upload, ${toDelete.size} delete)")

            // §8 Skip Duplicate Files: unchanged files never reach here — ComparisonEngine
            // only emits ADDED/MODIFIED for paths whose hash actually differs.
            val semaphore = Semaphore(MAX_CONCURRENT_BLOBS)
            val blobEntries: List<TreeEntryInput> = coroutineScope {
                toUpload.map { change ->
                    async {
                        semaphore.withPermit {
                            val bytes = readFileBytes(change.original.filePath)
                            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            val blob = githubCallWithRetry {
                                apiService.createBlob(repo.owner, repo.name, CreateBlobRequest(content = base64))
                            }
                            val done = completed.incrementAndGet()
                            onProgress(UploadPhase.CREATING_BLOBS, done, total, change.cleanPath)
                            TreeEntryInput(path = change.cleanPath, sha = blob.sha)
                        }
                    }
                }.map { it.await() }
            }

            // Removed paths: null sha in the new tree entries deletes them.
            val deleteEntries = toDelete.map { change ->
                val done = completed.incrementAndGet()
                onProgress(UploadPhase.CREATING_BLOBS, done, total, change.cleanPath)
                TreeEntryInput(path = change.cleanPath, sha = null)
            }

            // §10 Atomic Upload: one tree + commit + ref update lands everything at once.
            // Nothing is written to the branch until this succeeds.
            val commitSha = finalizeCommit(
                owner = repo.owner,
                repoName = repo.name,
                branch = branch,
                treeEntries = blobEntries + deleteEntries,
                message = buildCommitMessage(changes),
                total = total,
                onProgress = onProgress
            )

            // §13 Success Validation: confirm the branch head really did move to our commit
            // before telling the user it worked.
            onProgress(UploadPhase.VERIFYING, total, total, "")
            val verifyRef = githubCallWithRetry { apiService.getRef(repo.owner, repo.name, branch) }
            if (verifyRef.objectRef.sha != commitSha) {
                Log.w(TAG, "Post-push verification mismatch: expected=$commitSha actual=${verifyRef.objectRef.sha}")
                throw IOException("Push reported success but GitHub's branch head doesn't match the new commit yet. Please check the repository or retry.")
            }
            Log.d(TAG, "Sync verified: commit=$commitSha branch=$branch")

            onProgress(UploadPhase.DONE, total, total, "")
            commitSha
        }
    }

    /**
     * Runs the create-tree -> create-commit -> update/create-ref sequence. Refreshes the
     * ref and base tree SHA from scratch on every attempt (§5/§6 "never reuse a cached
     * SHA") and retries the whole sequence if the ref update hits a 409 or 422
     * "not a fast-forward" (branch moved elsewhere, or GitHub's own read-replica lag
     * served a stale ref right after repo/ref creation) — §9 Retry Strategy's one
     * explicit exception to "never retry a 422".
     */
    private suspend fun finalizeCommit(
        owner: String,
        repoName: String,
        branch: String,
        treeEntries: List<TreeEntryInput>,
        message: String,
        total: Int,
        onProgress: (phase: UploadPhase, completed: Int, total: Int, currentFile: String) -> Unit
    ): String {
        var lastError: Throwable? = null
        repeat(MAX_RACE_RETRIES) { attempt ->
            val refResult = runCatching { githubCallWithRetry { apiService.getRef(owner, repoName, branch) } }
            val branchExists = refResult.isSuccess

            val baseTreeSha: String?
            val parents: List<String>
            if (branchExists) {
                val ref = refResult.getOrThrow()
                val commit = githubCallWithRetry { apiService.getCommit(owner, repoName, ref.objectRef.sha) }
                baseTreeSha = commit.tree.sha
                parents = listOf(ref.objectRef.sha)
            } else {
                // §4 Handle Empty Repository: no commits yet, so no base tree / parent —
                // and Update Reference is never called for this case (createRef is used below).
                baseTreeSha = null
                parents = emptyList()
            }

            onProgress(UploadPhase.CREATING_TREE, total, total, "")
            val newTree = githubCallWithRetry {
                apiService.createTree(owner, repoName, CreateTreeRequest(baseTree = baseTreeSha, tree = treeEntries))
            }

            onProgress(UploadPhase.CREATING_COMMIT, total, total, "")
            val newCommit = githubCallWithRetry {
                apiService.createCommit(
                    owner, repoName,
                    CreateCommitRequest(message = message, tree = newTree.sha, parents = parents)
                )
            }

            Log.d(TAG, "attempt=$attempt branchExists=$branchExists baseTree=$baseTreeSha newTree=${newTree.sha} newCommit=${newCommit.sha}")
            onProgress(UploadPhase.UPDATING_BRANCH, total, total, "")

            try {
                if (branchExists) {
                    githubCallWithRetry { apiService.updateRef(owner, repoName, branch, UpdateRefRequest(sha = newCommit.sha)) }
                } else {
                    githubCallWithRetry {
                        apiService.createRef(owner, repoName, CreateRefRequest(ref = "refs/heads/$branch", sha = newCommit.sha))
                    }
                }
                return newCommit.sha
            } catch (e: HttpException) {
                // errorBody() can only be read once, so capture it up front and reuse it
                // for both the fast-forward check and the friendly message below.
                val bodyText = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                val isRaceCondition = e.code() == 409 ||
                    (e.code() == 422 && bodyText?.contains("fast forward", ignoreCase = true) == true)
                if (isRaceCondition && attempt < MAX_RACE_RETRIES - 1) {
                    lastError = e
                    Log.w(TAG, "Ref update raced (code=${e.code()}), retrying from step 1 (attempt=$attempt)")
                    delay(500L * (attempt + 1))
                    return@repeat
                }
                throw IOException(e.toFriendlyMessage(bodyText), e)
            }
        }
        throw lastError ?: IOException("Failed to update branch ref after retries.")
    }

    private fun buildCommitMessage(changes: List<FileChange>): String {
        val added = changes.count { it.type == ChangeType.ADDED }
        val modified = changes.count { it.type == ChangeType.MODIFIED }
        val removed = changes.count { it.type == ChangeType.REMOVED }
        return "Git Way sync: $added added, $modified modified, $removed removed"
    }

    /** Wraps a raw API call so every HttpException carries GitHub's real error body
     * instead of a bare status code (§1 Improve Error Handling), plus a quota-aware
     * message for 403 rate limits. Does NOT retry — see [githubCallWithRetry] for the
     * transient-error variant used on the hot upload path. */
    private suspend fun <T> githubCall(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: HttpException) {
            throw IOException(e.toFriendlyMessage(), e)
        }
    }

    /**
     * §9 Retry Strategy: automatically retries network/socket timeouts and HTTP
     * 500/502/503/504 with backoff. Never retries 401/403/404/422 — those are shown to
     * the user immediately with GitHub's exact reason instead.
     */
    private suspend fun <T> githubCallWithRetry(block: suspend () -> T): T {
        var lastError: Throwable? = null
        repeat(MAX_TRANSIENT_RETRIES) { attempt ->
            try {
                return block()
            } catch (e: HttpException) {
                if (e.code() == 403) throw IOException(e.toFriendlyMessage(), e)
                val transient = e.code() in intArrayOf(500, 502, 503, 504)
                if (transient && attempt < MAX_TRANSIENT_RETRIES - 1) {
                    lastError = e
                    Log.w(TAG, "Transient HTTP ${e.code()}, retrying (attempt=$attempt)")
                    delay(400L * (attempt + 1))
                    return@repeat
                }
                // 401 / 403 / 404 / 422 (and any other non-retryable code) surface immediately.
                throw IOException(e.toFriendlyMessage(), e)
            } catch (e: SocketTimeoutException) {
                lastError = e
                if (attempt < MAX_TRANSIENT_RETRIES - 1) {
                    Log.w(TAG, "Socket timeout, retrying (attempt=$attempt)")
                    delay(400L * (attempt + 1))
                    return@repeat
                }
                throw IOException("Request timed out after $MAX_TRANSIENT_RETRIES attempts. Check your connection and try again.", e)
            } catch (e: IOException) {
                lastError = e
                if (attempt < MAX_TRANSIENT_RETRIES - 1) {
                    Log.w(TAG, "Network error, retrying (attempt=$attempt): ${e.message}")
                    delay(400L * (attempt + 1))
                    return@repeat
                }
                throw IOException("Network error after $MAX_TRANSIENT_RETRIES attempts: ${e.message}", e)
            }
        }
        throw lastError ?: IOException("Request failed after retries.")
    }

    /**
     * Parses GitHub's actual JSON error body ("message" + "errors[]") instead of
     * returning a bare "HTTP 422" (§1 Improve Error Handling). Falls back to a raw
     * body snippet, then to Retrofit's default message, only if parsing fails.
     *
     * [preReadBody] lets a caller that already consumed `errorBody()` (e.g. to check
     * for "fast forward" in [finalizeCommit]) pass that text back in — `errorBody()`'s
     * stream can only be read once, so a second `.string()` call here would throw.
     */
    private fun HttpException.toFriendlyMessage(preReadBody: String? = null): String {
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

        val rawBody = preReadBody ?: runCatching { response()?.errorBody()?.string() }.getOrNull()
        if (!rawBody.isNullOrBlank()) {
            val parsed = runCatching { errorJson.decodeFromString(GitHubErrorResponseDto.serializer(), rawBody) }.getOrNull()
            if (parsed != null) {
                val detail = parsed.errors
                    ?.mapNotNull { it.message ?: it.code }
                    ?.filter { it.isNotBlank() }
                    ?.joinToString("; ")
                val base = parsed.message?.takeIf { it.isNotBlank() } ?: "GitHub returned an error (${code()})"
                return if (!detail.isNullOrBlank()) "$base — $detail" else base
            }
            // Body wasn't the expected shape (rare) — better than nothing.
            return "GitHub returned HTTP ${code()}: ${rawBody.take(300)}"
        }

        return message()?.takeIf { it.isNotBlank() } ?: "GitHub API error ${code()}"
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
