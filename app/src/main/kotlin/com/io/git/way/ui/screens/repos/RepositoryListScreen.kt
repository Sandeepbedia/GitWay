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

package com.io.git.way.ui.screens.repos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.lifecycle.viewmodel.compose.viewModel
import com.io.git.way.GitWayApp
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.ui.common.GitWayViewModelFactory
import com.io.git.way.ui.common.formatRelativeTime
import com.io.git.way.ui.theme.AppThemeMode
import com.io.git.way.ui.theme.BottomNavTab
import com.io.git.way.ui.theme.GlassChip
import com.io.git.way.ui.theme.GlassClickableCard
import com.io.git.way.ui.theme.GlassFab
import com.io.git.way.ui.theme.GlassFloatingBottomNav
import com.io.git.way.ui.theme.GlassIconButton
import com.io.git.way.ui.theme.GlassPrimaryButton
import com.io.git.way.ui.theme.GlassSearchField
import com.io.git.way.ui.theme.GlassSecondaryButton
import com.io.git.way.ui.theme.GlassSkeletonCard
import com.io.git.way.ui.theme.LiquidGlassBackground
import com.io.git.way.ui.theme.repoLanguageLabel
import com.io.git.way.ui.theme.repoVisualFor

private enum class RepoSort { RECENT, NAME, NEWEST, OLDEST }
private enum class PrivacyFilter { ALL, PRIVATE_ONLY, PUBLIC_ONLY }

/**
 * Screen 3: "Your Repositories" — premium Material 3 Expressive glass redesign
 * (see Repository Screen UI/UX PRD): frosted cards, floating bottom nav, animated
 * entrance, skeleton loading, and Recent/A-Z sort with a Filter sheet for the rest.
 */
