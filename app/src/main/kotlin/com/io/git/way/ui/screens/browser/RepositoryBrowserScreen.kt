package com.io.git.way.ui.screens.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.io.git.way.domain.model.BrowserEntry
import com.io.git.way.ui.common.FileTypeIcons
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.theme.GlassBlobBlue
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassScaffold
import com.io.git.way.ui.theme.GlassSecondaryButton

/**
 * File-manager view of the selected GitHub repository: browse folders, see files with
 * type-aware icons, select/copy/paste files between folders, and read or edit a file's
 * content in place. Every write (create/paste/edit) lands as its own small commit via the
 * same tested sync pipeline used for the main upload flow.
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

    LaunchedEffect(state.selectedRepo) {
        if (state.selectedRepo != null && state.remoteTreeCache.isEmpty() && !state.isBrowserLoading) {
            sessionViewModel.loadBrowserRoot()
        }
    }

    // The file viewer/editor replaces the browser list in place, so back/close behaviour
    // stays intuitive without introducing a second nav-graph destination.
    if (state.viewingFile != null) {
        FileViewerScreen(sessionViewModel = sessionViewModel)
        return
    }

    val selectionMode = state.selectedBrowserPaths.isNotEmpty()
    val title = if (selectionMode) {
        "${state.selectedBrowserPaths.size} selected"
    } else {
        state.browserPath.substringAfterLast("/").ifEmpty { state.selectedRepo?.name ?: "Repository" }
    }

    GlassScaffold(
        title = title,
        navigationIcon = {
            IconButton(
                onClick = {
                    when {
                        selectionMode -> sessionViewModel.clearBrowserSelection()
                        state.browserPath.isNotEmpty() -> sessionViewModel.navigateUp()
                        else -> onBack()
                    }
                }
            ) {
                Icon(
                    if (selectionMode) Icons.Filled.Close else Icons.Filled.ArrowBack,
                    contentDescription = if (selectionMode) "Cancel selection" else "Back"
                )
            }
        },
        actions = {
            if (selectionMode) {
                IconButton(onClick = { sessionViewModel.copySelectionToClipboard() }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy selected")
                }
            } else {
                if (state.clipboard.isNotEmpty()) {
                    IconButton(
                        onClick = { sessionViewModel.pasteClipboardHere() },
                        enabled = !state.isPasting
                    ) {
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
                        DropdownMenuItem(
                            text = { Text("New file") },
                            onClick = { showCreateMenu = false; showNewFileDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("New folder") },
                            onClick = { showCreateMenu = false; showNewFolderDialog = true }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (state.browserPath.isNotEmpty() && !selectionMode) {
                BreadcrumbRow(path = state.browserPath, onCrumbClick = sessionViewModel::navigateToBreadcrumb)
            }
            if (state.pasteError != null) {
                Text(
                    state.pasteError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    state.isBrowserLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    state.browserError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.browserError, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { sessionViewModel.loadBrowserRoot() }) { Text("Retry") }
                        }
                    }

                    state.browserEntries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("This folder is empty.")
                    }

                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.browserEntries, key = { it.path }) { entry ->
                            BrowserRow(
                                entry = entry,
                                selectionMode = selectionMode,
                                selected = entry.path in state.selectedBrowserPaths,
                                onClick = {
                                    when {
                                        selectionMode -> sessionViewModel.toggleBrowserSelection(entry)
                                        entry.isFolder -> sessionViewModel.navigateInto(entry)
                                        else -> sessionViewModel.openFile(entry)
                                    }
                                },
                                onLongClick = { sessionViewModel.toggleBrowserSelection(entry) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            GlassSecondaryButton(
                text = "Sync updated project from device",
                onClick = onSyncFromDevice,
                leadingIcon = { Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }

    if (showNewFileDialog) {
        NewEntryDialog(
            title = "New file",
            label = "File name (e.g. notes.md)",
            isCreating = state.isCreatingEntry,
            errorMessage = state.createEntryError,
            onDismiss = { showNewFileDialog = false; sessionViewModel.clearCreateEntryError() },
            onConfirm = { name -> sessionViewModel.createFile(name) },
            onCreated = { showNewFileDialog = false }
        )
    }
    if (showNewFolderDialog) {
        NewEntryDialog(
            title = "New folder",
            label = "Folder name",
            isCreating = state.isCreatingEntry,
            errorMessage = state.createEntryError,
            onDismiss = { showNewFolderDialog = false; sessionViewModel.clearCreateEntryError() },
            onConfirm = { name -> sessionViewModel.createFolder(name) },
            onCreated = { showNewFolderDialog = false }
        )
    }
}

@Composable
private fun BreadcrumbRow(path: String, onCrumbClick: (String) -> Unit) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "root",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onCrumbClick("") }
        )
        var accumulated = ""
        path.split("/").forEach { segment ->
            accumulated = if (accumulated.isEmpty()) segment else "$accumulated/$segment"
            val target = accumulated
            Text("  /  ", style = MaterialTheme.typography.labelMedium)
            Text(
                segment,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onCrumbClick(target) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserRow(
    entry: BrowserEntry,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { if (!entry.isFolder) onLongClick() }),
        padding = 12.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode && !entry.isFolder) {
                Icon(
                    if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (selected) "Selected" else "Not selected",
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.size(10.dp))
            }
            val icon = if (entry.isFolder) Icons.Filled.Folder else FileTypeIcons.iconFor(entry.name)
            val tint = if (entry.isFolder) GlassBlobBlue else FileTypeIcons.colorFor(entry.name)
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
            Text(
                entry.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp).weight(1f)
            )
            if (entry.isFolder) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Read/edit view for a single file, shown in place of the browser list. */
@Composable
private fun FileViewerScreen(sessionViewModel: GitWaySessionViewModel) {
    val state = sessionViewModel.state
    val entry = state.viewingFile ?: return
    var draft by remember(state.viewingContent) { mutableStateOf(state.viewingContent.orEmpty()) }

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
                    TextButton(
                        onClick = { sessionViewModel.saveFileEdits(draft) },
                        enabled = !state.isSavingFile
                    ) {
                        if (state.isSavingFile) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save")
                        }
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
            Text(
                entry.path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (state.saveFileError != null) {
                Text(
                    state.saveFileError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            when {
                state.isLoadingContent -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.viewerError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.viewerError, color = MaterialTheme.colorScheme.error)
                }
                state.isEditingFile -> OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    enabled = !state.isSavingFile,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                )
                else -> Text(
                    state.viewingContent.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxSize()
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
        if (submitted && !isCreating && errorMessage == null) {
            onCreated()
        }
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
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { submitted = true; onConfirm(name) },
                enabled = name.isNotBlank() && !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel") }
        }
    )
}
