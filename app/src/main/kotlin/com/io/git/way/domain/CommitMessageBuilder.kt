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
package com.io.git.way.domain

import com.io.git.way.domain.model.ChangeType
import com.io.git.way.domain.model.FileChange

object CommitMessageBuilder {
    // ============================================================
    //  COMMIT WATERMARK
    //  Yahan apna watermark text likho — jab chaho laga/hata sakte ho.
    //  Har commit message ke neeche (body me) ye text add hoga.
    //  Example: "Made with Git Way"
    //  Blank ("") = watermark OFF.
    // ============================================================
    const val WATERMARK: String = ""

    fun summary(changes: List<FileChange>): String {
        val added = changes.count { it.type == ChangeType.ADDED }
        val modified = changes.count { it.type == ChangeType.MODIFIED }
        val removed = changes.count { it.type == ChangeType.REMOVED }
        return "$added added, $modified modified, $removed removed"
    }

    /** Appends the watermark as a commit-message body when one is configured. */
    fun applyWatermark(message: String): String {
        val watermark = WATERMARK.trim()
        return if (watermark.isEmpty()) message else "$message\n\n$watermark"
    }

    fun resolve(customMessage: String, changes: List<FileChange>): String =
        customMessage.trim().ifBlank { summary(changes) }

    fun defaultPreview(changes: List<FileChange>): String =
        applyWatermark(summary(changes))
}
