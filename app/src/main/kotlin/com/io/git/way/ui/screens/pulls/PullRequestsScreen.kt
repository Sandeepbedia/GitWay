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
package com.io.git.way.ui.screens.pulls

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.io.git.way.domain.model.PullRequest
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.common.formatRelativeTime
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassChip
import com.io.git.way.ui.theme.GlassScaffold
import com.io.git.way.ui.theme.GlassSecondaryButton

/**
 * Pull requests of the selected repository: open/closed/all filters, PR details with
 * per-file patches, merge, and close/reopen.
 */
@Composable
fun PullRequestsScreen(
    sessionViewModel: GitWaySessionViewModel,
    onBack: () -> Unit
) {
    val state = sessionViewModel.state
    var filter by remember { mutableStateOf("open") }

    LaunchedEffect(state.selectedRepo) {
        if (state.selectedRepo != null) sessionViewModel.loadPullRequests(filter)
    }

    GlassScaffold(
        title = "Pull Requests",
        subtitle = state.selectedRepo?.name ?: "Pull requests",
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                listOf("open", "closed", "all").forEach { f ->
                    GlassChip(
                        text = f.replaceFirstChar { it.uppercase() },
                        selected = filter == f,
                        onClick = {
                            filter = f
                            sessionViewModel.loadPullRequests(f)
                        }
                    )
                }
            }
            if (state.pullRequestsError != null) {
                Text(
                    state.pullRequestsError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            when {
                state.isLoadingPullRequests && state.pullRequests.isEmpty() -> Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { CircularProgressIndicator() }

                state.pullRequests.isEmpty() -> Text(
                    "No pull requests here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 40.dp).align(Alignment.CenterHorizontally)
                )

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.pullRequests, key = { it.number }) { pr ->
                        PullRequestCard(pr = pr, onClick = { sessionViewModel.loadPullRequestFiles(pr) })
                    }
                }
            }
        }
    }

    val selected = state.selectedPullRequest
    if (selected != null) {
        PullRequestDetailDialog(
            pr = selected,
            files = state.pullRequestFiles,
            isLoadingFiles = state.isLoadingPullRequestFiles,
            filesError = state.pullRequestFilesError,
            isMerging = state.isMergingPullRequest,
            actionError = state.pullRequestActionError,
            onMerge = { sessionViewModel.mergePullRequest(selected) },
            onClose = { sessionViewModel.setPullRequestState(selected, "closed") },
            onReopen = { sessionViewModel.setPullRequestState(selected, "open") },
            onDismiss = { sessionViewModel.clearSelectedPullRequest() }
        )
    }
}

@Composable
private fun PullRequestCard(pr: PullRequest, onClick: () -> Unit) {
    val color = when {
        pr.isMerged -> Color(0xFF8B5CF6)
        pr.isOpen -> Color(0xFF22C55E)
        else -> MaterialTheme.colorScheme.error
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (pr.isMerged) Icons.Filled.Merge else if (pr.isOpen) Icons.Filled.CheckCircle else Icons.Filled.VisibilityOff,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(pr.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "#${pr.number} · ${pr.headRef} → ${pr.baseRef} · ${formatRelativeTime(pr.updatedAt.orEmpty())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "+${pr.additions} −${pr.deletions} · ${pr.changedFiles} files · by ${pr.author}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 10.dp)
        ) {
            GlassSecondaryButton(text = "Details", onClick = onClick, modifier = Modifier.weight(1f))
            if (pr.isOpen) {
                GlassSecondaryButton(text = "Merge", onClick = onClick, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PullRequestDetailDialog(
    pr: PullRequest,
    files: List<com.io.git.way.domain.model.PullRequestFile>,
    isLoadingFiles: Boolean,
    filesError: String?,
    isMerging: Boolean,
    actionError: String?,
    onMerge: () -> Unit,
    onClose: () -> Unit,
    onReopen: () -> Unit,
    onDismiss: () -> Unit
) {
    var showMergeConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("#${pr.number} ${pr.title}", maxLines = 2) },
        text = {
            Column {
                Text(
                    "${pr.headRef} → ${pr.baseRef} · ${formatRelativeTime(pr.createdAt)} · by ${pr.author}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (pr.body != null) {
                    Text(
                        pr.body,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    "Files changed (${files.size}):",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                if (isLoadingFiles) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else if (filesError != null) {
                    Text(filesError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    files.take(20).forEach { file ->
                        Text(
                            "${file.filename}  (+${file.additions} −${file.deletions})",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (files.size > 20) {
                        Text("…and ${files.size - 20} more", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (actionError != null) {
                    Text(actionError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            if (pr.isOpen) {
                TextButton(
                    onClick = { showMergeConfirm = true },
                    enabled = !isMerging
                ) { if (isMerging) Text("Merging…") else Text("Merge") }
                TextButton(onClick = onClose, enabled = !isMerging) { Text("Close") }
            } else {
                TextButton(onClick = onReopen, enabled = !isMerging) { Text("Reopen") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )

    if (showMergeConfirm) {
        AlertDialog(
            onDismissRequest = { showMergeConfirm = false },
            title = { Text("Merge pull request #${pr.number}?") },
            text = { Text("The branch \"${pr.headRef}\" will be merged into \"${pr.baseRef}\".") },
            confirmButton = {
                TextButton(onClick = { showMergeConfirm = false; onMerge() }) { Text("Merge") }
            },
            dismissButton = { TextButton(onClick = { showMergeConfirm = false }) { Text("Cancel") } }
        )
    }
}
