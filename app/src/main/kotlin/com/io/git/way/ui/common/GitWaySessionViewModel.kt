package com.io.git.way.ui.common

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.io.git.way.data.local.FolderScanner
import com.io.git.way.data.local.NetworkUtils
import com.io.git.way.data.local.ProtectionScanner
import com.io.git.way.domain.ComparisonEngine
import com.io.git.way.domain.model.BrowserEntry
import com.io.git.way.domain.model.ChangeType
import com.io.git.way.domain.model.FileChange
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.domain.model.LocalFile
import com.io.git.way.domain.model.ScanReport
import com.io.git.way.domain.model.TreeRow
import com.io.git.way.domain.model.UploadPhase
import com.io.git.way.domain.repository.GitHubRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GitWaySessionState(
    val selectedRepo: GitRepository? = null,

    val folderUri: Uri? = null,
    val folderName: String = "",
    val localFiles: List<LocalFile> = emptyList(),
    val isScanning: Boolean = false,
    val scanError: String? = null,

    /** Smart Upload Protection result for the current [localFiles] — [localFiles] is
     * already filtered down to [ScanReport.safeFiles], this is kept for the summary UI
     * (counts, ignored/blocked/secret lists). Null before a folder has been scanned. */
    val scanReport: ScanReport? = null,
    /** Per-path user overrides of the Smart Upload Protection verdict — true = force
     * include even though flagged, false = force exclude even though it looked safe.
     * Absent = trust the scan's own verdict. The user gets the final say (PRD §16). */
    val fileInclusionOverrides: Map<String, Boolean> = emptyMap(),

    val isComparing: Boolean = false,
    val compareProgress: Pair<Int, Int>? = null,
    val compareError: String? = null,
    val fileChanges: List<FileChange> = emptyList(),

    /** Which changes the user has chosen to actually push — defaults to "all selected"
     * as soon as a comparison finishes, but can be narrowed manually per file or per
     * section (Added/Modified/Removed) via the Analysis screen's checkboxes. */
    val selectedPaths: Set<String> = emptySet(),

    val isUploading: Boolean = false,
    val uploadPhase: UploadPhase = UploadPhase.IDLE,
    val uploadProgress: Pair<Int, Int> = 0 to 0,
    val uploadCurrentFile: String = "",
    val uploadError: String? = null,
    val commitSha: String? = null,

    // ===== Repository Browser (VS Code-style tree explorer + manual file/folder create) =====
    /** Last-tapped folder — the target for "New file/folder" and "Paste", not a navigation
     * location; the whole tree stays visible at once (VS Code Explorer style, not drill-down). */
    val browserPath: String = "",
    /** Flattened, indentation-ready rows for the currently expanded parts of the tree. */
    val browserEntries: List<TreeRow> = emptyList(),
    val expandedFolders: Set<String> = emptySet(),
    val isBrowserLoading: Boolean = false,
    val browserError: String? = null,
    val remoteTreeCache: Map<String, String> = emptyMap(),
    val isCreatingEntry: Boolean = false,
    val createEntryError: String? = null,
    val isDeletingEntry: Boolean = false,
    val deleteEntryError: String? = null,

    /** Multi-select in the browser (long-press to start, tap to toggle). Only files are
     * selectable — folders aren't copyable/deletable as a single selection unit yet, use
     * the per-row delete action for a whole folder instead. */
    val selectedBrowserPaths: Set<String> = emptySet(),
    /** Files copied via the selection toolbar, ready to paste into another folder. */
    val clipboard: List<BrowserEntry> = emptyList(),
    val isPasting: Boolean = false,
    val pasteError: String? = null,

    /** Read/edit viewer for a single file, opened by tapping it outside select mode. */
    val viewingFile: BrowserEntry? = null,
    val viewingContent: String? = null,
    val isLoadingContent: Boolean = false,
    val viewerError: String? = null,
    val isEditingFile: Boolean = false,
    val isSavingFile: Boolean = false,
    val saveFileError: String? = null
) {
    val addedCount get() = fileChanges.count { it.type == ChangeType.ADDED }
    val modifiedCount get() = fileChanges.count { it.type == ChangeType.MODIFIED }
    val removedCount get() = fileChanges.count { it.type == ChangeType.REMOVED }

    val selectedChanges get() = fileChanges.filter { selectedPaths.contains(it.filePath) }
    val selectedAddedCount get() = selectedChanges.count { it.type == ChangeType.ADDED }
    val selectedModifiedCount get() = selectedChanges.count { it.type == ChangeType.MODIFIED }
    val selectedRemovedCount get() = selectedChanges.count { it.type == ChangeType.REMOVED }
}

