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
 * remote-only paths were repository scaffolding (README, LICENSE, .github/, etc.) and
 * therefore deliberately excluded from [ChangeType.REMOVED] — see [RepositoryScaffoldFiles].
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
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): DiffResult = withContext(Dispatchers.Default) {
        val localByPath = localFiles.associateBy { it.relativePath }
        val localPaths = localByPath.keys
        val remoteKeys = remotePaths.keys

        val added = localPaths - remoteKeys
        val remoteOnly = remoteKeys - localPaths
        val (scaffold, removed) = remoteOnly.partition { RepositoryScaffoldFiles.isScaffoldFile(it) }
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
