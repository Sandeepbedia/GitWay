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
package com.io.git.way.ui.screens.repos

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.io.git.way.GitWayApp
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.ui.common.GitWayViewModelFactory
import com.io.git.way.ui.common.formatRelativeTime
import com.io.git.way.ui.theme.AppThemeMode
import com.io.git.way.ui.theme.BottomNavTab
import com.io.git.way.ui.theme.GlassFloatingBottomNav
import com.io.git.way.ui.theme.GlassPrimaryButton
import com.io.git.way.ui.theme.GlassSearchField
import com.io.git.way.ui.theme.GlassSecondaryButton
import com.io.git.way.ui.theme.LiquidGlassBackground
import com.io.git.way.ui.theme.RepoWarning
import com.io.git.way.ui.theme.repoLanguageLabel
import com.io.git.way.ui.theme.repoVisualFor

/* -------------------------------------------------------------------------- */
/* Bravo-inspired palette for the repository list (neumorphic purple accent)  */
/* -------------------------------------------------------------------------- */

private val MiuBlue = Color(0xFF7F5CF0)

private val MiuDarkRow = Color(0xFF15171C)
private val MiuDarkPressed = Color(0xFF1D2026)
private val MiuDarkSelected = Color(0xFF20242E)
private val MiuLightRow = Color(0xFFFFFFFF)
private val MiuLightPressed = Color(0xFFE7EBF0)
private val MiuLightSelected = Color(0xFFE9E2FF)

private val MiuBorderDark = Color(0x12FFFFFF)   // ~7% white hairline
private val MiuBorderLight = Color(0x0F000000)  // ~6% black hairline

/* Language indicator dots use the standard GitHub language colors (PRD §7). */
private val LangColorKotlin = Color(0xFF7F52FF)
private val LangColorJava = Color(0xFFF89820)
private val LangColorPython = Color(0xFF3776AB)
private val LangColorJavaScript = Color(0xFFF7DF1E)
private val LangColorCpp = Color(0xFF00599C)
private val LangColorRust = Color(0xFFDEA584)
private val LangColorGo = Color(0xFF00ADD8)
private val LangColorNeutral = Color(0xFF6B7280)

private fun languageColor(language: String?): Color = when (language?.lowercase()) {
    "kotlin", "kts" -> LangColorKotlin
    "java" -> LangColorJava
    "python" -> LangColorPython
    "javascript", "typescript", "js", "ts", "jsx", "tsx" -> LangColorJavaScript
    "c++", "cpp", "c", "objective-c" -> LangColorCpp
    "rust" -> LangColorRust
    "go", "golang" -> LangColorGo
    else -> LangColorNeutral
}

private enum class RepoFilter(val label: String) {
    ALL("All"),
    RECENT("Recent"),
    STARRED("Starred"),
    FORKS("Forks")
}

private enum class RepoSorter(val label: String) {
    UPDATED("Recently updated"),
    CREATED("Recently created"),
    NAME_ASC("Name A–Z"),
    NAME_DESC("Name Z–A"),
    STARS("Most stars"),
    FORKS("Most forks")
}

/* -------------------------------------------------------------------------- */
/* SCREEN                                                                     */
/* -------------------------------------------------------------------------- */

/**
 * Screen 3: "Your Repositories" — Bravo-inspired compact, information-dense
 * repository list (see Repository List Rearrangement PRD): neutral surfaces, a
 * top search field, filter chips, a sort sheet, pinned/recent sections, compact
 * 88dp rows with a small language dot, and a per-row overflow sheet.
 */
