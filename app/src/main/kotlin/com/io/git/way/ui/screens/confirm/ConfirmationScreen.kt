package com.io.git.way.ui.screens.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.io.git.way.ui.common.GitWaySessionViewModel

/** Screen 6: final summary + single Upload button (PRD2 §2 Confirmation Screen). */
@Composable
fun ConfirmationScreen(
    sessionViewModel: GitWaySessionViewModel,
    onConfirmUpload: () -> Unit
) {
    val context = LocalContext.current
    val state = sessionViewModel.state
    var showDeleteWarning by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Confirm Upload") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${state.selectedRepo?.name.orEmpty()}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "${state.addedCount} added, ${state.modifiedCount} modified, ${state.removedCount} removed",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (state.removedCount > 0) {
                Text(
                    "⚠ ${state.removedCount} file(s) will be permanently removed from GitHub.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    if (state.removedCount > 0) {
                        showDeleteWarning = true
                    } else {
                        sessionViewModel.uploadChanges(context)
                        onConfirmUpload()
                    }
                },
                enabled = !state.isUploading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Upload to GitHub") }
        }
    }

    if (showDeleteWarning) {
        AlertDialog(
            onDismissRequest = { showDeleteWarning = false },
            title = { Text("Delete files from GitHub?") },
            text = { Text("This will delete ${state.removedCount} file(s) from GitHub. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteWarning = false
                    sessionViewModel.uploadChanges(context)
                    onConfirmUpload()
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteWarning = false }) { Text("Cancel") }
            }
        )
    }
}
