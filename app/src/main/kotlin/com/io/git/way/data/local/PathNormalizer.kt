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
