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

import android.content.Context
import com.io.git.way.data.local.GitBlobHasher
import com.io.git.way.domain.model.ChangeType
import com.io.git.way.domain.model.FileChange
import com.io.git.way.domain.model.LocalFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Result of a comparison: the actual Added/Modified/Removed changes, plus how many
 * remote-only paths were excluded from [ChangeType.REMOVED] — either built-in
 * repository scaffolding (README, LICENSE, .github/, etc. — see
 * [RepositoryScaffoldFiles]) or paths the person manually marked "don't track".
 */
data class DiffResult(
    val changes: List<FileChange>,
    val ignoredScaffoldFiles: List<String>
)

/**
 * Compares the locally scanned file list against the GitHub repository's current tree
 * and classifies every path into Added / Modified / Removed (PRD1 §3.3).
 *
 * Local blob-SHA computation only runs for paths that exist on both sides (PRD1 §3.5) —
 * files that are clearly Added are never hashed. Hashing runs with limited concurrency
 * since it dominates comparison time for large trees.
 *
 * Removed detection only ever applies to the app's own project files — repository
 * scaffolding GitHub or a maintainer added directly on github.com (README, LICENSE,
 * .gitignore, .github/ workflows, etc.) is never part of the local folder in the first
 * place, so it's excluded before it can be mistaken for a deletion. See
 * [RepositoryScaffoldFiles].
 */
object ComparisonEngine {

    private const val MAX_CONCURRENT_HASHES = 6
    private const val PROGRESS_THRESHOLD = 200

    suspend fun computeDiff(
        context: Context,
        localFiles: List<LocalFile>,
        remotePaths: Map<String, String>,
        contentOverrides: Map<String, ByteArray> = emptyMap(),
        customIgnoredPaths: Set<String> = emptySet(),
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): DiffResult = withContext(Dispatchers.Default) {
        val localByPath = localFiles.associateBy { it.relativePath }
        val localPaths = localByPath.keys
        val remoteKeys = remotePaths.keys

        val added = localPaths - remoteKeys
        val remoteOnly = remoteKeys - localPaths
        val (scaffold, removed) = remoteOnly.partition {
            RepositoryScaffoldFiles.isScaffoldFile(it) || it in customIgnoredPaths
        }
        val common = localPaths intersect remoteKeys

        val modified = mutableListOf<String>()
        if (common.isNotEmpty()) {
            val semaphore = Semaphore(MAX_CONCURRENT_HASHES)
            val completed = AtomicInteger(0)
            val total = common.size
            val showProgress = total > PROGRESS_THRESHOLD

            val results = coroutineScope {
                common.map { path ->
                    async {
                        semaphore.withPermit {
                            val file = localByPath.getValue(path)
                            // Redacted/generated files (Smart Upload Protection) hash the
                            // overridden bytes that will actually be uploaded, not the
                            // real on-disk content, so the diff matches what's pushed.
                            val override = contentOverrides[path]
                            val localSha = if (override != null) {
                                GitBlobHasher.hashBytes(override)
                            } else {
                                GitBlobHasher.hash(context, file.documentUri)
                            }
                            val done = completed.incrementAndGet()
                            if (showProgress) onProgress(done, total)
                            if (localSha != remotePaths[path]) path else null
                        }
                    }
                }.map { it.await() }
            }
            modified += results.filterNotNull()
        }

        val changes = buildList {
            added.forEach { add(FileChange(fileName = it.substringAfterLast('/'), filePath = it, type = ChangeType.ADDED)) }
            modified.forEach { add(FileChange(fileName = it.substringAfterLast('/'), filePath = it, type = ChangeType.MODIFIED)) }
            removed.forEach { add(FileChange(fileName = it.substringAfterLast('/'), filePath = it, type = ChangeType.REMOVED)) }
        }

        DiffResult(changes = changes, ignoredScaffoldFiles = scaffold.sorted())
    }
}
