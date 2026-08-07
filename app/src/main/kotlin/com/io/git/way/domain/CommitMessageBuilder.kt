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

package com.io.git.way.domain

import com.io.git.way.domain.model.ChangeType
import com.io.git.way.domain.model.FileChange

/**
 * Builds the commit message that actually reaches GitHub. Every commit Git Way makes is
 * always prefixed "Git Way Sync: " — the user's own custom text (if they typed one) or
 * the auto-generated "N added, M modified, K removed" summary (if the field was left
 * blank) becomes the rest of it. Shared so the Confirmation screen's preview/placeholder
 * and the actual upload always agree on exactly what will be committed.
 */
object CommitMessageBuilder {
    private const val PREFIX = "Git Way Sync: "

    /** Just the "N added, M modified, K removed" part, no prefix — this is what the
     * default commit message's body looks like when the user leaves the field blank. */
    fun summary(changes: List<FileChange>): String {
        val added = changes.count { it.type == ChangeType.ADDED }
        val modified = changes.count { it.type == ChangeType.MODIFIED }
        val removed = changes.count { it.type == ChangeType.REMOVED }
        return "$added added, $modified modified, $removed removed"
    }

    /** The full message to send to GitHub: always "Git Way Sync: <body>". [customMessage]
     * is the user's own typed text; blank/whitespace-only falls back to [summary]. */
    fun resolve(customMessage: String, changes: List<FileChange>): String =
        PREFIX + customMessage.trim().ifBlank { summary(changes) }

    /** What the Confirmation screen shows as a live preview/placeholder while the field
     * is still blank — same text [resolve] would actually send. */
    fun defaultPreview(changes: List<FileChange>): String = PREFIX + summary(changes)
}
