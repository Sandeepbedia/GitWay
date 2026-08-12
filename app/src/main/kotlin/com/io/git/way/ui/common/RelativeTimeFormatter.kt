package com.io.git.way.ui.common

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

/** Turns a GitHub ISO-8601 timestamp (e.g. "2026-07-24T10:12:00Z") into "2m ago" / "3d ago" style text. */
fun formatRelativeTime(isoTimestamp: String): String {
    if (isoTimestamp.isBlank()) return ""
    val instant = try {
        Instant.parse(isoTimestamp)
    } catch (e: DateTimeParseException) {
        return isoTimestamp
    }

    val duration = Duration.between(instant, Instant.now())
    val seconds = duration.seconds.coerceAtLeast(0)

    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3600}h ago"
        seconds < 172_800 -> "Yesterday"
        seconds < 604_800 -> "${seconds / 86_400}d ago"
        seconds < 2_629_800 -> "${seconds / 604_800}w ago"
        else -> "${seconds / 2_629_800}mo ago"
    }
}