@Composable
fun RepositoryListScreen(
    onRepositorySelected: (GitRepository) -> Unit,
    onDisconnect: () -> Unit,
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
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var filter by remember { mutableStateOf(RepoFilter.ALL) }
    var sort by remember { mutableStateOf(RepoSorter.UPDATED) }
    var searchOpen by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var actionsRepo by remember { mutableStateOf<GitRepository?>(null) }
    var pinned by rememberSaveable { mutableStateOf(listOf<String>()) }
    var showCreateRepoDialog by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    // Search covers name, owner and language (the model has no description field).
    val searched = remember(state.repositories, state.searchResults, state.isGitHubSearch, state.searchQuery) {
        if (state.isGitHubSearch) {
            state.searchResults
        } else if (state.searchQuery.isBlank()) {
            state.repositories
        } else {
            val q = state.searchQuery.trim()
            state.repositories.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.owner.contains(q, ignoreCase = true) ||
                    (it.language?.contains(q, ignoreCase = true) ?: false)
            }
        }
    }

    val baseList = when (filter) {
        RepoFilter.STARRED -> searched.filter { state.starredRepos.contains(it.fullName) }
        RepoFilter.FORKS -> searched.filter { it.isFork }
        RepoFilter.ALL, RepoFilter.RECENT -> searched
    }
    val ordered = when {
        filter == RepoFilter.RECENT || sort == RepoSorter.UPDATED -> baseList.sortedByDescending { it.lastUpdated }
        sort == RepoSorter.CREATED -> baseList.sortedByDescending { it.createdAt }
        sort == RepoSorter.NAME_ASC -> baseList.sortedBy { it.name.lowercase() }
        sort == RepoSorter.NAME_DESC -> baseList.sortedByDescending { it.name.lowercase() }
        sort == RepoSorter.STARS -> baseList.sortedByDescending { it.stargazersCount }
        else -> baseList.sortedByDescending { it.forksCount }
    }

    val pinnedRepos = ordered.filter { it.fullName in pinned }
    val unpinned = ordered.filter { it.fullName !in pinned }
    val showRecentHeader = filter == RepoFilter.ALL && sort == RepoSorter.UPDATED && unpinned.isNotEmpty()
    val recentRepos = if (filter == RepoFilter.RECENT) unpinned else if (showRecentHeader) unpinned.take(3) else emptyList()
    val restRepos = if (filter == RepoFilter.RECENT) emptyList() else if (showRecentHeader) unpinned.drop(3) else unpinned
    val visibleCount = ordered.size

    fun togglePinned(fullName: String) {
        if (fullName in pinned) {
            pinned = pinned - fullName
        } else if (pinned.size >= 3) {
            Toast.makeText(context, "Maximum 3 pinned repositories", Toast.LENGTH_SHORT).show()
        } else {
            pinned = pinned + fullName
        }
    }

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                /* Top app bar — Title / Search / New (PRD §2) */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(54.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Repositories",
                        modifier = Modifier.weight(1f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { searchOpen = !searchOpen }) {
                        Icon(
                            if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showCreateRepoDialog = true }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "New repository",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                /* Search field (PRD §11) */
                if (searchOpen) {
                    GlassSearchField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = if (state.isGitHubSearch) "Search GitHub (all repositories)..." else "Search repositories...",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                    )
                    if (state.isGitHubSearch) {
                        Text(
                            "Live results from GitHub's Search API.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
                        )
                        if (state.searchError != null) {
                            Text(
                                state.searchError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
                            )
                        }
                    }
                }

                /* Summary row — count + sort (PRD §2, §10) */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (state.searchQuery.isNotBlank()) "$visibleCount results" else "$visibleCount repositories",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (dark) MiuDarkRow else MiuLightPressed)
                            .clickable { showSortSheet = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Filled.Sort, contentDescription = null, tint = MiuBlue, modifier = Modifier.size(16.dp))
                        Text("Sort", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MiuBlue)
                    }
                }

                /* Filter chips — All / Recent / Starred / Forks (PRD §9) */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepoFilter.entries.forEach { f ->
                        MiuFilterChip(
                            label = f.label,
                            selected = filter == f,
                            dark = dark,
                            onClick = { filter = f }
                        )
                    }
                }

                /* Content — skeleton / error / empty / list (PRD §16-19) */
                val enterProgress by animateFloatAsState(
                    targetValue = if (entered) 1f else 0f,
                    animationSpec = tween(280),
                    label = "screenEnter"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = enterProgress
                            translationY = (1f - enterProgress) * 24f
                        }
                ) {
                    when {
                            state.isLoading -> LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp)
                            ) {
                                items(5) { RepositorySkeleton(dark = dark) }
                            }

                            state.errorMessage != null -> RepositoryErrorState(
                                message = state.errorMessage,
                                onRetry = { viewModel.loadRepositories() }
                            )

                            ordered.isEmpty() -> RepositoryEmptyState(
                                hasQuery = state.searchQuery.isNotBlank() || filter != RepoFilter.ALL,
                                onRefresh = { viewModel.loadRepositories() },
                                onCreate = { showCreateRepoDialog = true }
                            )

                            else -> LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp)
                            ) {
                                if (pinnedRepos.isNotEmpty()) {
                                    item(key = "pinned-header") { SectionHeader("Pinned") }
                                    items(pinnedRepos, key = { "pinned-${it.fullName}" }) { repo ->
                                        RepositoryRow(
                                            repo = repo,
                                            isStarred = state.starredRepos.contains(repo.fullName),
                                            isPinned = true,
                                            dark = dark,
                                            onClick = { onRepositorySelected(repo) },
                                            onActions = { actionsRepo = repo },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                                if (recentRepos.isNotEmpty()) {
                                    item(key = "recent-header") { SectionHeader("Recent repositories") }
                                    items(recentRepos, key = { "recent-${it.fullName}" }) { repo ->
                                        RepositoryRow(
                                            repo = repo,
                                            isStarred = state.starredRepos.contains(repo.fullName),
                                            isPinned = false,
                                            dark = dark,
                                            onClick = { onRepositorySelected(repo) },
                                            onActions = { actionsRepo = repo },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                                if (restRepos.isNotEmpty()) {
                                    if (showRecentHeader) item(key = "all-header") { SectionHeader("All repositories") }
                                    itemsIndexed(restRepos, key = { _, repo -> repo.fullName }) { _, repo ->
                                        RepositoryRow(
                                            repo = repo,
                                            isStarred = state.starredRepos.contains(repo.fullName),
                                            isPinned = false,
                                            dark = dark,
                                            onClick = { onRepositorySelected(repo) },
                                            onActions = { actionsRepo = repo },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }
                    }
                }
            }
        }

        GlassFloatingBottomNav(
            selected = BottomNavTab.REPOSITORIES,
            onSelect = { tab ->
                when (tab) {
                    BottomNavTab.REPOSITORIES -> Unit
                    BottomNavTab.PROFILE -> onNavigateProfile()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    actionsRepo?.let { repo ->
        RepoActionsSheet(
            repo = repo,
            isStarred = state.starredRepos.contains(repo.fullName),
            isPinned = repo.fullName in pinned,
            isTogglingStar = state.isTogglingStar == repo.fullName,
            onOpen = { onRepositorySelected(repo); actionsRepo = null },
            onOpenBrowser = { context.openRepoInBrowser(repo); actionsRepo = null },
            onToggleStar = { viewModel.toggleStar(repo) },
            onTogglePin = { togglePinned(repo.fullName); actionsRepo = null },
            onCopyUrl = { context.copyRepoUrl(repo); actionsRepo = null },
            onShare = { context.shareRepo(repo); actionsRepo = null },
            onRefresh = { viewModel.loadRepositories(); actionsRepo = null },
            onDismiss = { actionsRepo = null }
        )
    }

    if (showSortSheet) {
        SortSheet(
            current = sort,
            onSelect = { sort = it },
            onDismiss = { showSortSheet = false }
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

/* -------------------------------------------------------------------------- */
/* COMPONENTS                                                                 */
/* -------------------------------------------------------------------------- */

@Composable
private fun MiuFilterChip(label: String, selected: Boolean, dark: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MiuBlue else if (dark) MiuDarkRow else MiuLightPressed
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
    )
}

/** Compact Bravo-style row: 44dp tile + name/owner/metadata/stats + overflow (PRD §3-7). */
@Composable
private fun RepositoryRow(
    repo: GitRepository,
    isStarred: Boolean,
    isPinned: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
    onActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            pressed -> if (dark) MiuDarkPressed else MiuLightPressed
            isPinned -> if (dark) MiuDarkSelected else MiuLightSelected
            else -> if (dark) MiuDarkRow else MiuLightRow
        },
        animationSpec = tween(140),
        label = "repoRowBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(140),
        label = "repoRowScale"
    )
    val visual = repoVisualFor(repo.name, repo.language)
    val langColor = languageColor(repo.language)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(bg)
            .border(1.dp, if (dark) MiuBorderDark else MiuBorderLight, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(langColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(visual.icon, contentDescription = null, tint = langColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = repo.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = repo.owner,
                    fontSize = 13.sp,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(langColor))
                    Text(repoLanguageLabel(repo.language), fontSize = 12.sp, color = muted, maxLines = 1)
                    Text("•", fontSize = 12.sp, color = muted)
                    Text(formatRelativeTime(repo.lastUpdated), fontSize = 12.sp, color = muted, maxLines = 1)
                }
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(
                            if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (isStarred) RepoWarning else muted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("${repo.stargazersCount}", fontSize = 12.sp, color = muted)
                    }
                    if (repo.forksCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Filled.CallSplit, contentDescription = null, tint = muted, modifier = Modifier.size(14.dp))
                            Text("${repo.forksCount}", fontSize = 12.sp, color = muted)
                        }
                    }
                    RepoStatus(repo = repo, color = muted)
                }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onActions
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Repository actions", tint = muted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Muted Public / Private / Fork indicator (PRD §8). */
@Composable
private fun RepoStatus(repo: GitRepository, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        when {
            repo.isFork -> {
                Icon(Icons.Filled.CallSplit, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                Text("Fork", fontSize = 12.sp, color = color)
            }
            repo.isPrivate -> {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                Text("Private", fontSize = 12.sp, color = color)
            }
            else -> {
                Icon(Icons.Filled.Public, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                Text("Public", fontSize = 12.sp, color = color)
            }
        }
    }
}

/** Compact skeleton matching the row layout — no colored placeholder cards (PRD §17). */
@Composable
private fun RepositorySkeleton(dark: Boolean) {
    val shimmer = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (dark) MiuDarkRow else MiuLightRow)
            .border(1.dp, if (dark) MiuBorderDark else MiuBorderLight, shape)
            .padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(shimmer))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth(0.42f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(shimmer))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.3f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(shimmer))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.55f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(shimmer))
        }
    }
}

@Composable
private fun RepositoryEmptyState(hasQuery: Boolean, onRefresh: () -> Unit, onCreate: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.FolderOff,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "No repositories yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                if (hasQuery) "No repositories match your search." else "Connect your GitHub account or refresh to load repositories.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
            )
            GlassPrimaryButton(text = "Refresh", onClick = onRefresh, modifier = Modifier.width(180.dp))
            if (!hasQuery) {
                TextButton(onClick = onCreate, modifier = Modifier.padding(top = 8.dp)) { Text("New repository") }
            }
        }
    }
}

