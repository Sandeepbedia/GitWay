package com.io.git.way.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.io.git.way.domain.repository.GitHubRepository
import com.io.git.way.ui.screens.auth.AuthViewModel
import com.io.git.way.ui.screens.repos.RepositoryListViewModel

/** Constructs the app's ViewModels with their [GitHubRepository] dependency. */
class GitWayViewModelFactory(
    private val gitHubRepository: GitHubRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AuthViewModel::class.java) ->
            AuthViewModel(gitHubRepository) as T
        modelClass.isAssignableFrom(RepositoryListViewModel::class.java) ->
            RepositoryListViewModel(gitHubRepository) as T
        modelClass.isAssignableFrom(GitWaySessionViewModel::class.java) ->
            GitWaySessionViewModel(gitHubRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
