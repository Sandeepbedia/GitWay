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

/** A GitHub pull request in the selected repository. */
data class PullRequest(
    val number: Int,
    val title: String,
    val state: String,
    val body: String?,
    val htmlUrl: String?,
    val createdAt: String,
    val updatedAt: String?,
    val mergedAt: String?,
    val headRef: String,
    val baseRef: String,
    val headLabel: String?,
    val author: String,
    val mergeable: Boolean?,
    val changedFiles: Int,
    val additions: Int,
    val deletions: Int,
    val commentsCount: Int = 0
) {
    val isOpen: Boolean get() = state == "open"
    val isMerged: Boolean get() = mergedAt != null
}

/** A single file changed by a pull request, with its unified diff patch. */
data class PullRequestFile(
    val filename: String,
    val status: String,
    val additions: Int,
    val deletions: Int,
    val patch: String?
)
