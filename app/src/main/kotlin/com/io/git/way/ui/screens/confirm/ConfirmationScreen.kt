package com.io.git.way.ui.screens.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassPrimaryButton
import com.io.git.way.ui.theme.GlassScaffold

/** Screen 6: final summary + single Upload button, scoped to whatever the user selected
 * back on the Analysis screen (PRD2 §2 Confirmation Screen). */
@Composable
fun ConfirmationScreen(
    sessionViewModel: GitWaySessionViewModel,
    onConfirmUpload: () -> Unit
) {
    val context = LocalContext.current
    val state = sessionViewModel.state
    var showDeleteWarning by remember { mutableStateOf(false) }
    val selectedTotal = state.selectedPaths.size

    GlassScaffold(title = "Confirm Upload") { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${state.selectedRepo?.name.orEmpty()}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${state.selectedAddedCount} added, ${state.selectedModifiedCount} modified, ${state.selectedRemovedCount} removed",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (selectedTotal != state.fileChanges.size) {
                    Text(
                        "$selectedTotal of ${state.fileChanges.size} detected changes selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (state.selectedRemovedCount > 0) {
                Text(
                    "⚠ ${state.selectedRemovedCount} file(s) will be permanently removed from GitHub.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            GlassPrimaryButton(
                text = if (selectedTotal > 0) "Upload $selectedTotal change(s) to GitHub" else "Nothing selected",
                onClick = {
                    if (state.selectedRemovedCount > 0) {
                        showDeleteWarning = true
                    } else {
                        sessionViewModel.uploadChanges(context)
                        onConfirmUpload()
                    }
                },
                enabled = selectedTotal > 0 && !state.isUploading,
                loading = state.isUploading
            )
        }
    }

    if (showDeleteWarning) {
        AlertDialog(
            onDismissRequest = { showDeleteWarning = false },
            title = { Text("Delete files from GitHub?") },
            text = { Text("This will delete ${state.selectedRemovedCount} file(s) from GitHub. Continue?") },
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
