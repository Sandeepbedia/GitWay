package com.io.git.way.ui.screens.repos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.io.git.way.GitWayApp
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.ui.common.GitWayViewModelFactory
import com.io.git.way.ui.theme.AppThemeMode
import com.io.git.way.ui.theme.GlassChip
import com.io.git.way.ui.theme.GlassClickableCard
import com.io.git.way.ui.theme.GlassScaffold

private enum class RepoSort { RECENT, NAME }

/** Screen 3: real repositories for the connected token, with search, sort and selection. */
@Composable
fun RepositoryListScreen(
    onRepositorySelected: (GitRepository) -> Unit,
    onDisconnect: () -> Unit,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    viewModel: RepositoryListViewModel = viewModel(
        factory = GitWayViewModelFactory(
            (LocalContext.current.applicationContext as GitWayApp).container.gitHubRepository
        )
    )
) {
    val state = viewModel.uiState
    var sort by remember { mutableStateOf(RepoSort.RECENT) }

    val sortedRepos = when (sort) {
        RepoSort.RECENT -> state.filtered
        RepoSort.NAME -> state.filtered.sortedBy { it.name.lowercase() }
    }

    GlassScaffold(
        title = "Your Repositories",
        actions = {
            IconButton(onClick = { onThemeModeChange(nextThemeMode(themeMode)) }) {
                Icon(themeModeIcon(themeMode), contentDescription = "Toggle theme")
            }
            IconButton(onClick = {
                viewModel.disconnect()
                onDisconnect()
            }) {
                Icon(Icons.Filled.Logout, contentDescription = "Disconnect")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text("Search repositories") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                GlassChip(text = "Recent", selected = sort == RepoSort.RECENT, onClick = { sort = RepoSort.RECENT })
                GlassChip(text = "A–Z", selected = sort == RepoSort.NAME, onClick = { sort = RepoSort.NAME })
            }

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.errorMessage != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.loadRepositories() }) { Text("Retry") }
                    }
                }

                sortedRepos.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("No repositories found.") }

                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sortedRepos, key = { it.fullName }) { repo ->
                        RepositoryRow(repo = repo, onClick = { onRepositorySelected(repo) })
                    }
                }
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

@Composable
private fun RepositoryRow(repo: GitRepository, onClick: () -> Unit) {
    GlassClickableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(repo.name, style = MaterialTheme.typography.titleMedium)
            if (repo.isPrivate) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Private",
                    modifier = Modifier.padding(start = 6.dp).size(14.dp)
                )
            }
        }
        Text(repo.owner, style = MaterialTheme.typography.bodySmall)
        if (repo.lastUpdated.isNotBlank()) {
            Text(
                "Updated ${repo.lastUpdated}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
