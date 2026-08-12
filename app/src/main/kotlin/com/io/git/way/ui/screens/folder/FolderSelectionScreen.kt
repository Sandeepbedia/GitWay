package com.io.git.way.ui.screens.folder

import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.io.git.way.domain.model.AppIdentity
import com.io.git.way.domain.model.LocalFile
import com.io.git.way.domain.model.ScanIssue
import com.io.git.way.domain.model.ScanReport
import com.io.git.way.ui.common.FileTypeIcons
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.theme.DiffModifiedYellow
import com.io.git.way.ui.theme.DiffRemovedRed
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

    val canContinue = !state.isScanning &&
        state.localFiles.isNotEmpty() && state.scanError == null &&
        !state.isCheckingAppIdentity && state.identityCheckError == null && !state.appIdentityMismatch
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

            // Everything below — status/summary cards AND the file list — lives in one
            // LazyColumn so the whole thing scrolls as a unit. Splitting the summary
            // cards into a plain (non-scrolling) Column above a weighted LazyColumn was
            // the previous layout, but that meant a tall summary (Smart Upload
            // Protection details, the identity-mismatch warning, etc.) could push the
            // file list down with no way to scroll back up to see the cards or down to
            // see the rest of the files.
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    state.isScanning -> item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text("Scanning folder…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    state.scanError != null -> item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(state.scanError, color = MaterialTheme.colorScheme.error)
                            androidx.compose.material3.TextButton(
                                onClick = { folderPickerLauncher.launch(null) },
                                modifier = Modifier.padding(top = 4.dp)
                            ) { Text("Re-select folder") }
                        }
                    }

                    state.localFiles.isNotEmpty() -> {
                        item {
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
                        }

                        state.scanReport?.let { report ->
                            item {
                                ProtectionSummaryCard(
                                    report = report,
                                    overrides = state.fileInclusionOverrides,
                                    onToggleOverride = sessionViewModel::setFileInclusionOverride
                                )
                            }
                        }

                        if (state.isCheckingAppIdentity) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                    Text(
                                        "Checking this folder matches the repository…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (state.identityCheckError != null) {
                            item {
                                IdentityCheckErrorCard(
                                    message = state.identityCheckError,
                                    onRetry = { sessionViewModel.retryIdentityCheck(context) }
                                )
                            }
                        }

                        if (state.appIdentityMismatch) {
                            item {
                                AppMismatchWarningCard(
                                    repoName = state.selectedRepo?.name.orEmpty(),
                                    local = state.localAppIdentity,
                                    remote = state.remoteAppIdentity,
                                    onReselectFolder = { folderPickerLauncher.launch(null) }
                                )
                            }
                        }

                        item {
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
                        }

                        items(visibleFiles, key = { it.relativePath }) { file ->
                            FileRow(
                                file = file,
                                onExclude = { sessionViewModel.setFileInclusionOverride(file.relativePath, false) }
                            )
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

/** Repository / Project Match Protection — PRD "block wrong project upload". Shown the
 * moment a package mismatch is detected, before the user can even tap Continue.
 * Deliberately has no "upload anyway" escape hatch: the only ways forward are picking a
 * different folder here, or pressing back to pick a different repository. */
@Composable
private fun IdentityCheckErrorCard(message: String, onRetry: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                "Couldn't verify project match",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            "Continue stays blocked until this check succeeds — a connection hiccup should " +
                "never be the reason two different app projects end up mixed into one repository.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 4.dp)) {
            Text("Retry check")
        }
    }
}

@Composable
private fun AppMismatchWarningCard(
    repoName: String,
    local: AppIdentity?,
    remote: AppIdentity?,
    onReselectFolder: () -> Unit
) {
    val packageMismatch = local?.packageName != null && remote?.packageName != null &&
        local.packageName != remote.packageName
    val nameMismatch = local?.appName != null && remote?.appName != null &&
        local.appName != remote.appName

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                "Selected project does not match this repository",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (packageMismatch) {
            MismatchFieldRow("Package Name", local?.packageName.orEmpty(), remote?.packageName.orEmpty())
        }
        if (nameMismatch) {
            MismatchFieldRow("App Name", local?.appName.orEmpty(), remote?.appName.orEmpty())
        }

        Text(
            "\"$repoName\" on GitHub already belongs to a different project. Uploading would mix " +
                "two different apps into one repository, so it's blocked to protect your repository.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "Please select the correct project folder before continuing.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        TextButton(onClick = onReselectFolder, modifier = Modifier.padding(top = 4.dp)) {
            Text("Choose a different folder")
        }
    }
}

