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
package com.io.git.way.ui.screens.browser

/**
 * Pure helper for the file tree's thread lines.
 *
 * For a row at depth `k` the result holds a bitmask:
 *  - bit `L` for `1 <= L < k`: the thread that connects the depth `L-1` parent to its later children
 *    still runs through this row;
 *  - bit `k` (own level, only when `k >= 1`): this row has a later sibling, so its thread goes on below it.
 *    When clear, the row is the last child and its thread stops at the connector.
 * Depth 0 rows have no thread. Depths above 62 are clamped.
 */
object TreeGuides {
    const val MAX_DEPTH = 62

    fun masks(depths: List<Int>): LongArray {
        val out = LongArray(depths.size)
        val pending = BooleanArray(MAX_DEPTH + 2)
        for (i in depths.indices.reversed()) {
            val k = depths[i].coerceIn(0, MAX_DEPTH)
            var mask = 0L
            for (level in 1 until k) if (pending[level]) mask = mask or (1L shl level)
            if (k >= 1 && pending[k]) mask = mask or (1L shl k)
            out[i] = mask
            pending[k] = true
            for (d in k + 1 until pending.size) pending[d] = false
        }
        return out
    }

    fun continues(mask: Long, level: Int): Boolean = level in 1..MAX_DEPTH && (mask shr level) and 1L == 1L
}
