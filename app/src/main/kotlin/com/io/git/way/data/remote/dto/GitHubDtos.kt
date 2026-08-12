package com.io.git.way.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubUserDto(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val name: String? = null,
    val bio: String? = null,
    val company: String? = null,
    val location: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("public_repos") val publicRepos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class GitHubOwnerDto(
    val login: String
)

@Serializable
data class GitHubRepoDto(
    val name: String,
    @SerialName("full_name") val fullName: String,
    val owner: GitHubOwnerDto,
    @SerialName("private") val isPrivate: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("default_branch") val defaultBranch: String? = null,
    val language: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val archived: Boolean = false,
    val disabled: Boolean = false,
    val fork: Boolean = false,
    @SerialName("stargazers_count") val stargazersCount: Int = 0,
    @SerialName("forks_count") val forksCount: Int = 0,
    val permissions: GitHubRepoPermissionsDto? = null
)

@Serializable
data class GitHubRepoPermissionsDto(
    val push: Boolean = false,
    val admin: Boolean = false,
    val pull: Boolean = false
)

/** Shape of GitHub's standard Git Data API error body (PRD §1 "parse and display GitHub's error response"). */
@Serializable
data class GitHubErrorResponseDto(
    val message: String? = null,
    val errors: List<GitHubErrorItemDto>? = null,
    @SerialName("documentation_url") val documentationUrl: String? = null
)

@Serializable
data class GitHubErrorItemDto(
    val resource: String? = null,
    val field: String? = null,
    val code: String? = null,
    val message: String? = null
)

// ===== Git Data API (PRD1 §3.2 / PRD2 §3.2) =====

@Serializable
data class GitTreeEntryDto(
    val path: String,
    val mode: String? = null,
    val type: String,
    val sha: String? = null,
    val size: Long? = null,
    val url: String? = null
)

@Serializable
data class GitTreeResponseDto(
    val sha: String,
    val tree: List<GitTreeEntryDto> = emptyList(),
    val truncated: Boolean = false
)

@Serializable
data class GitRefObjectDto(
    val sha: String,
    val type: String? = null
)

@Serializable
data class GitRefDto(
    val ref: String,
    @SerialName("object") val objectRef: GitRefObjectDto
)

@Serializable
data class GitTreeRefDto(
    val sha: String,
    val url: String? = null
)

@Serializable
data class GitCommitDetailDto(
    val sha: String,
    val tree: GitTreeRefDto
)

@Serializable
data class CreateBlobRequest(
    val content: String,
    val encoding: String = "base64"
)

@Serializable
data class GitBlobRefDto(
    val sha: String,
    val url: String? = null
)

@Serializable
data class TreeEntryInput(
    val path: String,
    val mode: String = "100644",
    val type: String = "blob",
    val sha: String? = null
)

/** Body for the Contents API "create file" call used only to bootstrap a genuinely
 * empty repository (see [com.io.git.way.data.remote.GitHubApiService.createFileContent]).
 * `branch` must be sent explicitly — omitting it makes GitHub guess the default branch,
 * which is unreliable for a repo that has no commits/refs yet. */
@Serializable
data class CreateFileContentRequest(
    val message: String,
    val content: String,
    val branch: String
)

@Serializable
data class ContentsCommitDto(
    val sha: String,
    val tree: GitTreeRefDto
)

@Serializable
data class CreateFileContentResponseDto(
    val commit: ContentsCommitDto
)

/**
 * The app-wide Json config uses `encodeDefaults = true` (needed so [CreateCommitRequest]'s
 * `parents` is always sent, even as `[]`, to explicitly signal "root commit, no parent" —
 * omitting it would make GitHub infer a parent from the branch's current commit instead).
 * But that same setting would also force `base_tree` to serialize as an explicit JSON
 * `null` whenever it's unset, and GitHub's Git Data API does NOT treat an explicit
 * `"base_tree": null` the same as the key being absent — on a brand-new/empty repository
 * (no git objects at all yet) sending that explicit null makes /git/trees fail with
 * "409 Git Repository is empty." instead of creating a fresh root tree. Per GitHub's own
 * docs, base_tree must simply be omitted to build a tree with no base. @EncodeDefault(NEVER)
 * overrides the file-wide setting for just this one property: still omitted when null
 * (the default), still sent normally whenever a real tree sha is provided.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CreateTreeRequest(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("base_tree") val baseTree: String? = null,
    val tree: List<TreeEntryInput>
)

@Serializable
data class CreateCommitRequest(
    val message: String,
    val tree: String,
    val parents: List<String> = emptyList()
)

@Serializable
data class GitCommitRefDto(
    val sha: String,
    val url: String? = null
)

@Serializable
data class UpdateRefRequest(
    val sha: String,
    val force: Boolean = false
)

@Serializable
data class CreateRefRequest(
    val ref: String,
    val sha: String
)

/** Response of GET git/blobs/{sha} — used to read a file's current content for the
 * Repository Browser's viewer/editor/copy-paste features (blob sha already known from
 * the cached repo tree, so no extra Contents-API round trip is needed). */
@Serializable
data class GitBlobContentDto(
    val sha: String,
    val content: String,
    val encoding: String,
    val size: Long = 0
)

/** GET repos/{owner}/{repo}/releases/latest — used by the in-app update checker. Only
 * the fields the update dialog actually needs are modeled; unknown fields are ignored. */
@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GitHubReleaseAssetDto> = emptyList()
)