@Composable
private fun MismatchFieldRow(label: String, localValue: String, remoteValue: String) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            "$label mismatch",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            "Selected project: $localValue",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            "Repository: $remoteValue",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun FileRow(file: LocalFile, onExclude: () -> Unit) {
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp)
            )
            IconButton(onClick = onExclude, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Filled.GppGood,
                    contentDescription = "Exclude from upload",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** PRD "Smart Upload Protection" §14/§16 — Upload Summary + expandable Ignored/Blocked
 * (secrets included in Blocked) lists, right where the file list already lives so the
 * user sees exactly what will and won't be uploaded before tapping Continue. Every row
 * is individually overridable — Smart Upload Protection flags files, the user decides. */
@Composable
private fun ProtectionSummaryCard(
    report: ScanReport,
    overrides: Map<String, Boolean>,
    onToggleOverride: (path: String, include: Boolean?) -> Unit
) {
    var showIgnored by remember { mutableStateOf(false) }
    var showBlocked by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Shield, contentDescription = null)
            Text("Smart Upload Protection", style = MaterialTheme.typography.titleSmall)
        }
        Text(
            "${report.safeCount} safe · tap any file below to include or exclude it",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CountBadge(Icons.Filled.GppGood, report.safeCount, "Safe", MaterialTheme.colorScheme.primary)
            CountBadge(Icons.Filled.VisibilityOff, report.ignoredCount, "Ignored", DiffModifiedYellow)
            CountBadge(Icons.Filled.Warning, report.blockedCount + report.secretCount, "Blocked", DiffRemovedRed)
        }

        if (report.sanitizedFiles.isNotEmpty()) {
            Text(
                "🔒 ${report.sanitizedFiles.size} credential file(s) had real passwords redacted " +
                    "— see jks_config.txt for setup steps.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (report.ignoredFiles.isNotEmpty()) {
            ExpandableIssueSection(
                title = "View ${report.ignoredCount} ignored file(s)",
                expanded = showIgnored,
                onToggle = { showIgnored = !showIgnored },
                issues = report.ignoredFiles,
                overrides = overrides,
                onToggleOverride = onToggleOverride
            )
        }

        val blockedAll = report.blockedFiles + report.secretsFound
        if (blockedAll.isNotEmpty()) {
            ExpandableIssueSection(
                title = "View ${blockedAll.size} blocked file(s)",
                expanded = showBlocked,
                onToggle = { showBlocked = !showBlocked },
                issues = blockedAll,
                overrides = overrides,
                onToggleOverride = onToggleOverride,
                accent = DiffRemovedRed
            )
        }
    }
}

@Composable
private fun CountBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int, label: String, tint: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text("$count", style = MaterialTheme.typography.labelLarge, color = tint)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExpandableIssueSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    issues: List<ScanIssue>,
    overrides: Map<String, Boolean>,
    onToggleOverride: (path: String, include: Boolean?) -> Unit,
    accent: androidx.compose.ui.graphics.Color? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = accent ?: MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = accent ?: MaterialTheme.colorScheme.primary
            )
        }
    }
    AnimatedVisibility(visible = expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 4.dp)) {
            issues.take(50).forEach { issue ->
                val included = overrides[issue.relativePath] == true
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            issue.relativePath,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            issue.reason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = { onToggleOverride(issue.relativePath, if (included) null else true) }
                    ) {
                        Text(if (included) "Included ✓" else "Include anyway")
                    }
                }
            }
            if (issues.size > 50) {
                Text(
                    "+ ${issues.size - 50} more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
