package com.io.git.way.ui.screens.repos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.io.git.way.domain.model.GitRepository
import com.io.git.way.domain.repository.GitHubRepository
import kotlinx.coroutines.launch

data class RepoListUiState(
    val isLoading: Boolean = false,
    val repositories: List<GitRepository> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val isCreatingRepo: Boolean = false,
    val createRepoError: String? = null,

    /** GitHub-wide search mode (toggled from the FAB menu): results come from the
     * Search API instead of the local name filter. */
    val isGitHubSearch: Boolean = false,
    val searchResults: List<GitRepository> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,

    /** fullName set of repos starred by the token user (for the per-card star toggle). */
    val starredRepos: Set<String> = emptySet(),
    val isTogglingStar: String? = null
) {
    val filtered: List<GitRepository>
        get() = if (isGitHubSearch) {
            searchResults
        } else if (searchQuery.isBlank()) {
            repositories
        } else {
            repositories.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
}

/** Drives the Repository List screen: loads repos for the stored token, supports search. */
class RepositoryListViewModel(
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    var uiState by mutableStateOf(RepoListUiState())
        private set

    init {
        loadRepositories()
    }

    fun loadRepositories() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            gitHubRepository.listRepositories()
                .onSuccess { repos ->
                    uiState = uiState.copy(isLoading = false, repositories = repos)
                    loadStarredIfNeeded()
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Couldn't load repositories."
                    )
                }
        }
    }

    private fun loadStarredIfNeeded() {
        if (uiState.starredRepos.isNotEmpty()) return
        viewModelScope.launch {
            gitHubRepository.listStarredRepositories()
                .onSuccess { starred -> uiState = uiState.copy(starredRepos = starred) }
                .onFailure { /* non-critical — toggle just flips optimistically */ }
        }
    }

    fun onSearchQueryChange(query: String) {
        uiState = uiState.copy(searchQuery = query)
        if (uiState.isGitHubSearch && query.isNotBlank()) {
            searchGitHub(query)
        } else if (uiState.isGitHubSearch) {
            uiState = uiState.copy(searchResults = emptyList(), searchError = null)
        }
    }

    fun setGitHubSearch(enabled: Boolean) {
        uiState = uiState.copy(isGitHubSearch = enabled, searchResults = emptyList(), searchError = null)
        if (enabled && uiState.searchQuery.isNotBlank()) searchGitHub(uiState.searchQuery)
    }

    private fun searchGitHub(query: String) {
        uiState = uiState.copy(isSearching = true, searchError = null)
        viewModelScope.launch {
            gitHubRepository.searchRepositories(query.trim())
                .onSuccess { results -> uiState = uiState.copy(isSearching = false, searchResults = results) }
                .onFailure { e -> uiState = uiState.copy(isSearching = false, searchError = e.message ?: "Search failed.") }
        }
    }

    /** Optimistic star toggle; reverts on failure. */
    fun toggleStar(repo: GitRepository) {
        if (uiState.isTogglingStar != null) return
        val isStarred = uiState.starredRepos.contains(repo.fullName)
        val newSet = if (isStarred) uiState.starredRepos - repo.fullName else uiState.starredRepos + repo.fullName
        val updated = repo.copy(stargazersCount = (repo.stargazersCount + if (isStarred) -1 else 1).coerceAtLeast(0))
        uiState = uiState.copy(isTogglingStar = repo.fullName, starredRepos = newSet)
        viewModelScope.launch {
            val result = if (isStarred) gitHubRepository.unstarRepository(repo) else gitHubRepository.starRepository(repo)
            result
                .onSuccess {
                    uiState = uiState.copy(
                        isTogglingStar = null,
                        repositories = uiState.repositories.map { if (it.fullName == updated.fullName) updated else it },
                        searchResults = uiState.searchResults.map { if (it.fullName == updated.fullName) updated else it }
                    )
                }
                .onFailure {
                    uiState = uiState.copy(
                        isTogglingStar = null,
                        starredRepos = if (isStarred) uiState.starredRepos + repo.fullName else uiState.starredRepos - repo.fullName
                    )
                }
        }
    }

    fun clearCreateRepoError() {
        uiState = uiState.copy(createRepoError = null)
    }

    /** Creates a brand-new GitHub repository and prepends it to the list — [onCreated]
     * fires only on success, so the caller can navigate straight into it. */
    fun createRepository(name: String, description: String, isPrivate: Boolean, onCreated: (GitRepository) -> Unit) {
        if (name.isBlank() || uiState.isCreatingRepo) return
        uiState = uiState.copy(isCreatingRepo = true, createRepoError = null)
        viewModelScope.launch {
            gitHubRepository.createRepository(name.trim(), description.trim(), isPrivate)
                .onSuccess { repo ->
                    uiState = uiState.copy(isCreatingRepo = false, repositories = listOf(repo) + uiState.repositories)
                    onCreated(repo)
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(isCreatingRepo = false, createRepoError = throwable.message ?: "Couldn't create repository.")
                }
        }
    }

    fun disconnect() {
        gitHubRepository.clearToken()
    }
}
