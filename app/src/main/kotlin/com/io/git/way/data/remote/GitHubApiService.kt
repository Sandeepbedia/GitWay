package com.io.git.way.data.remote

import com.io.git.way.data.remote.dto.CreateBlobRequest
import com.io.git.way.data.remote.dto.CreateCommitRequest
import com.io.git.way.data.remote.dto.CreateFileContentRequest
import com.io.git.way.data.remote.dto.CreateFileContentResponseDto
import com.io.git.way.data.remote.dto.CreateForkRequest
import com.io.git.way.data.remote.dto.CreateIssueCommentRequest
import com.io.git.way.data.remote.dto.CreateIssueRequest
import com.io.git.way.data.remote.dto.CreatePullRequestRequest
import com.io.git.way.data.remote.dto.CreateRefRequest
import com.io.git.way.data.remote.dto.CreateReleaseRequest
import com.io.git.way.data.remote.dto.CreateRepoRequest
import com.io.git.way.data.remote.dto.CreateTreeRequest
import com.io.git.way.data.remote.dto.GitBlobContentDto
import com.io.git.way.data.remote.dto.GitBlobRefDto
import com.io.git.way.data.remote.dto.GitCommitDetailDto
import com.io.git.way.data.remote.dto.GitCommitRefDto
import com.io.git.way.data.remote.dto.GitHubArtifactListDto
import com.io.git.way.data.remote.dto.GitHubBranchDto
import com.io.git.way.data.remote.dto.GitHubCommitDetailDto
import com.io.git.way.data.remote.dto.GitHubCommitListItemDto
import com.io.git.way.data.remote.dto.GitHubIssueCommentDto
import com.io.git.way.data.remote.dto.GitHubIssueDto
import com.io.git.way.data.remote.dto.GitHubPullRequestDto
import com.io.git.way.data.remote.dto.GitHubPullRequestFileDto
import com.io.git.way.data.remote.dto.GitHubReleaseAssetListDto
import com.io.git.way.data.remote.dto.GitHubReleaseDto
import com.io.git.way.data.remote.dto.GitHubReleaseListDto
import com.io.git.way.data.remote.dto.GitHubRepoDto
import com.io.git.way.data.remote.dto.GitHubSearchCodeDto
import com.io.git.way.data.remote.dto.GitHubSearchRepoDto
import com.io.git.way.data.remote.dto.GitHubUserDto
import com.io.git.way.data.remote.dto.GitHubWorkflowDispatchRequest
import com.io.git.way.data.remote.dto.GitHubWorkflowListDto
import com.io.git.way.data.remote.dto.GitHubWorkflowRunListDto
import com.io.git.way.data.remote.dto.MergePullRequestRequest
import com.io.git.way.data.remote.dto.RateLimitDto
import com.io.git.way.data.remote.dto.GitRefDto
import com.io.git.way.data.remote.dto.GitTreeRefDto
import com.io.git.way.data.remote.dto.GitTreeResponseDto
import com.io.git.way.data.remote.dto.UpdateIssueRequest
import com.io.git.way.data.remote.dto.UpdatePullRequestRequest
import com.io.git.way.data.remote.dto.UpdateRefRequest
import com.io.git.way.data.remote.dto.UpdateRepoRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** GitHub REST API v3 surface: auth, repository listing, and the Git Data API. */
interface GitHubApiService {

    @GET("user")
    suspend fun getAuthenticatedUser(): Response<GitHubUserDto>

    @GET("user/repos")
    suspend fun listRepositories(
        @Query("per_page") perPage: Int = 100,
        @Query("sort") sort: String = "updated",
        @Query("affiliation") affiliation: String = "owner,collaborator,organization_member"
    ): List<GitHubRepoDto>

    /** Fresh single-repo lookup: used right before upload to re-check permissions,
     * archived/disabled state, and the current default branch (422-fix PRD §2/§3). */
    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRepoDto

    // ===== Git Data API (PRD1 §3.2 / PRD2 §3.2) =====

