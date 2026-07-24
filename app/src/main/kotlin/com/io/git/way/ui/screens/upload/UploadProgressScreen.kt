package com.io.git.way.ui.screens.upload

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.io.git.way.domain.model.UploadPhase
import com.io.git.way.ui.common.GitWaySessionViewModel

/** Screen 7: real-time upload status, two visible phases (PRD2 §4 Upload Progress Screen). */
@Composable
fun UploadProgressScreen(
    sessionViewModel: GitWaySessionViewModel,
    onUploadFinished: () -> Unit
) {
    val context = LocalContext.current
    val state = sessionViewModel.state

    LaunchedEffect(state.commitSha) {
        if (state.commitSha != null) onUploadFinished()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Uploading") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            val phaseLabel = when (state.uploadPhase) {
                UploadPhase.PREPARING -> "Preparing changes"
                UploadPhase.FINALIZING -> "Finalizing commit"
                UploadPhase.DONE -> "Done"
                UploadPhase.IDLE -> "Starting…"
            }
            Text(phaseLabel, style = MaterialTheme.typography.titleMedium)

            val (completed, total) = state.uploadProgress
            Text(
                text = if (state.uploadCurrentFile.isNotBlank()) {
                    "Uploading $completed of $total — ${state.uploadCurrentFile}"
                } else {
                    "Uploading $completed of $total"
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            val progressFraction = if (total > 0) completed.toFloat() / total.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )

            if (state.uploadError != null) {
                Text(
                    state.uploadError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
                OutlinedButton(
                    onClick = { sessionViewModel.uploadChanges(context) },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Text("Retry upload") }
            }
        }
    }
}
