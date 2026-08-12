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
package com.io.git.way.ui.screens.releases

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.io.git.way.domain.model.GitRelease
import com.io.git.way.domain.model.ReleaseAsset
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.common.PendingDownloadHandler
import com.io.git.way.ui.common.formatRelativeTime
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassIconButton
import com.io.git.way.ui.theme.GlassScaffold
import com.io.git.way.ui.theme.GlassSecondaryButton
import java.io.File

/**
 * Release management for the selected repository: list releases, create a new release
 * (draft/prerelease), attach an APK from device storage, download & install assets,
 * and delete releases.
 */
@Composable
fun ReleasesScreen(
    sessionViewModel: GitWaySessionViewModel,
    onBack: () -> Unit
) {
    val state = sessionViewModel.state
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var assetsRelease by remember { mutableStateOf<GitRelease?>(null) }

    LaunchedEffect(state.selectedRepo) {
        if (state.selectedRepo != null) sessionViewModel.loadReleases()
    }

    GlassScaffold(
        title = "Releases",
        subtitle = state.selectedRepo?.name ?: "Releases",
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        },
        actions = {
            GlassIconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New release")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (state.releasesError != null) {
                Text(state.releasesError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
            }
            if (state.releaseActionError != null) {
                Text(state.releaseActionError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
            }
            when {
                state.isLoadingReleases && state.releases.isEmpty() -> Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { CircularProgressIndicator() }

                state.releases.isEmpty() -> Text(
                    "No releases yet. Push a tag or create one here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 40.dp).align(Alignment.CenterHorizontally)
                )

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.releases, key = { it.id }) { release ->
                        ReleaseCard(
                            release = release,
                            isDeleting = state.isDeletingRelease,
                            onDelete = { sessionViewModel.deleteRelease(release) },
                            onOpenAssets = {
                                assetsRelease = release
                                sessionViewModel.loadReleaseAssets(release)
                            },
                            onDownloadAsset = { sessionViewModel.downloadReleaseAsset(it) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateReleaseDialog(
            isCreating = state.isCreatingRelease,
            errorMessage = state.createReleaseError,
            onDismiss = { showCreateDialog = false; sessionViewModel.clearCreateReleaseError() },
            onConfirm = { tag, name, body, draft, prerelease ->
                sessionViewModel.createRelease(tag, name, body, draft, prerelease) { showCreateDialog = false }
            }
        )
    }

    val selectedRelease = assetsRelease
    if (selectedRelease != null) {
        AssetsDialog(
            release = selectedRelease,
            assets = state.releaseAssets,
            isLoading = state.isLoadingReleaseAssets,
            error = state.releaseAssetsError,
            isUploading = state.isUploadingAsset,
            uploadError = state.uploadAssetError,
            onDownloadAsset = { sessionViewModel.downloadReleaseAsset(it) },
            onUploadApk = { name, bytes ->
                sessionViewModel.uploadReleaseAsset(selectedRelease, name, bytes) { assetsRelease = null }
            },
            onDismiss = {
                assetsRelease = null
                sessionViewModel.clearReleaseActionErrors()
            }
        )
    }

    PendingDownloadHandler(
        pending = state.pendingDownload,
        downloading = state.downloadingArtifactName != null && state.pendingDownload == null,
        downloadError = state.downloadError,
        onClear = { sessionViewModel.clearPendingDownload() },
        onClearError = { sessionViewModel.clearDownloadError() }
    )
}

@Composable
private fun ReleaseCard(
    release: GitRelease,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onOpenAssets: () -> Unit,
    onDownloadAsset: (ReleaseAsset) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAssets by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(release.tagName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (release.prerelease) {
                        Text(
                            "  prerelease",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    if (release.draft) {
                        Text(
                            "  draft",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
                Text(
                    release.name ?: release.tagName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatRelativeTime(release.publishedAt ?: release.createdAt)} · ${release.assets.size} asset(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }, enabled = !isDeleting) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete release")
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 10.dp)
        ) {
            GlassSecondaryButton(text = "Assets", onClick = {
                showAssets = true
                onOpenAssets()
            }, modifier = Modifier.weight(1f))
            release.apkAsset?.let { asset ->
                GlassSecondaryButton(
                    text = "Install APK",
                    onClick = { onDownloadAsset(asset) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete release ${release.tagName}?") },
            text = { Text("The release (and its assets) will be removed. The tag stays.") },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AssetsDialog(
    release: GitRelease,
    assets: List<ReleaseAsset>,
    isLoading: Boolean,
    error: String?,
    isUploading: Boolean,
    uploadError: String?,
    onDownloadAsset: (ReleaseAsset) -> Unit,
    onUploadApk: (name: String, bytes: ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var uploadTarget by remember { mutableStateOf<Pair<String, ByteArray>?>(null) }
    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "asset.apk"
            if (bytes != null) uploadTarget = name to bytes
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assets · ${release.tagName}") },
        text = {
            Column {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    if (assets.isEmpty()) Text("No assets attached.")
                    assets.forEach { asset ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(asset.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (asset.size > 0) "${asset.size / 1024} KB" else "Download via GitHub",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDownloadAsset(asset) }) {
                                Icon(Icons.Filled.Download, contentDescription = "Download ${asset.name}")
                            }
                        }
                    }
                }
                if (uploadError != null) {
                    Text(uploadError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                }
                if (uploadTarget != null || isUploading) {
                    Text(
                        "Uploading…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { uploadLauncher.launch("application/vnd.android.package-archive") },
                enabled = !isUploading
            ) {
                Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("  Upload APK")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )

    uploadTarget?.let { target ->
        LaunchedEffect(target) {
            onUploadApk(target.first, target.second)
            uploadTarget = null
        }
    }
}

@Composable
private fun CreateReleaseDialog(
    isCreating: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (tag: String, name: String?, body: String?, draft: Boolean, prerelease: Boolean) -> Unit
) {
    var tag by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(false) }
    var prerelease by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("New release") },
        text = {
            Column {
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag name (e.g. v1.2)") },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Release title (optional)") },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Notes (optional)") },
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Draft", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = draft, onCheckedChange = { draft = it }, enabled = !isCreating)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Prerelease", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = prerelease, onCheckedChange = { prerelease = it }, enabled = !isCreating)
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(tag, name.ifBlank { null }, body.ifBlank { null }, draft, prerelease) },
                enabled = tag.isNotBlank() && !isCreating
            ) {
                if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel") } }
    )
}
