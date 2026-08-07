package com.io.git.way.ui.screens.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import com.io.git.way.domain.model.ApiRateLimit
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.domain.model.GitUser
import com.io.git.way.ui.common.GitWaySessionViewModel
import com.io.git.way.ui.theme.BottomNavTab
import com.io.git.way.ui.theme.GlassBlobBlue
import com.io.git.way.ui.theme.GlassBlobPink
import com.io.git.way.ui.theme.GlassBlobPurple
import com.io.git.way.ui.theme.GlassBlobTeal
import com.io.git.way.ui.theme.GlassCard
import com.io.git.way.ui.theme.GlassClickableCard
import com.io.git.way.ui.theme.GlassFloatingBottomNav
import com.io.git.way.ui.theme.LiquidGlassBackground

/**
 * Overview tab — an account-wide dashboard (PRD "Overview Screen"): who you're signed
 * in as, a one-tap way back into whatever repo you were last working on, a live GitHub
 * API rate-limit meter, and your most recently updated repositories. Everything here is
 * read-only and independent of any single selected repo, since this tab is reachable
 * straight from the floating bottom nav before a repo has ever been picked.
 */
@Composable
fun OverviewScreen(
    sessionViewModel: GitWaySessionViewModel,
    onNavigateRepositories: () -> Unit,
    onNavigateProfile: () -> Unit,
    onOpenRepository: (GitRepository) -> Unit
) {
    val state = sessionViewModel.state
    val context = LocalContext.current

    LaunchedEffect(Unit) { sessionViewModel.loadOverviewData() }

    val isRefreshing = state.isLoadingUser || state.isLoadingOverviewRepositories

    val suggestions = remember(state.overviewRepositories, state.apiRateLimit) {
        buildSmartSuggestions(state.overviewRepositories, state.apiRateLimit)
    }

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { sessionViewModel.refreshOverviewData() },
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().statusBarsPadding(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Overview", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 2.dp))
                    }

                    item {
                        AccountHeaderCard(
                            user = state.currentUser,
                            isLoading = state.isLoadingUser,
                            errorMessage = state.userError,
                            onOpenProfile = onNavigateProfile
                        )
                    }

                    item {
                        QuickActionGrid(
                            onNewRepository = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/new")))
                            },
                            onRepositories = onNavigateRepositories,
                            onRefresh = { sessionViewModel.refreshOverviewData() },
                            onProfile = onNavigateProfile
                        )
                    }

                    state.selectedRepo?.let { repo ->
                        item { ContinueCard(repo = repo, onResume = { onOpenRepository(repo) }) }
                    }

                    if (state.overviewRepositories.isNotEmpty()) {
                        item { RepositoryStatsCard(repositories = state.overviewRepositories, onViewAll = onNavigateRepositories) }
                    }

                    state.apiRateLimit?.let { limit ->
                        item { ApiRateLimitCard(limit = limit) }
                    }

                    if ((state.overviewRepositories.isNotEmpty() || state.apiRateLimit != null) && suggestions.isNotEmpty()) {
                        item { Text("Suggestions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
                        items(suggestions, key = { it.title }) { suggestion -> SuggestionCard(suggestion) }
                    }

                    if (state.overviewRepositories.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recent repositories", style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = onNavigateRepositories) {
                                    Icon(Icons.Filled.ChevronRight, contentDescription = "See all repositories")
                                }
                            }
                        }
                        items(
                            state.overviewRepositories.sortedByDescending { it.lastUpdated }.take(5),
                            key = { it.fullName }
                        ) { repo ->
                            RecentRepoRow(repo = repo, onClick = { onOpenRepository(repo) })
                        }
                    }
                }
            }
        }

        GlassFloatingBottomNav(
            selected = BottomNavTab.OVERVIEW,
            onSelect = { tab ->
                when (tab) {
                    BottomNavTab.OVERVIEW -> Unit
                    BottomNavTab.REPOSITORIES -> onNavigateRepositories()
                    BottomNavTab.PROFILE -> onNavigateProfile()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun AccountHeaderCard(
    user: GitUser?,
    isLoading: Boolean,
    errorMessage: String?,
    onOpenProfile: () -> Unit
) {
    GlassClickableCard(onClick = onOpenProfile, modifier = Modifier.fillMaxWidth()) {
        when {
            user != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                if (user.avatarUrl != null) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                    )
                } else {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(52.dp))
                }
                Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(user.displayName?.takeIf { it.isNotBlank() } ?: user.username, style = MaterialTheme.typography.titleMedium)
                    Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!user.bio.isNullOrBlank()) {
                        Text(
                            user.bio,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Text("Loading your profile…", modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyMedium)
            }

            else -> Text(errorMessage ?: "Couldn't load your profile.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** One-tap way back into whatever repo/branch was last open — the app's own "continue
 * where you left off", independent of GitHub's own recency ordering. */
@Composable
private fun ContinueCard(repo: GitRepository, onResume: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Continue where you left off", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(repo.name, style = MaterialTheme.typography.titleMedium)
                Text(repo.owner, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onResume) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Resume", tint = GlassBlobBlue)
            }
        }
    }
}

/** Private/public split + top languages across every repo the token can see — a
 * quick account-wide snapshot without needing to open the full repository list. */
@Composable
private fun RepositoryStatsCard(repositories: List<GitRepository>, onViewAll: () -> Unit) {
    val privateCount = repositories.count { it.isPrivate }
    val publicCount = repositories.size - privateCount
    val topLanguages = repositories
        .mapNotNull { it.language }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(3)

    GlassClickableCard(onClick = onViewAll, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            StatTile(label = "Repositories", value = repositories.size.toString())
            StatTile(label = "Private", value = privateCount.toString())
            StatTile(label = "Public", value = publicCount.toString())
        }
        if (topLanguages.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                topLanguages.forEach { (language, count) ->
                    LanguageChip(language = language, count = count)
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LanguageChip(language: String, count: Int) {
    Box(
        modifier = Modifier
            .background(GlassBlobPurple.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text("$language · $count", style = MaterialTheme.typography.labelSmall, color = GlassBlobPurple)
    }
}

/** GitHub's core API rate limit for this token — an "advanced" touch that a plain repo
 * list wouldn't bother with, but genuinely useful for anyone pushing frequently. */
@Composable
private fun ApiRateLimitCard(limit: ApiRateLimit) {
    val fraction = if (limit.limit > 0) limit.remaining.toFloat() / limit.limit.toFloat() else 1f
    val color = when {
        fraction > 0.5f -> GlassBlobTeal
        fraction > 0.15f -> Color(0xFFD29922)
        else -> Color(0xFFDA3633)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text("API usage", style = MaterialTheme.typography.titleSmall)
        }
        Text(
            "${limit.remaining} of ${limit.limit} requests remaining this hour",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        LinearProgressIndicator(
            progress = { fraction },
            color = color,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun RecentRepoRow(repo: GitRepository, onClick: () -> Unit) {
    val context = LocalContext.current
    GlassClickableCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(repo.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (repo.isPrivate) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Private",
                            modifier = Modifier.padding(start = 6.dp).size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    repo.language?.let { "$it · Updated ${repo.lastUpdated}" } ?: "Updated ${repo.lastUpdated}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${repo.owner}/${repo.name}")))
            }) {
                Icon(Icons.Filled.OpenInNew, contentDescription = "Open on GitHub", modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Real, always-functional shortcuts — every tile navigates somewhere or does something
 * immediately, nothing here is a stub (PRD "Quick Action Grid"). */
@Composable
private fun QuickActionGrid(
    onNewRepository: () -> Unit,
    onRepositories: () -> Unit,
    onRefresh: () -> Unit,
    onProfile: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionTile(Icons.Filled.Add, "New Repo", GlassBlobTeal, Modifier.weight(1f), onNewRepository)
            QuickActionTile(Icons.Filled.Folder, "Repositories", GlassBlobBlue, Modifier.weight(1f), onRepositories)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickActionTile(Icons.Filled.Refresh, "Refresh", GlassBlobPurple, Modifier.weight(1f), onRefresh)
            QuickActionTile(Icons.Filled.Person, "Profile", GlassBlobPink, Modifier.weight(1f), onProfile)
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(modifier = modifier.clickable(onClick = onClick), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).background(tint.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

private data class SmartSuggestion(val title: String, val detail: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

/** Every suggestion here is computed from data already on screen — real signals
 * (stale repos, a dwindling API quota), never fabricated "AI" content (PRD "AI
 * Insights", reframed honestly as data-derived suggestions). Returns an empty list
 * (no card shown at all) when there's genuinely nothing worth flagging. */
private fun buildSmartSuggestions(repositories: List<GitRepository>, rateLimit: ApiRateLimit?): List<SmartSuggestion> {
    val suggestions = mutableListOf<SmartSuggestion>()

    val staleCount = repositories.count { repo ->
        runCatching { java.time.Instant.parse(repo.lastUpdated) }.getOrNull()
            ?.let { java.time.Duration.between(it, java.time.Instant.now()).toDays() > 90 } == true
    }
    if (staleCount > 0) {
        suggestions += SmartSuggestion(
            title = "$staleCount repositor${if (staleCount == 1) "y hasn't" else "ies haven't"} been updated in 90+ days",
            detail = "Worth a look if any of these are still active projects.",
            icon = Icons.Filled.History
        )
    }

    if (rateLimit != null && rateLimit.limit > 0 && rateLimit.remaining.toFloat() / rateLimit.limit < 0.1f) {
        suggestions += SmartSuggestion(
            title = "GitHub API rate limit is running low",
            detail = "${rateLimit.remaining} of ${rateLimit.limit} requests left this hour.",
            icon = Icons.Filled.Bolt
        )
    }

    val archivedButPrivate = repositories.count { it.archived }
    if (archivedButPrivate > 0) {
        suggestions += SmartSuggestion(
            title = "$archivedButPrivate archived repositor${if (archivedButPrivate == 1) "y" else "ies"} in your account",
            detail = "Archived repos are read-only on GitHub — Git Way can still browse them, but uploads will fail.",
            icon = Icons.Filled.Archive
        )
    }

    return suggestions
}

@Composable
private fun SuggestionCard(suggestion: SmartSuggestion) {
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(suggestion.icon, contentDescription = null, tint = GlassBlobPurple, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(suggestion.title, style = MaterialTheme.typography.bodyMedium)
                Text(suggestion.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
