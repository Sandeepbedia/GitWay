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

/**
 * Fine-grained phases of [com.io.git.way.domain.repository.GitHubRepository.syncChanges]
 * (PRD "Push HTTP 422 Fix" §11 Progress UI). Nothing is written to the branch until
 * UPDATING_BRANCH, so every earlier phase can be retried/aborted with zero remote
 * side effects.
 */
enum class UploadPhase {
    IDLE,
    VALIDATING,
    PREPARING,
    CREATING_BLOBS,
    CREATING_TREE,
    CREATING_COMMIT,
    UPDATING_BRANCH,
    VERIFYING,
    DONE
}