    @GET("repos/{owner}/{repo}/git/trees/{sha}")
    suspend fun getTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String,
        @Query("recursive") recursive: Int = 1
    ): GitTreeResponseDto

    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    suspend fun getRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): GitRefDto

    @GET("repos/{owner}/{repo}/git/commits/{sha}")
    suspend fun getCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String
    ): GitCommitDetailDto

    @POST("repos/{owner}/{repo}/git/blobs")
    suspend fun createBlob(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateBlobRequest
    ): GitBlobRefDto

    @POST("repos/{owner}/{repo}/git/trees")
    suspend fun createTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateTreeRequest
    ): GitTreeRefDto

    @POST("repos/{owner}/{repo}/git/commits")
    suspend fun createCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateCommitRequest
    ): GitCommitRefDto

    @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun updateRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Body body: UpdateRefRequest
    ): GitRefDto

    /** Used only when the default branch has no commits yet (PRD1 §3.6 empty-branch case). */
    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateRefRequest
    ): GitRefDto

    /** Bootstraps a genuinely empty repository (zero commits). GitHub's Git Data API
     * (createBlob/createTree/createCommit/getRef, everything above) returns
     * "409 Git Repository is empty." for EVERY call — not just tree/ref lookups —
     * until the repo has at least one commit; see GitHub's own Git Data API docs.
     * The Contents API is the one endpoint that can create that first commit (and the
     * branch ref along with it) on a repo with zero git objects. Used once, for exactly
     * one file, before the normal blob/tree/commit flow runs for everything else. */
    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body body: CreateFileContentRequest
    ): CreateFileContentResponseDto

    /** Reads a blob's raw content by sha — the sha is already known from the cached repo
     * tree, so this powers the Repository Browser's file viewer/editor without a second
     * Contents-API lookup. */
    @GET("repos/{owner}/{repo}/git/blobs/{sha}")
    suspend fun getBlob(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String
    ): GitBlobContentDto

    /** Latest published (non-draft, non-prerelease) release — powers the in-app update
     * checker. Public endpoint: works with or without an auth token. 404s if the repo
     * has no releases yet, which the caller treats as "no update available" rather
     * than an error. */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubReleaseDto

    // ===== Repository Management: branches, commit history, create/update/delete =====

    @GET("repos/{owner}/{repo}/branches")
    suspend fun listBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100
    ): List<GitHubBranchDto>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun listCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("sha") branch: String,
        @Query("per_page") perPage: Int = 30
    ): List<GitHubCommitListItemDto>

    @POST("user/repos")
    suspend fun createRepository(@Body body: CreateRepoRequest): GitHubRepoDto

    @PATCH("repos/{owner}/{repo}")
    suspend fun updateRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: UpdateRepoRequest
    ): GitHubRepoDto

    @DELETE("repos/{owner}/{repo}")
    suspend fun deleteRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @GET("rate_limit")
    suspend fun getRateLimit(): RateLimitDto

    // ===== GitHub Actions (workflow scope) =====

    @GET("repos/{owner}/{repo}/actions/workflows")
    suspend fun listWorkflows(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100
    ): GitHubWorkflowListDto

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun listWorkflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("workflow_id") workflowId: Long? = null,
        @Query("branch") branch: String? = null,
        @Query("event") event: String? = null,
        @Query("status") status: String? = null,
        @Query("per_page") perPage: Int = 30
    ): GitHubWorkflowRunListDto

    @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
    suspend fun dispatchWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: Long,
        @Body body: GitHubWorkflowDispatchRequest
    ): retrofit2.Response<Unit>

    @POST("repos/{owner}/{repo}/actions/runs/{run_id}/cancel")
    suspend fun cancelWorkflowRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): retrofit2.Response<Unit>

    @POST("repos/{owner}/{repo}/actions/runs/{run_id}/rerun-failed")
    suspend fun rerunFailedJobs(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): retrofit2.Response<Unit>

    @GET("repos/{owner}/{repo}/actions/artifacts")
    suspend fun listArtifacts(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100
    ): GitHubArtifactListDto

    @GET("repos/{owner}/{repo}/actions/artifacts/{artifact_id}/zip")
    suspend fun downloadArtifactZip(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("artifact_id") artifactId: Long
    ): okhttp3.ResponseBody

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/logs")
    suspend fun downloadRunLogs(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): okhttp3.ResponseBody

    // ===== Pull requests =====

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun listPullRequests(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 50
    ): List<GitHubPullRequestDto>

    @POST("repos/{owner}/{repo}/pulls")
    suspend fun createPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreatePullRequestRequest
    ): GitHubPullRequestDto

    @GET("repos/{owner}/{repo}/pulls/{pull_number}")
    suspend fun getPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int
    ): GitHubPullRequestDto

    @PATCH("repos/{owner}/{repo}/pulls/{pull_number}")
    suspend fun updatePullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Body body: UpdatePullRequestRequest
    ): GitHubPullRequestDto

    @PUT("repos/{owner}/{repo}/pulls/{pull_number}/merge")
    suspend fun mergePullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Body body: MergePullRequestRequest
    ): GitHubPullRequestDto

    @GET("repos/{owner}/{repo}/pulls/{pull_number}/files")
    suspend fun listPullRequestFiles(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Query("per_page") perPage: Int = 100
    ): List<GitHubPullRequestFileDto>

    // ===== Issues =====

    @GET("repos/{owner}/{repo}/issues")
    suspend fun listIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 50
    ): List<GitHubIssueDto>

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateIssueRequest
    ): GitHubIssueDto

    @PATCH("repos/{owner}/{repo}/issues/{issue_number}")
    suspend fun updateIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Body body: UpdateIssueRequest
    ): GitHubIssueDto

    @GET("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun listIssueComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Query("per_page") perPage: Int = 100
    ): List<GitHubIssueCommentDto>

    @POST("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun createIssueComment(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Body body: CreateIssueCommentRequest
    ): GitHubIssueCommentDto

    // ===== Star / unstar / fork =====

    @GET("user/starred")
    suspend fun listStarredRepositories(
        @Query("per_page") perPage: Int = 100
    ): List<GitHubRepoDto>

    @PUT("user/starred/{owner}/{repo}")
    suspend fun starRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): retrofit2.Response<Unit>

    @DELETE("user/starred/{owner}/{repo}")
    suspend fun unstarRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): retrofit2.Response<Unit>

    @POST("repos/{owner}/{repo}/forks")
    suspend fun forkRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateForkRequest
    ): GitHubRepoDto

    // ===== Releases (full management) =====

    @GET("repos/{owner}/{repo}/releases")
    suspend fun listReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 50
    ): List<GitHubReleaseListDto>

    @POST("repos/{owner}/{repo}/releases")
    suspend fun createRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateReleaseRequest
    ): GitHubReleaseListDto

    @DELETE("repos/{owner}/{repo}/releases/{release_id}")
    suspend fun deleteRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("release_id") releaseId: Long
    ): retrofit2.Response<Unit>

    @GET("repos/{owner}/{repo}/releases/{release_id}/assets")
    suspend fun listReleaseAssets(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("release_id") releaseId: Long
    ): List<GitHubReleaseAssetListDto>

    @POST("repos/{owner}/{repo}/releases/{release_id}/assets")
    @retrofit2.http.Multipart
    suspend fun uploadReleaseAsset(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("release_id") releaseId: Long,
        @retrofit2.http.Part("name") name: okhttp3.MultipartBody.Part,
        @retrofit2.http.Part asset: okhttp3.MultipartBody.Part
    ): GitHubReleaseAssetListDto

    @GET("repos/{owner}/{repo}/releases/assets/{asset_id}")
    suspend fun downloadReleaseAsset(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("asset_id") assetId: Long,
        @retrofit2.http.Header("Accept") accept: String = "application/octet-stream"
    ): okhttp3.ResponseBody

    // ===== Search =====

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int = 30
    ): GitHubSearchRepoDto

    @GET("search/code")
    suspend fun searchCode(
        @Query("q") query: String,
        @Query("per_page") perPage: Int = 30
    ): GitHubSearchCodeDto

    // ===== Commit detail (diff) + repo archive =====

    @GET("repos/{owner}/{repo}/commits/{sha}")
    suspend fun getCommitDetail(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String
    ): GitHubCommitDetailDto

    @GET("repos/{owner}/{repo}/zipball/{ref}")
    suspend fun downloadRepoZip(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("ref") ref: String
    ): okhttp3.ResponseBody
}
