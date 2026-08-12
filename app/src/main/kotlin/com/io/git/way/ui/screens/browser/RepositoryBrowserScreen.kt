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
package com.io.git.way.ui.screens.browser

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.io.git.way.domain.model.BrowserEntry
import com.io.git.way.domain.model.BrowserSortMode
import com.io.git.way.domain.model.BrowserTypeFilter
import com.io.git.way.domain.model.CommitSummary
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.domain.model.TreeRow
import com.io.git.way.domain.WorkflowTemplate
import com.io.git.way.domain.WorkflowTemplates
import com.io.git.way.ui.common.FileTypeIcons
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.common.MarkdownLinkResolver
import com.io.git.way.ui.common.MarkdownView
import com.io.git.way.ui.common.PendingDownloadHandler
import com.io.git.way.ui.common.SyntaxHighlightTransformation
import com.io.git.way.ui.common.SyntaxHighlighter
import com.io.git.way.ui.common.formatRelativeTime
import com.io.git.way.ui.theme.GlassBlobBlue
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassScaffold
import com.io.git.way.ui.theme.GlassSecondaryButton

/** Files whose accidental deletion can break the build — get the stronger warning dialog
 * (PRD "Repository Explorer" §24 Delete Protection) instead of the plain confirmation. */
private val CRITICAL_FILE_NAMES = setOf(
    "AndroidManifest.xml", "build.gradle.kts", "build.gradle",
    "settings.gradle.kts", "settings.gradle", "proguard-rules.pro"
)

