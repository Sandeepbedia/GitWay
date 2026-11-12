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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
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

class GitHubRepositoryImpl(
    private val tokenManager: TokenManager,
    private val apiService: GitHubApiService
) : GitHubRepository {

    override val activeAccountFlow: StateFlow<String?> = tokenManager.activeAccountFlow
    private val _remoteDataChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val remoteDataChanged: SharedFlow<Unit> = _remoteDataChanged

    private fun notifyRemoteDataChanged() {
        _remoteDataChanged.tryEmit(Unit)
    }

    private companion object {
        const val TAG = "GitWayUpload"
        const val MAX_CONCURRENT_BLOBS = 3
        const val MAX_TRANSIENT_RETRIES = 3
        const val MAX_SECONDARY_RATE_LIMIT_RETRIES = 6
        const val MAX_RACE_RETRIES = 4
        const val MAX_VERIFY_RETRIES = 4
    }

    private val errorJson = Json { ignoreUnknownKeys = true }

    private val treeJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = true }

    private fun String.encodedForContentsPath(): String =
        split("/").joinToString("/") { segment ->
            java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
        }

    override suspend fun validateTokenAndFetchUser(token: String): Result<TokenValidationResult> =
        withContext(Dispatchers.IO) {
            val previousToken = tokenManager.getToken()
            try {
                tokenManager.saveToken(token)
                val response = apiService.getAuthenticatedUser()
                if (!response.isSuccessful) throw HttpException(response)

                val user = response.body()!!.toDomain()
                tokenManager.saveAccount(user.username, token)

                Result.success(
                    TokenValidationResult(
                        user = user,
                        grantedScopes = response.headers()["X-OAuth-Scopes"]
                            ?.split(",")
                            ?.map { it.trim() }
                            ?.filter { it.isNotBlank() }
                            ?.toSet()
                            ?: emptySet()
                    )
                )
            } catch (t: Throwable) {
                // Adding a second account must never log the user out if validation fails.
                tokenManager.restoreToken(previousToken)
                throw t
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
            val branch = targetBranch?.takeIf { it.isNotBlank() } ?: validatedDefaultBranch

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

            val commitSha = finalizeCommit(
                owner = repo.owner,
                repoName = repo.name,
                branch = branch,
                treeEntries = blobEntries + deleteEntries,
                message = commitMessage,
                total = total,
                onProgress = onProgress
            )

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
                if (branchExists) {
                    apiService.updateRef(owner, repoName, branch, UpdateRefRequest(sha = newCommit.sha))
                } else {
                    apiService.createRef(owner, repoName, CreateRefRequest(ref = "refs/heads/$branch", sha = newCommit.sha))
                }
                notifyRemoteDataChanged()
                return newCommit.sha
            } catch (e: HttpException) {
                val bodyText = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                val isRaceCondition = e.code() == 409 ||
                    (e.code() == 422 && bodyText?.contains("fast forward", ignoreCase = true) == true)
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

    private suspend fun <T> githubCall(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: HttpException) {
            throw IOException(e.toFriendlyMessage(), e)
        }
    }

    private suspend fun <T> githubCallWithRetry(block: suspend () -> T): T {
        var lastError: Throwable? = null
        var transientAttempt = 0
        var rateLimitAttempt = 0
        repeat(MAX_TRANSIENT_RETRIES + MAX_SECONDARY_RATE_LIMIT_RETRIES) {
            try {
                return block()
            } catch (e: HttpException) {
                if (e.code() == 403) {
                    val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                    val retryAfterSeconds = e.response()?.headers()?.get("Retry-After")?.toLongOrNull()
                    val isSecondaryRateLimit = retryAfterSeconds != null ||
                        body?.contains("secondary rate limit", ignoreCase = true) == true ||
                        body?.contains("abuse detection", ignoreCase = true) == true
                    if (isSecondaryRateLimit && rateLimitAttempt < MAX_SECONDARY_RATE_LIMIT_RETRIES) {
                        rateLimitAttempt++
                        lastError = e
                        val waitMs = (retryAfterSeconds ?: (3L * rateLimitAttempt)) * 1000
                        Log.w(TAG, "GitHub secondary rate limit hit, waiting ${waitMs}ms (attempt=$rateLimitAttempt)")
                        delay(waitMs)
                        return@repeat
                    }
                    throw IOException(e.toFriendlyMessage(body), e)
                }
                val transient = e.code() in intArrayOf(500, 502, 503, 504)
                if (transient && transientAttempt < MAX_TRANSIENT_RETRIES - 1) {
                    transientAttempt++
                    lastError = e
                    Log.w(TAG, "Transient HTTP ${e.code()}, retrying (attempt=$transientAttempt)")
                    delay(400L * transientAttempt)
                    return@repeat
                }
                // 401 / 404 / 422 (and any other non-retryable code) surface immediately.
                throw IOException(e.toFriendlyMessage(), e)
            } catch (e: SocketTimeoutException) {
                lastError = e
                if (transientAttempt < MAX_TRANSIENT_RETRIES - 1) {
                    transientAttempt++
                    Log.w(TAG, "Socket timeout, retrying (attempt=$transientAttempt)")
                    delay(400L * transientAttempt)
                    return@repeat
                }
                throw IOException("Request timed out after $MAX_TRANSIENT_RETRIES attempts. Check your connection and try again.", e)
            } catch (e: IOException) {
                lastError = e
                if (transientAttempt < MAX_TRANSIENT_RETRIES - 1) {
                    transientAttempt++
                    Log.w(TAG, "Network error, retrying (attempt=$transientAttempt): ${e.message}")
                    delay(400L * transientAttempt)
                    return@repeat
                }
                throw IOException("Network error after $MAX_TRANSIENT_RETRIES attempts: ${e.message}", e)
            }
        }
        throw lastError ?: IOException("Request failed after retries.")
    }

    private fun HttpException.toFriendlyMessage(preReadBody: String? = null): String {
        if (code() == 403) {
            val headers = response()?.headers()
            val remaining = headers?.get("X-RateLimit-Remaining")
            val reset = headers?.get("X-RateLimit-Reset")
            if (remaining == "0" && reset != null) {
                return "GitHub API rate limit reached. Resets at ${formatResetTime(reset)}."
            }

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

            val retryAfterSeconds = headers?.get("Retry-After")?.toLongOrNull()
            val isSecondaryRateLimit = retryAfterSeconds != null ||
                body403?.contains("secondary rate limit", ignoreCase = true) == true ||
                body403?.contains("abuse detection", ignoreCase = true) == true
            if (isSecondaryRateLimit) {
                return "GitHub temporarily blocked this upload for sending too many requests too quickly " +
                    "(its secondary rate limit) — this is common on large projects with hundreds of files. " +
                    "Wait a few minutes, then tap Retry upload; it'll pick up from a clean state rather than " +
                    "duplicating what already went through."
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
                    }.toDomain().also { notifyRemoteDataChanged() }
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
            val body = bytes.toRequestBody("application/vnd.android.package-archive".toMediaType())
            val uploadUrl = "https://uploads.github.com/repos/${repo.owner}/${repo.name}/releases/$releaseId/assets"
            githubCallWithRetry {
                apiService.uploadReleaseAsset(uploadUrl, fileName, body)
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
