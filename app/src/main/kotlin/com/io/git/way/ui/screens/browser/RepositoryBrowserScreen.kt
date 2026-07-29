package com.io.git.way.ui.screens.browser

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.io.git.way.domain.WorkflowTemplate
import com.io.git.way.domain.WorkflowTemplates
import com.io.git.way.domain.model.BrowserEntry
import com.io.git.way.domain.model.TreeRow
import com.io.git.way.ui.common.FileTypeIcons
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.common.MarkdownLinkResolver
import com.io.git.way.ui.common.MarkdownView
import com.io.git.way.ui.common.SyntaxHighlightTransformation
import com.io.git.way.ui.common.SyntaxHighlighter
import com.io.git.way.ui.theme.GlassBlobBlue
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassScaffold
import com.io.git.way.ui.theme.GlassSecondaryButton

/**
 * VS Code-style Explorer view of the selected GitHub repository: an indented, expandable
 * tree (no drill-down navigation — folders open in place), type-aware file icons,
 * multi-select with copy/paste/delete, and a read/edit viewer with syntax highlighting.
 * Every write (create/paste/edit/delete) lands as its own small commit via the same
 * tested sync pipeline used for the main upload flow.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepositoryBrowserScreen(
    sessionViewModel: GitWaySessionViewModel,
    onBack: () -> Unit,
    onSyncFromDevice: () -> Unit
) {
    val state = sessionViewModel.state
    var showCreateMenu by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BrowserEntry?>(null) }
    var pendingDeleteSelection by remember { mutableStateOf(false) }
    var showWorkflowPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedRepo) {
        if (state.selectedRepo != null && state.remoteTreeCache.isEmpty() && !state.isBrowserLoading) {
            sessionViewModel.loadBrowserRoot()
        }
    }

    // The file viewer/editor replaces the tree in place, so back/close behaviour stays
    // intuitive without introducing a second nav-graph destination.
    if (state.viewingFile != null) {
        FileViewerScreen(sessionViewModel = sessionViewModel)
        return
    }

    val selectionMode = state.selectedBrowserPaths.isNotEmpty()
    val title = if (selectionMode) "${state.selectedBrowserPaths.size} selected" else state.selectedRepo?.name ?: "Repository"

    GlassScaffold(
        title = title,
        navigationIcon = {
            IconButton(onClick = { if (selectionMode) sessionViewModel.clearBrowserSelection() else onBack() }) {
                Icon(if (selectionMode) Icons.Filled.Close else Icons.Filled.ArrowBack, contentDescription = if (selectionMode) "Cancel selection" else "Back")
            }
        },
        actions = {
            if (selectionMode) {
                IconButton(onClick = { pendingDeleteSelection = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
                }
                IconButton(onClick = { sessionViewModel.copySelectionToClipboard() }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy selected")
                }
            } else {
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
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (!selectionMode) {
                Text(
                    "New files go into: ${state.browserPath.ifEmpty { "root" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            if (state.pasteError != null) {
                Text(state.pasteError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
            }
            if (state.deleteEntryError != null) {
                Text(state.deleteEntryError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
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
                when {
                    state.isBrowserLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

                    state.browserError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.browserError, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { sessionViewModel.loadBrowserRoot() }) { Text("Retry") }
                        }
                    }

                    state.browserEntries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("This repository is empty.")
                    }

                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                onDelete = { pendingDelete = row.entry }
                            )
                        }
                    }
                }
                if (state.isDeletingEntry) {
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
        AlertDialog(
            onDismissRequest = { if (!state.isDeletingEntry) pendingDelete = null },
            title = { Text(if (entry.isFolder) "Delete folder?" else "Delete file?") },
            text = { Text("\"${entry.name}\" ${if (entry.isFolder) "and everything inside it" else ""} will be removed from GitHub. This can't be undone from within Git Way.") },
            confirmButton = {
                TextButton(onClick = { sessionViewModel.deleteEntry(entry); pendingDelete = null }, enabled = !state.isDeletingEntry) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }, enabled = !state.isDeletingEntry) { Text("Cancel") } }
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreeRowItem(
    row: TreeRow,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val entry = row.entry

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        padding = 10.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width((row.depth * 16).dp))

            if (entry.isFolder) {
                Icon(
                    if (row.isExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            } else if (selectionMode) {
                Icon(
                    if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (selected) "Selected" else "Not selected",
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Spacer(Modifier.width(18.dp))
            }

            Spacer(Modifier.width(8.dp))
            val icon = if (entry.isFolder) Icons.Filled.Folder else FileTypeIcons.iconFor(entry.name)
            val tint = if (entry.isFolder) GlassBlobBlue else FileTypeIcons.colorFor(entry.name)
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Text(
                entry.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 10.dp).weight(1f)
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete ${entry.name}",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
    val scrollState = rememberScrollState()
    val isMarkdown = entry.name.endsWith(".md", ignoreCase = true) || entry.name.endsWith(".markdown", ignoreCase = true)
    var showPreview by remember(entry.path) { mutableStateOf(isMarkdown) }

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
            if (state.viewingContent != null && state.viewerError == null) {
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

            if (isMarkdown && !state.isEditingFile && state.viewingContent != null && state.viewerError == null) {
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
                state.isEditingFile -> OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    enabled = !state.isSavingFile,
                    visualTransformation = SyntaxHighlightTransformation(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
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
                            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)
                        )
                    }
                }
                else -> Text(
                    SyntaxHighlighter.highlight(state.viewingContent.orEmpty()),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)
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
