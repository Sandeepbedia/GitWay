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

/** One entry in a repository's commit history (read-only — Git Way still only ever
 * writes new commits via [com.io.git.way.domain.repository.GitHubRepository.syncChanges]). */
data class CommitSummary(
    val sha: String,
    val message: String,
    val authorName: String,
    val date: String,
    val htmlUrl: String
) {
    val shortSha: String get() = sha.take(7)
    /** First line only — commit bodies can be long, the history list just needs the title. */
    val title: String get() = message.substringBefore('\n')
}
