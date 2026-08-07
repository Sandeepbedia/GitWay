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

/** One blob in a GitHub repository's file tree — sha (for content/diff lookups) plus
 * its size in bytes, when GitHub reports one. See [com.io.git.way.domain.repository.GitHubRepository.getRepositoryTreeDetailed]. */
data class RemoteTreeEntry(
    val sha: String,
    val size: Long?
)
