/*
 * Git Way
 * Copyright (C) 2026 Sandeep Bedia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.io.git.way.data.remote

import com.io.git.way.data.remote.dto.CreateBlobRequest
import com.io.git.way.data.remote.dto.CreateCommitRequest
import com.io.git.way.data.remote.dto.CreateFileContentRequest
import com.io.git.way.data.remote.dto.CreateFileContentResponseDto
import com.io.git.way.data.remote.dto.CreateRefRequest
import com.io.git.way.data.remote.dto.CreateRepoRequest
import com.io.git.way.data.remote.dto.CreateTreeRequest
import com.io.git.way.data.remote.dto.GitBlobContentDto
import com.io.git.way.data.remote.dto.GitBlobRefDto
import com.io.git.way.data.remote.dto.GitCommitDetailDto
import com.io.git.way.data.remote.dto.GitCommitRefDto
import com.io.git.way.data.remote.dto.GitHubBranchDto
import com.io.git.way.data.remote.dto.GitHubCommitListItemDto
import com.io.git.way.data.remote.dto.GitHubRepoDto
import com.io.git.way.data.remote.dto.GitHubReleaseDto
import com.io.git.way.data.remote.dto.GitHubUserDto
import com.io.git.way.data.remote.dto.RateLimitDto
import com.io.git.way.data.remote.dto.GitRefDto
import com.io.git.way.data.remote.dto.GitTreeRefDto
import com.io.git.way.data.remote.dto.GitTreeResponseDto
import com.io.git.way.data.remote.dto.UpdateRefRequest
import com.io.git.way.data.remote.dto.UpdateRepoRequest
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
}
