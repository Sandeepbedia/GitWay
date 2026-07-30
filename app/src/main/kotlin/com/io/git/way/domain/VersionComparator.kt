package com.io.git.way.domain

/**
 * Compares version strings like "v1.0", "1.2.3", "v2.0.1" numerically, segment by
 * segment, ignoring a leading "v"/"V". Used to decide whether a GitHub release tag is
 * actually newer than the installed app, rather than just "different".
 */
object VersionComparator {

    /** True if [remote] is a strictly newer version than [current]. Malformed input on
     * either side returns false — a broken comparison should never nag the user with a
     * false "update available", it should just stay quiet. */
    fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = parse(remote) ?: return false
        val currentParts = parse(current) ?: return false

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }

    private fun parse(version: String): List<Int>? {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
        if (cleaned.isEmpty()) return null
        // Drop any pre-release/build suffix like "-beta" or "+build.1" before splitting.
        val core = cleaned.substringBefore('-').substringBefore('+')
        val segments = core.split(".")
        val numbers = segments.map { it.toIntOrNull() }
        return if (numbers.any { it == null }) null else numbers.filterNotNull()
    }
}
