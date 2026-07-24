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
import com.io.git.way.domain.ComparisonEngine
import com.io.git.way.domain.model.ChangeType
import com.io.git.way.domain.model.FileChange
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.domain.model.LocalFile
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
    val commitSha: String? = null
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
            state = state.copy(isScanning = true, scanError = null, folderUri = uri)
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
                        scanError = "Selected folder is empty."
                    )
                } else {
                    state = state.copy(
                        isScanning = false,
                        folderName = folderName,
                        localFiles = files,
                        scanError = null
                    )
                }
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
        val readBytes: suspend (String) -> ByteArray = { path ->
            withContext(Dispatchers.IO) {
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
}