@Serializable
data class GitHubReleaseAssetDto(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)

// ===== Branches (Repository Management) =====

@Serializable
data class GitHubBranchDto(
    val name: String,
    val commit: GitTreeRefDto,
    val protected: Boolean = false
)

// ===== Commit history (Repository Management) =====

@Serializable
data class GitHubCommitAuthorDto(
    val name: String? = null,
    val date: String? = null
)

@Serializable
data class GitHubCommitMessageDto(
    val message: String,
    val author: GitHubCommitAuthorDto? = null
)

@Serializable
data class GitHubCommitListItemDto(
    val sha: String,
    val commit: GitHubCommitMessageDto,
    val author: GitHubUserDto? = null,
    @SerialName("html_url") val htmlUrl: String? = null
)

// ===== Repository create/update (Repository Management) =====

@Serializable
data class CreateRepoRequest(
    val name: String,
    val description: String? = null,
    @SerialName("private") val isPrivate: Boolean = false,
    @SerialName("auto_init") val autoInit: Boolean = true
)

@Serializable
data class UpdateRepoRequest(
    val name: String? = null,
    val description: String? = null,
    @SerialName("private") val isPrivate: Boolean? = null
)

// ===== API rate limit (Overview dashboard) =====

@Serializable
data class RateLimitDto(val resources: RateLimitResourcesDto)

@Serializable
data class RateLimitResourcesDto(val core: RateLimitCoreDto)

@Serializable
data class RateLimitCoreDto(val limit: Int, val remaining: Int, val reset: Long)

// ===== GitHub Actions (workflow scope) =====

