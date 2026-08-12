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
package com.io.git.way.ui.screens.complete

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.components.CreatePullRequestDialog
import com.io.git.way.ui.theme.DiffAddedGreen
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassPrimaryButton
import com.io.git.way.ui.theme.GlassScaffold
import com.io.git.way.ui.theme.GlassSecondaryButton

/** Screen 8: success summary + updated repository info (PRD2 §5 Completion Screen). */
@Composable
fun CompletionScreen(
    sessionViewModel: GitWaySessionViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val state = sessionViewModel.state
    val repo = state.selectedRepo
    val commitSha = state.commitSha
    val commitUrl = if (repo != null && commitSha != null) {
        "https://github.com/${repo.owner}/${repo.name}/commit/$commitSha"
    } else null

    // The push targeted an explicit non-default branch, so offer to open a PR for it.
    val pushedBranch = state.selectedBranch
    val showCreatePr = repo != null && pushedBranch != null && pushedBranch != repo.defaultBranch
    var showCreatePrDialog by remember { mutableStateOf(false) }

    GlassScaffold(title = "Done") { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = DiffAddedGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Repository updated successfully",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = "${state.selectedAddedCount} added, ${state.selectedModifiedCount} modified, " +
                            "${state.selectedRemovedCount} removed",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (!state.lastCommitMessage.isNullOrBlank()) {
                        Text(
                            text = "\"${state.lastCommitMessage}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    if (showCreatePr && pushedBranch != null) {
                        Text(
                            text = "Pushed to branch \"$pushedBranch\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            if (commitUrl != null) {
                GlassSecondaryButton(
                    text = "View commit on GitHub",
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(commitUrl))) },
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            if (showCreatePr && pushedBranch != null && repo != null) {
                GlassSecondaryButton(
                    text = "Create Pull Request",
                    onClick = { showCreatePrDialog = true },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            GlassPrimaryButton(
                text = "Back to Repositories",
                onClick = {
                    sessionViewModel.resetForNewRepository()
                    onDone()
                },
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }

    if (showCreatePrDialog && pushedBranch != null && repo != null) {
        CreatePullRequestDialog(
            defaultTitle = state.lastCommitMessage.orEmpty(),
            head = pushedBranch,
            base = repo.defaultBranch,
            isCreating = state.isCreatingPullRequest,
            errorMessage = state.createPullRequestError,
            onDismiss = {
                sessionViewModel.clearCreatePullRequestError()
                showCreatePrDialog = false
            },
            onConfirm = { title, body ->
                sessionViewModel.createPullRequest(
                    title = title,
                    head = pushedBranch,
                    base = repo.defaultBranch,
                    body = body
                ) { pr ->
                    showCreatePrDialog = false
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${repo.owner}/${repo.name}/pull/${pr.number}"))
                    )
                }
            }
        )
    }
}
