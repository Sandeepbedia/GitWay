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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Merge
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
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
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
import com.io.git.way.ui.common.showGitWayToast
import com.io.git.way.ui.common.MarkdownLinkResolver
import com.io.git.way.ui.common.MarkdownView
import com.io.git.way.ui.common.PendingDownloadHandler
import com.io.git.way.ui.common.RevealStyle
import com.io.git.way.ui.common.TreeSearchQuery
import com.io.git.way.ui.common.compareTreeResults
import com.io.git.way.ui.common.matchTreeName
import com.io.git.way.ui.common.rememberReveal
import com.io.git.way.ui.common.revealIn
import com.io.git.way.ui.common.SyntaxHighlightTransformation
import com.io.git.way.ui.common.SyntaxHighlighter
import com.io.git.way.ui.common.formatRelativeTime
import com.io.git.way.ui.theme.DiffAddedGreen
import com.io.git.way.ui.theme.GlassBlobBlue
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassScaffold
import com.io.git.way.ui.theme.GlassSecondaryButton

private val CRITICAL_FILE_NAMES = setOf(
    "AndroidManifest.xml", "build.gradle.kts", "build.gradle",
    "settings.gradle.kts", "settings.gradle", "proguard-rules.pro"
)

// Geometry shared by the tree rows and their thread lines.
private const val TREE_START_DP = 16
private const val TREE_INDENT_DP = 20
private const val TREE_ICON_HALF_DP = 12

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
    var explorerZoom by remember { mutableStateOf(1f) }
    val explorerHorizontalScroll = rememberScrollState()
    var rootExpanded by remember { mutableStateOf(true) }
    var showBranchPicker by remember { mutableStateOf(false) }
    var showCreateBranchDialog by remember { mutableStateOf(false) }
    var showCommitHistory by remember { mutableStateOf(false) }
    var showRepoSettings by remember { mutableStateOf(false) }
    var showDeleteRepoDialog by remember { mutableStateOf(false) }

    // System back unwinds in-app layers one at a time (editor → file viewer →
    // selection → search) before it pops the whole browser destination.
    BackHandler(
        enabled = state.viewingFile != null ||
            state.selectedBrowserPaths.isNotEmpty() ||
            browserQuery.isNotBlank()
    ) {
        when {
            state.viewingFile != null -> {
                if (state.isEditingFile) {
                    sessionViewModel.cancelEditingFile()
                } else {
                    sessionViewModel.closeFileViewer()
                }
            }
            state.selectedBrowserPaths.isNotEmpty() -> sessionViewModel.clearBrowserSelection()
            else -> browserQuery = ""
        }
    }

    val uploadFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) sessionViewModel.uploadPickedFiles(context, uris)
    }

    LaunchedEffect(state.selectedRepo) {
        if (state.selectedRepo != null && state.remoteTreeCache.isEmpty() && !state.isBrowserLoading) {
            sessionViewModel.loadBrowserRoot()
        }
        if (state.selectedRepo != null && state.availableBranches.isEmpty() && !state.isLoadingBranches) {
            sessionViewModel.loadBranches()
        }
    }

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
                        DropdownMenuItem(
                            text = { Text("Upload file(s)") },
                            onClick = {
                                showCreateMenu = false
                                uploadFilesLauncher.launch(arrayOf("*/*"))
                            },
                            leadingIcon = { Icon(Icons.Filled.UploadFile, contentDescription = null) }
                        )
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
                                        showGitWayToast(
                                            context,
                                            if (targetPrivate) "Repository is now private" else "Repository is now public"
                                        )
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
            if (state.uploadFilesError != null) {
                Text(state.uploadFilesError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
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

            PullToRefreshBox(
                isRefreshing = state.isBrowserLoading && state.remoteTreeCache.isNotEmpty(),
                onRefresh = {
                    sessionViewModel.loadBrowserRoot(collapseFolders = true)
                    sessionViewModel.loadBranches()
                },
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                val searchResults = remember(browserQuery, state.remoteTreeCache) {
                    if (browserQuery.isBlank()) {
                        emptyList()
                    } else {
                        val query = TreeSearchQuery.parse(browserQuery)
                        state.remoteTreeCache.keys
                            .asSequence()
                            .filter { !it.endsWith("/.gitkeep") && it != ".gitkeep" }
                            .mapNotNull { path ->
                                val name = path.substringAfterLast('/')
                                if (query.extensions.isNotEmpty() &&
                                    name.substringAfterLast('.', "").lowercase() !in query.extensions
                                ) {
                                    return@mapNotNull null
                                }
                                val match = matchTreeName(name, query.needle) ?: return@mapNotNull null
                                Triple(match.score, name, path)
                            }
                            .sortedWith(::compareTreeResults)
                            .map { it.third }
                            .toList()
                    }
                }

                when {
                    state.isBrowserLoading && state.remoteTreeCache.isEmpty() -> ExplorerSkeleton()

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

                    else -> {
                        val displayDepths = remember(state.browserEntries) {
                            listOf(0) + state.browserEntries.map { it.depth + 1 }
                        }
                        val treeGuides = remember(displayDepths) { TreeGuides.masks(displayDepths) }
                        val longestExplorerText = state.browserEntries
                            .maxOfOrNull { entry ->
                                val depth = entry.entry.path.count { it == '/' }
                                entry.entry.name.length + (depth * 4)
                            } ?: 0
                        val contentWidth = (
                            760.dp + (longestExplorerText.coerceAtLeast(32) * 9).dp
                        ).coerceAtLeast(980.dp) * explorerZoom

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(explorerHorizontalScroll)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                            var event = awaitPointerEvent()
                                            while (event.changes.count { it.pressed } < 2) {
                                                if (event.changes.none { it.pressed }) return@awaitEachGesture
                                                event = awaitPointerEvent()
                                            }

                                            var previousDistance =
                                                (event.changes[0].position - event.changes[1].position).getDistance()

                                            while (event.changes.any { it.pressed }) {
                                                val active = event.changes.filter { it.pressed }
                                                if (active.size >= 2) {
                                                    val distance =
                                                        (active[0].position - active[1].position).getDistance()
                                                    if (previousDistance > 0f && distance > 0f) {
                                                        val zoom = distance / previousDistance
                                                        if (kotlin.math.abs(zoom - 1f) > 0.002f) {
                                                            explorerZoom =
                                                                (explorerZoom * zoom).coerceIn(0.75f, 1.8f)
                                                        }
                                                    }
                                                    previousDistance = distance
                                                    active.forEach { it.consume() }
                                                }
                                                event = awaitPointerEvent()
                                            }
                                    }
                                }
                        ) {
                            val treeListState = rememberLazyListState()
                            val treeReveal = rememberReveal()
                            val firstRowAtOpen = remember { treeListState.firstVisibleItemIndex }
                            var knownRowPaths by remember(treeReveal) {
                                mutableStateOf<Set<String>>(
                                    state.browserEntries.mapTo(mutableSetOf()) { it.entry.path }
                                )
                            }
                            val firstNewRowIndex = remember(state.browserEntries, knownRowPaths) {
                                state.browserEntries.indexOfFirst { it.entry.path !in knownRowPaths }
                            }
                            LaunchedEffect(state.browserEntries) {
                                val newPaths = state.browserEntries.mapTo(mutableSetOf()) { it.entry.path }
                                for (goneRowPath in knownRowPaths - newPaths) treeReveal.played.remove(goneRowPath)
                                knownRowPaths = newPaths
                            }
                            LazyColumn(
                                state = treeListState,
                                modifier = Modifier.width(contentWidth)
                            ) {
                                item(key = "repository-root") {
                                    RepositoryRootRow(
                                        repositoryName = repo?.name ?: "Repository",
                                        itemCount = state.browserEntries.size,
                                        zoomScale = explorerZoom,
                                        onToggleExpanded = { rootExpanded = !rootExpanded },
                                        onNewFile = { sessionViewModel.setCreateTarget(""); showNewFileDialog = true },
                                        onNewFolder = { sessionViewModel.setCreateTarget(""); showNewFolderDialog = true },
                                        onUploadInto = {
                                            sessionViewModel.setCreateTarget("")
                                            uploadFilesLauncher.launch(arrayOf("*/*"))
                                        }
                                    )
                                }
                                if (rootExpanded) {
                                itemsIndexed(state.browserEntries, key = { _, row -> row.entry.path }) { index, row ->
                                    val justRevealed = row.entry.path !in knownRowPaths
                                    val entranceOrder = remember(row.entry.path) {
                                        if (justRevealed && firstNewRowIndex >= 0) index - firstNewRowIndex
                                        else (index - firstRowAtOpen).coerceAtLeast(0)
                                    }
                                    DisposableEffect(row.entry.path) {
                                        onDispose { treeReveal.played.remove(row.entry.path) }
                                    }
                                    Box(
                                        Modifier
                                            .animateItem()
                                            .revealIn(treeReveal, row.entry.path, entranceOrder, RevealStyle.Row)
                                    ) {
                                    TreeRowItem(
                                        row = row,
                                        selectionMode = selectionMode,
                                        zoomScale = explorerZoom,
                                        guideMask = treeGuides[index + 1],
                                        depth = row.depth + 1,
                                selected = row.entry.path in state.selectedBrowserPaths,
                                onClick = {
                                    when {
                                        selectionMode && !row.entry.isFolder -> sessionViewModel.toggleBrowserSelection(row.entry)
                                        row.entry.isFolder -> sessionViewModel.toggleFolderExpanded(row.entry)
                                        else -> sessionViewModel.openFile(row.entry)
                                    }
                                },
                                onDelete = { pendingDelete = row.entry },
                                onRename = { pendingRename = row.entry },
                                onDuplicate = { sessionViewModel.duplicateEntry(row.entry) },
                                onNewFile = { sessionViewModel.setCreateTarget(row.entry.path); showNewFileDialog = true },
                                onNewFolder = { sessionViewModel.setCreateTarget(row.entry.path); showNewFolderDialog = true },
                                onUploadInto = {
                                    sessionViewModel.setCreateTarget(row.entry.path)
                                    uploadFilesLauncher.launch(arrayOf("*/*"))
                                },
                                onRefresh = { sessionViewModel.loadBrowserRoot(collapseFolders = true) },
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
                            }
                        }
                    }
                }
                if (state.isDeletingEntry || state.isRenamingEntry || state.isDuplicatingEntry || state.isUploadingFiles) {
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

/** PRD §22 Empty State. Wrapped in a LazyColumn so pull-to-refresh still works here. */
@Composable
private fun EmptyFolderState(onNewFile: () -> Unit, onUpload: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultRow(path: String, onClick: () -> Unit) {
    val name = path.substringAfterLast('/')
    GlassCard(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = {}), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val iconRes = FileTypeIcons.iconResFor(name)
            if (iconRes != null) {
                SvgRawIcon(resId = iconRes, contentDescription = null, modifier = Modifier.size(22.dp))
            } else {
                Icon(FileTypeIcons.iconFor(name), contentDescription = null, tint = FileTypeIcons.colorFor(name), modifier = Modifier.size(22.dp))
            }
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
    itemCount: Int,
    zoomScale: Float = 1f,
    onToggleExpanded: () -> Unit = {},
    onNewFile: () -> Unit = {},
    onNewFolder: () -> Unit = {},
    onUploadInto: () -> Unit = {}
) {
    val hasChildren = itemCount > 0

    val uiScale = zoomScale.coerceIn(0.75f, 1.8f)
    val rowHeight = 52.dp * uiScale

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = hasChildren, onClick = onToggleExpanded)
            .padding(start = TREE_START_DP.dp * uiScale, end = 16.dp * uiScale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SvgRawIcon(
            resId = com.io.git.way.R.raw.folder_root,
            contentDescription = "Repository root",
            modifier = Modifier.size(24.dp * uiScale)
        )
        Text(
            repositoryName,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = MaterialTheme.typography.titleMedium.fontSize * uiScale,
                lineHeight = MaterialTheme.typography.titleMedium.lineHeight * uiScale
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier.padding(start = 10.dp * uiScale).widthIn(min = 40.dp * uiScale)
        )
        IconButton(onClick = onNewFile, modifier = Modifier.size(28.dp * uiScale)) {
            Icon(
                Icons.Filled.NoteAdd,
                contentDescription = "New file in root",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp * uiScale)
            )
        }
        IconButton(onClick = onNewFolder, modifier = Modifier.size(28.dp * uiScale)) {
            Icon(
                Icons.Filled.CreateNewFolder,
                contentDescription = "New folder in root",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp * uiScale)
            )
        }
        IconButton(onClick = onUploadInto, modifier = Modifier.size(28.dp * uiScale)) {
            Icon(
                Icons.Filled.UploadFile,
                contentDescription = "Upload files to root",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp * uiScale)
            )
        }
        Spacer(Modifier.width(4.dp * uiScale))
        Text(
            itemCount.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = MaterialTheme.typography.labelMedium.fontSize * uiScale,
                lineHeight = MaterialTheme.typography.labelMedium.lineHeight * uiScale
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private object SvgTreeIconLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: android.content.Context): ImageLoader = instance ?: synchronized(this) {
        instance ?: ImageLoader.Builder(context.applicationContext)
            .components { add(SvgDecoder.Factory()) }
            .build()
            .also { instance = it }
    }
}

@Composable
fun SvgRawIcon(
    resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = resId,
        imageLoader = SvgTreeIconLoader.get(context)
    )

    Image(
        painter = painter,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
private fun SvgFolderIcon(
    open: Boolean,
    modifier: Modifier = Modifier
) {
    SvgRawIcon(
        resId = if (open) com.io.git.way.R.raw.folder_open else com.io.git.way.R.raw.folder_closed,
        contentDescription = if (open) "Open folder" else "Closed folder",
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreeRowItem(
    row: TreeRow,
    selectionMode: Boolean,
    zoomScale: Float = 1f,
    guideMask: Long,
    depth: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onNewFile: () -> Unit,
    onNewFolder: () -> Unit,
    onUploadInto: () -> Unit,
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
    val guideColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f)

    // Fixed height keeps every guide row aligned to the same vertical rhythm.
    val uiScale = zoomScale.coerceIn(0.75f, 1.8f)
    val rowHeight = 50.dp * uiScale

    // Hold-click on the row opens the actions menu — a tap always just opens the item.
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(rowBackground)
                .drawBehind { drawTreeThreads(guideMask, depth, guideColor, uiScale) }
                .combinedClickable(onClick = onClick, onLongClick = { showMenu = true })
                .padding(
                    start = (TREE_START_DP + depth * TREE_INDENT_DP).dp * uiScale,
                    end = 2.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode && !entry.isFolder) {
                Icon(
                    if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (selected) "Selected" else "Not selected",
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp * uiScale)
                )
                Spacer(Modifier.width(6.dp))
            }

            Box(
                modifier = Modifier.size(24.dp * uiScale),
                contentAlignment = Alignment.Center
            ) {
                if (entry.isFolder) {
                    SvgFolderIcon(
                        open = row.isExpanded,
                        modifier = Modifier.size(24.dp * uiScale)
                    )
                } else {
                    val iconRes = FileTypeIcons.iconResFor(entry.name)
                    if (iconRes != null) {
                        SvgRawIcon(
                            resId = iconRes,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp * uiScale)
                        )
                    } else {
                        Icon(
                            FileTypeIcons.iconFor(entry.name),
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp * uiScale)
                        )
                    }
                }
            }

            Text(
                entry.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp * uiScale, lineHeight = 20.sp * uiScale),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier = Modifier
                    .padding(start = 9.dp * uiScale)
                    .widthIn(min = 40.dp * uiScale)
            )

            if (entry.isFolder && !selectionMode) {
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onNewFile,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.NoteAdd,
                        contentDescription = "New file",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onNewFolder,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.CreateNewFolder,
                        contentDescription = "New folder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onUploadInto,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.UploadFile,
                        contentDescription = "Upload files here",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

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
                }
            }
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
                    leadingIcon = { Icon(Icons.Filled.UploadFile, null) },
                    text = { Text("Upload here") },
                    onClick = { showMenu = false; onUploadInto() }
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
}

