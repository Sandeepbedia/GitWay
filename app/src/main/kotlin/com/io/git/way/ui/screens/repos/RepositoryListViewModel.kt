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
    val errorMessage: String? = null
) {
    val filtered: List<GitRepository>
        get() = if (searchQuery.isBlank()) {
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
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Couldn't load repositories."
                    )
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        uiState = uiState.copy(searchQuery = query)
    }

    fun disconnect() {
        gitHubRepository.clearToken()
    }
}
