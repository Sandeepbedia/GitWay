package com.io.git.way.ui.screens.analysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.io.git.way.domain.model.ChangeType
import com.io.git.way.domain.model.FileChange
import com.io.git.way.ui.common.FileTypeIcons
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.theme.DiffAddedGreen
import com.io.git.way.ui.theme.DiffModifiedYellow
import com.io.git.way.ui.theme.DiffRemovedRed
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassPrimaryButton
import com.io.git.way.ui.theme.GlassScaffold

/** Screen 5: Added / Modified / Removed diff preview, with manual + select-all controls
 * over exactly which changes get pushed (PRD1 §3.4 Analysis Screen). */
@Composable
fun AnalysisScreen(
    sessionViewModel: GitWaySessionViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val state = sessionViewModel.state
    val allChanges = state.fileChanges
    val hasChanges = allChanges.isNotEmpty()
    var query by remember { mutableStateOf("") }

    val visibleChanges = if (query.isBlank()) {
        allChanges
    } else {
        allChanges.filter {
            it.fileName.contains(query, ignoreCase = true) || it.filePath.contains(query, ignoreCase = true)
        }
    }

    GlassScaffold(
        title = "Changes",
        actions = {
            if (hasChanges) {
                IconButton(onClick = { sessionViewModel.selectAllChanges() }) {
                    Icon(Icons.Filled.DoneAll, contentDescription = "Select all")
                }
                IconButton(onClick = { sessionViewModel.deselectAllChanges() }) {
                    Icon(Icons.Filled.RemoveDone, contentDescription = "Clear selection")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (!state.isComparing && state.compareError == null && state.ignoredScaffoldFiles.isNotEmpty()) {
                ScaffoldFilesNotice(files = state.ignoredScaffoldFiles)
            }

            when {
                state.isComparing -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        val progress = state.compareProgress
                        Text(
                            text = if (progress != null) "Comparing ${progress.first} of ${progress.second} files" else "Comparing with GitHub…",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                state.compareError != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(state.compareError, color = MaterialTheme.colorScheme.error)
                        TextButton(
                            onClick = { sessionViewModel.runComparison(context) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Retry") }
                    }
                }

                !hasChanges -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No changes detected", style = MaterialTheme.typography.titleMedium)
                }

                else -> {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search files") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    )

                    Text(
                        "${state.selectedPaths.size} of ${allChanges.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            DiffSection(
                                title = "Added",
                                color = DiffAddedGreen,
                                type = ChangeType.ADDED,
                                items = visibleChanges.filter { it.type == ChangeType.ADDED },
                                selectedPaths = state.selectedPaths,
                                onToggleItem = sessionViewModel::toggleChangeSelection,
                                onToggleSection = { selected -> sessionViewModel.setSelectionForType(ChangeType.ADDED, selected) }
                            )
                        }
                        item {
                            DiffSection(
                                title = "Modified",
                                color = DiffModifiedYellow,
                                type = ChangeType.MODIFIED,
                                items = visibleChanges.filter { it.type == ChangeType.MODIFIED },
                                selectedPaths = state.selectedPaths,
                                onToggleItem = sessionViewModel::toggleChangeSelection,
                                onToggleSection = { selected -> sessionViewModel.setSelectionForType(ChangeType.MODIFIED, selected) }
                            )
                        }
                        item {
                            DiffSection(
                                title = "Removed",
                                color = DiffRemovedRed,
                                type = ChangeType.REMOVED,
                                items = visibleChanges.filter { it.type == ChangeType.REMOVED },
                                selectedPaths = state.selectedPaths,
                                onToggleItem = sessionViewModel::toggleChangeSelection,
                                onToggleSection = { selected -> sessionViewModel.setSelectionForType(ChangeType.REMOVED, selected) },
                                onIgnoreItem = { path -> sessionViewModel.ignoreFileForever(context, path) }
                            )
                        }
                    }

                    GlassPrimaryButton(
                        text = "Review ${state.selectedPaths.size} change(s)",
                        onClick = onContinue,
                        enabled = state.selectedPaths.isNotEmpty(),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiffSection(
    title: String,
    color: Color,
    type: ChangeType,
    items: List<FileChange>,
    selectedPaths: Set<String>,
    onToggleItem: (String) -> Unit,
    onToggleSection: (Boolean) -> Unit,
    onIgnoreItem: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(true) }
    val selectedInSection = items.count { selectedPaths.contains(it.filePath) }
    val sectionState = when {
        items.isEmpty() -> ToggleableState.Off
        selectedInSection == 0 -> ToggleableState.Off
        selectedInSection == items.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), padding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color, CircleShape)
                )
                Text(title, style = MaterialTheme.typography.titleSmall)
                CountBadge(count = items.size, color = color)
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                if (items.isEmpty()) {
                    Text(
                        "None",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onToggleSection(sectionState != ToggleableState.On) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TriStateCheckbox(
                            state = sectionState,
                            onClick = { onToggleSection(sectionState != ToggleableState.On) },
                            colors = CheckboxDefaults.colors(checkedColor = color, uncheckedColor = color.copy(alpha = 0.5f))
                        )
                        Text(
                            "Select all in $title",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items.forEach { change ->
                        val checked = selectedPaths.contains(change.filePath)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleItem(change.filePath) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onToggleItem(change.filePath) },
                                colors = CheckboxDefaults.colors(checkedColor = color)
                            )
                            Icon(
                                FileTypeIcons.iconFor(change.fileName),
                                contentDescription = null,
                                tint = FileTypeIcons.colorFor(change.fileName),
                                modifier = Modifier.size(20.dp).padding(end = 8.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    change.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    change.filePath,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (onIgnoreItem != null) {
                                IconButton(onClick = { onIgnoreItem(change.filePath) }) {
                                    Icon(
                                        Icons.Filled.VisibilityOff,
                                        contentDescription = "Don't track this file",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** Explains, in place, why README/LICENSE/.github/etc. that only exist on GitHub don't
 * show up under Removed — they're repository scaffolding, not app project files, so
 * this app never treats them as something the person deleted locally. */
@Composable
private fun ScaffoldFilesNotice(files: List<String>) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { expanded = !expanded },
        padding = 12.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.DoneAll,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                "${files.size} repository file${if (files.size == 1) "" else "s"} kept as-is (not part of your project)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                files.forEach { path ->
                    Text(
                        path,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
