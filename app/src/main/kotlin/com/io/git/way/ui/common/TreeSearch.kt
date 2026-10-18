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
package com.io.git.way.ui.common

/**
 * Ranked file-name search, ported from the PhoneIDE/LSP project tree's
 * `FileSearch.kt` and `TREE_REVEAL_ROWS` search box — adapted here to score
 * remote repository paths (plain strings from [com.io.git.way.domain.model.RemoteTreeEntry])
 * instead of `java.io.File`.
 *
 * The box accepts one piece of shorthand rather than a query language: a token of
 * the form `*.kt`, `.kt` or `kt:` is lifted out as an extension constraint and the
 * rest is matched against the name. That covers the thing people actually type
 * ("just the Kotlin files") without asking anyone to learn a syntax, and it is
 * unambiguous — a real filename token cannot start with `*.` or `.` followed only
 * by letters.
 */
data class TreeSearchQuery(
    val raw: String,
    /** Lowercased name fragment, with any extension token removed. */
    val needle: String,
    /** Extensions to require, lowercase and without the dot. Empty means any. */
    val extensions: Set<String>,
) {
    val isBlank: Boolean get() = needle.isEmpty() && extensions.isEmpty()

    companion object {
        fun parse(raw: String): TreeSearchQuery {
            val extensions = mutableSetOf<String>()
            val words = mutableListOf<String>()

            for (token in raw.trim().split(' ').filter { it.isNotBlank() }) {
                val ext = extensionToken(token)
                if (ext != null) extensions += ext else words += token
            }
            return TreeSearchQuery(
                raw = raw,
                needle = words.joinToString(" ").lowercase(),
                extensions = extensions,
            )
        }

        /** `*.kt`, `.kt` and `kt:` all mean "extension kt". Anything else is a name. */
        private fun extensionToken(token: String): String? {
            val body = when {
                token.startsWith("*.") -> token.drop(2)
                token.startsWith(".") -> token.drop(1)
                token.endsWith(":") -> token.dropLast(1)
                else -> return null
            }
            // Letters and digits only, so `.env.local` and `Foo:Bar` stay names.
            return body.lowercase().takeIf { it.isNotEmpty() && it.all(Char::isLetterOrDigit) }
        }
    }
}

/**
 * Where a query matched a name, and how well.
 *
 * [ranges] are indices into the original name so a result row can embolden exactly
 * the characters that matched — without it a fuzzy result looks arbitrary, which is
 * the usual reason people distrust fuzzy finders.
 */
data class TreeNameMatch(val score: Int, val ranges: List<IntRange>)

/**
 * Score [name] against [needle], or null when it does not match at all.
 *
 * Five tiers, best first, so `Main` puts `Main.kt` above `MainActivity.kt` above
 * `RemainderTest.kt`. The last tier is a subsequence match — `mvm` finds
 * `MainViewModel.kt` — which is what makes a finder usable when you remember a
 * type's shape but not its spelling. It scores far below the literal tiers so it
 * can never outrank a real substring hit.
 */
fun matchTreeName(name: String, needle: String): TreeNameMatch? {
    if (needle.isEmpty()) return TreeNameMatch(SCORE_NO_QUERY, emptyList())
    val lower = name.lowercase()

    if (lower == needle) return TreeNameMatch(SCORE_EXACT, listOf(name.indices.first..name.indices.last))

    if (lower.startsWith(needle)) {
        return TreeNameMatch(SCORE_PREFIX, listOf(0..needle.lastIndex))
    }

    val contains = lower.indexOf(needle)
    if (contains >= 0) {
        // A hit that starts at a word boundary reads as intentional; one in the
        // middle of a word is usually incidental, so they are separate tiers.
        val boundary = contains == 0 || isTreeBoundary(name, contains)
        return TreeNameMatch(
            score = if (boundary) SCORE_WORD_START else SCORE_CONTAINS,
            ranges = listOf(contains..contains + needle.lastIndex),
        )
    }

    return fuzzyTreeMatch(name, lower, needle)
}

/**
 * Subsequence match: every needle character appears in order.
 *
 * Consecutive hits and hits on a word boundary are worth more, which is what makes
 * `mvm` prefer `MainViewModel` over `myvaluemap`. Bounded by the name length, so
 * this is linear and safe to run over a few thousand entries per keystroke.
 */
private fun fuzzyTreeMatch(name: String, lower: String, needle: String): TreeNameMatch? {
    val ranges = mutableListOf<IntRange>()
    var bonus = 0
    var from = 0
    var previousIndex = -2

    for (char in needle) {
        val at = lower.indexOf(char, from)
        if (at < 0) return null
        if (at == previousIndex + 1) {
            bonus += FUZZY_ADJACENT_BONUS
            // Extend the previous range instead of adding a second one, so a run of
            // adjacent hits highlights as one span.
            ranges[ranges.lastIndex] = ranges.last().first..at
        } else {
            if (at == 0 || isTreeBoundary(name, at)) bonus += FUZZY_BOUNDARY_BONUS
            ranges += at..at
        }
        previousIndex = at
        from = at + 1
    }
    return TreeNameMatch(SCORE_FUZZY + bonus, ranges)
}

/** True when [index] starts a new word — after a separator, or a camelCase hump. */
private fun isTreeBoundary(name: String, index: Int): Boolean {
    if (index <= 0 || index > name.lastIndex) return false
    val previous = name[index - 1]
    if (previous == '.' || previous == '_' || previous == '-' || previous == ' ') return true
    return previous.isLowerCase() && name[index].isUpperCase()
}

/**
 * Order for search results.
 *
 * Score first, then the shorter name, then the shorter path. The two tie-breakers
 * matter more than they look: a repo has `strings.xml` in six resource folders,
 * and without them the winner would be whichever directory the listing reached first.
 */
fun compareTreeResults(a: Triple<Int, String, String>, b: Triple<Int, String, String>): Int {
    val byScore = b.first.compareTo(a.first)
    if (byScore != 0) return byScore
    val byName = a.second.length.compareTo(b.second.length)
    if (byName != 0) return byName
    return a.third.length.compareTo(b.third.length)
}

private const val SCORE_NO_QUERY = 500
private const val SCORE_EXACT = 1000
private const val SCORE_PREFIX = 800
private const val SCORE_WORD_START = 700
private const val SCORE_CONTAINS = 600
private const val SCORE_FUZZY = 200
private const val FUZZY_ADJACENT_BONUS = 8
private const val FUZZY_BOUNDARY_BONUS = 14
