/*
 * Git Way
 * Copyright (C) 2026 Sandeep Bedia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.io.git.way.ui.screens.upload

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.io.git.way.domain.model.UploadPhase
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassPrimaryButton
import com.io.git.way.ui.theme.GlassScaffold
import com.io.git.way.ui.theme.GlassSecondaryButton

/** Screen 7: real-time upload status through every stage of the push, not just a stuck
 * "42/42" (422-fix PRD §11 Progress UI / §14 Better UI). */
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

    GlassScaffold(title = "Uploading") { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                val phaseLabel = phaseLabel(state.uploadPhase)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(phaseLabel, style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        Text(phaseLabel, style = MaterialTheme.typography.titleMedium)
                    }
                }

                val (completed, total) = state.uploadProgress
                Text(
                    text = if (state.uploadCurrentFile.isNotBlank()) {
                        "$completed / $total files — ${state.uploadCurrentFile}"
                    } else {
                        "$completed / $total files"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                val progressFraction = if (total > 0) completed.toFloat() / total.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(8.dp))
                )

                if (state.isUploading) {
                    TextButton(
                        onClick = { sessionViewModel.cancelUpload() },
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("Cancel") }
                }
            }

            if (state.uploadError != null) {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    // §1 Improve Error Handling: GitHub's real reason, not a bare status code.
                    Text(
                        "Upload Failed",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        state.uploadError,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPrimaryButton(
                            text = "Retry upload",
                            onClick = { sessionViewModel.uploadChanges(context) },
                            modifier = Modifier.weight(1f)
                        )
                        GlassSecondaryButton(
                            text = "Copy Error",
                            onClick = { copyErrorToClipboard(context, state.uploadError) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private fun phaseLabel(phase: UploadPhase): String = when (phase) {
    UploadPhase.IDLE -> "Starting…"
    UploadPhase.VALIDATING -> "Preparing repository…"
    UploadPhase.PREPARING -> "Preparing changes…"
    UploadPhase.CREATING_BLOBS -> "Uploading files…"
    UploadPhase.CREATING_TREE -> "Creating tree…"
    UploadPhase.CREATING_COMMIT -> "Creating commit…"
    UploadPhase.UPDATING_BRANCH -> "Updating branch…"
    UploadPhase.VERIFYING -> "Verifying push…"
    UploadPhase.DONE -> "Done"
}

private fun copyErrorToClipboard(context: Context, error: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Git Way upload error", error))
}
