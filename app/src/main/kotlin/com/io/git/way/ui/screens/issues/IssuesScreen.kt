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
package com.io.git.way.ui.screens.issues

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.io.git.way.domain.model.Issue
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.common.formatRelativeTime
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.RepoSuccess
import com.io.git.way.ui.theme.GlassChip
import com.io.git.way.ui.theme.GlassIconButton
import com.io.git.way.ui.theme.GlassScaffold

/**
 * Issues of the selected repository: open/closed filters, create dialog, detail view
 * with comments, close/reopen, and commenting.
 */
@Composable
fun IssuesScreen(
    sessionViewModel: GitWaySessionViewModel,
    onBack: () -> Unit
) {
    val state = sessionViewModel.state
    var filter by remember { mutableStateOf("open") }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedRepo) {
        if (state.selectedRepo != null) sessionViewModel.loadIssues(filter)
    }

    GlassScaffold(
        title = "Issues",
        subtitle = state.selectedRepo?.name ?: "Issues",
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        },
        actions = {
            GlassIconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New issue")
            }
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
                            sessionViewModel.loadIssues(f)
                        }
                    )
                }
            }
            if (state.issuesError != null) {
                Text(
                    state.issuesError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            when {
                state.isLoadingIssues && state.issues.isEmpty() -> Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { CircularProgressIndicator() }

                state.issues.isEmpty() -> Text(
                    "No issues here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 40.dp).align(Alignment.CenterHorizontally)
                )

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.issues, key = { it.number }) { issue ->
                        IssueCard(issue = issue, onClick = { sessionViewModel.loadIssueComments(issue) })
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateIssueDialog(
            isCreating = state.isCreatingIssue,
            errorMessage = state.createIssueError,
            onDismiss = { showCreateDialog = false; sessionViewModel.clearCreateIssueError() },
            onConfirm = { title, body ->
                sessionViewModel.createIssue(title, body) { showCreateDialog = false }
            }
        )
    }

    val selected = state.selectedIssue
    if (selected != null) {
        IssueDetailDialog(
            issue = selected,
            comments = state.issueComments,
            isLoadingComments = state.isLoadingIssueComments,
            commentsError = state.issueCommentsError,
            isPosting = state.isPostingComment,
            actionError = state.issueActionError,
            onPostComment = { body -> sessionViewModel.postIssueComment(selected, body) },
            onClose = { sessionViewModel.setIssueState(selected, "closed") },
            onReopen = { sessionViewModel.setIssueState(selected, "open") },
            onDismiss = { sessionViewModel.clearSelectedIssue() }
        )
    }
}

@Composable
private fun IssueCard(issue: Issue, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.BugReport,
                contentDescription = null,
                tint = if (issue.isOpen) RepoSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(issue.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "#${issue.number} · ${issue.author} · ${formatRelativeTime(issue.createdAt)} · ${issue.commentsCount} comments",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CreateIssueDialog(
    isCreating: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, body: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("New issue") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Description (optional)") },
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, body.ifBlank { null }) },
                enabled = title.isNotBlank() && !isCreating
            ) {
                if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel") } }
    )
}

@Composable
private fun IssueDetailDialog(
    issue: Issue,
    comments: List<com.io.git.way.domain.model.IssueComment>,
    isLoadingComments: Boolean,
    commentsError: String?,
    isPosting: Boolean,
    actionError: String?,
    onPostComment: (String) -> Unit,
    onClose: () -> Unit,
    onReopen: () -> Unit,
    onDismiss: () -> Unit
) {
    var commentDraft by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("#${issue.number} ${issue.title}", maxLines = 2) },
        text = {
            Column {
                if (issue.body != null) {
                    Text(issue.body, style = MaterialTheme.typography.bodySmall, maxLines = 8, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    if (issue.isOpen) "Open · ${formatRelativeTime(issue.createdAt)}" else "Closed",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (issue.isOpen) RepoSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    if (comments.isEmpty()) "No comments yet." else "Comments (${comments.size}):",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                if (isLoadingComments) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else if (commentsError != null) {
                    Text(commentsError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    comments.forEach { comment ->
                        Text(
                            "${comment.author} · ${formatRelativeTime(comment.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(comment.body, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }
                }
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { commentDraft = it },
                    label = { Text("Add a comment…") },
                    enabled = !isPosting,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                if (actionError != null) {
                    Text(actionError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onPostComment(commentDraft)
                    commentDraft = ""
                },
                enabled = commentDraft.isNotBlank() && !isPosting
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("  Comment")
            }
        },
        dismissButton = {
            if (issue.isOpen) {
                TextButton(onClick = onClose, enabled = !isPosting) { Text("Close issue") }
            } else {
                TextButton(onClick = onReopen, enabled = !isPosting) { Text("Reopen") }
            }
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