/**
 * GitWay Project Explorer: a GitHub-aware Android project/file explorer over the
 * selected repository. Indented, expandable tree (no drill-down navigation — folders
 * open in place), current-path bar, Android project detection, type-aware icons with
 * metadata, sort/filter, multi-select, and a read/edit viewer with syntax highlighting.
 * Every write (create/paste/rename/duplicate/edit/delete) lands as its own small commit
 * via the same tested sync pipeline used for the main upload flow.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepositoryBrowserScreen(
    sessionViewModel: GitWaySessionViewModel,
    onBack: () -> Unit,
    onSyncFromDevice: () -> Unit,
    onOpenActions: () -> Unit = {},
    onOpenPullRequests: () -> Unit = {},
    onOpenIssues: () -> Unit = {},
    onOpenReleases: () -> Unit = {}
) {
    val state = sessionViewModel.state
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var showCreateMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showSortFilterSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BrowserEntry?>(null) }
    var pendingDeleteSelection by remember { mutableStateOf(false) }
    var pendingRename by remember { mutableStateOf<BrowserEntry?>(null) }
    var showWorkflowPicker by remember { mutableStateOf(false) }
    var browserQuery by remember { mutableStateOf("") }
    var showBranchPicker by remember { mutableStateOf(false) }
    var showCreateBranchDialog by remember { mutableStateOf(false) }
    var showCommitHistory by remember { mutableStateOf(false) }
    var showRepoSettings by remember { mutableStateOf(false) }
    var showDeleteRepoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedRepo) {
        if (state.selectedRepo != null && state.remoteTreeCache.isEmpty() && !state.isBrowserLoading) {
            sessionViewModel.loadBrowserRoot()
        }
        if (state.selectedRepo != null && state.availableBranches.isEmpty() && !state.isLoadingBranches) {
            sessionViewModel.loadBranches()
        }
    }

    // The file viewer/editor replaces the tree in place, so back/close behaviour stays
    // intuitive without introducing a second nav-graph destination.
    if (state.viewingFile != null) {
        FileViewerScreen(sessionViewModel = sessionViewModel)
        return
    }

    val repo = state.selectedRepo
    val selectionMode = state.selectedBrowserPaths.isNotEmpty()
    val title = if (selectionMode) "${state.selectedBrowserPaths.size} selected" else repo?.name ?: "Repository"

    fun githubUrl(path: String, isFolder: Boolean): String? {
        val r = repo ?: return null
        val kind = if (isFolder) "tree" else "blob"
        return "https://github.com/${r.owner}/${r.name}/$kind/${state.selectedBranch ?: r.defaultBranch}/$path"
    }

    GlassScaffold(
        title = title,
        subtitle = if (!selectionMode) (state.androidProjectInfo?.packageName ?: "Project Explorer") else null,
        navigationIcon = {
            IconButton(onClick = { if (selectionMode) sessionViewModel.clearBrowserSelection() else onBack() }) {
                Icon(if (selectionMode) Icons.Filled.Close else Icons.Filled.ArrowBack, contentDescription = if (selectionMode) "Cancel selection" else "Back")
            }
        },
        actions = {
            if (selectionMode) {
                IconButton(onClick = { sessionViewModel.addSelectedToGitignore() }) {
                    Icon(Icons.Filled.VisibilityOff, contentDescription = "Add to .gitignore")
                }
                IconButton(onClick = { pendingDeleteSelection = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
                }
                IconButton(onClick = { sessionViewModel.copySelectionToClipboard() }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy selected")
                }
            } else {
                IconButton(onClick = { showBranchPicker = true }) {
                    Icon(Icons.Filled.AccountTree, contentDescription = "Branch: ${state.selectedBranch ?: repo?.defaultBranch ?: "default"}")
                }
                IconButton(onClick = { showSortFilterSheet = true }) {
                    Icon(Icons.Filled.Sort, contentDescription = "Sort & filter")
                }
                if (state.clipboard.isNotEmpty()) {
                    IconButton(onClick = { sessionViewModel.pasteClipboardHere() }, enabled = !state.isPasting) {
                        if (state.isPasting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.ContentPaste, contentDescription = "Paste ${state.clipboard.size} file(s) here")
                        }
                    }
                }
                Box {
                    IconButton(onClick = { showCreateMenu = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "New file or folder")
                    }
                    DropdownMenu(expanded = showCreateMenu, onDismissRequest = { showCreateMenu = false }) {
                        DropdownMenuItem(text = { Text("New file") }, onClick = { showCreateMenu = false; showNewFileDialog = true })
                        DropdownMenuItem(text = { Text("New folder") }, onClick = { showCreateMenu = false; showNewFolderDialog = true })
                    }
                }
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("GitHub Actions") },
                            onClick = { showMoreMenu = false; onOpenActions() },
                            leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Pull Requests") },
                            onClick = { showMoreMenu = false; onOpenPullRequests() },
                            leadingIcon = { Icon(Icons.Filled.Merge, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Issues") },
                            onClick = { showMoreMenu = false; onOpenIssues() },
                            leadingIcon = { Icon(Icons.Filled.BugReport, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Releases") },
                            onClick = { showMoreMenu = false; onOpenReleases() },
                            leadingIcon = { Icon(Icons.Filled.TrackChanges, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Commit history") },
                            onClick = { showMoreMenu = false; showCommitHistory = true; sessionViewModel.loadCommitHistory() },
                            leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Download repo ZIP") },
                            onClick = { showMoreMenu = false; sessionViewModel.downloadRepoZip() },
                            leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (repo?.isPrivate == true) "Make public" else "Make private") },
                            onClick = {
                                showMoreMenu = false
                                if (repo != null) {
                                    val targetPrivate = !repo.isPrivate
                                    sessionViewModel.updateRepositorySettings(null, null, targetPrivate) {
                                        Toast.makeText(
                                            context,
                                            if (targetPrivate) "Repository is now private" else "Repository is now public",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    if (repo?.isPrivate == true) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename repository") },
                            onClick = { showMoreMenu = false; showRepoSettings = true },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete repository", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMoreMenu = false; showDeleteRepoDialog = true },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {

            if (!selectionMode) {
                OutlinedTextField(
                    value = browserQuery,
                    onValueChange = { browserQuery = it },
                    placeholder = { Text("Search files and folders...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (browserQuery.isNotEmpty()) {
                            IconButton(onClick = { browserQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp)
                )

                if (state.browserTypeFilter != BrowserTypeFilter.ALL) {
                    FilterChip(
                        selected = true,
                        onClick = { sessionViewModel.setBrowserTypeFilter(BrowserTypeFilter.ALL) },
                        label = { Text("Filter: ${state.browserTypeFilter.label} ✕") },
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            if (state.pasteError != null) {
                Text(state.pasteError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
            }
            if (state.deleteEntryError != null) {
                Text(state.deleteEntryError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
            }
            if (state.renameError != null) {
                Text(state.renameError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
            }
            if (state.duplicateError != null) {
                Text(state.duplicateError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
            }
            if (state.repoSettingsError != null) {
                Text(state.repoSettingsError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
            }

            if (state.showWorkflowSuggestion && !selectionMode) {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Text("No GitHub Actions CI workflow found", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "This repository has nothing under .github/workflows/. Git Way can add a ready-made CI workflow — you choose exactly which one(s), nothing is added automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassSecondaryButton(
                            text = "Add workflow",
                            onClick = { showWorkflowPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { sessionViewModel.dismissWorkflowSuggestion() }) { Text("Not now") }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val searchResults = remember(browserQuery, state.remoteTreeCache) {
                    if (browserQuery.isBlank()) {
                        emptyList()
                    } else {
                        state.remoteTreeCache.keys
                            .filter { !it.endsWith("/.gitkeep") && it != ".gitkeep" }
                            .filter { it.substringAfterLast('/').contains(browserQuery, ignoreCase = true) }
                            .sorted()
                    }
                }

                when {
                    state.isBrowserLoading -> ExplorerSkeleton()

                    state.browserError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                            Text("Unable to load folder", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                            Text(
                                state.browserError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            TextButton(onClick = { sessionViewModel.loadBrowserRoot() }) { Text("Retry") }
                        }
                    }

                    browserQuery.isNotBlank() -> if (searchResults.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No files match \"$browserQuery\".") }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(searchResults, key = { it }) { path ->
                                SearchResultRow(
                                    path = path,
                                    onClick = { sessionViewModel.openFile(BrowserEntry(name = path.substringAfterLast('/'), path = path, isFolder = false)) }
                                )
                            }
                        }
                    }

                    state.browserEntries.isEmpty() -> EmptyFolderState(
                        onNewFile = { showNewFileDialog = true },
                        onUpload = onSyncFromDevice
                    )

                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        item(key = "repository-root") {
                            RepositoryRootRow(
                                repositoryName = repo?.name ?: "Repository",
                                itemCount = state.browserEntries.size
                            )
                        }
                        items(state.browserEntries, key = { it.entry.path }) { row ->
                            TreeRowItem(
                                row = row,
                                selectionMode = selectionMode,
                                selected = row.entry.path in state.selectedBrowserPaths,
                                onClick = {
                                    when {
                                        selectionMode && !row.entry.isFolder -> sessionViewModel.toggleBrowserSelection(row.entry)
                                        row.entry.isFolder -> sessionViewModel.toggleFolderExpanded(row.entry)
                                        else -> sessionViewModel.openFile(row.entry)
                                    }
                                },
                                onLongClick = { if (!row.entry.isFolder) sessionViewModel.toggleBrowserSelection(row.entry) },
                                onDelete = { pendingDelete = row.entry },
                                onRename = { pendingRename = row.entry },
                                onDuplicate = { sessionViewModel.duplicateEntry(row.entry) },
                                onNewFile = { sessionViewModel.setCreateTarget(row.entry.path); showNewFileDialog = true },
                                onNewFolder = { sessionViewModel.setCreateTarget(row.entry.path); showNewFolderDialog = true },
                                onRefresh = { sessionViewModel.loadBrowserRoot() },
                                onCopyPath = { clipboard.setText(AnnotatedString(row.entry.path)) },
                                onCopyName = { clipboard.setText(AnnotatedString(row.entry.name)) },
                                onViewOnGitHub = {
                                    githubUrl(row.entry.path, row.entry.isFolder)?.let { url ->
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                },
                                fileSize = state.remoteFileSizes[row.entry.path]
                            )
                        }
                    }
                }
                if (state.isDeletingEntry || state.isRenamingEntry || state.isDuplicatingEntry) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            GlassSecondaryButton(
                text = "Sync updated project from device",
                onClick = onSyncFromDevice,
                leadingIcon = { Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }

    if (showNewFileDialog) {
        NewEntryDialog(
            title = "New file", label = "File name (e.g. notes.md)",
            isCreating = state.isCreatingEntry, errorMessage = state.createEntryError,
            onDismiss = { showNewFileDialog = false; sessionViewModel.clearCreateEntryError() },
            onConfirm = { name -> sessionViewModel.createFile(name) },
            onCreated = { showNewFileDialog = false }
        )
    }
    if (showNewFolderDialog) {
        NewEntryDialog(
            title = "New folder", label = "Folder name",
            isCreating = state.isCreatingEntry, errorMessage = state.createEntryError,
            onDismiss = { showNewFolderDialog = false; sessionViewModel.clearCreateEntryError() },
            onConfirm = { name -> sessionViewModel.createFolder(name) },
            onCreated = { showNewFolderDialog = false }
        )
    }
    pendingRename?.let { entry ->
        RenameDialog(
            currentName = entry.path.substringAfterLast("/"),
            isRenaming = state.isRenamingEntry,
            errorMessage = state.renameError,
            onDismiss = { pendingRename = null; sessionViewModel.clearRenameError() },
            onConfirm = { newName -> sessionViewModel.renameEntry(entry, newName) },
            onRenamed = { pendingRename = null }
        )
    }
    if (showSortFilterSheet) {
        SortFilterDialog(
            sortMode = state.browserSortMode,
            typeFilter = state.browserTypeFilter,
            onSortModeChange = sessionViewModel::setBrowserSortMode,
            onTypeFilterChange = sessionViewModel::setBrowserTypeFilter,
            onDismiss = { showSortFilterSheet = false }
        )
    }
    if (showBranchPicker && repo != null) {
        BranchPickerDialog(
            branches = state.availableBranches,
            defaultBranch = repo.defaultBranch,
            selectedBranch = state.selectedBranch,
            isLoading = state.isLoadingBranches,
            errorMessage = state.branchError,
            onSelect = { branch -> sessionViewModel.selectBranch(branch); showBranchPicker = false },
            onCreateNew = { showBranchPicker = false; showCreateBranchDialog = true },
            onDismiss = { showBranchPicker = false }
        )
    }
    if (showCreateBranchDialog) {
        CreateBranchDialog(
            sourceBranch = state.selectedBranch ?: repo?.defaultBranch ?: "default",
            isCreating = state.isCreatingBranch,
            errorMessage = state.createBranchError,
            onDismiss = { showCreateBranchDialog = false; sessionViewModel.clearCreateBranchError() },
            onConfirm = { name -> sessionViewModel.createBranch(name) },
            onCreated = { showCreateBranchDialog = false }
        )
    }
    if (showCommitHistory) {
        CommitHistoryDialog(
            commits = state.commitHistory,
            isLoading = state.isLoadingCommitHistory,
            errorMessage = state.commitHistoryError,
            onOpenCommit = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
            onViewDiff = { commit -> sessionViewModel.loadCommitDiff(commit.sha) },
            onDismiss = { showCommitHistory = false }
        )
    }
    if (showRepoSettings && repo != null) {
        RepositorySettingsDialog(
            repo = repo,
            isSaving = state.isUpdatingRepoSettings,
            saveError = state.repoSettingsError,
            onSave = { newName, newDescription, newIsPrivate -> sessionViewModel.updateRepositorySettings(newName, newDescription, newIsPrivate) },
            onSaved = { showRepoSettings = false },
            onDismiss = { showRepoSettings = false; sessionViewModel.clearRepoSettingsError() }
        )
    }
    if (showDeleteRepoDialog && repo != null) {
        DeleteRepositoryDialog(
            repo = repo,
            isDeleting = state.isDeletingRepo,
            deleteError = state.deleteRepoError,
            onConfirm = { sessionViewModel.deleteRepository(onDeleted = { showDeleteRepoDialog = false; onBack() }) },
            onDismiss = { showDeleteRepoDialog = false; sessionViewModel.clearDeleteRepoError() }
        )
    }
    if (showWorkflowPicker) {
        WorkflowPickerDialog(
            isAdding = state.isAddingWorkflows,
            errorMessage = state.addWorkflowsError,
            onDismiss = { showWorkflowPicker = false },
            onConfirm = { selected -> sessionViewModel.addWorkflowFiles(selected) },
            onAdded = { showWorkflowPicker = false }
        )
    }
    pendingDelete?.let { entry ->
        DeleteConfirmDialog(
            entry = entry,
            isDeleting = state.isDeletingEntry,
            onConfirm = { sessionViewModel.deleteEntry(entry); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
    if (pendingDeleteSelection) {
        AlertDialog(
            onDismissRequest = { if (!state.isDeletingEntry) pendingDeleteSelection = false },
            title = { Text("Delete ${state.selectedBrowserPaths.size} file(s)?") },
            text = { Text("These files will be removed from GitHub. This can't be undone from within Git Way.") },
            confirmButton = {
                TextButton(onClick = { sessionViewModel.deleteSelected(); pendingDeleteSelection = false }, enabled = !state.isDeletingEntry) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteSelection = false }, enabled = !state.isDeletingEntry) { Text("Cancel") } }
        )
    }

    if (state.viewingCommitSha != null) {
        CommitDiffDialog(
            files = state.commitDiff,
            isLoading = state.isLoadingCommitDiff,
            errorMessage = state.commitDiffError,
            onDismiss = { sessionViewModel.clearCommitDiff() }
        )
    }

    PendingDownloadHandler(
        pending = state.pendingDownload,
        downloading = state.downloadingArtifactName != null && state.pendingDownload == null,
        downloadError = state.downloadError,
        onClear = { sessionViewModel.clearPendingDownload() },
        onClearError = { sessionViewModel.clearDownloadError() }
    )
}

/** PRD §21 Loading — a handful of placeholder rows instead of a bare spinner, so the
 * tree's shape doesn't visually "pop in" once it loads. */