/**
 * Holds everything that needs to survive across the Folder Selection -> Analysis ->
 * Confirmation -> Upload Progress -> Completion flow (PRD1 + PRD2). Scoped to the Activity
 * so rotation/config change doesn't lose an in-flight scan, comparison, or upload
 * (PRD2 §4 "must survive process death / configuration change").
 */
class GitWaySessionViewModel(
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    var state by mutableStateOf(GitWaySessionState())
        private set

    fun selectRepository(repo: GitRepository) {
        state = GitWaySessionState(selectedRepo = repo)
    }

    fun onFolderPicked(context: Context, uri: Uri) {
        viewModelScope.launch {
            state = state.copy(isScanning = true, scanError = null, folderUri = uri, scanReport = null, fileInclusionOverrides = emptyMap())
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            } catch (e: SecurityException) {
                // Permission grant can fail if the URI no longer supports persistence;
                // scanning below will surface the real problem if read access is actually gone.
            }

            try {
                val files = FolderScanner.scan(context, uri)
                val folderName = DocumentFile.fromTreeUri(context, uri)?.name ?: "Selected folder"
                if (files.isEmpty()) {
                    state = state.copy(
                        isScanning = false,
                        folderName = folderName,
                        localFiles = emptyList(),
                        scanReport = null,
                        scanError = "Selected folder is empty."
                    )
                    return@launch
                }

                // Smart Upload Protection (see matching PRD): ignore rules, secret
                // detection, and large-file blocking all run before anything reaches the
                // diff engine — localFiles below is exactly ScanReport.safeFiles.
                val report = ProtectionScanner.scan(context, files)
                state = state.copy(
                    isScanning = false,
                    folderName = folderName,
                    localFiles = report.safeFiles,
                    scanReport = report,
                    scanError = if (report.safeFiles.isEmpty()) {
                        "Every file was ignored or blocked by Smart Upload Protection — nothing safe to upload."
                    } else null
                )
            } catch (e: SecurityException) {
                state = state.copy(
                    isScanning = false,
                    scanError = "Permission was revoked. Please re-select the folder."
                )
            } catch (e: Exception) {
                state = state.copy(isScanning = false, scanError = e.message ?: "Couldn't read that folder.")
            }
        }
    }

    fun clearScanError() {
        state = state.copy(scanError = null)
    }

    /** Lets the user override a single file's Smart Upload Protection verdict — force an
     * ignored/blocked file in, or force a safe-looking file out. [include] null clears the
     * override and goes back to trusting the scan. Recomputes [GitWaySessionState.localFiles]
     * immediately so it stays the single source of truth the diff/upload pipeline reads. */
    fun setFileInclusionOverride(path: String, include: Boolean?) {
        val report = state.scanReport ?: return
        val newOverrides = if (include == null) {
            state.fileInclusionOverrides - path
        } else {
            state.fileInclusionOverrides + (path to include)
        }
        state = state.copy(
            fileInclusionOverrides = newOverrides,
            localFiles = effectiveLocalFiles(report, newOverrides)
        )
    }

    private fun effectiveLocalFiles(report: ScanReport, overrides: Map<String, Boolean>): List<LocalFile> {
        val flagged = (report.ignoredFiles + report.blockedFiles + report.secretsFound).associateBy { it.relativePath }
        val included = mutableListOf<LocalFile>()
        report.safeFiles.forEach { file ->
            if (overrides[file.relativePath] != false) included += file
        }
        flagged.forEach { (path, issue) ->
            if (overrides[path] == true) included += issue.file
        }
        return included
    }

    fun runComparison(context: Context) {
        val repo = state.selectedRepo ?: return
        if (state.localFiles.isEmpty()) return

        viewModelScope.launch {
            state = state.copy(isComparing = true, compareError = null, compareProgress = null, fileChanges = emptyList())

            gitHubRepository.getRepositoryTree(repo)
                .onSuccess { remoteMap ->
                    try {
                        val changes = ComparisonEngine.computeDiff(
                            context = context,
                            localFiles = state.localFiles,
                            remotePaths = remoteMap,
                            contentOverrides = state.scanReport?.contentOverrides.orEmpty(),
                            onProgress = { done, total ->
                                state = state.copy(compareProgress = done to total)
                            }
                        )
                        state = state.copy(
                            isComparing = false,
                            fileChanges = changes,
                            compareProgress = null,
                            // Everything starts selected — the user can then manually
                            // uncheck items or use "Select all" / "Clear" per section.
                            selectedPaths = changes.map { it.filePath }.toSet()
                        )
                    } catch (e: Exception) {
                        state = state.copy(
                            isComparing = false,
                            compareError = e.message ?: "Comparison failed.",
                            compareProgress = null
                        )
                    }
                }
                .onFailure { throwable ->
                    state = state.copy(
                        isComparing = false,
                        compareError = throwable.message ?: "Couldn't fetch the repository's file tree.",
                        compareProgress = null
                    )
                }
        }
    }

    /** Toggle a single file's inclusion — the "manual" selection option. */
    fun toggleChangeSelection(filePath: String) {
        val current = state.selectedPaths
        state = state.copy(
            selectedPaths = if (filePath in current) current - filePath else current + filePath
        )
    }

    /** Select or clear every change of one type at once (the section-level "Select all"). */
    fun setSelectionForType(type: ChangeType, selected: Boolean) {
        val pathsOfType = state.fileChanges.filter { it.type == type }.map { it.filePath }.toSet()
        state = state.copy(
            selectedPaths = if (selected) state.selectedPaths + pathsOfType else state.selectedPaths - pathsOfType
        )
    }

    /** Global "Select all" across every detected change. */
    fun selectAllChanges() {
        state = state.copy(selectedPaths = state.fileChanges.map { it.filePath }.toSet())
    }

    /** Global "Clear selection". */
    fun deselectAllChanges() {
        state = state.copy(selectedPaths = emptySet())
    }

    private var uploadJob: Job? = null

    fun uploadChanges(context: Context) {
        val repo = state.selectedRepo ?: return
        val changesToUpload = state.selectedChanges
        if (changesToUpload.isEmpty() || state.isUploading) return

        // §2 Validate Repository Before Upload: fail fast on no connection rather than
        // letting blob creation start and die partway through.
        if (!NetworkUtils.isOnline(context)) {
            state = state.copy(uploadError = "No internet connection. Please reconnect and try again.")
            return
        }

        val localByPath = state.localFiles.associateBy { it.relativePath }
        val overrides = state.scanReport?.contentOverrides.orEmpty()
        val readBytes: suspend (String) -> ByteArray = { path ->
            overrides[path] ?: withContext(Dispatchers.IO) {
                val uri = localByPath.getValue(path).documentUri
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            }
        }

        uploadJob = viewModelScope.launch {
            state = state.copy(
                isUploading = true,
                uploadError = null,
                uploadPhase = UploadPhase.VALIDATING,
                uploadProgress = 0 to changesToUpload.size,
                uploadCurrentFile = ""
            )

            try {
                gitHubRepository.syncChanges(
                    repo = repo,
                    changes = changesToUpload,
                    readFileBytes = readBytes,
                    onProgress = { phase, completed, total, currentFile ->
                        state = state.copy(
                            uploadPhase = phase,
                            uploadProgress = completed to total,
                            uploadCurrentFile = currentFile
                        )
                    }
                ).onSuccess { sha ->
                    state = state.copy(isUploading = false, uploadPhase = UploadPhase.DONE, commitSha = sha)
                }.onFailure { throwable ->
                    state = state.copy(
                        isUploading = false,
                        uploadError = throwable.message ?: "Upload failed.",
                        uploadPhase = UploadPhase.IDLE
                    )
                }
            } catch (e: CancellationException) {
                state = state.copy(
                    isUploading = false,
                    uploadError = "Upload cancelled. Nothing was written to the repository — it's safe to retry.",
                    uploadPhase = UploadPhase.IDLE
                )
                throw e
            }
        }
    }

    /** §14 Cancel button. Safe at any point before UPDATING_BRANCH finishes — the repo's
     * branch is only ever touched by the final ref update. */
    fun cancelUpload() {
        uploadJob?.cancel()
    }

    /** Called when returning to the Repository List after Completion, or backing out of a repo. */
    fun resetForNewRepository() {
        state = GitWaySessionState()
    }

    // ===== Repository Browser (VS Code-style tree explorer) =====

    /** Loads the repo's full file tree once and shows the root, collapsed. Cheap to call
     * again (pull-to-refresh) — it always re-fetches from GitHub rather than trusting the
     * cache, but keeps whatever folders were already expanded. */
    fun loadBrowserRoot() {
        val repo = state.selectedRepo ?: return
        state = state.copy(isBrowserLoading = true, browserError = null)
        viewModelScope.launch {
            gitHubRepository.getRepositoryTree(repo)
                .onSuccess { tree ->
                    state = state.copy(
                        isBrowserLoading = false,
                        remoteTreeCache = tree,
                        browserEntries = buildVisibleRows(tree.keys, state.expandedFolders)
                    )
                }
                .onFailure { throwable ->
                    state = state.copy(
                        isBrowserLoading = false,
                        browserError = throwable.message ?: "Couldn't load repository files."
                    )
                }
        }
    }

    /** Expands or collapses a folder in place (VS Code Explorer style — the whole tree
     * stays on screen, no drill-down navigation). The tapped folder also becomes the
     * target for the next "New file/folder" or "Paste". */
    fun toggleFolderExpanded(entry: BrowserEntry) {
        if (!entry.isFolder) return
        val expanded = state.expandedFolders
        val newExpanded = if (entry.path in expanded) expanded - entry.path else expanded + entry.path
        state = state.copy(
            expandedFolders = newExpanded,
            browserPath = entry.path,
            browserEntries = buildVisibleRows(state.remoteTreeCache.keys, newExpanded)
        )
    }

    fun clearCreateEntryError() {
        state = state.copy(createEntryError = null)
    }

    /** Creates an empty text file inside [GitWaySessionState.browserPath] (the last-tapped
     * folder, or root) as a single, immediate commit — reuses the same tested
     * [GitHubRepository.syncChanges] pipeline (validation, retries, atomic tree/commit/ref,
     * post-push verification) as the main sync flow. */
    fun createFile(fileName: String, content: String = "") {
        val repo = state.selectedRepo ?: return
        val name = fileName.trim()
        if (name.isBlank() || state.isCreatingEntry) return

        val fullPath = if (state.browserPath.isEmpty()) name else "${state.browserPath}/$name"
        if (state.remoteTreeCache.containsKey(fullPath)) {
            state = state.copy(createEntryError = "\"$name\" already exists here.")
            return
        }

        val bytes = content.toByteArray(Charsets.UTF_8)
        state = state.copy(isCreatingEntry = true, createEntryError = null)
        viewModelScope.launch {
            gitHubRepository.syncChanges(
                repo = repo,
                changes = listOf(FileChange(fileName = name, filePath = fullPath, type = ChangeType.ADDED)),
                readFileBytes = { bytes },
                onProgress = { _, _, _, _ -> }
            ).onSuccess {
                val updatedTree = state.remoteTreeCache + (fullPath to "")
                val expanded = if (state.browserPath.isEmpty()) state.expandedFolders else state.expandedFolders + state.browserPath
                state = state.copy(
                    isCreatingEntry = false,
                    remoteTreeCache = updatedTree,
                    expandedFolders = expanded,
                    browserEntries = buildVisibleRows(updatedTree.keys, expanded)
                )
            }.onFailure { throwable ->
                state = state.copy(isCreatingEntry = false, createEntryError = throwable.message ?: "Couldn't create file.")
            }
        }
    }

    /** Git has no real folder objects — an empty folder is created by committing a hidden
     * ".gitkeep" placeholder inside it, the standard convention. The placeholder itself is
     * filtered out of [buildVisibleRows] so the folder just looks empty until a real file
     * is added. */
    fun createFolder(folderName: String) {
        val repo = state.selectedRepo ?: return
        val name = folderName.trim()
        if (name.isBlank() || state.isCreatingEntry) return

        val folderPath = if (state.browserPath.isEmpty()) name else "${state.browserPath}/$name"
        val alreadyExists = state.remoteTreeCache.keys.any { it == folderPath || it.startsWith("$folderPath/") }
        if (alreadyExists) {
            state = state.copy(createEntryError = "\"$name\" already exists here.")
            return
        }

        val placeholderPath = "$folderPath/.gitkeep"
        state = state.copy(isCreatingEntry = true, createEntryError = null)
        viewModelScope.launch {
            gitHubRepository.syncChanges(
                repo = repo,
                changes = listOf(FileChange(fileName = ".gitkeep", filePath = placeholderPath, type = ChangeType.ADDED)),
                readFileBytes = { ByteArray(0) },
                onProgress = { _, _, _, _ -> }
            ).onSuccess {
                val updatedTree = state.remoteTreeCache + (placeholderPath to "")
                val expanded = if (state.browserPath.isEmpty()) state.expandedFolders else state.expandedFolders + state.browserPath
                state = state.copy(
                    isCreatingEntry = false,
                    remoteTreeCache = updatedTree,
                    expandedFolders = expanded,
                    browserEntries = buildVisibleRows(updatedTree.keys, expanded)
                )
            }.onFailure { throwable ->
                state = state.copy(isCreatingEntry = false, createEntryError = throwable.message ?: "Couldn't create folder.")
            }
        }
    }

    fun clearDeleteEntryError() {
        state = state.copy(deleteEntryError = null)
    }

    /** Deletes a single file, or an entire folder (every path under it), as one commit.
     * Reuses [GitHubRepository.syncChanges] with REMOVED changes — nothing is read from
     * disk/GitHub for a delete, the tree/commit pipeline just drops those paths. */
    fun deleteEntry(entry: BrowserEntry) {
        val paths = if (entry.isFolder) {
            state.remoteTreeCache.keys.filter { it == entry.path || it.startsWith("${entry.path}/") }
        } else {
            listOf(entry.path)
        }
        deletePaths(paths)
    }

    /** Deletes every currently selected file as one commit, then exits selection mode. */
    fun deleteSelected() {
        if (state.selectedBrowserPaths.isEmpty()) return
        deletePaths(state.selectedBrowserPaths.toList())
    }

    private fun deletePaths(paths: List<String>) {
        val repo = state.selectedRepo ?: return
        if (paths.isEmpty() || state.isDeletingEntry) return

        state = state.copy(isDeletingEntry = true, deleteEntryError = null)
        viewModelScope.launch {
            val changes = paths.map { path ->
                FileChange(fileName = path.substringAfterLast('/'), filePath = path, type = ChangeType.REMOVED)
            }
            gitHubRepository.syncChanges(
                repo = repo,
                changes = changes,
                readFileBytes = { ByteArray(0) }, // never invoked — REMOVED changes carry no content
                onProgress = { _, _, _, _ -> }
            ).onSuccess {
                val pathSet = paths.toSet()
                val updatedTree = state.remoteTreeCache - pathSet
                val updatedExpanded = state.expandedFolders - pathSet
                state = state.copy(
                    isDeletingEntry = false,
                    remoteTreeCache = updatedTree,
                    expandedFolders = updatedExpanded,
                    selectedBrowserPaths = state.selectedBrowserPaths - pathSet,
                    browserEntries = buildVisibleRows(updatedTree.keys, updatedExpanded)
                )
            }.onFailure { throwable ->
                state = state.copy(isDeletingEntry = false, deleteEntryError = throwable.message ?: "Couldn't delete.")
            }
        }
    }

    // ===== Select / Copy / Paste =====

    fun toggleBrowserSelection(entry: BrowserEntry) {
        if (entry.isFolder) return // only files are selectable for now
        val current = state.selectedBrowserPaths
        state = state.copy(
            selectedBrowserPaths = if (entry.path in current) current - entry.path else current + entry.path
        )
    }

    fun clearBrowserSelection() {
        state = state.copy(selectedBrowserPaths = emptySet())
    }

    /** Copies every currently selected file into the in-memory clipboard and exits
     * selection mode. Only the path/sha is kept — bytes are fetched lazily on paste. */
    fun copySelectionToClipboard() {
        val selectedEntries = state.browserEntries.map { it.entry }
            .filter { it.path in state.selectedBrowserPaths && !it.isFolder }
        if (selectedEntries.isEmpty()) return
        state = state.copy(clipboard = selectedEntries, selectedBrowserPaths = emptySet())
    }

    fun clearClipboard() {
        state = state.copy(clipboard = emptyList())
    }

    /** Pastes every clipboard entry into [GitWaySessionState.browserPath] (the last-tapped
     * folder, or root) as a single commit — reads each file's bytes from GitHub by its
     * cached blob sha, then re-uploads them at the new path(s). Auto-renames on a name
     * collision ("name (copy).ext", then " (copy 2)"…). */
    fun pasteClipboardHere() {
        val repo = state.selectedRepo ?: return
        val toPaste = state.clipboard
        if (toPaste.isEmpty() || state.isPasting) return

        state = state.copy(isPasting = true, pasteError = null)
        viewModelScope.launch {
            try {
                val siblingNames = state.remoteTreeCache.keys
                    .filter { it.substringBeforeLast("/", "") == state.browserPath }
                    .map { it.substringAfterLast("/") }
                    .toMutableSet()

                val destinations = toPaste.map { entry ->
                    val uniqueName = uniqueFileName(entry.name, siblingNames)
                    siblingNames += uniqueName
                    val destPath = if (state.browserPath.isEmpty()) uniqueName else "${state.browserPath}/$uniqueName"
                    entry to destPath
                }

                val byteCache = mutableMapOf<String, ByteArray>()
                for ((entry, destPath) in destinations) {
                    val sha = state.remoteTreeCache[entry.path]
                    if (sha.isNullOrBlank()) {
                        state = state.copy(isPasting = false, pasteError = "Couldn't read \"${entry.name}\" — try reloading.")
                        return@launch
                    }
                    val bytes = gitHubRepository.getFileContent(repo, sha).getOrElse {
                        state = state.copy(isPasting = false, pasteError = it.message ?: "Couldn't read \"${entry.name}\".")
                        return@launch
                    }
                    byteCache[destPath] = bytes
                }

                val changes = destinations.map { (_, destPath) ->
                    FileChange(fileName = destPath.substringAfterLast('/'), filePath = destPath, type = ChangeType.ADDED)
                }

                gitHubRepository.syncChanges(
                    repo = repo,
                    changes = changes,
                    readFileBytes = { path -> byteCache.getValue(path) },
                    onProgress = { _, _, _, _ -> }
                ).onSuccess {
                    val updatedTree = state.remoteTreeCache + byteCache.keys.associateWith { "" }
                    val expanded = if (state.browserPath.isEmpty()) state.expandedFolders else state.expandedFolders + state.browserPath
                    state = state.copy(
                        isPasting = false,
                        clipboard = emptyList(),
                        remoteTreeCache = updatedTree,
                        expandedFolders = expanded,
                        browserEntries = buildVisibleRows(updatedTree.keys, expanded)
                    )
                }.onFailure { throwable ->
                    state = state.copy(isPasting = false, pasteError = throwable.message ?: "Paste failed.")
                }
            } catch (e: Exception) {
                state = state.copy(isPasting = false, pasteError = e.message ?: "Paste failed.")
            }
        }
    }

    private fun uniqueFileName(original: String, taken: Set<String>): String {
        if (original !in taken) return original
        val dot = original.lastIndexOf('.')
        val base = if (dot > 0) original.substring(0, dot) else original
        val ext = if (dot > 0) original.substring(dot) else ""
        var n = 1
        var candidate: String
        do {
            candidate = if (n == 1) "$base (copy)$ext" else "$base (copy $n)$ext"
            n++
        } while (candidate in taken)
        return candidate
    }

    // ===== Read / Edit =====

    fun openFile(entry: BrowserEntry) {
        val repo = state.selectedRepo ?: return
        if (entry.isFolder) return
        val sha = state.remoteTreeCache[entry.path]
        state = state.copy(
            viewingFile = entry,
            viewingContent = null,
            isLoadingContent = true,
            viewerError = null,
            isEditingFile = false,
            saveFileError = null
        )
        if (sha.isNullOrBlank()) {
            state = state.copy(isLoadingContent = false, viewerError = "Couldn't locate this file's content.")
            return
        }
        viewModelScope.launch {
            gitHubRepository.getFileContent(repo, sha)
                .onSuccess { bytes ->
                    val text = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull()
                    state = state.copy(
                        isLoadingContent = false,
                        viewingContent = text,
                        viewerError = if (text == null) "This looks like a binary file — preview isn't available." else null
                    )
                }
                .onFailure { throwable ->
                    state = state.copy(isLoadingContent = false, viewerError = throwable.message ?: "Couldn't load this file.")
                }
        }
    }

    fun closeFileViewer() {
        state = state.copy(
            viewingFile = null,
            viewingContent = null,
            isLoadingContent = false,
            viewerError = null,
            isEditingFile = false,
            isSavingFile = false,
            saveFileError = null
        )
    }

    fun startEditingFile() {
        if (state.viewingContent == null) return
        state = state.copy(isEditingFile = true, saveFileError = null)
    }

    fun cancelEditingFile() {
        state = state.copy(isEditingFile = false, saveFileError = null)
    }

    /** Commits the edited content as a single-file MODIFIED change, via the same tested
     * sync pipeline used everywhere else — then updates the viewer to show the saved text. */
    fun saveFileEdits(newContent: String) {
        val repo = state.selectedRepo ?: return
        val entry = state.viewingFile ?: return
        if (state.isSavingFile) return

        val bytes = newContent.toByteArray(Charsets.UTF_8)
        state = state.copy(isSavingFile = true, saveFileError = null)
        viewModelScope.launch {
            gitHubRepository.syncChanges(
                repo = repo,
                changes = listOf(FileChange(fileName = entry.name, filePath = entry.path, type = ChangeType.MODIFIED)),
                readFileBytes = { bytes },
                onProgress = { _, _, _, _ -> }
            ).onSuccess {
                state = state.copy(
                    isSavingFile = false,
                    isEditingFile = false,
                    viewingContent = newContent
                )
            }.onFailure { throwable ->
                state = state.copy(isSavingFile = false, saveFileError = throwable.message ?: "Couldn't save changes.")
            }
        }
    }

    /** Computes the immediate children of [currentPath] from a flat set of full repo paths. */
    private fun childrenOf(paths: Set<String>, currentPath: String): List<BrowserEntry> {
        val prefix = if (currentPath.isEmpty()) "" else "$currentPath/"
        val seen = linkedSetOf<String>()
        val entries = mutableListOf<BrowserEntry>()
        for (path in paths) {
            if (!path.startsWith(prefix)) continue
            val remainder = path.removePrefix(prefix)
            if (remainder.isEmpty()) continue
            val firstSegment = remainder.substringBefore("/")
            val isFolder = remainder.contains("/")
            if (!isFolder && firstSegment == ".gitkeep") continue // hide folder placeholder files
            val fullPath = prefix + firstSegment
            if (seen.add(fullPath)) {
                entries += BrowserEntry(name = firstSegment, path = fullPath, isFolder = isFolder)
            }
        }
        return entries.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase() }))
    }

    /** Flattens the tree into indentation-ready rows (VS Code Explorer style): every
     * folder always appears, its children are spliced in right after it only while its
     * path is in [expanded]. Rebuilt on every tree/expansion change — cheap enough for
     * the file counts a mobile repo browser deals with. */
    private fun buildVisibleRows(paths: Set<String>, expanded: Set<String>): List<TreeRow> {
        val rows = mutableListOf<TreeRow>()
        fun walk(currentPath: String, depth: Int) {
            childrenOf(paths, currentPath).forEach { entry ->
                val isExpanded = entry.isFolder && entry.path in expanded
                rows += TreeRow(entry = entry, depth = depth, isExpanded = isExpanded)
                if (isExpanded) walk(entry.path, depth + 1)
            }
        }
        walk("", 0)
        return rows
    }
}
