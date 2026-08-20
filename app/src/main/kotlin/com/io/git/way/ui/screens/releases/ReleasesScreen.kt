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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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

private enum class ReleaseFilter {
    ALL,
    STABLE,
    PRERELEASE,
    DRAFT
}

@Composable
fun ReleasesScreen(
    sessionViewModel: GitWaySessionViewModel,
    onBack: () -> Unit
) {
    val state = sessionViewModel.state
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedRelease by remember { mutableStateOf<GitRelease?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ReleaseFilter.ALL) }

    LaunchedEffect(state.selectedRepo) {
        if (state.selectedRepo != null) {
            sessionViewModel.loadReleases()
        }
    }

    val filteredReleases = remember(
        state.releases,
        searchQuery,
        filter
    ) {
        state.releases
            .filter { release ->
                when (filter) {
                    ReleaseFilter.ALL -> true
                    ReleaseFilter.STABLE ->
                        !release.draft && !release.prerelease
                    ReleaseFilter.PRERELEASE ->
                        release.prerelease
                    ReleaseFilter.DRAFT ->
                        release.draft
                }
            }
            .filter { release ->
                if (searchQuery.isBlank()) {
                    true
                } else {
                    release.tagName.contains(
                        searchQuery,
                        ignoreCase = true
                    ) ||
                        release.name.orEmpty().contains(
                            searchQuery,
                            ignoreCase = true
                        )
                }
            }
    }

    val latestRelease = state.releases
        .firstOrNull { !it.draft && !it.prerelease }

    GlassScaffold(
        title = "Releases",
        subtitle = state.selectedRepo?.name ?: "Repository",
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
                    sessionViewModel.loadReleases()
                },
                enabled = !state.isLoadingReleases
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh"
                )
            }

            GlassIconButton(
                onClick = {
                    showCreateDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create release"
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

            if (state.releasesError != null) {
                ErrorBanner(state.releasesError)
                Spacer(Modifier.height(8.dp))
            }

            if (state.releaseActionError != null) {
                ErrorBanner(state.releaseActionError)
                Spacer(Modifier.height(8.dp))
            }

            ReleaseSearchBar(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                onClear = {
                    searchQuery = ""
                }
            )

            Spacer(Modifier.height(10.dp))

            ReleaseFilterRow(
                selected = filter,
                onSelected = {
                    filter = it
                }
            )

            Spacer(Modifier.height(12.dp))

            when {
                state.isLoadingReleases && state.releases.isEmpty() -> {
                    LoadingState()
                }

                state.releases.isEmpty() -> {
                    EmptyReleasesState(
                        onCreate = {
                            showCreateDialog = true
                        }
                    )
                }

                filteredReleases.isEmpty() -> {
                    NoSearchResults()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            bottom = 24.dp
                        )
                    ) {

                        latestRelease?.let { latest ->
                            item {
                                LatestReleaseCard(
                                    release = latest,
                                    onOpenAssets = {
                                        selectedRelease = latest
                                        sessionViewModel
                                            .loadReleaseAssets(latest)
                                    },
                                    onDownload = {
                                        latest.apkAsset?.let { apk ->
                                            sessionViewModel
                                                .downloadReleaseAsset(apk)
                                        }
                                    },
                                    onShare = {
                                        latest.htmlUrl?.let { url ->
                                            shareUrl(context, url)
                                        }
                                    },
                                    onOpen = {
                                        latest.htmlUrl?.let { url ->
                                            openUrl(context, url)
                                        }
                                    }
                                )
                            }
                        }

                        item {
                            Text(
                                text = "${filteredReleases.size} release(s)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        items(
                            items = filteredReleases,
                            key = { it.id }
                        ) { release ->

                            ReleaseCard(
                                release = release,
                                isDeleting = state.isDeletingRelease,
                                onDelete = {
                                    sessionViewModel
                                        .deleteRelease(release)
                                },
                                onOpenAssets = {
                                    selectedRelease = release
                                    sessionViewModel
                                        .loadReleaseAssets(release)
                                },
                                onDownloadAsset = {
                                    sessionViewModel
                                        .downloadReleaseAsset(it)
                                },
                                onShare = {
                                    release.htmlUrl?.let { url ->
                                        shareUrl(context, url)
                                    }
                                },
                                onOpen = {
                                    release.htmlUrl?.let { url ->
                                        openUrl(context, url)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateReleaseDialog(
            isCreating = state.isCreatingRelease,
            errorMessage = state.createReleaseError,
            onDismiss = {
                if (!state.isCreatingRelease) {
                    showCreateDialog = false
                    sessionViewModel.clearCreateReleaseError()
                }
            },
            onConfirm = { tag, name, body, draft, prerelease ->

                sessionViewModel.createRelease(
                    tag,
                    name,
                    body,
                    draft,
                    prerelease
                ) {
                    showCreateDialog = false
                }
            }
        )
    }

    selectedRelease?.let { release ->

        AssetsDialog(
            release = release,
            assets = state.releaseAssets,
            isLoading = state.isLoadingReleaseAssets,
            error = state.releaseAssetsError,
            isUploading = state.isUploadingAsset,
            uploadError = state.uploadAssetError,
            onDownloadAsset = {
                sessionViewModel.downloadReleaseAsset(it)
            },
            onUploadApk = { name, bytes ->

                sessionViewModel.uploadReleaseAsset(
                    release,
                    name,
                    bytes
                ) {
                    selectedRelease = null
                }
            },
            onDismiss = {
                selectedRelease = null
                sessionViewModel.clearReleaseActionErrors()
            }
        )
    }

    PendingDownloadHandler(
        pending = state.pendingDownload,
        downloading =
            state.downloadingArtifactName != null &&
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

@Composable
private fun ReleaseSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        placeholder = {
            Text("Search releases or tags")
        },
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun ReleaseFilterRow(
    selected: ReleaseFilter,
    onSelected: (ReleaseFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        FilterChip(
            selected = selected == ReleaseFilter.ALL,
            onClick = {
                onSelected(ReleaseFilter.ALL)
            },
            label = {
                Text("All")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        FilterChip(
            selected = selected == ReleaseFilter.STABLE,
            onClick = {
                onSelected(ReleaseFilter.STABLE)
            },
            label = {
                Text("Stable")
            }
        )

        FilterChip(
            selected = selected == ReleaseFilter.PRERELEASE,
            onClick = {
                onSelected(ReleaseFilter.PRERELEASE)
            },
            label = {
                Text("Beta")
            }
        )

        FilterChip(
            selected = selected == ReleaseFilter.DRAFT,
            onClick = {
                onSelected(ReleaseFilter.DRAFT)
            },
            label = {
                Text("Draft")
            }
        )
    }
}

@Composable
private fun LatestReleaseCard(
    release: GitRelease,
    onOpenAssets: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(4.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.primary
                        .copy(alpha = 0.13f)
                ) {
                    Text(
                        text = "LATEST",
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.weight(1f))

                if (release.htmlUrl != null) {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share release"
                        )
                    }

                    IconButton(onClick = onOpen) {
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = "Open on GitHub"
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = release.name ?: release.tagName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = release.tagName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(7.dp))

            Text(
                text = "${formatRelativeTime(
                    release.publishedAt ?: release.createdAt
                )} • ${release.assets.size} asset(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                OutlinedButton(
                    onClick = onOpenAssets,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Assets")
                }

                if (release.apkAsset != null) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(Modifier.width(5.dp))

                        Text("Install APK")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseCard(
    release: GitRelease,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onOpenAssets: () -> Unit,
    onDownloadAsset: (ReleaseAsset) -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    var showDelete by remember {
        mutableStateOf(false)
    }

    var showMenu by remember {
        mutableStateOf(false)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            verticalAlignment = Alignment.Top
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = release.name ?: release.tagName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = release.tagName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box {

                IconButton(
                    onClick = {
                        showMenu = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = {
                        showMenu = false
                    }
                ) {

                    if (release.htmlUrl != null) {

                        DropdownMenuItem(
                            text = {
                                Text("Share")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onShare()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("Open on GitHub")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.OpenInNew,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onOpen()
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            showDelete = true
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            StatusChip(
                text = if (release.draft) {
                    "Draft"
                } else {
                    "Published"
                },
                color = if (release.draft) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color(0xFF2E9D65)
                }
            )

            if (release.prerelease) {
                StatusChip(
                    text = "Prerelease",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(Modifier.height(9.dp))

        Text(
            text = "${formatRelativeTime(
                release.publishedAt ?: release.createdAt
            )} • ${release.assets.size} asset(s)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedButton(
                onClick = onOpenAssets,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {

                Icon(
                    imageVector = Icons.Filled.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )

                Spacer(Modifier.width(5.dp))

                Text("Assets")
            }

            release.apkAsset?.let { apk ->

                Button(
                    onClick = {
                        onDownloadAsset(apk)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )

                    Spacer(Modifier.width(5.dp))

                    Text("Install APK")
                }
            }
        }
    }

    if (showDelete) {

        AlertDialog(
            onDismissRequest = {
                showDelete = false
            },
            title = {
                Text("Delete release?")
            },
            text = {
                Text(
                    "Release ${release.tagName} and its assets will be removed. The Git tag will remain."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        onDelete()
                    },
                    enabled = !isDeleting
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                    }
                ) {
                    Text("Cancel")
                }
            }
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
    onUploadApk: (String, ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var uploadTarget by remember {
        mutableStateOf<Pair<String, ByteArray>?>(null)
    }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            val name =
                uri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.substringAfterLast(':')
                    ?: "asset.apk"

            if (!name.lowercase().endsWith(".apk")) {

                Toast.makeText(
                    context,
                    "Please select an APK file",
                    Toast.LENGTH_SHORT
                ).show()

                return@rememberLauncherForActivityResult
            }

            val bytes = runCatching {
                context.contentResolver
                    .openInputStream(uri)
                    ?.use { input ->
                        input.readBytes()
                    }
            }.getOrNull()

            if (bytes != null) {
                uploadTarget = name to bytes
            } else {
                Toast.makeText(
                    context,
                    "Unable to read APK",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    AlertDialog(
        onDismissRequest = {
            if (!isUploading) {
                onDismiss()
            }
        },
        title = {
            Column {

                Text(
                    text = "Release assets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = release.tagName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {

            Column {

                if (isLoading) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp)
                        )
                    }

                } else if (error != null) {

                    ErrorBanner(error)

                } else if (assets.isEmpty()) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme
                                .onSurfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))

                        Text("No assets attached")
                    }

                } else {

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        assets.forEach { asset ->

                            AssetRow(
                                asset = asset,
                                onDownload = {
                                    onDownloadAsset(asset)
                                },
                                onCopyUrl = {
                                    asset.browserDownloadUrl
                                        ?.let { url ->

                                            clipboard.setText(
                                                AnnotatedString(url)
                                            )

                                            Toast.makeText(
                                                context,
                                                "Asset URL copied",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            )
                        }
                    }
                }

                if (uploadError != null) {

                    Spacer(Modifier.height(10.dp))

                    ErrorBanner(uploadError)
                }

                if (uploadTarget != null || isUploading) {

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme
                            .primary.copy(alpha = 0.08f)
                    ) {

                        Row(
                            modifier = Modifier.padding(11.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(Modifier.width(9.dp))

                            Text(
                                "Uploading APK…",
                                style = MaterialTheme.typography
                                    .bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {

            Button(
                onClick = {
                    launcher.launch("*/*")
                },
                enabled = !isUploading,
                shape = RoundedCornerShape(12.dp)
            ) {

                Icon(
                    imageVector = Icons.Filled.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text("Upload APK")
            }
        },
        dismissButton = {

            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("Done")
            }
        }
    )

    uploadTarget?.let { target ->

        LaunchedEffect(target) {

            onUploadApk(
                target.first,
                target.second
            )

            uploadTarget = null
        }
    }
}

@Composable
private fun AssetRow(
    asset: ReleaseAsset,
    onDownload: () -> Unit,
    onCopyUrl: () -> Unit
) {
    var showMenu by remember {
        mutableStateOf(false)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme
            .surfaceVariant.copy(alpha = 0.45f)
    ) {

        Row(
            modifier = Modifier.padding(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(9.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = if (asset.size > 0) {
                        formatFileSize(asset.size)
                    } else {
                        "GitHub asset"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDownload
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription =
                        "Download ${asset.name}"
                )
            }

            if (asset.browserDownloadUrl != null) {

                Box {

                    IconButton(
                        onClick = {
                            showMenu = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription =
                                "Asset options"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text("Copy download URL")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector =
                                        Icons.Filled.ContentCopy,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onCopyUrl()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 4.dp
            ),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyReleasesState(
    onCreate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 55.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    MaterialTheme.colorScheme.primary
                        .copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = Icons.Filled.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "No releases yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(7.dp))

        Text(
            text = "Create your first GitHub release and attach an APK.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = onCreate,
            shape = RoundedCornerShape(13.dp)
        ) {

            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null
            )

            Spacer(Modifier.width(6.dp))

            Text("Create release")
        }
    }
}

@Composable
private fun NoSearchResults() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 45.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.size(38.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "No matching releases",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Try another release name, tag, or filter.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 45.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        CircularProgressIndicator(
            modifier = Modifier.size(34.dp),
            strokeWidth = 3.dp
        )

        Spacer(Modifier.height(13.dp))

        Text(
            text = "Loading releases…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {

        Row(
            modifier = Modifier.padding(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(Modifier.width(9.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun CreateReleaseDialog(
    isCreating: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (
        String,
        String?,
        String?,
        Boolean,
        Boolean
    ) -> Unit
) {
    var tag by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(false) }
    var prerelease by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isCreating) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = "Create release",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {

            Column {

                OutlinedTextField(
                    value = tag,
                    onValueChange = {
                        tag = it
                    },
                    label = {
                        Text("Tag name")
                    },
                    placeholder = {
                        Text("v1.0.0")
                    },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(9.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("Release title")
                    },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(9.dp))

                OutlinedTextField(
                    value = body,
                    onValueChange = {
                        body = it
                    },
                    label = {
                        Text("Release notes")
                    },
                    minLines = 4,
                    maxLines = 7,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(10.dp))

                SettingRow(
                    title = "Draft",
                    subtitle = "Keep release unpublished",
                    checked = draft,
                    enabled = !isCreating,
                    onCheckedChange = {
                        draft = it
                    }
                )

                SettingRow(
                    title = "Prerelease",
                    subtitle = "Mark as testing release",
                    checked = prerelease,
                    enabled = !isCreating,
                    onCheckedChange = {
                        prerelease = it
                    }
                )

                if (errorMessage != null) {

                    Spacer(Modifier.height(8.dp))

                    ErrorBanner(errorMessage)
                }
            }
        },
        confirmButton = {

            Button(
                onClick = {
                    onConfirm(
                        tag.trim(),
                        name.trim().ifBlank { null },
                        body.trim().ifBlank { null },
                        draft,
                        prerelease
                    )
                },
                enabled = tag.isNotBlank() && !isCreating,
                shape = RoundedCornerShape(12.dp)
            ) {

                if (isCreating) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(17.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(Modifier.width(7.dp))

                    Text("Creating…")

                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {

            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0

    return when {
        bytes >= gb ->
            String.format("%.1f GB", bytes / gb)

        bytes >= mb ->
            String.format("%.1f MB", bytes / mb)

        bytes >= kb ->
            String.format("%.0f KB", bytes / kb)

        else ->
            "$bytes B"
    }
}

private fun shareUrl(
    context: Context,
    url: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }

    context.startActivity(
        Intent.createChooser(
            intent,
            "Share release"
        )
    )
}

private fun openUrl(
    context: Context,
    url: String
) {
    runCatching {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )
    }.onFailure {
        Toast.makeText(
            context,
            "Unable to open link",
            Toast.LENGTH_SHORT
        ).show()
    }
}