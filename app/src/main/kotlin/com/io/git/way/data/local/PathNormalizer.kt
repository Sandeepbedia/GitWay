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

package com.io.git.way.data.local

/**
 * Normalizes and validates file paths before they're used as Git tree entry paths
 * (422-fix PRD §7 "Normalize File Paths"). GitHub's `POST git/trees` returns HTTP 422
 * for tree entries with invalid paths (empty segments from `//`, a leading `/`, or
 * control characters), and that 422 has nothing to do with the SHA/ref logic — so it
 * must be caught here, before a blob is ever created for the path.
 */
object PathNormalizer {

    /** Characters GitHub rejects outright in a tree entry path. */
    private val INVALID_CHARS = charArrayOf('\u0000')

    class InvalidPathException(val path: String) :
        IllegalArgumentException("Invalid file path for GitHub: \"$path\"")

    /**
     * Collapses duplicate slashes, strips a leading "/", and rejects paths GitHub's
     * Git Data API cannot accept. Throws [InvalidPathException] rather than silently
     * dropping the file, so the caller can surface it instead of getting a mystery 422.
     */
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
