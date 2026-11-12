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

/** A single file or folder row in the Repository Browser (file-manager view of a repo). */
data class BrowserEntry(
    val name: String,
    val path: String,
    val isFolder: Boolean
)


data class TreeRow(
    val entry: BrowserEntry,
    val depth: Int,
    val isExpanded: Boolean = false,
    val directChildCount: Int = 0,
    val ancestorLines: List<Boolean> = emptyList()
)

enum class BrowserSortMode(val label: String) {
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    TYPE("Type")
}

enum class BrowserTypeFilter(val label: String) {
    ALL("All"),
    KOTLIN("Kotlin"),
    XML("XML"),
    GRADLE("Gradle"),
    IMAGES("Images"),
    OTHER("Other")
}
