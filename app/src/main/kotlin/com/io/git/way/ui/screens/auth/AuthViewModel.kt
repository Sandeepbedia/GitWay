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

package com.io.git.way.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.io.git.way.domain.repository.GitHubRepository
import java.io.IOException
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AuthUiState(
    val token: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/** Drives the Token screen: takes a PAT, validates it against GitHub, persists on success. */
class AuthViewModel(
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    var uiState by mutableStateOf(AuthUiState())
        private set

    fun onTokenChange(value: String) {
        uiState = uiState.copy(token = value, errorMessage = null)
    }

    fun connect(onSuccess: () -> Unit) {
        val token = uiState.token.trim()
        if (token.isBlank()) return

        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            gitHubRepository.validateTokenAndFetchUser(token)
                .onSuccess {
                    uiState = uiState.copy(isLoading = false)
                    onSuccess()
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(isLoading = false, errorMessage = mapError(throwable))
                }
        }
    }

    private fun mapError(throwable: Throwable): String = when (throwable) {
        is HttpException -> when (throwable.code()) {
            401 -> "Invalid or expired token. Generate a new one on GitHub."
            403 -> "Token doesn't have the required scope, or the rate limit was hit."
            404 -> "Couldn't reach that GitHub account."
            else -> "GitHub returned an error (${throwable.code()})."
        }
        is IOException -> "Network error — check your connection and try again."
        else -> throwable.message ?: "Something went wrong. Please try again."
    }
}
