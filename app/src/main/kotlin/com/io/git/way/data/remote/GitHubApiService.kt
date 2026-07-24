package com.io.git.way.data.remote

import com.io.git.way.data.remote.dto.CreateBlobRequest
import com.io.git.way.data.remote.dto.CreateCommitRequest
import com.io.git.way.data.remote.dto.CreateRefRequest
import com.io.git.way.data.remote.dto.CreateTreeRequest
import com.io.git.way.data.remote.dto.GitBlobRefDto
import com.io.git.way.data.remote.dto.GitCommitDetailDto
import com.io.git.way.data.remote.dto.GitCommitRefDto
import com.io.git.way.data.remote.dto.GitHubRepoDto
import com.io.git.way.data.remote.dto.GitHubUserDto
import com.io.git.way.data.remote.dto.GitRefDto
import com.io.git.way.data.remote.dto.GitTreeRefDto
import com.io.git.way.data.remote.dto.GitTreeResponseDto
import com.io.git.way.data.remote.dto.UpdateRefRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** GitHub REST API v3 surface: auth, repository listing, and the Git Data API. */
interface GitHubApiService {

    @GET("user")
    suspend fun getAuthenticatedUser(): GitHubUserDto

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
}
