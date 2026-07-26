package com.io.git.way.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubUserDto(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val name: String? = null
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
