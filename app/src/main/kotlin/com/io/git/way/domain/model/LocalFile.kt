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

import android.net.Uri

/**
 * A single file discovered while walking the user-selected local project folder
 * (PRD1 "Folder Selection Screen"). [relativePath] is forward-slash separated and
 * relative to the selected root, matching GitHub's path format (e.g. "app/src/Main.kt").
 */
data class LocalFile(
    val relativePath: String,
    val displayName: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val documentUri: Uri
)
