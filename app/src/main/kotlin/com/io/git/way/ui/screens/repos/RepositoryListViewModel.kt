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
    val createRepoError: String? = null
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
