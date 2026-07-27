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
