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
package com.io.git.way.ui.screens.actions

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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.io.git.way.domain.model.ArtifactInfo
import com.io.git.way.domain.model.GitHubWorkflow
import com.io.git.way.domain.model.WorkflowRun
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.common.PendingDownloadHandler
import com.io.git.way.ui.common.formatRelativeTime
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassChip
import com.io.git.way.ui.theme.GlassScaffold
import com.io.git.way.ui.theme.GlassSecondaryButton
import com.io.git.way.ui.theme.RepoSuccess
import kotlinx.coroutines.delay

private enum class ActionsSection {
    RUNS,
    WORKFLOWS,
    ARTIFACTS
}

private const val RUN_POLL_INTERVAL = 8_000L

@Composable
fun ActionsScreen(
    sessionViewModel: GitWaySessionViewModel,
    onBack: () -> Unit
) {
    val state = sessionViewModel.state

    var sectionIndex by remember {
        mutableIntStateOf(ActionsSection.RUNS.ordinal)
    }

    val activeSection = ActionsSection.entries[sectionIndex]

    /*
     * Load Actions data whenever the selected repository changes.
     */
    LaunchedEffect(state.selectedRepo?.fullName) {
        if (state.selectedRepo != null) {
            sessionViewModel.loadWorkflowRuns()
            sessionViewModel.loadWorkflows()
            sessionViewModel.loadArtifacts()
        }
    }

    /*
     * Poll only while there is an active workflow run.
     */
    val hasActiveRuns = state.workflowRuns.any { !it.isCompleted }

    LaunchedEffect(
        state.selectedRepo?.fullName,
        hasActiveRuns
    ) {
        if (!hasActiveRuns) return@LaunchedEffect

        while (true) {
            delay(RUN_POLL_INTERVAL)

            if (state.selectedRepo == null) {
                break
            }

            sessionViewModel.loadWorkflowRuns()
        }
    }

    GlassScaffold(
        title = "GitHub Actions",
        subtitle = state.selectedRepo?.name ?: "Actions",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    sessionViewModel.loadWorkflowRuns()
                    sessionViewModel.loadWorkflows()
                    sessionViewModel.loadArtifacts()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                ActionsSection.entries.forEach { item ->
                    GlassChip(
                        text = item.displayName(),
                        selected = item == activeSection,
                        onClick = {
                            sectionIndex = item.ordinal
                        }
                    )
                }
            }

            state.workflowDispatchError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            state.workflowActionError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            state.downloadError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            when (activeSection) {

                ActionsSection.RUNS -> {
                    RunsSection(
                        runs = state.workflowRuns,
                        isLoading = state.isLoadingWorkflowRuns,
                        error = state.workflowRunsError,
                        isActionRunning = state.isRunningWorkflowAction,
                        onCancel = {
                            sessionViewModel.cancelWorkflowRun(it)
                        },
                        onRerunFailed = {
                            sessionViewModel.rerunFailedJobs(it)
                        },
                        onDownloadLogs = {
                            sessionViewModel.downloadRunLogs(it)
                        },
                        onRetry = {
                            sessionViewModel.loadWorkflowRuns()
                        }
                    )
                }

                ActionsSection.WORKFLOWS -> {
                    val branch =
                        state.selectedBranch
                            ?: state.selectedRepo?.defaultBranch
                            ?: "main"

                    WorkflowsSection(
                        workflows = state.workflows,
                        isLoading = state.isLoadingWorkflows,
                        error = state.workflowsError,
                        isDispatching = state.isDispatchingWorkflow,
                        branch = branch,
                        onRun = { workflow ->
                            sessionViewModel.triggerWorkflow(workflow)
                        },
                        onRetry = {
                            sessionViewModel.loadWorkflows()
                        }
                    )
                }

                ActionsSection.ARTIFACTS -> {
                    ArtifactsSection(
                        artifacts = state.artifacts,
                        isLoading = state.isLoadingArtifacts,
                        error = state.artifactsError,
                        downloadingName = state.downloadingArtifactName,
                        onDownload = {
                            sessionViewModel.downloadArtifact(it)
                        },
                        onRetry = {
                            sessionViewModel.loadArtifacts()
                        }
                    )
                }
            }
        }
    }

    PendingDownloadHandler(
        pending = state.pendingDownload,
        downloading = state.downloadingArtifactName != null &&
            state.pendingDownload == null,
        downloadError = state.downloadError,
        onClear = {
            sessionViewModel.clearPendingDownload()
        },
        onClearError = {
            sessionViewModel.clearDownloadError()
        }
    )
}

