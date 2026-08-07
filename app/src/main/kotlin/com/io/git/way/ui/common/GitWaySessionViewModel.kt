package com.io.git.way.ui.common

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.io.git.way.data.local.AndroidProjectInspector
import com.io.git.way.data.local.AppIdentityDetector
import com.io.git.way.data.local.FolderScanner
import com.io.git.way.data.local.IgnoreListManager
import com.io.git.way.data.local.NetworkUtils
import com.io.git.way.data.local.ProtectionScanner
import com.io.git.way.domain.ComparisonEngine
import com.io.git.way.domain.CommitMessageBuilder
import com.io.git.way.domain.WorkflowTemplate
import com.io.git.way.domain.WorkflowTemplates
import com.io.git.way.domain.model.AndroidProjectInfo
import com.io.git.way.domain.model.ApiRateLimit
import com.io.git.way.domain.model.AppIdentity
import com.io.git.way.domain.model.BrowserEntry
import com.io.git.way.domain.model.BrowserSortMode
import com.io.git.way.domain.model.BrowserTypeFilter
import com.io.git.way.domain.model.ChangeType
import com.io.git.way.domain.model.CommitSummary
import com.io.git.way.domain.model.FileChange
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.domain.model.GitUser
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
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

data class GitWaySessionState(
    val selectedRepo: GitRepository? = null,

    /** Account-wide data for the Overview dashboard — deliberately independent of
     * [selectedRepo] since Overview is reachable before any repo has ever been picked. */
    val currentUser: GitUser? = null,
    val isLoadingUser: Boolean = false,
    val userError: String? = null,
    val overviewRepositories: List<GitRepository> = emptyList(),
    val isLoadingOverviewRepositories: Boolean = false,
    val apiRateLimit: ApiRateLimit? = null,

    val folderUri: Uri? = null,
    val folderName: String = "",
    val localFiles: List<LocalFile> = emptyList(),
    val isScanning: Boolean = false,
    val scanError: String? = null,

    /** Repository / Project Match Protection: package/applicationId detected from the
     * local folder and from the selected repo's remote tree, checked right after the
     * folder is scanned — before any diff or upload. Null on either side just means
     * "couldn't detect a package name there" (non-Android project, unusual layout),
     * which is never itself treated as a mismatch. */
    val isCheckingAppIdentity: Boolean = false,
    val localAppIdentity: AppIdentity? = null,
    val remoteAppIdentity: AppIdentity? = null,
    /** Set only when the remote half of the check genuinely couldn't run (network error,
     * timeout) — as opposed to running cleanly and finding no identity to compare. This
     * keeps Continue/Upload blocked on a connectivity hiccup instead of silently treating
     * "couldn't check" the same as "checked, no mismatch". */
    val identityCheckError: String? = null,

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
    /** Repository scaffolding (README, LICENSE, .github/, etc.) that exists on GitHub
     * but was correctly excluded from Removed detection — see [RepositoryScaffoldFiles]. */
    val ignoredScaffoldFiles: List<String> = emptyList(),

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

    /** User-editable commit message on the Confirmation screen. Blank means "use the
     * auto-generated default" ([com.io.git.way.domain.CommitMessageBuilder.summary]) —
     * [lastCommitMessage] is the actual, resolved text used for the most recent
     * successful push, shown on the Completion screen so the user sees exactly what
     * they (or the default) wrote, not just a generic success message. */
    val commitMessageDraft: String = "",
    val lastCommitMessage: String? = null,

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
    /** File sizes for the currently loaded repo tree, keyed by path — populated
     * alongside [remoteTreeCache] for the Explorer's metadata subtitle (PRD §7). Not
     * every entry is guaranteed a size (GitHub can omit it), so this is best-effort. */
    val remoteFileSizes: Map<String, Long> = emptyMap(),
    /** Bytes for paths written THIS session (create/rename/duplicate/edit) whose real blob
     * sha isn't known yet — [remoteTreeCache] holds a "" placeholder for them until the next
     * full reload. Consulted before falling back to a sha-based GitHub fetch so those files
     * can immediately be opened, renamed, or duplicated again without a "try reloading" error. */
    val pendingFileBytes: Map<String, ByteArray> = emptyMap(),
    val isCreatingEntry: Boolean = false,
    val createEntryError: String? = null,
    val isDeletingEntry: Boolean = false,
    val deleteEntryError: String? = null,

    /** PRD "Repository Explorer" §19 Sort/View. */
    val browserSortMode: BrowserSortMode = BrowserSortMode.NAME_ASC,
    val browserTypeFilter: BrowserTypeFilter = BrowserTypeFilter.ALL,

    val isRenamingEntry: Boolean = false,
    val renameError: String? = null,
    val isDuplicatingEntry: Boolean = false,
    val duplicateError: String? = null,

    /** §16 Android Project Intelligence — best-effort summary card at the root of the
     * Explorer, detected from the repo's own build files. Null until detected, and stays
     * null (no card shown) for a non-Android repository. */
    val androidProjectInfo: AndroidProjectInfo? = null,

    /** Manual-opt-in "add a CI workflow" suggestion (PRD: "CI Workflow Suggestions") — the
     * banner is shown whenever the loaded repo tree has nothing under `.github/workflows/`
     * and hasn't been dismissed yet this session; nothing is ever added automatically. */
    val workflowSuggestionDismissed: Boolean = false,
    val isAddingWorkflows: Boolean = false,
    val addWorkflowsError: String? = null,

    /** Repository Management: branches. `selectedBranch == null` means "the repo's own
     * default branch" — every push (main upload flow AND every Repository Browser write)
     * targets whichever this resolves to at the time. */
    val availableBranches: List<String> = emptyList(),
    val selectedBranch: String? = null,
    val isLoadingBranches: Boolean = false,
    val branchError: String? = null,
    val isCreatingBranch: Boolean = false,
    val createBranchError: String? = null,

    /** Repository Management: read-only commit history for [selectedBranch] (or the
     * default branch). Loaded on demand — not fetched just for opening a repo. */
    val commitHistory: List<CommitSummary> = emptyList(),
    val isLoadingCommitHistory: Boolean = false,
    val commitHistoryError: String? = null,

    /** Repository Management: rename / re-describe / re-visibility / delete the repo
     * itself (as opposed to its files, which the rest of this state already covers). */
    val isUpdatingRepoSettings: Boolean = false,
    val repoSettingsError: String? = null,
    val isDeletingRepo: Boolean = false,
    val deleteRepoError: String? = null,

    /** Multi-select in the browser (long-press to start, tap to toggle). Only files are
     * selectable — folders aren't copyable/deletable as a single selection unit yet, use
     * the per-row delete action for a whole folder instead. */
    val selectedBrowserPaths: Set<String> = emptySet(),
    /** Files copied via the selection toolbar, ready to paste into another folder. */
    val clipboard: List<BrowserEntry> = emptyList(),
    val isPasting: Boolean = false,
    val pasteError: String? = null,

    /** Read/edit viewer for a single file, opened by tapping it outside select mode.
     * Exactly one of [viewingContent] / [viewingImageBytes] is set once loading finishes
     * — text files decode to [viewingContent], recognised image formats stay as raw
     * bytes in [viewingImageBytes] for actual on-screen preview instead of a "binary
     * file" message. Anything else (that isn't valid UTF-8 and isn't an image) leaves
     * both null with [viewerError] explaining there's nothing to preview. */
    val viewingFile: BrowserEntry? = null,
    val viewingContent: String? = null,
    val viewingImageBytes: ByteArray? = null,
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

    /** True only when BOTH sides yielded a detected package name AND they disagree —
     * this is the hard-block condition. A repo with no Android project yet (first
     * push) or a folder whose package couldn't be detected never trips this. */
    /** PRD "Validation Rules": Package Name, Application ID (both captured in
     * [AppIdentity.packageName] — Android's applicationId/namespace and a repo's
     * `package` attribute are the same signal at different project ages), and App Name
     * each independently block on a disagreement. Only compares a field when BOTH sides
     * actually yielded a value for it — a repo with no Android project yet (first push)
     * or a folder/field that couldn't be detected never trips this. */
    val appIdentityMismatch: Boolean
        get() {
            val localPackage = localAppIdentity?.packageName
            val remotePackage = remoteAppIdentity?.packageName
            val packageMismatch = localPackage != null && remotePackage != null && localPackage != remotePackage

            val localName = localAppIdentity?.appName
            val remoteName = remoteAppIdentity?.appName
            val appNameMismatch = localName != null && remoteName != null && localName != remoteName

            return packageMismatch || appNameMismatch
        }

    /** Shown only once the tree has actually loaded (an empty map before that would
     * false-positive as "no workflows") and only until the user dismisses it or adds one. */
    val showWorkflowSuggestion: Boolean
        get() = !workflowSuggestionDismissed &&
            !isBrowserLoading &&
            remoteTreeCache.isNotEmpty() &&
            WorkflowTemplates.hasNoWorkflows(remoteTreeCache.keys)
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
        // Full reset except the account-wide Overview data (user/repos/rate-limit) —
        // that's independent of which repo is selected and shouldn't need re-fetching
        // every time the user switches repos.
        state = GitWaySessionState(
            selectedRepo = repo,
            currentUser = state.currentUser,
            overviewRepositories = state.overviewRepositories,
            apiRateLimit = state.apiRateLimit
        )
    }

    /** Loads (or refreshes) everything the Overview dashboard shows: the account
     * profile, every repo the token can see, and the current API rate-limit usage.
     * Safe to call every time Overview appears — each piece only shows its own loading
     * state, so a slow rate-limit call never blocks the profile card from appearing. */
    fun loadOverviewData() {
        if (state.currentUser == null && !state.isLoadingUser) {
            state = state.copy(isLoadingUser = true, userError = null)
            viewModelScope.launch {
                gitHubRepository.getCurrentUser()
                    .onSuccess { user -> state = state.copy(isLoadingUser = false, currentUser = user) }
                    .onFailure { throwable -> state = state.copy(isLoadingUser = false, userError = throwable.message ?: "Couldn't load your profile.") }
            }
        }
        if (state.overviewRepositories.isEmpty() && !state.isLoadingOverviewRepositories) {
            state = state.copy(isLoadingOverviewRepositories = true)
            viewModelScope.launch {
                gitHubRepository.listRepositories()
                    .onSuccess { repos -> state = state.copy(isLoadingOverviewRepositories = false, overviewRepositories = repos) }
                    .onFailure { state = state.copy(isLoadingOverviewRepositories = false) }
            }
        }
        if (state.apiRateLimit == null) {
            viewModelScope.launch {
                gitHubRepository.getApiRateLimit()
                    .onSuccess { limit -> state = state.copy(apiRateLimit = limit) }
                    .onFailure { /* purely informational — silently skip if unavailable */ }
            }
        }
    }

    /** Force-refreshes Overview data even if it's already loaded — used by pull-to-refresh. */
    fun refreshOverviewData() {
        state = state.copy(currentUser = null, overviewRepositories = emptyList(), apiRateLimit = null)
        loadOverviewData()
    }

    /** Clears the saved token (Profile "Danger Zone > Disconnect") — the caller is
     * responsible for navigating back to the Token screen afterwards, same pattern as
     * [com.io.git.way.ui.screens.repos.RepositoryListViewModel.disconnect]. */
    fun disconnect() {
        gitHubRepository.clearToken()
        state = GitWaySessionState()
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

                // Repository / Project Match Protection: runs right away, before the
                // user can tap Continue, so a wrong-folder-for-this-repo mistake is
                // caught here instead of surfacing later as a confusing diff or a
                // broken upload.
                if (report.safeFiles.isNotEmpty()) {
                    checkAppIdentity(context, repo = state.selectedRepo, localFiles = report.safeFiles)
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

    /** Repository / Project Match Protection (advance warning, before Continue is even
     * tappable): detects the local folder's and the selected repo's app
     * package/applicationId/project name and flags a mismatch immediately, rather than
     * waiting until the diff or the upload itself.
     *
     * Best-effort ONLY in the sense that a repo/folder with no recognisable identifier on
     * either side never blocks — there's genuinely nothing to compare. A network error or
     * timeout while trying to fetch the repo's tree is a completely different case and is
     * NOT treated as "no mismatch": it sets [GitWaySessionState.identityCheckError] and
     * keeps Continue/Upload blocked until the check actually succeeds (or the folder/repo
     * is changed) — silently waving an upload through just because a connectivity hiccup
     * happened during the safety check would defeat the entire point of this feature.
     * Also caches the fetched remote tree so [runComparison] doesn't have to fetch it a
     * second time right after. */
    private fun checkAppIdentity(context: Context, repo: GitRepository?, localFiles: List<LocalFile>) {
        if (repo == null) return
        identityCheckJob?.cancel()
        identityCheckJob = viewModelScope.launch {
            state = state.copy(
                isCheckingAppIdentity = true,
                localAppIdentity = null,
                remoteAppIdentity = null,
                identityCheckError = null
            )

            val local = runCatching { AppIdentityDetector.detectLocal(context, localFiles) }.getOrNull()

            val treeResult = withTimeoutOrNull(25_000L) { gitHubRepository.getRepositoryTree(repo) }
            if (treeResult == null) {
                // Timed out — could not verify. Block, with a clear retry path, rather
                // than proceeding as if there were no mismatch.
                state = state.copy(
                    isCheckingAppIdentity = false,
                    localAppIdentity = local,
                    identityCheckError = "Couldn't verify this folder matches \"${repo.name}\" (timed out). " +
                        "Check your connection and retry before continuing."
                )
                return@launch
            }

            treeResult
                .onSuccess { remoteTree ->
                    val remote = if (remoteTree.isEmpty()) {
                        // Empty/new default branch — nothing pushed yet, so there's
                        // nothing to disagree with. Not a mismatch, and the single most
                        // common case: a repository created just for this upload.
                        null
                    } else {
                        // Detection itself failing (no recognisable file on the remote
                        // side) is a normal, expected outcome — not a connectivity error —
                        // so it correctly stays fail-open (remote = null, no block).
                        runCatching {
                            AppIdentityDetector.detectRemote(remoteTree) { sha ->
                                gitHubRepository.getFileContent(repo, sha).getOrNull()
                            }
                        }.getOrNull()
                    }
                    state = state.copy(
                        isCheckingAppIdentity = false,
                        localAppIdentity = local,
                        remoteAppIdentity = remote,
                        remoteTreeCache = remoteTree,
                        identityCheckError = null
                    )
                }
                .onFailure { throwable ->
                    // A real failure (network/auth/API error) — block with a retry
                    // option instead of quietly clearing the mismatch check.
                    state = state.copy(
                        isCheckingAppIdentity = false,
                        localAppIdentity = local,
                        identityCheckError = "Couldn't verify this folder matches \"${repo.name}\": " +
                            (throwable.message ?: "unknown error") + ". Retry before continuing."
                    )
                }
        }
    }

    /** Retries [checkAppIdentity] with the current repo/folder — shown next to
     * [GitWaySessionState.identityCheckError] on the Folder Selection screen. */
    fun retryIdentityCheck(context: Context) {
        checkAppIdentity(context, repo = state.selectedRepo, localFiles = state.localFiles)
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
        // Safety net — the Folder screen already keeps Continue disabled on a
        // mismatch, but never let a comparison run against a wrong repo/folder pair
        // even if this is reached some other way.
        if (state.appIdentityMismatch) return

        viewModelScope.launch {
            state = state.copy(isComparing = true, compareError = null, compareProgress = null, fileChanges = emptyList())

            // Deliberately NOT reusing state.remoteTreeCache here, even though
            // checkAppIdentity() may have just fetched one: reusing a tree from an
            // earlier repo/folder pairing is exactly how a file that's genuinely still
            // on GitHub can look locally like it was "already removed" — the diff must
            // always be computed fresh against GitHub's actual current state.
            val treeResult = gitHubRepository.getRepositoryTree(repo)

            treeResult
                .onSuccess { remoteMap ->
                    try {
                        val diff = ComparisonEngine.computeDiff(
                            context = context,
                            localFiles = state.localFiles,
                            remotePaths = remoteMap,
                            contentOverrides = state.scanReport?.contentOverrides.orEmpty(),
                            customIgnoredPaths = IgnoreListManager(context).getAll(),
                            onProgress = { done, total ->
                                state = state.copy(compareProgress = done to total)
                            }
                        )
                        state = state.copy(
                            isComparing = false,
                            fileChanges = diff.changes,
                            ignoredScaffoldFiles = diff.ignoredScaffoldFiles,
                            remoteTreeCache = remoteMap,
                            compareProgress = null,
                            // Everything starts selected — the user can then manually
                            // uncheck items or use "Select all" / "Clear" per section.
                            selectedPaths = diff.changes.map { it.filePath }.toSet()
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

    /** "Don't track this file" — persists [path] so it's never flagged Removed again in
     * any future comparison (see [IgnoreListManager]), and immediately drops it out of
     * the current diff without needing to re-run the whole comparison against GitHub. */
    fun ignoreFileForever(context: Context, path: String) {
        IgnoreListManager(context).add(path)
        state = state.copy(
            fileChanges = state.fileChanges.filterNot { it.filePath == path },
            selectedPaths = state.selectedPaths - path,
            ignoredScaffoldFiles = (state.ignoredScaffoldFiles + path).distinct().sorted()
        )
    }

    /** Commit message the user typed on the Confirmation screen — blank is fine, the
     * default is substituted at upload time (see [uploadChanges]). */
    fun setCommitMessageDraft(message: String) {
        state = state.copy(commitMessageDraft = message)
    }

    private var uploadJob: Job? = null
    private var identityCheckJob: Job? = null

    fun uploadChanges(context: Context) {
        val repo = state.selectedRepo ?: return
        val changesToUpload = state.selectedChanges
        if (changesToUpload.isEmpty() || state.isUploading) return

        // Repository / Project Match Protection — final safety net. The Folder and
        // Confirmation screens already keep the user from reaching this point on a
        // mismatch or an unresolved check error, but nothing ever pushes to GitHub while
        // either is flagged — a connectivity hiccup during the check must never silently
        // downgrade to "assume it's fine".
        if (state.identityCheckError != null) {
            state = state.copy(uploadError = "Blocked: ${state.identityCheckError}")
            return
        }
        if (state.appIdentityMismatch) {
            val local = state.localAppIdentity
            val remote = state.remoteAppIdentity
            val reasons = buildList {
                if (local?.packageName != null && remote?.packageName != null && local.packageName != remote.packageName) {
                    add("Package Name (\"${local.packageName}\" vs \"${remote.packageName}\")")
                }
                if (local?.appName != null && remote?.appName != null && local.appName != remote.appName) {
                    add("App Name (\"${local.appName}\" vs \"${remote.appName}\")")
                }
            }
            state = state.copy(
                uploadError = "Blocked: selected project does not match \"${state.selectedRepo?.name}\". " +
                    "Mismatch: ${reasons.joinToString("; ")}. Select the correct project folder before continuing."
            )
            return
        }

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

        // Always "Git Way Sync: <body>" — <body> is the user's own custom text if they
        // typed one, otherwise the "N added, M modified, K removed" summary already
        // previewed on the Confirmation screen. Resolved once, here, so what's sent to
        // GitHub and what ends up in [lastCommitMessage] for the Completion screen are
        // guaranteed to be the exact same string.
        val resolvedMessage = CommitMessageBuilder.resolve(state.commitMessageDraft, changesToUpload)

        uploadJob = viewModelScope.launch {
            state = state.copy(
                isUploading = true,
                uploadError = null,
                uploadPhase = UploadPhase.VALIDATING,
                uploadProgress = 0 to changesToUpload.size,
                uploadCurrentFile = "",
                lastCommitMessage = resolvedMessage
            )

            try {
                gitHubRepository.syncChanges(
                    repo = repo,
                    changes = changesToUpload,
                    commitMessage = resolvedMessage,
                    targetBranch = state.selectedBranch,
                    readFileBytes = readBytes,
                    onProgress = { phase, completed, total, currentFile ->
                        state = state.copy(
                            uploadPhase = phase,
                            uploadProgress = completed to total,
                            uploadCurrentFile = currentFile
                        )
                    }
                ).onSuccess { sha ->
                    state = state.copy(
                        isUploading = false,
                        uploadPhase = UploadPhase.DONE,
                        commitSha = sha,
                        commitMessageDraft = ""
                    )
                }.onFailure { throwable ->
                    // Deliberately NOT resetting uploadPhase to IDLE here: the phase it
                    // was in when this failed (e.g. "Updating branch") is exactly what
                    // tells the user how far it got before dying — resetting it made the
                    // failure screen show "Starting..." next to "16/16 files", which
                    // reads as if nothing happened when really everything up to that
                    // stage had already succeeded.
                    state = state.copy(
                        isUploading = false,
                        uploadError = throwable.message ?: "Upload failed."
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
            gitHubRepository.getRepositoryTreeDetailed(repo, branch = state.selectedBranch)
                .onSuccess { detailed ->
                    val tree = detailed.mapValues { it.value.sha }
                    val sizes = detailed.mapNotNull { (path, entry) -> entry.size?.let { path to it } }.toMap()
                    state = state.copy(
                        isBrowserLoading = false,
                        remoteTreeCache = tree,
                        remoteFileSizes = sizes,
                        androidProjectInfo = null,
                        browserEntries = buildVisibleRows(tree.keys, state.expandedFolders)
                    )
                    detectAndroidProjectInfo(repo, tree)
                }
                .onFailure { throwable ->
                    state = state.copy(
                        isBrowserLoading = false,
                        browserError = throwable.message ?: "Couldn't load repository files."
                    )
                }
        }
    }

    /** §16 Android Project Intelligence: reuses [AppIdentityDetector] for the package,
     * then separately inspects the same gradle file for SDK versions. Runs after
     * [loadBrowserRoot] and updates the card in place — never blocks the tree itself
     * from showing, and silently leaves [GitWaySessionState.androidProjectInfo] null if
     * nothing recognisable is found (non-Android repos, unusual build setups). */
    private fun detectAndroidProjectInfo(repo: GitRepository, tree: Map<String, String>) {
        if (tree.isEmpty()) return
        viewModelScope.launch {
            val identity = runCatching {
                AppIdentityDetector.detectRemote(tree) { sha -> gitHubRepository.getFileContent(repo, sha).getOrNull() }
            }.getOrNull() ?: return@launch

            val gradlePath = tree.keys.filter { it.endsWith("build.gradle") || it.endsWith("build.gradle.kts") }
                .minByOrNull { it.count { c -> c == '/' } }
            val sdkVersions = gradlePath?.let { path ->
                val sha = tree[path] ?: return@let null
                val bytes = gitHubRepository.getFileContent(repo, sha).getOrNull() ?: return@let null
                val text = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull() ?: return@let null
                AndroidProjectInspector.inspect(text)
            }

            state = state.copy(
                androidProjectInfo = AndroidProjectInfo(
                    packageName = identity.packageName,
                    minSdk = sdkVersions?.minSdk,
                    targetSdk = sdkVersions?.targetSdk,
                    compileSdk = sdkVersions?.compileSdk
                )
            )
        }
    }

    fun setBrowserSortMode(mode: BrowserSortMode) {
        state = state.copy(
            browserSortMode = mode,
            browserEntries = buildVisibleRows(state.remoteTreeCache.keys, state.expandedFolders)
        )
    }

    fun setBrowserTypeFilter(filter: BrowserTypeFilter) {
        state = state.copy(
            browserTypeFilter = filter,
            browserEntries = buildVisibleRows(state.remoteTreeCache.keys, state.expandedFolders)
        )
    }

    fun clearRenameError() {
        state = state.copy(renameError = null)
    }

    /** Renames a file or an entire folder (every path under it) as one commit — read the
     * old content(s) by blob sha, write them at the new path(s), delete the old path(s). */
    /** Resolves a repo file's current bytes: prefers [GitWaySessionState.pendingFileBytes]
     * (content this session already wrote for [path], whose real sha isn't known yet), and
     * only falls back to a sha-based GitHub fetch for paths that came from the last full
     * tree load. This is what lets rename/duplicate/open work immediately on something you
     * just created, renamed, or edited, instead of demanding a reload first. */
    private suspend fun resolveBytes(repo: GitRepository, path: String): Result<ByteArray> {
        state.pendingFileBytes[path]?.let { return Result.success(it) }
        val sha = state.remoteTreeCache[path]
        if (sha.isNullOrBlank()) {
            return Result.failure(IOException("Couldn't read \"$path\" — try reloading."))
        }
        return gitHubRepository.getFileContent(repo, sha)
    }

    fun renameEntry(entry: BrowserEntry, newName: String) {
        val repo = state.selectedRepo ?: return
        val trimmed = newName.trim()
        val currentName = entry.path.substringAfterLast("/")
        if (trimmed.isBlank() || trimmed == currentName || state.isRenamingEntry) return

        val parent = entry.path.substringBeforeLast("/", "")
        val newPath = if (parent.isEmpty()) trimmed else "$parent/$trimmed"
        val oldPrefix = if (entry.isFolder) "${entry.path}/" else null

        val siblingNames = state.remoteTreeCache.keys
            .filter { it.substringBeforeLast("/", "") == parent }
            .map { it.substringAfterLast("/") }
        if (trimmed in siblingNames) {
            state = state.copy(renameError = "\"$trimmed\" already exists here.")
            return
        }

        val affectedPaths = if (entry.isFolder) {
            state.remoteTreeCache.keys.filter { it == entry.path || it.startsWith("${entry.path}/") }
        } else {
            listOf(entry.path)
        }
        if (affectedPaths.isEmpty()) return

        state = state.copy(isRenamingEntry = true, renameError = null)
        viewModelScope.launch {
            val byteCache = mutableMapOf<String, ByteArray>()
            val destinationOf = mutableMapOf<String, String>()
            for (path in affectedPaths) {
                val bytes = resolveBytes(repo, path).getOrElse {
                    state = state.copy(isRenamingEntry = false, renameError = it.message ?: "Couldn't read \"$path\".")
                    return@launch
                }
                val dest = if (oldPrefix != null) newPath + "/" + path.removePrefix(oldPrefix) else newPath
                byteCache[dest] = bytes
                destinationOf[path] = dest
            }

            val changes = affectedPaths.map { path ->
                FileChange(fileName = path.substringAfterLast('/'), filePath = path, type = ChangeType.REMOVED)
            } + destinationOf.values.map { dest ->
                FileChange(fileName = dest.substringAfterLast('/'), filePath = dest, type = ChangeType.ADDED)
            }

            gitHubRepository.syncChanges(
                repo = repo,
                changes = changes,
                commitMessage = "Git Way: rename ${entry.path} to $newPath",
                targetBranch = state.selectedBranch,
                readFileBytes = { path -> byteCache.getValue(path) },
                onProgress = { _, _, _, _ -> }
            ).onSuccess {
                val updatedTree = (state.remoteTreeCache - affectedPaths.toSet()) + byteCache.keys.associateWith { "" }
                val updatedPendingBytes = (state.pendingFileBytes - affectedPaths.toSet()) + byteCache
                val expandedFolders = state.expandedFolders - affectedPaths.toSet() + (if (entry.isFolder) setOf(newPath) else emptySet())
                state = state.copy(
                    isRenamingEntry = false,
                    remoteTreeCache = updatedTree,
                    pendingFileBytes = updatedPendingBytes,
                    expandedFolders = expandedFolders,
                    browserEntries = buildVisibleRows(updatedTree.keys, expandedFolders)
                )
            }.onFailure { throwable ->
                state = state.copy(isRenamingEntry = false, renameError = throwable.message ?: "Couldn't rename.")
            }
        }
    }

    fun clearDuplicateError() {
        state = state.copy(duplicateError = null)
    }

    /** Duplicates a single file in the same folder with an auto-generated "(copy)" name
     * — same one-commit pattern as [pasteClipboardHere], just source == destination folder. */
    fun duplicateEntry(entry: BrowserEntry) {
        val repo = state.selectedRepo ?: return
        if (entry.isFolder || state.isDuplicatingEntry) return

        val parent = entry.path.substringBeforeLast("/", "")
        val siblingNames = state.remoteTreeCache.keys
            .filter { it.substringBeforeLast("/", "") == parent }
            .map { it.substringAfterLast("/") }
            .toSet()
        val newName = uniqueFileName(entry.name, siblingNames)
        val newPath = if (parent.isEmpty()) newName else "$parent/$newName"

        state = state.copy(isDuplicatingEntry = true, duplicateError = null)
        viewModelScope.launch {
            val bytes = resolveBytes(repo, entry.path).getOrElse {
                state = state.copy(isDuplicatingEntry = false, duplicateError = it.message ?: "Couldn't read \"${entry.name}\".")
                return@launch
            }
            gitHubRepository.syncChanges(
                repo = repo,
                changes = listOf(FileChange(fileName = newName, filePath = newPath, type = ChangeType.ADDED)),
                commitMessage = "Git Way: duplicate ${entry.path} to $newPath",
                targetBranch = state.selectedBranch,
                readFileBytes = { bytes },
                onProgress = { _, _, _, _ -> }
            ).onSuccess {
                val updatedTree = state.remoteTreeCache + (newPath to "")
                state = state.copy(
                    isDuplicatingEntry = false,
                    remoteTreeCache = updatedTree,
                    pendingFileBytes = state.pendingFileBytes + (newPath to bytes),
                    browserEntries = buildVisibleRows(updatedTree.keys, state.expandedFolders)
                )
            }.onFailure { throwable ->
                state = state.copy(isDuplicatingEntry = false, duplicateError = throwable.message ?: "Couldn't duplicate.")
            }
        }
    }

    /** PRD §18 multi-select toolbar "Add to .gitignore": appends every selected path to
     * the repo's root .gitignore (creating it if it doesn't exist yet) as one commit —
     * doesn't remove the files themselves, just stops them being tracked going forward. */
    fun addSelectedToGitignore() {
        val repo = state.selectedRepo ?: return
        val paths = state.selectedBrowserPaths
        if (paths.isEmpty() || state.isCreatingEntry) return

        state = state.copy(isCreatingEntry = true, createEntryError = null)
        viewModelScope.launch {
            val existingSha = state.remoteTreeCache[".gitignore"]
            val existingText = if (existingSha != null) {
                resolveBytes(repo, ".gitignore").getOrNull()?.let {
                    runCatching { it.toString(Charsets.UTF_8) }.getOrNull()
                }
            } else {
                null
            }.orEmpty()

            val existingLines = existingText.lines().map { it.trim() }.toSet()
            val newLines = paths.filter { it !in existingLines }
            if (newLines.isEmpty()) {
                state = state.copy(isCreatingEntry = false, selectedBrowserPaths = emptySet())
                return@launch
            }

            val updatedText = buildString {
                append(existingText)
                if (existingText.isNotEmpty() && !existingText.endsWith("\n")) append("\n")
                newLines.forEach { appendLine(it) }
            }
            val bytes = updatedText.toByteArray(Charsets.UTF_8)

            gitHubRepository.syncChanges(
                repo = repo,
                changes = listOf(
                    FileChange(
                        fileName = ".gitignore",
                        filePath = ".gitignore",
                        type = if (existingSha != null) ChangeType.MODIFIED else ChangeType.ADDED
                    )
                ),
                commitMessage = "Git Way: add ${newLines.size} path(s) to .gitignore",
                targetBranch = state.selectedBranch,
                readFileBytes = { bytes },
                onProgress = { _, _, _, _ -> }
            ).onSuccess {
                val updatedTree = state.remoteTreeCache + (".gitignore" to "")
                state = state.copy(
                    isCreatingEntry = false,
                    remoteTreeCache = updatedTree,
                    pendingFileBytes = state.pendingFileBytes + (".gitignore" to bytes),
                    selectedBrowserPaths = emptySet(),
                    browserEntries = buildVisibleRows(updatedTree.keys, state.expandedFolders)
                )
            }.onFailure { throwable ->
                state = state.copy(isCreatingEntry = false, createEntryError = throwable.message ?: "Couldn't update .gitignore.")
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

    /** Sets which folder "New file"/"New folder"/"Paste" target next, without touching
     * expansion state — used by a row's inline quick-action icons so tapping "+file" on
     * a folder doesn't also collapse/expand it. */
    fun setCreateTarget(path: String) {
        state = state.copy(browserPath = path)
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
                commitMessage = "Git Way: create $fullPath",
                targetBranch = state.selectedBranch,
                readFileBytes = { bytes },
                onProgress = { _, _, _, _ -> }
            ).onSuccess {
                val updatedTree = state.remoteTreeCache + (fullPath to "")
                val expanded = if (state.browserPath.isEmpty()) state.expandedFolders else state.expandedFolders + state.browserPath
                state = state.copy(
                    isCreatingEntry = false,
                    remoteTreeCache = updatedTree,
                    pendingFileBytes = state.pendingFileBytes + (fullPath to bytes),
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
                commitMessage = "Git Way: create folder $folderPath",
                targetBranch = state.selectedBranch,
                readFileBytes = { ByteArray(0) },
                onProgress = { _, _, _, _ -> }
            ).onSuccess {
                val updatedTree = state.remoteTreeCache + (placeholderPath to "")
                val expanded = if (state.browserPath.isEmpty()) state.expandedFolders else state.expandedFolders + state.browserPath
                state = state.copy(
                    isCreatingEntry = false,
                    remoteTreeCache = updatedTree,
                    pendingFileBytes = state.pendingFileBytes + (placeholderPath to ByteArray(0)),
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

    /** User closed the "no CI workflow" banner without adding anything — stays dismissed
     * for the rest of this repo session (reappears next time the repo is opened fresh). */
    fun dismissWorkflowSuggestion() {
        state = state.copy(workflowSuggestionDismissed = true)
    }

    /** Adds every template the user checked in the picker as ONE commit — reuses the same
     * tested sync pipeline as every other write in the app. Nothing here is automatic:
     * this only ever runs from the picker's own "Add" button, after the user has read
     * each template's YAML and ticked the ones they actually want (PRD "CI Workflow
     * Suggestions" — manual opt-in per file, never added silently). */
    fun addWorkflowFiles(templates: List<WorkflowTemplate>) {
        val repo = state.selectedRepo ?: return
        if (templates.isEmpty() || state.isAddingWorkflows) return

        val changes = templates.map { FileChange(fileName = it.path.substringAfterLast('/'), filePath = it.path, type = ChangeType.ADDED) }
        val bytesByPath = templates.associate { it.path to it.yaml.toByteArray(Charsets.UTF_8) }

        state = state.copy(isAddingWorkflows = true, addWorkflowsError = null)
        viewModelScope.launch {
            gitHubRepository.syncChanges(
                repo = repo,
                changes = changes,
                commitMessage = if (templates.size == 1) {
                    "Git Way: add ${templates.first().path}"
                } else {
                    "Git Way: add ${templates.size} GitHub Actions workflows"
                },
                targetBranch = state.selectedBranch,
                readFileBytes = { path -> bytesByPath.getValue(path) },
                onProgress = { _, _, _, _ -> }
            ).onSuccess {
                val updatedTree = state.remoteTreeCache + templates.associate { it.path to "" }
                val expandedWithGithub = state.expandedFolders + ".github" + ".github/workflows"
                state = state.copy(
                    isAddingWorkflows = false,
                    workflowSuggestionDismissed = true,
                    remoteTreeCache = updatedTree,
                    pendingFileBytes = state.pendingFileBytes + bytesByPath,
                    expandedFolders = expandedWithGithub,
                    browserEntries = buildVisibleRows(updatedTree.keys, expandedWithGithub)
                )
            }.onFailure { throwable ->
                state = state.copy(isAddingWorkflows = false, addWorkflowsError = throwable.message ?: "Couldn't add workflow file(s).")
            }
        }
    }

    // ===== Repository Management: branches =====

    fun loadBranches() {
        val repo = state.selectedRepo ?: return
        state = state.copy(isLoadingBranches = true, branchError = null)
        viewModelScope.launch {
            gitHubRepository.listBranches(repo)
                .onSuccess { branches -> state = state.copy(isLoadingBranches = false, availableBranches = branches) }
                .onFailure { throwable ->
                    state = state.copy(isLoadingBranches = false, branchError = throwable.message ?: "Couldn't load branches.")
                }
        }
    }

    /** Switches which branch every subsequent read/write in this repo session targets —
     * null means "the repo's own default branch". Reloads the tree for the new branch
     * and clears anything tied to the old one (open viewer, selection, commit history). */
    fun selectBranch(branch: String?) {
        if (branch == state.selectedBranch) return
        state = state.copy(
            selectedBranch = branch,
            expandedFolders = emptySet(),
            selectedBrowserPaths = emptySet(),
            viewingFile = null,
            viewingContent = null,
            commitHistory = emptyList()
        )
        loadBrowserRoot()
    }

    fun clearCreateBranchError() {
        state = state.copy(createBranchError = null)
    }

    /** Creates [newBranchName] as a real fork of whichever branch is currently selected
     * (or the default), then switches to it — never an empty ref. */
    fun createBranch(newBranchName: String) {
        val repo = state.selectedRepo ?: return
        if (newBranchName.isBlank() || state.isCreatingBranch) return
        val sourceBranch = state.selectedBranch ?: repo.defaultBranch
        state = state.copy(isCreatingBranch = true, createBranchError = null)
        viewModelScope.launch {
            gitHubRepository.createBranch(repo, newBranchName.trim(), sourceBranch)
                .onSuccess {
                    state = state.copy(
                        isCreatingBranch = false,
                        availableBranches = state.availableBranches + newBranchName.trim()
                    )
                    selectBranch(newBranchName.trim())
                }
                .onFailure { throwable ->
                    state = state.copy(isCreatingBranch = false, createBranchError = throwable.message ?: "Couldn't create branch.")
                }
        }
    }

    // ===== Repository Management: commit history =====

    fun loadCommitHistory() {
        val repo = state.selectedRepo ?: return
        state = state.copy(isLoadingCommitHistory = true, commitHistoryError = null)
        viewModelScope.launch {
            gitHubRepository.getCommitHistory(repo, branch = state.selectedBranch)
                .onSuccess { commits -> state = state.copy(isLoadingCommitHistory = false, commitHistory = commits) }
                .onFailure { throwable ->
                    state = state.copy(isLoadingCommitHistory = false, commitHistoryError = throwable.message ?: "Couldn't load commit history.")
                }
        }
    }

    // ===== Repository Management: repo settings (rename / describe / visibility / delete) =====

    fun clearRepoSettingsError() {
        state = state.copy(repoSettingsError = null)
    }

    fun updateRepositorySettings(newName: String?, newDescription: String?, newIsPrivate: Boolean?) {
        val repo = state.selectedRepo ?: return
        if (state.isUpdatingRepoSettings) return
        state = state.copy(isUpdatingRepoSettings = true, repoSettingsError = null)
        viewModelScope.launch {
            gitHubRepository.updateRepository(repo, newName, newDescription, newIsPrivate)
                .onSuccess { updated -> state = state.copy(isUpdatingRepoSettings = false, selectedRepo = updated) }
                .onFailure { throwable ->
                    state = state.copy(isUpdatingRepoSettings = false, repoSettingsError = throwable.message ?: "Couldn't update repository.")
                }
        }
    }

    /** Permanently deletes the current repo from GitHub. [onDeleted] fires only on
     * success, so the caller can navigate back to the repository list. */
    fun deleteRepository(onDeleted: () -> Unit) {
        val repo = state.selectedRepo ?: return
        if (state.isDeletingRepo) return
        state = state.copy(isDeletingRepo = true, deleteRepoError = null)
        viewModelScope.launch {
            gitHubRepository.deleteRepository(repo)
                .onSuccess {
                    state = state.copy(isDeletingRepo = false)
                    onDeleted()
                }
                .onFailure { throwable ->
                    state = state.copy(isDeletingRepo = false, deleteRepoError = throwable.message ?: "Couldn't delete repository.")
                }
        }
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
                commitMessage = if (paths.size == 1) {
                    "Git Way: delete ${paths.first()}"
                } else {
                    "Git Way: delete ${paths.size} files"
                },
                targetBranch = state.selectedBranch,
                readFileBytes = { ByteArray(0) }, // never invoked — REMOVED changes carry no content
                onProgress = { _, _, _, _ -> }
            ).onSuccess {
                val pathSet = paths.toSet()
                val updatedTree = state.remoteTreeCache - pathSet
                val updatedExpanded = state.expandedFolders - pathSet
                state = state.copy(
                    isDeletingEntry = false,
                    remoteTreeCache = updatedTree,
                    pendingFileBytes = state.pendingFileBytes - pathSet,
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
                    val bytes = resolveBytes(repo, entry.path).getOrElse {
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
                    commitMessage = if (changes.size == 1) {
                        "Git Way: paste ${changes.first().filePath}"
                    } else {
                        "Git Way: paste ${changes.size} files"
                    },
                    targetBranch = state.selectedBranch,
                    readFileBytes = { path -> byteCache.getValue(path) },
                    onProgress = { _, _, _, _ -> }
                ).onSuccess {
                    val updatedTree = state.remoteTreeCache + byteCache.keys.associateWith { "" }
                    val expanded = if (state.browserPath.isEmpty()) state.expandedFolders else state.expandedFolders + state.browserPath
                    state = state.copy(
                        isPasting = false,
                        clipboard = emptyList(),
                        remoteTreeCache = updatedTree,
                        pendingFileBytes = state.pendingFileBytes + byteCache,
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
        state = state.copy(
            viewingFile = entry,
            viewingContent = null,
            viewingImageBytes = null,
            isLoadingContent = true,
            viewerError = null,
            isEditingFile = false,
            saveFileError = null
        )
        viewModelScope.launch {
            resolveBytes(repo, entry.path)
                .onSuccess { bytes ->
                    if (FileTypeIcons.isImage(entry.name)) {
                        state = state.copy(isLoadingContent = false, viewingImageBytes = bytes, viewerError = null)
                    } else {
                        val text = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull()
                        state = state.copy(
                            isLoadingContent = false,
                            viewingContent = text,
                            viewerError = if (text == null) "This looks like a binary file — preview isn't available." else null
                        )
                    }
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
            viewingImageBytes = null,
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
                commitMessage = "Git Way: edit ${entry.path}",
                targetBranch = state.selectedBranch,
                readFileBytes = { bytes },
                onProgress = { _, _, _, _ -> }
            ).onSuccess {
                state = state.copy(
                    isSavingFile = false,
                    isEditingFile = false,
                    viewingContent = newContent,
                    pendingFileBytes = state.pendingFileBytes + (entry.path to bytes)
                )
            }.onFailure { throwable ->
                state = state.copy(isSavingFile = false, saveFileError = throwable.message ?: "Couldn't save changes.")
            }
        }
    }

    /** Computes the immediate children of [currentPath] from a flat set of full repo paths,
     * ordered by [GitWaySessionState.browserSortMode] and narrowed by
     * [GitWaySessionState.browserTypeFilter] (folders are always shown regardless of the
     * active filter, so the tree stays navigable to reach a filtered file deeper down). */
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
            if (!isFolder && !matchesTypeFilter(firstSegment, state.browserTypeFilter)) continue
            val fullPath = prefix + firstSegment
            if (seen.add(fullPath)) {
                entries += BrowserEntry(name = firstSegment, path = fullPath, isFolder = isFolder)
            }
        }
        val comparator = when (state.browserSortMode) {
            BrowserSortMode.NAME_ASC -> compareBy<BrowserEntry> { it.name.lowercase() }
            BrowserSortMode.NAME_DESC -> compareByDescending<BrowserEntry> { it.name.lowercase() }
            BrowserSortMode.TYPE -> compareBy<BrowserEntry>({ it.name.substringAfterLast('.', "") }, { it.name.lowercase() })
        }
        return entries.sortedWith(compareBy<BrowserEntry> { !it.isFolder }.then(comparator))
    }

    private fun matchesTypeFilter(fileName: String, filter: BrowserTypeFilter): Boolean {
        if (filter == BrowserTypeFilter.ALL) return true
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (filter) {
            BrowserTypeFilter.KOTLIN -> ext in setOf("kt", "kts")
            BrowserTypeFilter.XML -> ext == "xml"
            BrowserTypeFilter.GRADLE -> fileName.endsWith("build.gradle") || fileName.endsWith("build.gradle.kts") ||
                fileName == "settings.gradle.kts" || fileName == "settings.gradle" || ext == "properties"
            BrowserTypeFilter.IMAGES -> FileTypeIcons.isImage(fileName)
            BrowserTypeFilter.OTHER -> ext !in setOf(
                "kt", "kts", "xml", "properties", "png", "jpg", "jpeg", "webp", "gif", "svg", "bmp"
            ) && !fileName.endsWith("build.gradle") && !fileName.endsWith("build.gradle.kts")
            BrowserTypeFilter.ALL -> true
        }
    }

    /** Flattens the tree into indentation-ready rows (VS Code Explorer style): every
     * folder always appears, its children are spliced in right after it only while its
     * path is in [expanded]. Rebuilt on every tree/expansion change — cheap enough for
     * the file counts a mobile repo browser deals with.
     *
     * Single-child folder chains (a folder with exactly one subfolder and nothing else,
     * repeated — e.g. "kotlin/com/io/git/way") compact into ONE row showing the joined
     * path, same as Android Studio's package view. [TreeRow.entry.path] stays the real,
     * deepest folder in the chain, so expand/rename/create-target/delete all keep working
     * against an actual path unchanged. A folder that has more than one child, or a file
     * alongside its one subfolder — like "main" (kotlin/ + res/ + AndroidManifest.xml) —
     * is never part of a chain and always gets its own expandable row. */
    private fun buildVisibleRows(paths: Set<String>, expanded: Set<String>): List<TreeRow> {
        val rows = mutableListOf<TreeRow>()
        fun walk(currentPath: String, depth: Int, ancestorsContinue: List<Boolean>) {
            val children = childrenOf(paths, currentPath)
            children.forEachIndexed { index, entry ->
                val meContinues = index != children.lastIndex
                val myAncestorLines = ancestorsContinue + meContinues

                if (!entry.isFolder) {
                    rows += TreeRow(entry = entry, depth = depth, ancestorLines = myAncestorLines)
                    return@forEachIndexed
                }

                val chainNames = mutableListOf(entry.name)
                var tailPath = entry.path
                while (true) {
                    val kids = childrenOf(paths, tailPath)
                    if (kids.size == 1 && kids[0].isFolder) {
                        tailPath = kids[0].path
                        chainNames += kids[0].name
                    } else {
                        break
                    }
                }

                val isExpanded = tailPath in expanded
                val childCount = childrenOf(paths, tailPath).size
                rows += TreeRow(
                    entry = BrowserEntry(name = chainNames.joinToString("/"), path = tailPath, isFolder = true),
                    depth = depth,
                    isExpanded = isExpanded,
                    directChildCount = childCount,
                    ancestorLines = myAncestorLines
                )
                if (isExpanded) walk(tailPath, depth + 1, myAncestorLines)
            }
        }
        walk("", 0, emptyList())
        return rows
    }
}
