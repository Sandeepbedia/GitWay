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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.io.git.way.ui.common.GitWaySessionViewModel

/** Screen 4: local updated project folder selection via SAF (PRD1 §2 Folder Selection Screen). */
@Composable
fun FolderSelectionScreen(
    sessionViewModel: GitWaySessionViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val state = sessionViewModel.state

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            sessionViewModel.onFolderPicked(context, uri)
        }
    }

    val canContinue = !state.isScanning && state.localFiles.isNotEmpty() && state.scanError == null

    Scaffold(topBar = { TopAppBar(title = { Text("Select Project Folder") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Point Git Way at the local, updated copy of \"${state.selectedRepo?.name.orEmpty()}\" " +
                    "on your phone.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedButton(
                onClick = { folderPickerLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Select Folder", modifier = Modifier.padding(start = 8.dp))
            }

            when {
                state.isScanning -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text("Scanning folder…", style = MaterialTheme.typography.bodyMedium)
                }

                state.scanError != null -> Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(state.scanError, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Re-select folder") }
                    }
                }

                state.localFiles.isNotEmpty() -> Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Folder, contentDescription = null)
                        Column {
                            Text(state.folderName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${state.localFiles.size} file(s) found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    sessionViewModel.runComparison(context)
                    onContinue()
                },
                enabled = canContinue,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Continue") }
        }
    }
}
