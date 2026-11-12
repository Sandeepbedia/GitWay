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
package com.io.git.way.data.local

object PathNormalizer {

    /** Characters GitHub rejects outright in a tree entry path. */
    private val INVALID_CHARS = charArrayOf('\u0000')

    class InvalidPathException(val path: String) :
        IllegalArgumentException("Invalid file path for GitHub: \"$path\"")

    fun normalize(path: String): String {
        var result = path.replace('\\', '/')
        while (result.contains("//")) result = result.replace("//", "/")
        result = result.trim('/')

        if (result.isEmpty()) throw InvalidPathException(path)
        if (result.any { it in INVALID_CHARS }) throw InvalidPathException(path)
        if (result.split("/").any { it == "." || it == ".." }) throw InvalidPathException(path)

        return result
    }
}