@Composable
private fun RepositoryErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Text(
                "Unable to load repositories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
            )
            GlassSecondaryButton(text = "Retry", onClick = onRetry, modifier = Modifier.width(160.dp))
        }
    }
}

/** Sort options with a blue check on the active row (PRD §10). */
@Composable
private fun SortSheet(current: RepoSorter, onSelect: (RepoSorter) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                "Sort repositories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            RepoSorter.entries.forEach { option ->
                val selected = option == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSelect(option)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.label,
                        modifier = Modifier.weight(1f),
                        fontSize = 15.sp,
                        color = if (selected) MiuBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                    if (selected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MiuBlue, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Per-repository overflow sheet (PRD §12). */
@Composable
private fun RepoActionsSheet(
    repo: GitRepository,
    isStarred: Boolean,
    isPinned: Boolean,
    isTogglingStar: Boolean,
    onOpen: () -> Unit,
    onOpenBrowser: () -> Unit,
    onToggleStar: () -> Unit,
    onTogglePin: () -> Unit,
    onCopyUrl: () -> Unit,
    onShare: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        repo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        repo.fullName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (repo.isPrivate) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Private",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            SheetAction(Icons.Filled.Folder, "Open repository", onOpen)
            SheetAction(Icons.Filled.OpenInNew, "Open in browser", onOpenBrowser)
            SheetAction(
                icon = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                label = if (isStarred) "Unstar" else "Star",
                onClick = onToggleStar,
                trailing = {
                    if (isTogglingStar) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }
            )
            SheetAction(Icons.Filled.Link, "Copy URL", onCopyUrl)
            SheetAction(Icons.Filled.Share, "Share", onShare)
            SheetAction(Icons.Filled.PushPin, if (isPinned) "Unpin" else "Pin", onTogglePin)
            Spacer(Modifier.height(8.dp))
            SheetAction(Icons.Filled.Refresh, "Refresh list", onRefresh)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        trailing?.invoke()
    }
}

/* -------------------------------------------------------------------------- */
/* INTENTS                                                                     */
/* -------------------------------------------------------------------------- */

private fun repoUrl(repo: GitRepository) = "https://github.com/${repo.owner}/${repo.name}"

private fun Context.copyRepoUrl(repo: GitRepository) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Repository URL", repoUrl(repo)))
    Toast.makeText(this, "URL copied", Toast.LENGTH_SHORT).show()
}

private fun Context.shareRepo(repo: GitRepository) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, repo.name)
        putExtra(Intent.EXTRA_TEXT, "${repo.fullName}\n${repoUrl(repo)}")
    }
    startActivity(Intent.createChooser(send, "Share repository"))
}

private fun Context.openRepoInBrowser(repo: GitRepository) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl(repo)))) }
}

/* -------------------------------------------------------------------------- */
/* CREATE REPOSITORY                                                          */
/* -------------------------------------------------------------------------- */

/** In-app "New repository" — replaces the old external "open github.com/new in a
 *  browser" shortcut with a real create flow through the GitHub API. `auto_init = true`
 *  on the backend call means the new repo always has a real first commit (a README), so
 *  it's immediately usable — no "empty repository" edge case to navigate into. */
@Composable
fun CreateRepositoryDialog(
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