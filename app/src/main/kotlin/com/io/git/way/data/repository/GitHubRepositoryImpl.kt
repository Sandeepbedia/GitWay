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
package com.io.git.way.data.repository

import android.util.Base64
import android.util.Log
import com.io.git.way.data.local.PathNormalizer
import com.io.git.way.data.local.TokenManager
import com.io.git.way.data.remote.GitHubApiService
import com.io.git.way.data.remote.dto.CreateBlobRequest
import com.io.git.way.data.remote.dto.CreateCommitRequest
import com.io.git.way.data.remote.dto.CreateFileContentRequest
import com.io.git.way.data.remote.dto.CreateForkRequest
import com.io.git.way.data.remote.dto.CreateIssueCommentRequest
import com.io.git.way.data.remote.dto.CreateIssueRequest
import com.io.git.way.data.remote.dto.CreatePullRequestRequest
import com.io.git.way.data.remote.dto.CreateRefRequest
import com.io.git.way.data.remote.dto.CreateReleaseRequest
import com.io.git.way.data.remote.dto.CreateTreeRequest
import com.io.git.way.data.remote.dto.CreateRepoRequest
import com.io.git.way.data.remote.dto.GitHubErrorResponseDto
import com.io.git.way.data.remote.dto.GitHubPullRequestDto
import com.io.git.way.data.remote.dto.GitHubRepoDto
import com.io.git.way.data.remote.dto.GitHubUserDto
import com.io.git.way.data.remote.dto.GitHubWorkflowDispatchRequest
import com.io.git.way.data.remote.dto.MergePullRequestRequest
import com.io.git.way.data.remote.dto.TreeEntryInput
import com.io.git.way.data.remote.dto.UpdateIssueRequest
import com.io.git.way.data.remote.dto.UpdatePullRequestRequest
import com.io.git.way.data.remote.dto.UpdateRefRequest
import com.io.git.way.data.remote.dto.UpdateRepoRequest
import com.io.git.way.domain.VersionComparator
import com.io.git.way.domain.model.ApiRateLimit
import com.io.git.way.domain.model.AppUpdateInfo
import com.io.git.way.domain.model.ArtifactInfo
import com.io.git.way.domain.model.ChangeType
import com.io.git.way.domain.model.CodeSearchResult
import com.io.git.way.domain.model.CommitDiffFile
import com.io.git.way.domain.model.CommitSummary
import com.io.git.way.domain.model.FileChange
import com.io.git.way.domain.model.GitHubWorkflow
import com.io.git.way.domain.model.GitRelease
import com.io.git.way.domain.model.GitRepository as GitRepositoryModel
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
import com.io.git.way.domain.repository.GitHubRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
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
        // Post-push verification (§13): a handful of quick retries to absorb a brief
        // read-after-write lag on GitHub's side, not a real mismatch.
        const val MAX_VERIFY_RETRIES = 4
    }

    private val errorJson = Json { ignoreUnknownKeys = true }

    // The Retrofit-wide Json (see RetrofitProvider) sets explicitNulls = false so
    // partial PATCH bodies never send e.g. "name": null. But GitHub's Git Trees API
    // uses that exact shape — an entry with sha explicitly set to null — to mean
    // "delete this path from the tree" (https://docs.github.com/rest/git/trees).
    // With explicitNulls = false, a delete entry's null sha was silently OMITTED,
    // leaving the entry with neither `sha` nor `content`, which is exactly what
    // GitHub's "Must supply either tree.sha or tree.content" error is complaining
    // about. This instance is only used for encoding [CreateTreeRequest] bodies —
    // baseTree still correctly omits itself when null via @EncodeDefault(NEVER),
    // which happens independently of explicitNulls.
    private val treeJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = true }

    /** The Contents API path segment needs each directory/file name percent-encoded
     * (spaces, unicode, etc.) but the "/" separators themselves must survive — this
     * endpoint is declared with `@Path(..., encoded = true)` specifically so Retrofit
     * doesn't also escape those slashes into "%2F", which would turn a nested path like
     * "src/main/MainActivity.kt" into a single invalid path segment. */
    private fun String.encodedForContentsPath(): String =
        split("/").joinToString("/") { segment ->
            java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
        }

    override suspend fun validateTokenAndFetchUser(token: String): Result<TokenValidationResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                tokenManager.saveToken(token)
                val stored = tokenManager.getToken()
                Log.d("TokenValidation", "pasted len=${token.length} prefix=${token.take(6)} | stored len=${stored?.length} prefix=${stored?.take(6)}")
                val response = apiService.getAuthenticatedUser()
                Log.d("TokenValidation", "code=${response.code()}")
                if (!response.isSuccessful) throw HttpException(response)
                TokenValidationResult(
                    user = response.body()!!.toDomain(),
                    // Classic PATs report their scopes in this header; fine-grained
                    // tokens (and scoped-down classic tokens) send an empty header, in
                    // which case the UI shows a "couldn't verify" warning instead.
                    grantedScopes = response.headers()["X-OAuth-Scopes"]
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?.toSet()
                        ?: emptySet()
                )
            }.onFailure {
                Log.d("TokenValidation", "failure=${it::class.simpleName}: ${it.message}")
                tokenManager.clearToken()
            }
        }

    private fun GitHubUserDto.toDomain(): GitUser = GitUser(
        username = login,
        avatarUrl = avatarUrl,
        displayName = name,
        bio = bio,
        company = company,
        location = location,
        htmlUrl = htmlUrl ?: "https://github.com/$login",
        publicRepos = publicRepos,
        followers = followers,
        following = following,
        createdAt = createdAt.orEmpty()
    )

    override suspend fun listRepositories(): Result<List<GitRepositoryModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                apiService.listRepositories().map { it.toDomain() }
            }
        }

    private fun GitHubRepoDto.toDomain(): GitRepositoryModel = GitRepositoryModel(
        name = name,
        fullName = fullName,
        owner = owner.login,
        isPrivate = isPrivate,
        lastUpdated = updatedAt.orEmpty(),
        defaultBranch = defaultBranch ?: "main",
        language = language,
        createdAt = createdAt.orEmpty(),
        stargazersCount = stargazersCount,
        forksCount = forksCount,
        archived = archived,
        isFork = fork
    )

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

    override suspend fun getRepositoryTree(repo: GitRepositoryModel, branch: String?): Result<Map<String, String>> =
        getRepositoryTreeDetailed(repo, branch).map { detailed -> detailed.mapValues { it.value.sha } }

    override suspend fun getRepositoryTreeDetailed(repo: GitRepositoryModel, branch: String?): Result<Map<String, RemoteTreeEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                try {
                    // Deliberately NOT wrapped in githubCall()/githubCallWithRetry() — both
                    // of those catch HttpException and rethrow it as a plain IOException,
                    // which meant the catch block right below could never actually see a
                    // 404/409 to special-case. That bug made a brand-new/empty repository's
                    // real "Git Repository is empty." GitHub error leak straight to the UI
                    // as a hard failure (with no way to continue) instead of being treated
                    // as "no remote files yet".
                    val treeResponse = apiService.getTree(repo.owner, repo.name, branch ?: repo.defaultBranch, recursive = 1)
                    treeResponse.tree
                        .filter { it.type == "blob" }
                        .associate { it.path to RemoteTreeEntry(sha = it.sha.orEmpty(), size = it.size) }
                } catch (e: HttpException) {
                    // Empty/new default branch (no commits yet) -> treat as no remote files.
                    if (e.code() == 404 || e.code() == 409) emptyMap() else throw IOException(e.toFriendlyMessage(), e)
                }
            }
        }

    override suspend fun syncChanges(
        repo: GitRepositoryModel,
        changes: List<FileChange>,
        commitMessage: String,
        targetBranch: String?,
        readFileBytes: suspend (relativePath: String) -> ByteArray,
        onProgress: (phase: UploadPhase, completed: Int, total: Int, currentFile: String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress(UploadPhase.VALIDATING, 0, changes.size, "")
            val validatedDefaultBranch = validateRepositoryForUpload(repo).getOrThrow()
            // targetBranch (e.g. from the Repository Browser's branch picker) always wins
            // over the repo's default — validateRepositoryForUpload still ran above for
            // its permission/archived/disabled checks, which apply regardless of branch.
            val branch = targetBranch?.takeIf { it.isNotBlank() } ?: validatedDefaultBranch

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

            // Bootstrap a genuinely empty repository (zero commits). GitHub's Git Data
            // API — createBlob included, not just createTree/getRef — returns
            // "409 Git Repository is empty." for every call until the repo has at least
            // one commit. getRepositoryTree() already treats that 409 as "no remote
            // files", which is correct for the diff, but it doesn't make createBlob work
            // below. The Contents API is the one endpoint that can create that first
            // commit (and the branch ref with it), so it's used once here, for exactly
            // one file, before anything touches the normal blob/tree/commit flow.
            val branchAlreadyExists = runCatching {
                githubCallWithRetry { apiService.getRef(repo.owner, repo.name, branch) }
            }.isSuccess
            val remainingUpload = if (!branchAlreadyExists && toUpload.isNotEmpty()) {
                val bootstrap = toUpload.first()
                Log.d(TAG, "Repo has zero commits — bootstrapping via Contents API with ${bootstrap.cleanPath}")
                val bytes = readFileBytes(bootstrap.original.filePath)
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                githubCallWithRetry {
                    apiService.createFileContent(
                        repo.owner, repo.name,
                        path = bootstrap.cleanPath.encodedForContentsPath(),
                        body = CreateFileContentRequest(
                            message = "Initial commit",
                            content = base64,
                            branch = branch
                        )
                    )
                }
                val done = completed.incrementAndGet()
                onProgress(UploadPhase.CREATING_BLOBS, done, total, bootstrap.cleanPath)
                toUpload.drop(1)
            } else {
                toUpload
            }

            // §8 Skip Duplicate Files: unchanged files never reach here — ComparisonEngine
            // only emits ADDED/MODIFIED for paths whose hash actually differs.
            val semaphore = Semaphore(MAX_CONCURRENT_BLOBS)
            val blobEntries: List<TreeEntryInput> = coroutineScope {
                remainingUpload.map { change ->
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
            if (deleteEntries.isNotEmpty()) {
                Log.d(TAG, "Deleting ${deleteEntries.size} path(s): ${deleteEntries.joinToString { it.path }}")
            }

            // §10 Atomic Upload: one tree + commit + ref update lands everything at once.
            // Nothing is written to the branch until this succeeds.
            val commitSha = finalizeCommit(
                owner = repo.owner,
                repoName = repo.name,
                branch = branch,
                treeEntries = blobEntries + deleteEntries,
                message = commitMessage,
                total = total,
                onProgress = onProgress
            )

            // §13 Success Validation: confirm the branch head really did move to our commit
            // before telling the user it worked. Retried with backoff — GitHub's ref update
            // itself already succeeded (updateRef/createRef didn't throw), but the very next
            // GET can occasionally read a moment before that write is visible, which used to
            // surface as a false "doesn't match" failure on a push that actually worked.
            onProgress(UploadPhase.VERIFYING, total, total, "")
            var verifiedSha: String? = null
            var lastSeenSha: String? = null
            for (verifyAttempt in 0 until MAX_VERIFY_RETRIES) {
                val verifyRef = githubCallWithRetry { apiService.getRef(repo.owner, repo.name, branch) }
                lastSeenSha = verifyRef.objectRef.sha
                if (lastSeenSha == commitSha) {
                    verifiedSha = lastSeenSha
                    break
                }
                if (verifyAttempt < MAX_VERIFY_RETRIES - 1) {
                    Log.w(TAG, "Post-push verification not visible yet (attempt=$verifyAttempt), retrying: expected=$commitSha actual=$lastSeenSha")
                    delay(400L * (verifyAttempt + 1))
                }
            }
            if (verifiedSha == null) {
                Log.w(TAG, "Post-push verification mismatch after $MAX_VERIFY_RETRIES attempts: expected=$commitSha actual=$lastSeenSha")
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
                val commit = try {
                    githubCallWithRetry { apiService.getCommit(owner, repoName, ref.objectRef.sha) }
                } catch (e: IOException) {
                    throw IOException("Couldn't read branch \"$branch\"'s current commit: ${e.message}", e)
                }
                baseTreeSha = commit.tree.sha
                parents = listOf(ref.objectRef.sha)
            } else {
                // §4 Handle Empty Repository: no commits yet, so no base tree / parent —
                // and Update Reference is never called for this case (createRef is used below).
                baseTreeSha = null
                parents = emptyList()
            }

            onProgress(UploadPhase.CREATING_TREE, total, total, "")
            val newTree = try {
                githubCallWithRetry {
                    val requestJson = treeJson.encodeToString(
                        CreateTreeRequest.serializer(),
                        CreateTreeRequest(baseTree = baseTreeSha, tree = treeEntries)
                    )
                    val requestBody = requestJson.toRequestBody("application/json; charset=utf-8".toMediaType())
                    apiService.createTree(owner, repoName, requestBody)
                }
            } catch (e: IOException) {
                throw IOException("Couldn't create the new file tree: ${e.message}", e)
            }

            onProgress(UploadPhase.CREATING_COMMIT, total, total, "")
            val newCommit = try {
                githubCallWithRetry {
                    apiService.createCommit(
                        owner, repoName,
                        CreateCommitRequest(message = message, tree = newTree.sha, parents = parents)
                    )
                }
            } catch (e: IOException) {
                throw IOException("Couldn't create the new commit: ${e.message}", e)
            }

            Log.d(TAG, "attempt=$attempt branchExists=$branchExists baseTree=$baseTreeSha newTree=${newTree.sha} newCommit=${newCommit.sha}")
            onProgress(UploadPhase.UPDATING_BRANCH, total, total, "")

            try {
                // Deliberately NOT wrapped in githubCallWithRetry() here — same bug as
                // getRepositoryTree above: that wrapper catches HttpException and rethrows
                // it as IOException, so the catch block below (which needs the real
                // HttpException to check e.code() for the 409/422-fast-forward race
                // condition) could never fire. That silently disabled the fast-forward
                // retry this whole method exists for — every ref-update conflict was
                // surfacing as an immediate failure instead of retrying.
                if (branchExists) {
                    apiService.updateRef(owner, repoName, branch, UpdateRefRequest(sha = newCommit.sha))
                } else {
                    apiService.createRef(owner, repoName, CreateRefRequest(ref = "refs/heads/$branch", sha = newCommit.sha))
                }
                return newCommit.sha
            } catch (e: HttpException) {
                // errorBody() can only be read once, so capture it up front and reuse it
                // for both the fast-forward check and the friendly message below.
                val bodyText = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                val isRaceCondition = e.code() == 409 ||
                    (e.code() == 422 && bodyText?.contains("fast forward", ignoreCase = true) == true)
                // Transient server errors get the same retry-from-step-1 treatment as a
                // race condition (this loop's own backoff replaces the retry that
                // githubCallWithRetry used to provide before it was removed above).
                val isTransient = e.code() in intArrayOf(500, 502, 503, 504)
                if ((isRaceCondition || isTransient) && attempt < MAX_RACE_RETRIES - 1) {
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
            if (remaining == "0" && reset != null) {
                return "GitHub API rate limit reached. Resets at ${formatResetTime(reset)}."
            }

            // GitHub rejects ANY write under .github/workflows/ from a token that lacks
            // the separate "workflow" OAuth scope — even when the same token works fine
            // for every other path in the repo. This is exactly what silently broke
            // "Add CI workflow" (and made it look like dot-folders in general were
            // broken): every other file create/edit/delete used a token with only
            // "repo" scope and worked, so the one operation that touches
            // .github/workflows/ was the only thing that ever failed.
            val body403 = preReadBody ?: runCatching { response()?.errorBody()?.string() }.getOrNull()
            if (body403 != null && body403.contains("workflow", ignoreCase = true) &&
                (body403.contains("scope", ignoreCase = true) || body403.contains("OAuth", ignoreCase = true))
            ) {
                return "GitHub blocked this because your token doesn't have the \"workflow\" scope, " +
                    "which is required to create or update files under .github/workflows/ — every " +
                    "other file/folder works fine with your current token, only CI workflow files " +
                    "need this extra permission. Generate a new token with the \"workflow\" scope " +
                    "checked (classic tokens: check the top-level \"workflow\" checkbox; fine-grained " +
                    "tokens: grant \"Workflows\" read-and-write access), then reconnect on the Token screen."
            }

            return "GitHub request forbidden (403). Check the token's repo permissions."
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

    override suspend fun getFileContent(repo: GitRepositoryModel, blobSha: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val blob = githubCallWithRetry { apiService.getBlob(repo.owner, repo.name, blobSha) }
                if (blob.encoding == "base64") {
                    Base64.decode(blob.content.replace("\n", ""), Base64.DEFAULT)
                } else {
                    blob.content.toByteArray(Charsets.UTF_8)
                }
            }
        }

    override suspend fun checkForUpdate(
        owner: String,
        repo: String,
        currentVersionName: String
    ): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val release = try {
                apiService.getLatestRelease(owner, repo)
            } catch (e: HttpException) {
                // No releases published yet is not an error — just means no update.
                if (e.code() == 404) return@withContext Result.success(null)
                throw IOException(e.toFriendlyMessage(), e)
            }

            if (release.draft || release.prerelease) return@withContext Result.success(null)
            if (!VersionComparator.isNewer(release.tagName, currentVersionName)) {
                return@withContext Result.success(null)
            }

            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            AppUpdateInfo(
                versionTag = release.tagName,
                releaseTitle = release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
                releaseNotes = release.body?.takeIf { it.isNotBlank() } ?: "No release notes provided.",
                apkDownloadUrl = apkAsset?.browserDownloadUrl,
                releasePageUrl = release.htmlUrl
            )
        }
    }

    // ===== Repository Management =====

    override suspend fun listBranches(repo: GitRepositoryModel): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.listBranches(repo.owner, repo.name) }.map { it.name }
            }
        }

    override suspend fun createBranch(repo: GitRepositoryModel, newBranchName: String, fromBranch: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sourceRef = try {
                    githubCallWithRetry { apiService.getRef(repo.owner, repo.name, fromBranch) }
                } catch (e: HttpException) {
                    throw IOException("Couldn't read branch \"$fromBranch\": ${e.toFriendlyMessage()}", e)
                }
                try {
                    githubCallWithRetry {
                        apiService.createRef(
                            repo.owner, repo.name,
                            CreateRefRequest(ref = "refs/heads/$newBranchName", sha = sourceRef.objectRef.sha)
                        )
                    }
                } catch (e: HttpException) {
                    val friendly = if (e.code() == 422) {
                        "A branch named \"$newBranchName\" already exists."
                    } else {
                        e.toFriendlyMessage()
                    }
                    throw IOException(friendly, e)
                }
                Unit
            }
        }

    override suspend fun getCommitHistory(repo: GitRepositoryModel, branch: String?): Result<List<CommitSummary>> =
        withContext(Dispatchers.IO) {
            runCatching {
                try {
                    githubCallWithRetry {
                        apiService.listCommits(repo.owner, repo.name, branch ?: repo.defaultBranch)
                    }.map { item ->
                        CommitSummary(
                            sha = item.sha,
                            message = item.commit.message,
                            authorName = item.author?.login ?: item.commit.author?.name ?: "Unknown",
                            date = item.commit.author?.date.orEmpty(),
                            htmlUrl = item.htmlUrl ?: "https://github.com/${repo.owner}/${repo.name}/commit/${item.sha}"
                        )
                    }
                } catch (e: HttpException) {
                    // Empty repo / branch with no commits yet -> empty history, not an error.
                    if (e.code() == 404 || e.code() == 409) emptyList() else throw IOException(e.toFriendlyMessage(), e)
                }
            }
        }

    override suspend fun createRepository(name: String, description: String, isPrivate: Boolean): Result<GitRepositoryModel> =
        withContext(Dispatchers.IO) {
            runCatching {
                try {
                    githubCallWithRetry {
                        apiService.createRepository(
                            CreateRepoRequest(name = name, description = description.ifBlank { null }, isPrivate = isPrivate, autoInit = true)
                        )
                    }.toDomain()
                } catch (e: HttpException) {
                    val friendly = if (e.code() == 422) {
                        "A repository named \"$name\" already exists on this account."
                    } else {
                        e.toFriendlyMessage()
                    }
                    throw IOException(friendly, e)
                }
            }
        }

    override suspend fun updateRepository(
        repo: GitRepositoryModel,
        newName: String?,
        newDescription: String?,
        newIsPrivate: Boolean?
    ): Result<GitRepositoryModel> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry {
                    apiService.updateRepository(
                        repo.owner, repo.name,
                        UpdateRepoRequest(name = newName, description = newDescription, isPrivate = newIsPrivate)
                    )
                }.toDomain()
            }
        }

    override suspend fun deleteRepository(repo: GitRepositoryModel): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = apiService.deleteRepository(repo.owner, repo.name)
                if (!response.isSuccessful) {
                    val friendly = when (response.code()) {
                        403 -> "This token doesn't have delete permission — it needs the \"delete_repo\" scope."
                        404 -> "Repository \"${repo.owner}/${repo.name}\" no longer exists."
                        else -> "GitHub rejected the delete (HTTP ${response.code()})."
                    }
                    throw IOException(friendly)
                }
            }
        }

    override suspend fun getCurrentUser(): Result<GitUser> =
        withContext(Dispatchers.IO) {
            runCatching { githubCallWithRetry { apiService.getAuthenticatedUser() }.body()!!.toDomain() }
        }

    override suspend fun getApiRateLimit(): Result<ApiRateLimit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val core = githubCallWithRetry { apiService.getRateLimit() }.resources.core
                ApiRateLimit(limit = core.limit, remaining = core.remaining, resetEpochSeconds = core.reset)
            }
        }

    // ===== GitHub Actions =====

    override suspend fun listWorkflows(repo: GitRepositoryModel): Result<List<GitHubWorkflow>> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.listWorkflows(repo.owner, repo.name) }.workflows.map { w ->
                    GitHubWorkflow(
                        id = w.id,
                        name = w.name,
                        path = w.path,
                        state = w.state,
                        badgeUrl = w.badgeUrl,
                        htmlUrl = w.htmlUrl,
                        updatedAt = w.updatedAt
                    )
                }
            }
        }

    override suspend fun listWorkflowRuns(
        repo: GitRepositoryModel,
        workflowId: Long?,
        branch: String?,
        status: String?
    ): Result<List<WorkflowRun>> = withContext(Dispatchers.IO) {
        runCatching {
            githubCallWithRetry {
                apiService.listWorkflowRuns(repo.owner, repo.name, workflowId, branch, status = status)
            }.runs.map { run ->
                WorkflowRun(
                    id = run.id,
                    name = run.name ?: "Workflow",
                    displayTitle = run.displayTitle ?: run.name ?: "Run #${run.runNumber}",
                    branch = run.branch ?: "",
                    headSha = run.headSha ?: "",
                    runNumber = run.runNumber,
                    status = run.status,
                    conclusion = run.conclusion,
                    event = run.event,
                    htmlUrl = run.htmlUrl,
                    createdAt = run.createdAt.orEmpty(),
                    updatedAt = run.updatedAt,
                    workflowId = run.workflowId
                )
            }
        }
    }

    override suspend fun triggerWorkflow(repo: GitRepositoryModel, workflowId: Long, ref: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = githubCallWithRetry {
                    apiService.dispatchWorkflow(repo.owner, repo.name, workflowId, GitHubWorkflowDispatchRequest(ref = ref))
                }
                if (!response.isSuccessful) {
                    val friendly = if (response.code() == 404) {
                        "Workflow doesn't accept manual runs (no workflow_dispatch trigger)."
                    } else {
                        "GitHub rejected the workflow run (HTTP ${response.code()})."
                    }
                    throw IOException(friendly)
                }
            }
        }

    override suspend fun cancelWorkflowRun(repo: GitRepositoryModel, runId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Deliberately NOT wrapped in githubCallWithRetry: GitHub answers 409
                // for "run already completed" — which is a success case here, not an error.
                val response = try {
                    apiService.cancelWorkflowRun(repo.owner, repo.name, runId)
                } catch (e: HttpException) {
                    if (e.code() == 409) return@withContext Result.success(Unit)
                    throw IOException(e.toFriendlyMessage(), e)
                }
                if (!response.isSuccessful) {
                    throw IOException("GitHub couldn't cancel the run (HTTP ${response.code()}).")
                }
            }
        }

    override suspend fun rerunFailedJobs(repo: GitRepositoryModel, runId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = githubCallWithRetry {
                    apiService.rerunFailedJobs(repo.owner, repo.name, runId)
                }
                if (!response.isSuccessful) {
                    throw IOException("GitHub couldn't re-run failed jobs (HTTP ${response.code()}).")
                }
            }
        }

    override suspend fun listArtifacts(repo: GitRepositoryModel): Result<List<ArtifactInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.listArtifacts(repo.owner, repo.name) }.artifacts.map { a ->
                    ArtifactInfo(
                        id = a.id,
                        name = a.name,
                        size = a.size,
                        expired = a.expired,
                        createdAt = a.createdAt,
                        archiveDownloadUrl = a.archiveDownloadUrl
                    )
                }
            }
        }

    override suspend fun downloadArtifactZip(repo: GitRepositoryModel, artifactId: Long): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.downloadArtifactZip(repo.owner, repo.name, artifactId) }.bytes()
            }
        }

    override suspend fun downloadRunLogs(repo: GitRepositoryModel, runId: Long): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.downloadRunLogs(repo.owner, repo.name, runId) }.bytes()
            }
        }

    // ===== Pull requests =====

    override suspend fun listPullRequests(repo: GitRepositoryModel, state: String): Result<List<PullRequest>> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.listPullRequests(repo.owner, repo.name, state) }.map { it.toDomain() }
            }
        }

    private fun GitHubPullRequestDto.toDomain(): PullRequest = PullRequest(
        number = number,
        title = title,
        state = state,
        body = body,
        htmlUrl = htmlUrl,
        createdAt = createdAt.orEmpty(),
        updatedAt = updatedAt,
        mergedAt = mergedAt,
        headRef = head?.ref ?: "",
        baseRef = base?.ref ?: "",
        headLabel = head?.label,
        author = user?.login ?: "Unknown",
        mergeable = mergeable,
        changedFiles = changedFiles,
        additions = additions,
        deletions = deletions,
        commentsCount = 0
    )

    override suspend fun createPullRequest(
        repo: GitRepositoryModel,
        title: String,
        head: String,
        base: String,
        body: String?
    ): Result<PullRequest> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                githubCallWithRetry {
                    apiService.createPullRequest(
                        repo.owner, repo.name,
                        CreatePullRequestRequest(title = title, head = head, base = base, body = body)
                    )
                }.toDomain()
            } catch (e: HttpException) {
                val friendly = if (e.code() == 422) {
                    "GitHub couldn't create the PR — head branch \"$head\" and base branch \"$base\" may already have an open PR, or they're the same branch."
                } else {
                    e.toFriendlyMessage()
                }
                throw IOException(friendly, e)
            }
        }
    }

    override suspend fun updatePullRequestState(repo: GitRepositoryModel, number: Int, state: String): Result<PullRequest> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry {
                    apiService.updatePullRequest(repo.owner, repo.name, number, UpdatePullRequestRequest(state = state))
                }.toDomain()
            }
        }

    override suspend fun mergePullRequest(repo: GitRepositoryModel, number: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry {
                    apiService.mergePullRequest(repo.owner, repo.name, number, MergePullRequestRequest())
                }
                Unit
            }
        }

    override suspend fun listPullRequestFiles(repo: GitRepositoryModel, number: Int): Result<List<PullRequestFile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry {
                    apiService.listPullRequestFiles(repo.owner, repo.name, number)
                }.map { f ->
                    PullRequestFile(
                        filename = f.filename,
                        status = f.status,
                        additions = f.additions,
                        deletions = f.deletions,
                        patch = f.patch
                    )
                }
            }
        }

    // ===== Issues =====

    override suspend fun listIssues(repo: GitRepositoryModel, state: String): Result<List<Issue>> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.listIssues(repo.owner, repo.name, state) }.map { i ->
                    Issue(
                        number = i.number,
                        title = i.title,
                        state = i.state,
                        body = i.body,
                        htmlUrl = i.htmlUrl,
                        createdAt = i.createdAt.orEmpty(),
                        updatedAt = i.updatedAt,
                        author = i.user?.login ?: "Unknown",
                        commentsCount = i.commentsCount,
                        isPullRequest = i.pullRequest != null
                    )
                }.filterNot { it.isPullRequest }
            }
        }

    override suspend fun createIssue(repo: GitRepositoryModel, title: String, body: String?): Result<Issue> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry {
                    apiService.createIssue(repo.owner, repo.name, CreateIssueRequest(title = title, body = body))
                }.let { Issue(number = it.number, title = it.title, state = it.state, body = it.body, htmlUrl = it.htmlUrl, createdAt = it.createdAt.orEmpty(), updatedAt = it.updatedAt, author = it.user?.login ?: "Unknown", commentsCount = it.commentsCount, isPullRequest = false) }
            }
        }

    override suspend fun updateIssueState(repo: GitRepositoryModel, number: Int, state: String): Result<Issue> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry {
                    apiService.updateIssue(repo.owner, repo.name, number, UpdateIssueRequest(state = state))
                }.let { Issue(number = it.number, title = it.title, state = it.state, body = it.body, htmlUrl = it.htmlUrl, createdAt = it.createdAt.orEmpty(), updatedAt = it.updatedAt, author = it.user?.login ?: "Unknown", commentsCount = it.commentsCount, isPullRequest = false) }
            }
        }

    override suspend fun listIssueComments(repo: GitRepositoryModel, number: Int): Result<List<IssueComment>> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.listIssueComments(repo.owner, repo.name, number) }.map { c ->
                    IssueComment(
                        id = c.id,
                        body = c.body ?: "",
                        htmlUrl = c.htmlUrl,
                        createdAt = c.createdAt.orEmpty(),
                        author = c.user?.login ?: "Unknown"
                    )
                }
            }
        }

    override suspend fun createIssueComment(repo: GitRepositoryModel, number: Int, body: String): Result<IssueComment> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry {
                    apiService.createIssueComment(repo.owner, repo.name, number, CreateIssueCommentRequest(body))
                }.let { IssueComment(id = it.id, body = it.body ?: "", htmlUrl = it.htmlUrl, createdAt = it.createdAt.orEmpty(), author = it.user?.login ?: "Unknown") }
            }
        }

    // ===== Star / unstar / fork =====

    override suspend fun listStarredRepositories(): Result<Set<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.listStarredRepositories() }.map { it.fullName }.toSet()
            }
        }

    override suspend fun starRepository(repo: GitRepositoryModel): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = githubCallWithRetry { apiService.starRepository(repo.owner, repo.name) }
                if (!response.isSuccessful) throw IOException("GitHub couldn't star the repo (HTTP ${response.code()}).")
            }
        }

    override suspend fun unstarRepository(repo: GitRepositoryModel): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = githubCallWithRetry { apiService.unstarRepository(repo.owner, repo.name) }
                if (!response.isSuccessful) throw IOException("GitHub couldn't unstar the repo (HTTP ${response.code()}).")
            }
        }

    override suspend fun forkRepository(repo: GitRepositoryModel): Result<GitRepositoryModel> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.forkRepository(repo.owner, repo.name, CreateForkRequest()) }.toDomain()
            }
        }

    // ===== Releases =====

    override suspend fun listReleases(repo: GitRepositoryModel): Result<List<GitRelease>> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.listReleases(repo.owner, repo.name) }.map { r ->
                    GitRelease(
                        id = r.id,
                        tagName = r.tagName,
                        name = r.name,
                        body = r.body,
                        draft = r.draft,
                        prerelease = r.prerelease,
                        createdAt = r.createdAt.orEmpty(),
                        publishedAt = r.publishedAt,
                        htmlUrl = r.htmlUrl,
                        assets = r.assets.map { a ->
                            ReleaseAsset(id = 0, name = a.name, size = 0, browserDownloadUrl = a.browserDownloadUrl, contentType = null)
                        }
                    )
                }
            }
        }

    override suspend fun createRelease(
        repo: GitRepositoryModel,
        tagName: String,
        name: String?,
        body: String?,
        draft: Boolean,
        prerelease: Boolean,
        targetCommitish: String?
    ): Result<GitRelease> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                githubCallWithRetry {
                    apiService.createRelease(
                        repo.owner, repo.name,
                        CreateReleaseRequest(
                            tagName = tagName,
                            targetCommitish = targetCommitish,
                            name = name,
                            body = body,
                            draft = draft,
                            prerelease = prerelease
                        )
                    )
                }.let { GitRelease(id = it.id, tagName = it.tagName, name = it.name, body = it.body, draft = it.draft, prerelease = it.prerelease, createdAt = it.createdAt.orEmpty(), publishedAt = it.publishedAt, htmlUrl = it.htmlUrl, assets = emptyList()) }
            } catch (e: HttpException) {
                val friendly = if (e.code() == 422) {
                    "GitHub couldn't create the release — the tag \"$tagName\" may already exist."
                } else {
                    e.toFriendlyMessage()
                }
                throw IOException(friendly, e)
            }
        }
    }

    override suspend fun deleteRelease(repo: GitRepositoryModel, releaseId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = githubCallWithRetry { apiService.deleteRelease(repo.owner, repo.name, releaseId) }
                if (!response.isSuccessful) throw IOException("GitHub couldn't delete the release (HTTP ${response.code()}).")
            }
        }

    override suspend fun listReleaseAssets(repo: GitRepositoryModel, releaseId: Long): Result<List<ReleaseAsset>> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.listReleaseAssets(repo.owner, repo.name, releaseId) }.map { a ->
                    ReleaseAsset(id = a.id, name = a.name, size = a.size, browserDownloadUrl = a.browserDownloadUrl, contentType = a.contentType, createdAt = a.createdAt)
                }
            }
        }

    override suspend fun uploadReleaseAsset(
        repo: GitRepositoryModel,
        releaseId: Long,
        fileName: String,
        bytes: ByteArray
    ): Result<ReleaseAsset> = withContext(Dispatchers.IO) {
        runCatching {
            val namePart = okhttp3.MultipartBody.Part.createFormData("name", fileName)
            val body = bytes.toRequestBody("application/octet-stream".toMediaType())
            val filePart = okhttp3.MultipartBody.Part.createFormData("asset", fileName, body)
            githubCallWithRetry {
                apiService.uploadReleaseAsset(repo.owner, repo.name, releaseId, namePart, filePart)
            }.let { ReleaseAsset(id = it.id, name = it.name, size = it.size, browserDownloadUrl = it.browserDownloadUrl, contentType = it.contentType, createdAt = it.createdAt) }
        }
    }

    // ===== Search + commit diff + archive =====

    override suspend fun searchRepositories(query: String): Result<List<GitRepositoryModel>> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.searchRepositories(query) }.items.map { it.toDomain() }
            }
        }

    override suspend fun searchCode(query: String, owner: String, repo: String): Result<List<CodeSearchResult>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val fullQuery = if (owner.isNotBlank() && repo.isNotBlank()) {
                    "$query repo:$owner/$repo"
                } else {
                    query
                }
                githubCallWithRetry { apiService.searchCode(fullQuery) }.items.map { c ->
                    CodeSearchResult(
                        name = c.name,
                        path = c.path,
                        htmlUrl = c.htmlUrl,
                        repositoryFullName = c.repository?.fullName ?: ""
                    )
                }
            }
        }

    override suspend fun getCommitDiff(repo: GitRepositoryModel, sha: String): Result<List<CommitDiffFile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.getCommitDetail(repo.owner, repo.name, sha) }.files.map { f ->
                    CommitDiffFile(
                        filename = f.filename,
                        status = f.status,
                        additions = f.additions,
                        deletions = f.deletions,
                        changes = f.changes,
                        patch = f.patch
                    )
                }
            }
        }

    override suspend fun downloadRepoZip(repo: GitRepositoryModel, ref: String?): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry {
                    apiService.downloadRepoZip(repo.owner, repo.name, ref ?: repo.defaultBranch)
                }.bytes()
            }
        }

    override suspend fun downloadReleaseAsset(repo: GitRepositoryModel, assetId: Long): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                githubCallWithRetry { apiService.downloadReleaseAsset(repo.owner, repo.name, assetId) }.bytes()
            }
        }
}
