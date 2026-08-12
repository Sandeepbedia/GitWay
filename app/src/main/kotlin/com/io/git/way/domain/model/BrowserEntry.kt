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

/** One visible row in the VS Code/Acode-style tree explorer.
 * Each real directory remains a separate row so guide lines can connect parent,
 * child and sibling rows without package-chain compaction. */

data class TreeRow(
    val entry: BrowserEntry,
    val depth: Int,
    val isExpanded: Boolean = false,
    val directChildCount: Int = 0,
    /** One flag per indent column (size == [depth]): index i (< depth-1) says whether the
     * ancestor at that level still has more siblings after it (so its guide line should
     * keep running past this row); the last index says whether THIS row itself has more
     * siblings after it (so its own column's line should extend below its elbow for the
     * next sibling to connect into). Empty for a root-level row (nothing to draw). */
    val ancestorLines: List<Boolean> = emptyList()
)

/** PRD "Repository Explorer" §19 Sort/View — folders always sort ahead of files within
 * whichever mode is picked; NAME_DESC/TYPE only reorder within that. */
enum class BrowserSortMode(val label: String) {
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    TYPE("Type")
}

/** PRD §12 Search filters / §19 Sort — narrows the tree to one file-type family.
 * Folders are always shown regardless of the active filter so the tree stays navigable. */
enum class BrowserTypeFilter(val label: String) {
    ALL("All"),
    KOTLIN("Kotlin"),
    XML("XML"),
    GRADLE("Gradle"),
    IMAGES("Images"),
    OTHER("Other")
}