@Serializable
data class GitHubWorkflowDto(
    val id: Long = 0,
    val name: String = "",
    val path: String = "",
    val state: String = "active",
    @SerialName("badge_url") val badgeUrl: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class GitHubWorkflowListDto(
    @SerialName("total_count") val totalCount: Int = 0,
    val workflows: List<GitHubWorkflowDto> = emptyList()
)

@Serializable
data class GitHubWorkflowRunDto(
    val id: Long = 0,
    val name: String? = null,
    @SerialName("display_title") val displayTitle: String? = null,
    @SerialName("head_branch") val branch: String? = null,
    @SerialName("head_sha") val headSha: String? = null,
    @SerialName("run_number") val runNumber: Int = 0,
    val status: String = "queued",
    val conclusion: String? = null,
    val event: String = "push",
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("workflow_id") val workflowId: Long = 0,
    @SerialName("jobs_url") val jobsUrl: String? = null,
    @SerialName("artifacts_url") val artifactsUrl: String? = null
)

@Serializable
data class GitHubWorkflowRunListDto(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("workflow_runs") val runs: List<GitHubWorkflowRunDto> = emptyList()
)

@Serializable
data class GitHubWorkflowDispatchRequest(
    val ref: String,
    val inputs: Map<String, String> = emptyMap()
)

@Serializable
data class GitHubArtifactDto(
    val id: Long = 0,
    val name: String = "",
    val size: Long = 0,
    val expired: Boolean = false,
    @SerialName("expired_at") val expiredAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("archive_download_url") val archiveDownloadUrl: String? = null
)

@Serializable
data class GitHubArtifactListDto(
    @SerialName("total_count") val totalCount: Int = 0,
    val artifacts: List<GitHubArtifactDto> = emptyList()
)

// ===== Pull requests =====

@Serializable
data class GitHubPullRequestDto(
    val number: Int = 0,
    val title: String = "",
    val state: String = "open",
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("merged_at") val mergedAt: String? = null,
    @SerialName("head") val head: GitHubPullRefDto? = null,
    @SerialName("base") val base: GitHubPullRefDto? = null,
    @SerialName("user") val user: GitHubUserDto? = null,
    @SerialName("mergeable") val mergeable: Boolean? = null,
    @SerialName("commits") val commitsCount: Int = 0,
    @SerialName("changed_files") val changedFiles: Int = 0,
    @SerialName("additions") val additions: Int = 0,
    @SerialName("deletions") val deletions: Int = 0
)

@Serializable
data class GitHubPullRefDto(
    val label: String? = null,
    val ref: String = "",
    val sha: String? = null,
    val repo: GitHubPullRepoDto? = null
)

@Serializable
data class GitHubPullRepoDto(
    val name: String? = null,
    @SerialName("full_name") val fullName: String? = null
)

@Serializable
data class CreatePullRequestRequest(
    val title: String,
    val head: String,
    val base: String,
    val body: String? = null
)

@Serializable
data class UpdatePullRequestRequest(
    val state: String? = null,
    val title: String? = null,
    val body: String? = null
)

@Serializable
data class MergePullRequestRequest(
    @SerialName("commit_title") val commitTitle: String? = null,
    @SerialName("merge_method") val mergeMethod: String? = null
)

@Serializable
data class GitHubPullRequestFileDto(
    val filename: String = "",
    val status: String = "modified",
    val additions: Int = 0,
    val deletions: Int = 0,
    val patch: String? = null,
    @SerialName("raw_url") val rawUrl: String? = null
)

// ===== Issues =====

@Serializable
data class GitHubIssueDto(
    val number: Int = 0,
    val title: String = "",
    val state: String = "open",
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("closed_at") val closedAt: String? = null,
    @SerialName("user") val user: GitHubUserDto? = null,
    @SerialName("comments") val commentsCount: Int = 0,
    @SerialName("pull_request") val pullRequest: GitHubIssuePullMarkerDto? = null
)

@Serializable
data class GitHubIssuePullMarkerDto(
    @SerialName("html_url") val htmlUrl: String? = null
)

@Serializable
data class CreateIssueRequest(
    val title: String,
    val body: String? = null
)

@Serializable
data class UpdateIssueRequest(
    val state: String? = null,
    val title: String? = null,
    val body: String? = null
)

@Serializable
data class GitHubIssueCommentDto(
    val id: Long = 0,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user") val user: GitHubUserDto? = null
)

@Serializable
data class CreateIssueCommentRequest(
    val body: String
)

// ===== Star / fork =====

@Serializable
data class CreateForkRequest(
    val organization: String? = null,
    @SerialName("default_branch_only") val defaultBranchOnly: Boolean = false
)

// ===== Releases (full management) =====

@Serializable
data class GitHubReleaseListDto(
    val id: Long = 0,
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("target_commitish") val targetCommitish: String? = null,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("upload_url") val uploadUrl: String? = null,
    val assets: List<GitHubReleaseAssetDto> = emptyList()
)

@Serializable
data class CreateReleaseRequest(
    @SerialName("tag_name") val tagName: String,
    @SerialName("target_commitish") val targetCommitish: String? = null,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false
)

@Serializable
data class GitHubReleaseAssetListDto(
    val id: Long = 0,
    val name: String = "",
    val size: Long = 0,
    @SerialName("browser_download_url") val browserDownloadUrl: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// ===== Search + commit detail (diff) =====

@Serializable
data class GitHubSearchRepoDto(
    @SerialName("total_count") val totalCount: Int = 0,
    val items: List<GitHubRepoDto> = emptyList()
)

@Serializable
data class GitHubSearchCodeDto(
    @SerialName("total_count") val totalCount: Int = 0,
    val items: List<GitHubSearchCodeItemDto> = emptyList()
)

@Serializable
data class GitHubSearchCodeItemDto(
    val name: String = "",
    val path: String = "",
    @SerialName("html_url") val htmlUrl: String? = null,
    val repository: GitHubRepoDto? = null
)

@Serializable
data class GitHubCommitFileDto(
    val filename: String = "",
    val status: String = "modified",
    val additions: Int = 0,
    val deletions: Int = 0,
    val changes: Int = 0,
    val patch: String? = null,
    @SerialName("raw_url") val rawUrl: String? = null
)

@Serializable
data class GitHubCommitDetailDto(
    val sha: String = "",
    val commit: GitHubCommitMessageDto? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val files: List<GitHubCommitFileDto> = emptyList()
)
