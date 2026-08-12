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

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.io.git.way.domain.model.LocalFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Recursively walks a SAF tree Uri into a flat list of [LocalFile] (PRD1 §2.2).
 * Only metadata is read here — file bytes are read later, lazily, only for files that
 * end up Added or Modified (PRD1 §2.4).
 */
object FolderScanner {

    /** Noise directories skipped by default (PRD1 §2.2, not user-facing yet). */
    private val EXCLUDED_DIRS = setOf(".git", ".gradle", "build", ".idea", "node_modules")

    suspend fun scan(context: Context, treeUri: Uri): List<LocalFile> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val result = mutableListOf<LocalFile>()
        walk(root, "", result)
        result
    }

    /** Reads a scanned file's raw bytes — shared by the upload pipeline, the Smart Upload
     * Protection scan's .gitignore parsing, and its secret-detection pass. */
    suspend fun readBytes(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    }

    private fun walk(dir: DocumentFile, relativePrefix: String, out: MutableList<LocalFile>) {
        for (child in dir.listFiles()) {
            val name = child.name ?: continue
            val relativePath = if (relativePrefix.isEmpty()) name else "$relativePrefix/$name"
            if (child.isDirectory) {
                if (name in EXCLUDED_DIRS) continue
                walk(child, relativePath, out)
            } else if (child.isFile) {
                out += LocalFile(
                    relativePath = relativePath,
                    displayName = name,
                    sizeBytes = child.length(),
                    lastModified = child.lastModified(),
                    documentUri = child.uri
                )
            }
        }
    }
}