@Composable
fun RepositoryListScreen(
    onRepositorySelected: (GitRepository) -> Unit,
    onDisconnect: () -> Unit,
    onNavigateOverview: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    viewModel: RepositoryListViewModel = viewModel(
        factory = GitWayViewModelFactory(
            (LocalContext.current.applicationContext as GitWayApp).container.gitHubRepository
        )
    )
) {
    val context = LocalContext.current
    val state = viewModel.uiState

    var sort by remember { mutableStateOf(RepoSort.RECENT) }
    var privacyFilter by remember { mutableStateOf(PrivacyFilter.ALL) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var showCreateRepoDialog by remember { mutableStateOf(false) }

    val visibleRepos = state.filtered
        .let { list ->
            when (privacyFilter) {
                PrivacyFilter.ALL -> list
                PrivacyFilter.PRIVATE_ONLY -> list.filter { it.isPrivate }
                PrivacyFilter.PUBLIC_ONLY -> list.filter { !it.isPrivate }
            }
        }
        .let { list ->
            when (sort) {
                RepoSort.RECENT -> list
                RepoSort.NAME -> list.sortedBy { it.name.lowercase() }
                RepoSort.NEWEST -> list.sortedByDescending { it.createdAt }
                RepoSort.OLDEST -> list.sortedBy { it.createdAt }
            }
        }

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                GlassFab(
                    onClick = {
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) viewModel.onSearchQueryChange("")
                    },
                    size = 64.dp,
                    // With the floating nav now an overlay (not Scaffold's bottomBar,
                    // which used to lift the FAB above it automatically), the FAB needs
                    // its own clearance so it floats above the nav dock instead of
                    // behind/under it.
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 96.dp)
                ) {
                    Icon(
                        if (searchExpanded) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = if (searchExpanded) "Close search" else "Search repositories",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                RepositoryHeader(
                    onFilterClick = { showFilterSheet = true },
                    onNewRepoClick = { showCreateRepoDialog = true }
                )

                if (searchExpanded) {
                    GlassSearchField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = "Search repositories...",
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    GlassChip(
                        text = "Recent",
                        selected = sort == RepoSort.RECENT,
                        onClick = { sort = RepoSort.RECENT },
                        leadingIcon = { Icon(Icons.Filled.Schedule, null, Modifier.size(14.dp)) }
                    )
                    GlassChip(
                        text = "A-Z",
                        selected = sort == RepoSort.NAME,
                        onClick = { sort = RepoSort.NAME },
                        leadingIcon = { Icon(Icons.Filled.SortByAlpha, null, Modifier.size(14.dp)) }
                    )
                }

                when {
                    state.isLoading -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp)
                    ) {
                        items(6) { GlassSkeletonCard() }
                    }

                    state.errorMessage != null -> RepositoryErrorState(
                        message = state.errorMessage,
                        onRetry = { viewModel.loadRepositories() }
                    )

                    visibleRepos.isEmpty() -> RepositoryEmptyState(
                        onCreate = { showCreateRepoDialog = true }
                    )

                    else -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        // Bottom clearance for the floating nav dock, which now overlays
                        // the screen instead of reserving Scaffold bottomBar space.
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp)
                    ) {
                        itemsIndexed(visibleRepos, key = { _, repo -> repo.fullName }) { _, repo ->
                            RepoCard(
                                repo = repo,
                                onClick = { onRepositorySelected(repo) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }

        GlassFloatingBottomNav(
            selected = BottomNavTab.REPOSITORIES,
            onSelect = { tab ->
                when (tab) {
                    BottomNavTab.OVERVIEW -> onNavigateOverview()
                    BottomNavTab.REPOSITORIES -> Unit
                    BottomNavTab.PROFILE -> onNavigateProfile()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showFilterSheet) {
        FilterSheet(
            sort = sort,
            onSortChange = { sort = it },
            privacyFilter = privacyFilter,
            onPrivacyFilterChange = { privacyFilter = it },
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            onDisconnect = onDisconnect,
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showCreateRepoDialog) {
        CreateRepositoryDialog(
            isCreating = state.isCreatingRepo,
            errorMessage = state.createRepoError,
            onDismiss = { showCreateRepoDialog = false; viewModel.clearCreateRepoError() },
            onConfirm = { name, description, isPrivate ->
                viewModel.createRepository(name, description, isPrivate) { repo -> onRepositorySelected(repo) }
            },
            onCreated = { showCreateRepoDialog = false }
        )
    }
}

@Composable
private fun RepositoryHeader(
    onFilterClick: () -> Unit,
    onNewRepoClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                "Your Repositories",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Manage and collaborate on your projects",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassIconButton(onClick = onFilterClick) {
                Icon(Icons.Filled.Tune, contentDescription = "Filter")
            }
            GlassIconButton(onClick = onNewRepoClick) {
                Icon(Icons.Filled.Add, contentDescription = "New repository")
            }
        }
    }
}

@Composable
private fun RepoCard(
    repo: GitRepository,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = repoVisualFor(repo.name, repo.language)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "repoCardScale")

    GlassClickableCard(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        padding = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(visual.color, visual.color.copy(alpha = 0f))))
            )
            Row(
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(visual.color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(visual.icon, contentDescription = null, tint = visual.color, modifier = Modifier.size(22.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            repo.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (repo.isPrivate) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Private",
                                modifier = Modifier.padding(start = 6.dp).size(14.dp)
                            )
                        }
                    }
                    Text(
                        repo.owner,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    LanguageChip(text = repoLanguageLabel(repo.language), color = visual.color)
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight().padding(vertical = 2.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(visual.color))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            formatRelativeTime(repo.lastUpdated),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RepositoryEmptyState(onCreate: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.FolderOff,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "No repositories found",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
            )
            GlassPrimaryButton(
                text = "Create Repository",
                onClick = onCreate,
                modifier = Modifier.width(220.dp)
            )
        }
    }
}

@Composable
private fun RepositoryErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
            )
            GlassSecondaryButton(text = "Retry", onClick = onRetry, modifier = Modifier.width(160.dp))
        }
    }
}

