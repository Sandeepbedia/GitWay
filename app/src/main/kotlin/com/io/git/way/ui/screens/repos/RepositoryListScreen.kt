package com.io.git.way.ui.screens.repos

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.io.git.way.ui.theme.GlassFloatingBottomNav
import com.io.git.way.ui.theme.GlassIconButton
import com.io.git.way.ui.theme.GlassPrimaryButton
import com.io.git.way.ui.theme.GlassSearchField
import com.io.git.way.ui.theme.GlassSecondaryButton
import com.io.git.way.ui.theme.GlassSkeletonCard
import com.io.git.way.ui.theme.LiquidGlassBackground
import com.io.git.way.ui.theme.repoLanguageLabel
import com.io.git.way.ui.theme.repoVisualFor
import kotlinx.coroutines.delay

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
            bottomBar = {
                GlassFloatingBottomNav(
                    selected = BottomNavTab.REPOSITORIES,
                    onSelect = { tab ->
                        when (tab) {
                            BottomNavTab.OVERVIEW -> onNavigateOverview()
                            BottomNavTab.REPOSITORIES -> Unit
                            BottomNavTab.PROFILE -> onNavigateProfile()
                        }
                    }
                )
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
                    onNewRepoClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/new")))
                    }
                )

                GlassSearchField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = "Search repositories...",
                    modifier = Modifier.padding(bottom = 14.dp)
                )

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
                    state.isLoading -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(6) { GlassSkeletonCard() }
                    }

                    state.errorMessage != null -> RepositoryErrorState(
                        message = state.errorMessage,
                        onRetry = { viewModel.loadRepositories() }
                    )

                    visibleRepos.isEmpty() -> RepositoryEmptyState(
                        onCreate = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/new"))) }
                    )

                    else -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
                    ) {
                        itemsIndexed(visibleRepos, key = { _, repo -> repo.fullName }) { index, repo ->
                            AnimatedRepoCard(
                                repo = repo,
                                index = index,
                                onClick = { onRepositorySelected(repo) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
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
private fun AnimatedRepoCard(
    repo: GitRepository,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember(repo.fullName) { mutableStateOf(false) }
    LaunchedEffect(repo.fullName) {
        delay((index * 45L).coerceAtMost(400L))
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(280)) +
            slideInVertically(
                animationSpec = androidx.compose.animation.core.tween(280),
                initialOffsetY = { it / 4 }
            )
    ) {
        RepoCard(repo = repo, onClick = onClick, modifier = modifier)
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
        Row(modifier = Modifier.fillMaxWidth().height(104.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(visual.color)
            )
            Row(
                modifier = Modifier.weight(1f).padding(14.dp),
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
