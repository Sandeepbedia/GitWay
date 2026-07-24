package com.io.git.way.data.remote.dto

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
    @SerialName("default_branch") val defaultBranch: String? = null
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

@Serializable
data class CreateTreeRequest(
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