@Composable
private fun ExplorerSkeleton() {
    val shimmer = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(6) {
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(shimmer, RoundedCornerShape(10.dp)))
                    Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                        Box(Modifier.fillMaxWidth(0.5f).height(14.dp).background(shimmer, RoundedCornerShape(4.dp)))
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth(0.3f).height(10.dp).background(shimmer, RoundedCornerShape(4.dp)))
                    }
                }
            }
        }
    }
}

/** PRD §22 Empty State. */
@Composable
private fun EmptyFolderState(onNewFile: () -> Unit, onUpload: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.FolderOff, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Folder is empty", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
            Text(
                "Create a file or upload something here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassSecondaryButton(text = "New File", onClick = onNewFile)
                GlassSecondaryButton(text = "Upload", onClick = onUpload)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
/** One flat search-result row — shown instead of the tree while the search field has
 * text, since a match might be nested inside a folder the user hasn't expanded. */
@Composable
private fun SearchResultRow(path: String, onClick: () -> Unit) {
    val name = path.substringAfterLast('/')
    GlassCard(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = {}), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(FileTypeIcons.iconFor(name), contentDescription = null, tint = FileTypeIcons.colorFor(name), modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RepositoryRootRow(
    repositoryName: String,
    itemCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = "Repository root",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(2.dp))
        SvgFolderIcon(
            open = true,
            modifier = Modifier.size(28.dp)
        )
        Text(
            repositoryName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp).weight(1f)
        )
        Text(
            itemCount.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SvgFolderIcon(
    open: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resource = if (open) {
        com.io.git.way.R.raw.folder_open
    } else {
        com.io.git.way.R.raw.folder_closed
    }

    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    val painter = rememberAsyncImagePainter(
        model = resource,
        imageLoader = imageLoader
    )

    Image(
        painter = painter,
        contentDescription = if (open) "Open folder" else "Closed folder",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreeRowItem(
    row: TreeRow,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onNewFile: () -> Unit,
    onNewFolder: () -> Unit,
    onRefresh: () -> Unit,
    onCopyPath: () -> Unit,
    onCopyName: () -> Unit,
    onViewOnGitHub: () -> Unit,
    fileSize: Long?
) {
    val entry = row.entry
    var showMenu by remember { mutableStateOf(false) }
    val iconTint = if (entry.isFolder) GlassBlobBlue else FileTypeIcons.colorFor(entry.name)
    val rowBackground = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
    } else {
        Color.Transparent
    }

    // Fixed height keeps every guide column aligned to the same vertical rhythm.
    val rowHeight = 50.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(rowBackground)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val leadingScroll = rememberScrollState()

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(leadingScroll),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TreeIndentGuides(row, if (entry.isFolder) 0.dp else 24.dp)

            if (entry.isFolder) {
                val rotation by animateFloatAsState(
                    targetValue = if (row.isExpanded) 90f else 0f,
                    label = "tree-chevron"
                )
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = if (row.isExpanded) "Collapse folder" else "Expand folder",
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = rotation },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(2.dp))
            } else if (selectionMode) {
                Icon(
                    if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (selected) "Selected" else "Not selected",
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
            } else {
                // Keep files aligned with folder names without showing a fake expand arrow.
                Spacer(Modifier.width(22.dp))
            }

            if (entry.isFolder) {
                SvgFolderIcon(
                    open = row.isExpanded,
                    modifier = Modifier.size(25.dp)
                )
            } else {
                Icon(
                    FileTypeIcons.iconFor(entry.name),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(
                modifier = Modifier
                    .padding(start = 9.dp)
                    .widthIn(min = 80.dp, max = 420.dp)
            ) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
                if (!entry.isFolder) {
                    Text(
                        if (fileSize != null) "${FileTypeIcons.typeLabel(entry.name)} • ${FileTypeIcons.formatSize(fileSize)}" else FileTypeIcons.typeLabel(entry.name),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }

            Spacer(Modifier.width(12.dp))
        }

        if (!entry.isFolder && !selectionMode) {
            val badge = FileTypeIcons.badgeFor(entry.name)
            if (badge.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .background(iconTint.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = iconTint
                    )
                }
                Spacer(Modifier.width(5.dp))
            }
        }

        if (!selectionMode) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More actions",
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open") },
                        onClick = { showMenu = false; onClick() }
                    )
                    if (entry.isFolder) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.NoteAdd, null) },
                            text = { Text("New File") },
                            onClick = { showMenu = false; onNewFile() }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null) },
                            text = { Text("New Folder") },
                            onClick = { showMenu = false; onNewFolder() }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                            text = { Text("Refresh") },
                            onClick = { showMenu = false; onRefresh() }
                        )
                    } else {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.FileCopy, null) },
                            text = { Text("Duplicate") },
                            onClick = { showMenu = false; onDuplicate() }
                        )
                    }
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Filled.Edit, null) },
                        text = { Text("Rename") },
                        onClick = { showMenu = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy Path") },
                        onClick = { showMenu = false; onCopyPath() }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy Name") },
                        onClick = { showMenu = false; onCopyName() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Filled.OpenInNew, null) },
                        text = { Text("View on GitHub") },
                        onClick = { showMenu = false; onViewOnGitHub() }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }

            if (!entry.isFolder) {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(23.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowRight,
                            contentDescription = "Open",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Continuous VS Code-style tree guides.
 *
 * Each depth gets one fixed 18dp column. Ancestor columns remain vertical when that
 * ancestor has another sibling below; the current row gets an elbow into its icon/name.
 */
@Composable
private fun TreeIndentGuides(row: TreeRow, nameExtension: Dp = 0.dp) {
    val guideColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f)
    val indentUnit = 18.dp
    val strokeWidth = 1.5.dp
    val corner = 5.dp
    val extensionPx = with(LocalDensity.current) { nameExtension.toPx() }
    val expandedFolder = row.entry.isFolder && row.isExpanded && row.directChildCount > 0

    if (row.depth <= 0) {
        // Top-level expanded folder: a short nub + drop that connects to the first
        // child's guide column so its vertical line doesn't float with a gap.
        if (expandedFolder) {
            Canvas(
                modifier = Modifier
                    .width(indentUnit)
                    .fillMaxHeight()
            ) {
                val midY = size.height / 2f
                val radius = corner.toPx()
                val stroke = strokeWidth.toPx()
                val x = size.width / 2f
                drawLine(
                    color = guideColor,
                    start = Offset(0f, midY),
                    end = Offset(x + radius, midY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = guideColor,
                    start = Offset(x, midY),
                    end = Offset(x, size.height),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier.height(50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (level in 0 until row.depth) {
            val continues = row.ancestorLines.getOrElse(level) { false }

            Box(
                modifier = Modifier
                    .width(indentUnit)
                    .fillMaxHeight()
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val x = size.width / 2f
                    val midY = size.height / 2f
                    val stroke = strokeWidth.toPx()
                    val radius = corner.toPx()

                    if (level < row.depth - 1) {
                        if (continues) {
                            drawLine(
                                color = guideColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }
                    } else {
                        val elbow = Path().apply {
                            moveTo(x, 0f)
                            lineTo(x, midY - radius)
                            quadraticBezierTo(x, midY, x + radius, midY)
                            if (expandedFolder) {
                                // Reach the first child's stub column so the drop connects.
                                lineTo(x + indentUnit.toPx() + radius, midY)
                            } else {
                                lineTo(size.width + extensionPx, midY)
                            }
                        }

                        drawPath(
                            elbow,
                            color = guideColor,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )

                        if (expandedFolder) {
                            val dropX = x + indentUnit.toPx()
                            drawLine(
                                color = guideColor,
                                start = Offset(dropX, midY),
                                end = Offset(dropX, size.height),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }

                        if (continues) {
                            drawLine(
                                color = guideColor,
                                start = Offset(x, midY),
                                end = Offset(x, size.height),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

/** PRD §24 Delete Protection — a stronger warning for files that can break the build. */
@Composable
private fun DeleteConfirmDialog(entry: BrowserEntry, isDeleting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val isCritical = entry.name in CRITICAL_FILE_NAMES
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text(if (isCritical) "Critical Project File" else if (entry.isFolder) "Delete folder?" else "Delete file?") },
        text = {
            Text(
                if (isCritical) {
                    "\"${entry.name}\" is a critical project file. Deleting this file may break your build."
                } else {
                    "\"${entry.name}\" ${if (entry.isFolder) "and everything inside it" else ""} will be removed from GitHub. This can't be undone from within Git Way."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                Text(if (isCritical) "Delete Anyway" else "Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) { Text(if (isCritical) "Keep File" else "Cancel") }
        }
    )
}

@Composable
private fun RenameDialog(
    currentName: String,
    isRenaming: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onRenamed: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(isRenaming, errorMessage) {
        if (submitted && !isRenaming && errorMessage == null) onRenamed()
    }

    AlertDialog(
        onDismissRequest = { if (!isRenaming) onDismiss() },
        title = { Text("Rename") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    enabled = !isRenaming,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { submitted = true; onConfirm(name) },
                enabled = name.isNotBlank() && name != currentName && !isRenaming
            ) {
                if (isRenaming) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Rename")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isRenaming) { Text("Cancel") } }
    )
}

/** PRD §19 Sort / View. */
@Composable
private fun SortFilterDialog(
    sortMode: BrowserSortMode,
    typeFilter: BrowserTypeFilter,
    onSortModeChange: (BrowserSortMode) -> Unit,
    onTypeFilterChange: (BrowserTypeFilter) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort & Filter") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text("Sort by", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
                BrowserSortMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onSortModeChange(mode) }
                    ) {
                        RadioButton(selected = sortMode == mode, onClick = { onSortModeChange(mode) })
                        Text(mode.label)
                    }
                }
                Text("Filter by type", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrowserTypeFilter.entries.take(3).forEach { filter ->
                        FilterChip(selected = typeFilter == filter, onClick = { onTypeFilterChange(filter) }, label = { Text(filter.label) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrowserTypeFilter.entries.drop(3).forEach { filter ->
                        FilterChip(selected = typeFilter == filter, onClick = { onTypeFilterChange(filter) }, label = { Text(filter.label) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

/** Lists every branch GitHub knows about for this repo, radio-select style — whichever
 * one is picked becomes the target for every read/write in this repo session (the tree
 * view, the commit history, and every push) until changed again or the repo is reopened. */
@Composable
private fun BranchPickerDialog(
    branches: List<String>,
    defaultBranch: String,
    selectedBranch: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onSelect: (String?) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val effectiveSelected = selectedBranch ?: defaultBranch
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Switch branch") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (isLoading && branches.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    branches.forEach { branch ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(if (branch == defaultBranch) null else branch) }
                        ) {
                            RadioButton(selected = branch == effectiveSelected, onClick = { onSelect(if (branch == defaultBranch) null else branch) })
                            Text(branch)
                            if (branch == defaultBranch) {
                                Text(
                                    "  (default)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
                TextButton(onClick = onCreateNew, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Create new branch", modifier = Modifier.padding(start = 4.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/** Forks a brand-new branch off whichever one is currently active — a real ref pointing
 * at that branch's current tip, never an empty one. */
@Composable
private fun CreateBranchDialog(
    sourceBranch: String,
    isCreating: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(isCreating, errorMessage) {
        if (submitted && !isCreating && errorMessage == null) onCreated()
    }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Create branch") },
        text = {
            Column {
                Text(
                    "Forks from \"$sourceBranch\" as it is right now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("New branch name") },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { submitted = true; onConfirm(name) }, enabled = name.isNotBlank() && !isCreating) {
                if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel") } }
    )
}

/** Read-only commit log for whichever branch is currently active. Git Way only ever
 *  writes new commits through the tested sync pipeline elsewhere in the app — this is
 *  purely "what's already there", each row opening straight to that commit on GitHub. */
@Composable
private fun CommitHistoryDialog(
    commits: List<CommitSummary>,
    isLoading: Boolean,
    errorMessage: String?,
    onOpenCommit: (String) -> Unit,
    onViewDiff: (CommitSummary) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commit history") },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                when {
                    isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                    errorMessage != null -> Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    commits.isEmpty() -> Text("No commits yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> commits.forEach { commit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCommit(commit.htmlUrl) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(commit.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${commit.authorName} · ${formatRelativeTime(commit.date)} · ${commit.shortSha}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            TextButton(onClick = { onViewDiff(commit) }) { Text("Diff") }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/** Per-file unified diff of a single commit — green/red highlighted lines from the
 *  raw patch text GitHub returns. */
@Composable
private fun CommitDiffDialog(
    files: List<com.io.git.way.domain.model.CommitDiffFile>,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commit diff") },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                when {
                    isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                    errorMessage != null -> Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    files.isEmpty() -> Text("No file changes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> files.forEach { file ->
                        Text(
                            "${file.filename}  (+${file.additions} −${file.deletions})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        file.patch?.let { patch ->
                            patch.split("\n").forEach { line ->
                                val color = when {
                                    line.startsWith("+") && !line.startsWith("+++") -> Color(0xFF16A34A)
                                    line.startsWith("-") && !line.startsWith("---") -> MaterialTheme.colorScheme.error
                                    line.startsWith("@@") -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Text(
                                    line,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = color
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/** The repository itself, not its files: rename via GitHub's repo-settings endpoints. */
@Composable
private fun RepositorySettingsDialog(
    repo: GitRepository,
    isSaving: Boolean,
    saveError: String?,
    onSave: (newName: String?, newDescription: String?, newIsPrivate: Boolean?) -> Unit,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(repo.name) }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(isSaving, saveError) {
        if (submitted && !isSaving && saveError == null) onSaved()
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Rename repository") },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Repository name") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                if (saveError != null) {
                    Text(saveError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    submitted = true
                    onSave(
                        name.trim().takeIf { it.isNotBlank() && it != repo.name },
                        null,
                        null
                    )
                },
                enabled = !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Close") } }
    )
}

/** Confirmation dialog for permanently deleting the repo, reached from the top-bar
 * actions menu. Requires typing the repo name, same safeguard as the old settings
 * dialog's danger zone. */
@Composable
private fun DeleteRepositoryDialog(
    repo: GitRepository,
    isDeleting: Boolean,
    deleteError: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var deleteConfirmText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text("Delete repository") },
        text = {
            Column {
                Text(
                    "Deleting \"${repo.name}\" is permanent and cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Type \"${repo.name}\" to confirm:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                )
                OutlinedTextField(
                    value = deleteConfirmText,
                    onValueChange = { deleteConfirmText = it },
                    singleLine = true,
                    enabled = !isDeleting,
                    modifier = Modifier.fillMaxWidth()
                )
                if (deleteError != null) {
                    Text(deleteError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = deleteConfirmText == repo.name && !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Permanently delete", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isDeleting) { Text("Cancel") } }
    )
}

/** Read/edit view for a single file, shown in place of the tree — syntax-highlighted and
 * properly scrollable in both read and edit modes. Markdown files (README.md and friends)
 * also get a rendered Preview mode — headings, bold/italic, links, and images resolved
 * against this repo — alongside the raw source, toggled with a segmented control. */
@Composable
private fun FileViewerScreen(sessionViewModel: GitWaySessionViewModel) {
    val state = sessionViewModel.state
    val entry = state.viewingFile ?: return
    var draft by remember(state.viewingContent) { mutableStateOf(state.viewingContent.orEmpty()) }
    val isMarkdown = entry.name.endsWith(".md", ignoreCase = true) || entry.name.endsWith(".markdown", ignoreCase = true)
    var showPreview by remember(entry.path) { mutableStateOf(isMarkdown) }
    val isImage = state.viewingImageBytes != null

    GlassScaffold(
        title = entry.name,
        navigationIcon = {
            IconButton(onClick = {
                if (state.isEditingFile) sessionViewModel.cancelEditingFile() else sessionViewModel.closeFileViewer()
            }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (!isImage && state.viewingContent != null && state.viewerError == null) {
                if (state.isEditingFile) {
                    TextButton(onClick = { sessionViewModel.saveFileEdits(draft) }, enabled = !state.isSavingFile) {
                        if (state.isSavingFile) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Save")
                    }
                } else {
                    IconButton(onClick = { sessionViewModel.startEditingFile() }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(entry.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))

            if (state.saveFileError != null) {
                Text(state.saveFileError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
            }

            if (isMarkdown && !isImage && !state.isEditingFile && state.viewingContent != null && state.viewerError == null) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(bottom = 10.dp)) {
                    SegmentedButton(
                        selected = showPreview,
                        onClick = { showPreview = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Preview") }
                    SegmentedButton(
                        selected = !showPreview,
                        onClick = { showPreview = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Raw") }
                }
            }

            when {
                state.isLoadingContent -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.viewerError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.viewerError, color = MaterialTheme.colorScheme.error)
                }
                isImage -> ZoomableImage(
                    bytes = state.viewingImageBytes!!,
                    contentDescription = entry.name,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                state.isEditingFile -> CodeEditor(
                    value = draft,
                    onValueChange = { draft = it },
                    readOnly = false,
                    enabled = !state.isSavingFile,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                isMarkdown && showPreview -> {
                    val repo = state.selectedRepo
                    if (repo != null) {
                        MarkdownView(
                            markdown = state.viewingContent.orEmpty(),
                            resolver = MarkdownLinkResolver(
                                owner = repo.owner,
                                repo = repo.name,
                                branch = repo.defaultBranch,
                                currentFileDir = entry.path.substringBeforeLast('/', "")
                            ),
                            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        )
                    }
                }
                else -> CodeEditor(
                    value = state.viewingContent.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    enabled = true,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}

/** Pinch-to-zoom + pan image preview — plain [android.graphics.BitmapFactory] decode of
 * whatever bytes were already fetched for the viewer, no extra network round trip. */
@Composable
private fun ZoomableImage(bytes: ByteArray, contentDescription: String, modifier: Modifier = Modifier) {
    val bitmap = remember(bytes) {
        runCatching { android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
    }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    if (bitmap == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Couldn't decode this image.", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    offset = if (scale <= 1f) Offset.Zero else offset + pan
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit
        )
    }
}

/** A minimal code editor: a line-number gutter kept in sync with the content via a
 * shared vertical scroll state, syntax highlighting, and pinch-to-zoom font scaling
 * (two-finger gesture — the same interaction as [ZoomableImage]). [readOnly] renders
 * plain highlighted [Text] for the viewer; otherwise a borderless [BasicTextField] so
 * the gutter and the text share identical line metrics (an [OutlinedTextField]'s
 * internal padding doesn't line up with an external gutter the same way). */
@Composable
private fun CodeEditor(
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var fontScale by remember { mutableStateOf(1f) }
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val fontSize = (13 * fontScale).sp
    val lineHeight = (19 * fontScale).sp
    val lineCount = remember(value) { value.count { it == '\n' } + 1 }
    val gutterColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Row(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(10.dp)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    fontScale = (fontScale * zoom).coerceIn(0.6f, 2.5f)
                }
            }
    ) {
        Text(
            text = (1..lineCount).joinToString("\n") { it.toString() },
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize,
                lineHeight = lineHeight,
                textAlign = TextAlign.End,
                color = gutterColor
            ),
            softWrap = false,
            modifier = Modifier
                .verticalScroll(verticalScroll)
                .widthIn(min = 26.dp)
                .padding(end = 10.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll)
        ) {
            if (readOnly) {
                Text(
                    SyntaxHighlighter.highlight(value),
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = fontSize, lineHeight = lineHeight),
                    softWrap = false
                )
            } else {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        color = LocalContentColor.current
                    ),
                    visualTransformation = SyntaxHighlightTransformation(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun NewEntryDialog(
    title: String,
    label: String,
    isCreating: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(isCreating, errorMessage) {
        if (submitted && !isCreating && errorMessage == null) onCreated()
    }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(label) },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { submitted = true; onConfirm(name) }, enabled = name.isNotBlank() && !isCreating) {
                if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel") } }
    )
}

/** Lets the user tick exactly which CI workflow template(s) to add — each option shows
 * its full YAML so nothing is a surprise before it lands as a commit. Manual opt-in only:
 * there's no "add all" shortcut and no default selection. */
@Composable
private fun WorkflowPickerDialog(
    isAdding: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (List<WorkflowTemplate>) -> Unit,
    onAdded: () -> Unit
) {
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(isAdding, errorMessage) {
        if (submitted && !isAdding && errorMessage == null) onAdded()
    }

    AlertDialog(
        onDismissRequest = { if (!isAdding) onDismiss() },
        title = { Text("Add CI workflow") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                WorkflowTemplates.all.forEach { template ->
                    val isSelected = template.id in selected
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + template.id else selected - template.id
                                },
                                enabled = !isAdding
                            )
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text(template.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    template.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    template.path,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 40.dp, top = 4.dp)
                        ) {
                            Text(
                                SyntaxHighlighter.highlight(template.yaml),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    submitted = true
                    onConfirm(WorkflowTemplates.all.filter { it.id in selected })
                },
                enabled = selected.isNotEmpty() && !isAdding
            ) {
                if (isAdding) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Add selected")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isAdding) { Text("Cancel") } }
    )
}