@Composable
private fun FilterSheet(
    sort: RepoSort,
    onSortChange: (RepoSort) -> Unit,
    privacyFilter: PrivacyFilter,
    onPrivacyFilterChange: (PrivacyFilter) -> Unit,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Sort by", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp, bottom = 20.dp)
            ) {
                GlassChip("Newest", selected = sort == RepoSort.NEWEST, onClick = { onSortChange(RepoSort.NEWEST) })
                GlassChip("Oldest", selected = sort == RepoSort.OLDEST, onClick = { onSortChange(RepoSort.OLDEST) })
            }

            Text("Visibility", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp, bottom = 20.dp)
            ) {
                GlassChip(
                    "Private",
                    selected = privacyFilter == PrivacyFilter.PRIVATE_ONLY,
                    onClick = {
                        onPrivacyFilterChange(
                            if (privacyFilter == PrivacyFilter.PRIVATE_ONLY) PrivacyFilter.ALL else PrivacyFilter.PRIVATE_ONLY
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.Lock, null, Modifier.size(14.dp)) }
                )
                GlassChip(
                    "Public",
                    selected = privacyFilter == PrivacyFilter.PUBLIC_ONLY,
                    onClick = {
                        onPrivacyFilterChange(
                            if (privacyFilter == PrivacyFilter.PUBLIC_ONLY) PrivacyFilter.ALL else PrivacyFilter.PUBLIC_ONLY
                        )
                    },
                    leadingIcon = { Icon(Icons.Filled.Public, null, Modifier.size(14.dp)) }
                )
            }

            Divider(modifier = Modifier.padding(bottom = 16.dp))

            Text("Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassSecondaryButton(
                    text = themeModeLabel(themeMode),
                    onClick = { onThemeModeChange(nextThemeMode(themeMode)) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(themeModeIcon(themeMode), null, Modifier.size(18.dp)) }
                )
                GlassSecondaryButton(
                    text = "Disconnect",
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Filled.Logout, null, Modifier.size(18.dp)) }
                )
            }
        }
    }
}

private fun nextThemeMode(current: AppThemeMode): AppThemeMode = when (current) {
    AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
    AppThemeMode.LIGHT -> AppThemeMode.DARK
    AppThemeMode.DARK -> AppThemeMode.AMOLED
    AppThemeMode.AMOLED -> AppThemeMode.SYSTEM
}

private fun themeModeIcon(mode: AppThemeMode) = when (mode) {
    AppThemeMode.SYSTEM -> Icons.Filled.Brightness6
    AppThemeMode.LIGHT -> Icons.Filled.Brightness7
    AppThemeMode.DARK -> Icons.Filled.Brightness4
    AppThemeMode.AMOLED -> Icons.Filled.DarkMode
}

private fun themeModeLabel(mode: AppThemeMode): String = when (mode) {
    AppThemeMode.SYSTEM -> "Theme: System"
    AppThemeMode.LIGHT -> "Theme: Light"
    AppThemeMode.DARK -> "Theme: Dark"
    AppThemeMode.AMOLED -> "Theme: AMOLED"
}

/** In-app "New repository" — replaces the old external "open github.com/new in a
 * browser" shortcut with a real create flow through the GitHub API. `auto_init = true`
 * on the backend call means the new repo always has a real first commit (a README), so
 * it's immediately usable — no "empty repository" edge case to navigate into. */
@Composable
private fun CreateRepositoryDialog(
    isCreating: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, isPrivate: Boolean) -> Unit,
    onCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(true) }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(isCreating, errorMessage) {
        if (submitted && !isCreating && errorMessage == null) onCreated()
    }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("New repository") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Repository name") },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (isPrivate) "Private" else "Public", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (isPrivate) "Only you (and collaborators) can see this repo" else "Anyone can see this repo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it }, enabled = !isCreating)
                }
                if (errorMessage != null) {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { submitted = true; onConfirm(name, description, isPrivate) },
                enabled = name.isNotBlank() && !isCreating
            ) {
                if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel") } }
    )
}
