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
import com.io.git.way.domain.ComparisonEngine
import com.io.git.way.domain.model.ChangeType
import com.io.git.way.domain.model.FileChange
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.domain.model.LocalFile
import com.io.git.way.domain.model.UploadPhase
import com.io.git.way.domain.repository.GitHubRepository
import kotlinx.coroutines.Dispatchers
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
                        state = state.copy(isComparing = false, fileChanges = changes, compareProgress = null)
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

    fun uploadChanges(context: Context) {
        val repo = state.selectedRepo ?: return
        if (state.fileChanges.isEmpty() || state.isUploading) return

        val localByPath = state.localFiles.associateBy { it.relativePath }
        val readBytes: suspend (String) -> ByteArray = { path ->
            withContext(Dispatchers.IO) {
                val uri = localByPath.getValue(path).documentUri
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            }
        }

        viewModelScope.launch {
            state = state.copy(
                isUploading = true,
                uploadError = null,
                uploadPhase = UploadPhase.PREPARING,
                uploadProgress = 0 to state.fileChanges.size,
                uploadCurrentFile = ""
            )

            gitHubRepository.syncChanges(
                repo = repo,
                changes = state.fileChanges,
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
        }
    }

    /** Called when returning to the Repository List after Completion, or backing out of a repo. */
    fun resetForNewRepository() {
        state = GitWaySessionState()
    }
}
