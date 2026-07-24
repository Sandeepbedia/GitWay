package com.io.git.way.ui.screens.folder

import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.io.git.way.domain.model.LocalFile
import com.io.git.way.ui.common.FileTypeIcons
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassPrimaryButton
import com.io.git.way.ui.theme.GlassScaffold
import com.io.git.way.ui.theme.GlassSecondaryButton

/** Screen 4: local updated project folder selection via SAF, shown as a file-manager
 * style browsable list — icon, name, path and size per file (PRD1 §2 Folder Selection). */
@Composable
fun FolderSelectionScreen(
    sessionViewModel: GitWaySessionViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val state = sessionViewModel.state
    var query by remember { mutableStateOf("") }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            sessionViewModel.onFolderPicked(context, uri)
        }
    }

    val canContinue = !state.isScanning && state.localFiles.isNotEmpty() && state.scanError == null
    val visibleFiles = if (query.isBlank()) {
        state.localFiles
    } else {
        state.localFiles.filter {
            it.displayName.contains(query, ignoreCase = true) || it.relativePath.contains(query, ignoreCase = true)
        }
    }
    val totalSize = state.localFiles.sumOf { it.sizeBytes }

    GlassScaffold(title = "Select Project Folder") { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Point Git Way at the local, updated copy of \"${state.selectedRepo?.name.orEmpty()}\" " +
                    "on your phone.",
                style = MaterialTheme.typography.bodyMedium
            )

            GlassSecondaryButton(
                text = "Select Folder",
                onClick = { folderPickerLauncher.launch(null) },
                leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )

            when {
                state.isScanning -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text("Scanning folder…", style = MaterialTheme.typography.bodyMedium)
                }

                state.scanError != null -> GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(state.scanError, color = MaterialTheme.colorScheme.error)
                    androidx.compose.material3.TextButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.padding(top = 4.dp)
                    ) { Text("Re-select folder") }
                }

                state.localFiles.isNotEmpty() -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.Folder, contentDescription = null)
                            Column {
                                Text(state.folderName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${state.localFiles.size} file(s) · ${FileTypeIcons.formatSize(totalSize)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search this folder") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleFiles, key = { it.relativePath }) { file ->
                            FileRow(file)
                        }
                    }
                }
            }

            GlassPrimaryButton(
                text = "Continue",
                onClick = {
                    sessionViewModel.runComparison(context)
                    onContinue()
                },
                enabled = canContinue
            )
        }
    }
}

@Composable
private fun FileRow(file: LocalFile) {
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                FileTypeIcons.iconFor(file.displayName),
                contentDescription = null,
                tint = FileTypeIcons.colorFor(file.displayName),
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    file.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    file.relativePath,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                FileTypeIcons.formatSize(file.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
