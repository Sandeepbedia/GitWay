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
package com.io.git.way.domain.model

/** A GitHub Actions workflow file registered in the repo. */
data class GitHubWorkflow(
    val id: Long,
    val name: String,
    val path: String,
    val state: String,
    val badgeUrl: String? = null,
    val htmlUrl: String? = null,
    val updatedAt: String? = null
) {
    val canDispatch: Boolean
        get() = state != "disabled_manually" && state != "disabled_inactivity"
}

/** One GitHub Actions run of a workflow. */
data class WorkflowRun(
    val id: Long,
    val name: String,
    val displayTitle: String,
    val branch: String,
    val headSha: String,
    val runNumber: Int,
    val status: String,
    val conclusion: String?,
    val event: String,
    val htmlUrl: String?,
    val createdAt: String,
    val updatedAt: String?,
    val workflowId: Long
) {
    val isCompleted: Boolean
        get() = status == "completed"

    val isSuccess: Boolean
        get() = conclusion == "success"

    val isFailure: Boolean
        get() = conclusion != null &&
                conclusion != "success" &&
                conclusion != "skipped" &&
                conclusion != "cancelled"
}

/** A build artifact produced by an Actions run. */
data class ArtifactInfo(
    val id: Long,
    val name: String,
    val size: Long,
    val expired: Boolean,
    val createdAt: String? = null,
    val archiveDownloadUrl: String? = null
) {
    val isApk: Boolean
        get() = name.endsWith(".apk", ignoreCase = true)
}