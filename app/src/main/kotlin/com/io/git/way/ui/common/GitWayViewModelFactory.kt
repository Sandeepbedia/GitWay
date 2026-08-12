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
