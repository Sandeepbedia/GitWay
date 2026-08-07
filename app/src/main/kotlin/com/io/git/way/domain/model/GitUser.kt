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

package com.io.git.way.domain.model

/** The authenticated GitHub account — resolved after token validation, and re-fetchable
 * any time via [com.io.git.way.domain.repository.GitHubRepository.getCurrentUser] for
 * screens (Overview, Profile) that need it without re-validating the token. */
data class GitUser(
    val username: String,
    val avatarUrl: String?,
    val displayName: String?,
    val bio: String? = null,
    val company: String? = null,
    val location: String? = null,
    val htmlUrl: String = "",
    val publicRepos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val createdAt: String = ""
)

/** GitHub API core rate limit — surfaced on the Overview dashboard so a heavy user can
 * see when they're approaching the hourly cap instead of hitting an opaque 403. */
data class ApiRateLimit(
    val limit: Int,
    val remaining: Int,
    /** Epoch seconds when the limit resets. */
    val resetEpochSeconds: Long
)