/* -------------------------------------------------------------------------- */
/* RUNS                                                                       */
/* -------------------------------------------------------------------------- */

@Composable
private fun RunsSection(
    runs: List<WorkflowRun>,
    isLoading: Boolean,
    error: String?,
    isActionRunning: Boolean,
    onCancel: (WorkflowRun) -> Unit,
    onRerunFailed: (WorkflowRun) -> Unit,
    onDownloadLogs: (WorkflowRun) -> Unit,
    onRetry: () -> Unit
) {
    when {
        isLoading && runs.isEmpty() -> {
            LoadingView()
        }

        error != null && runs.isEmpty() -> {
            CenteredMessage(error) {
                GlassSecondaryButton(
                    text = "Retry",
                    onClick = onRetry
                )
            }
        }

        runs.isEmpty() -> {
            CenteredMessage("No workflow runs yet.") {
                GlassSecondaryButton(
                    text = "Refresh",
                    onClick = onRetry
                )
            }
        }

        else -> {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = runs,
                    key = { it.id }
                ) { run ->
                    RunCard(
                        run = run,
                        isActionRunning = isActionRunning,
                        onCancel = onCancel,
                        onRerunFailed = onRerunFailed,
                        onDownloadLogs = onDownloadLogs
                    )
                }
            }
        }
    }
}