/** Thread lines of one tree row: vertical lines of the ancestors, plus a rounded elbow (last child) or a branch. */
private fun DrawScope.drawTreeThreads(mask: Long, depth: Int, color: Color, scale: Float) {
    if (depth < 1) return
    // Every tree dimension scales with the explorer zoom so the thread never
    // drifts away from the icon it belongs to.
    val start = TREE_START_DP.dp.toPx() * scale
    val unit = TREE_INDENT_DP.dp.toPx() * scale
    val iconHalf = TREE_ICON_HALF_DP.dp.toPx() * scale
    val stroke = (1.dp.toPx() * scale).coerceAtLeast(1f)
    val radius = 8.dp.toPx() * scale
    fun x(level: Int) = start + (level - 1) * unit + iconHalf
    for (level in 1 until depth) {
        if (TreeGuides.continues(mask, level)) {
            drawLine(color, Offset(x(level), 0f), Offset(x(level), size.height), stroke)
        }
    }
    val ex = x(depth)
    val mid = size.height / 2f
    val iconStart = start + depth * unit
    if (TreeGuides.continues(mask, depth)) {
        drawLine(color, Offset(ex, 0f), Offset(ex, size.height), stroke)
        drawLine(color, Offset(ex, mid), Offset(iconStart, mid), stroke)
    } else {
        // Last child: the thread bends into the row with a rounded corner.
        drawLine(color, Offset(ex, 0f), Offset(ex, mid - radius), stroke)
        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(ex, mid - 2 * radius),
            size = Size(2 * radius, 2 * radius),
            style = Stroke(width = stroke)
        )
        drawLine(color, Offset(ex + radius, mid), Offset(iconStart, mid), stroke)
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
                                    line.startsWith("+") && !line.startsWith("+++") -> DiffAddedGreen
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