@Composable
private fun RunCard(
    run: WorkflowRun,
    isActionRunning: Boolean,
    onCancel: (WorkflowRun) -> Unit,
    onRerunFailed: (WorkflowRun) -> Unit,
    onDownloadLogs: (WorkflowRun) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RunStatusIcon(run)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = run.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${run.branch} · #${run.runNumber} · " +
                        "${run.event} · ${formatRelativeTime(run.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            if (!run.isCompleted) {
                GlassSecondaryButton(
                    text = "Cancel",
                    onClick = {
                        onCancel(run)
                    },
                    enabled = !isActionRunning,
                    modifier = Modifier.weight(1f)
                )
            } else if (
                run.isFailure &&
                run.conclusion == "failure"
            ) {
                GlassSecondaryButton(
                    text = "Re-run failed",
                    onClick = {
                        onRerunFailed(run)
                    },
                    enabled = !isActionRunning,
                    modifier = Modifier.weight(1f)
                )
            }

            GlassSecondaryButton(
                text = "Logs",
                onClick = {
                    onDownloadLogs(run)
                },
                enabled = !isActionRunning,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RunStatusIcon(
    run: WorkflowRun
) {
    val (icon, color) = when {
        run.isCompleted && run.isSuccess -> {
            Icons.Filled.CheckCircle to RepoSuccess
        }

        run.isCompleted && run.isFailure -> {
            Icons.Filled.Error to MaterialTheme.colorScheme.error
        }

        run.isCompleted -> {
            Icons.Filled.Schedule to
                MaterialTheme.colorScheme.onSurfaceVariant
        }

        else -> {
            Icons.Filled.Schedule to
                MaterialTheme.colorScheme.primary
        }
    }

    Icon(
        imageVector = icon,
        contentDescription = run.conclusion ?: run.status,
        tint = color,
        modifier = Modifier.size(26.dp)
    )
}

/* -------------------------------------------------------------------------- */
/* WORKFLOWS                                                                  */
/* -------------------------------------------------------------------------- */

@Composable
private fun WorkflowsSection(
    workflows: List<GitHubWorkflow>,
    isLoading: Boolean,
    error: String?,
    isDispatching: Boolean,
    branch: String,
    onRun: (GitHubWorkflow) -> Unit,
    onRetry: () -> Unit
) {
    when {
        isLoading && workflows.isEmpty() -> {
            LoadingView()
        }

        error != null && workflows.isEmpty() -> {
            CenteredMessage(error) {
                GlassSecondaryButton(
                    text = "Retry",
                    onClick = onRetry
                )
            }
        }

        workflows.isEmpty() -> {
            CenteredMessage("No workflows registered.") {
                GlassSecondaryButton(
                    text = "Refresh",
                    onClick = onRetry
                )
            }
        }

        else -> {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = workflows,
                    key = { it.id }
                ) { workflow ->

                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WorkOutline,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                            ) {
                                Text(
                                    text = workflow.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = workflow.path,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (!workflow.canDispatch) {
                                    Text(
                                        text = "Manual run is not supported",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    onRun(workflow)
                                },
                                enabled = workflow.canDispatch &&
                                    !isDispatching
                            ) {
                                if (isDispatching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription =
                                            "Run ${workflow.name} on $branch"
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Runs on branch \"$branch\". " +
                            "Only workflows with a workflow_dispatch " +
                            "trigger can be started manually.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* ARTIFACTS                                                                  */
/* -------------------------------------------------------------------------- */

@Composable
private fun ArtifactsSection(
    artifacts: List<ArtifactInfo>,
    isLoading: Boolean,
    error: String?,
    downloadingName: String?,
    onDownload: (ArtifactInfo) -> Unit,
    onRetry: () -> Unit
) {
    when {
        isLoading && artifacts.isEmpty() -> {
            LoadingView()
        }

        error != null && artifacts.isEmpty() -> {
            CenteredMessage(error) {
                GlassSecondaryButton(
                    text = "Retry",
                    onClick = onRetry
                )
            }
        }

        artifacts.isEmpty() -> {
            CenteredMessage(
                "No build artifacts yet.\nArtifacts appear after a run finishes."
            ) {
                GlassSecondaryButton(
                    text = "Refresh",
                    onClick = onRetry
                )
            }
        }

        else -> {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = artifacts,
                    key = { it.id }
                ) { artifact ->

                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = artifact.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = if (artifact.expired) {
                                        "${formatBytes(artifact.size)} · expired"
                                    } else {
                                        "${formatBytes(artifact.size)} · " +
                                            formatRelativeTime(
                                                artifact.createdAt.orEmpty()
                                            )
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    onDownload(artifact)
                                },
                                enabled = !artifact.expired &&
                                    downloadingName == null
                            ) {
                                if (downloadingName == artifact.name) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription =
                                            "Download ${artifact.name}"
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

/* -------------------------------------------------------------------------- */
/* COMMON                                                                     */
/* -------------------------------------------------------------------------- */

@Composable
private fun LoadingView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredMessage(
    message: String,
    action: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        action()
    }
}

/* -------------------------------------------------------------------------- */
/* HELPERS                                                                    */
/* -------------------------------------------------------------------------- */

private fun ActionsSection.displayName(): String {
    return name
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}

private fun formatBytes(
    bytes: Long
): String {
    if (bytes <= 0L) {
        return "0 B"
    }

    val units = arrayOf(
        "B",
        "KB",
        "MB",
        "GB"
    )

    var value = bytes.toDouble()
    var unitIndex = 0

    while (
        value >= 1024.0 &&
        unitIndex < units.lastIndex
    ) {
        value /= 1024.0
        unitIndex++
    }

    return if (unitIndex == 0) {
        "$bytes B"
    } else {
        "%.1f %s".format(
            value,
            units[unitIndex]
        )
    }
}