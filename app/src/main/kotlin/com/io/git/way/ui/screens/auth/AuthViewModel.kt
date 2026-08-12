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
    val errorMessage: String? = null,
    /** Classic-PAT scopes reported by GitHub after a successful connect; empty when
     * GitHub didn't report any (fine-grained token) — see [authScopeWarning]. */
    val grantedScopes: Set<String> = emptySet(),
    /** Non-null once the token was validated (so the scope checklist can render). */
    val scopeCheckVisible: Boolean = false,
    /** Set when GitHub reported no scopes at all — fine-grained tokens hide scopes. */
    val authScopeWarning: String? = null,
    /** Required scopes the token is missing (only known for classic tokens). */
    val missingScopes: List<String> = emptyList()
)

/** The scopes Git Way needs for its full feature set (see README → "Token scopes"). */
private val REQUIRED_SCOPES = setOf("repo", "workflow", "delete_repo")

/** Drives the Token screen: takes a PAT, validates it against GitHub, persists on success. */
class AuthViewModel(
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    var uiState by mutableStateOf(AuthUiState())
        private set

    fun onTokenChange(value: String) {
        uiState = uiState.copy(
            token = value,
            errorMessage = null,
            grantedScopes = emptySet(),
            scopeCheckVisible = false,
            authScopeWarning = null,
            missingScopes = emptyList()
        )
    }

    fun connect(onSuccess: () -> Unit) {
        val token = uiState.token.trim()
        if (token.isBlank()) return

        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            gitHubRepository.validateTokenAndFetchUser(token)
                .onSuccess { result ->
                    val granted = result.grantedScopes
                    val missing = if (granted.isEmpty()) emptyList() else REQUIRED_SCOPES.subtract(granted).toList()
                    uiState = uiState.copy(
                        isLoading = false,
                        grantedScopes = granted,
                        scopeCheckVisible = true,
                        authScopeWarning = if (granted.isEmpty()) {
                            "GitHub didn't report any scopes — a fine-grained token. " +
                                "Some features may need extra permissions."
                        } else {
                            null
                        },
                        missingScopes = missing
                    )
                    // Classic token missing required scopes: warn first, let the user
                    // decide (Continue anyway). Fine-grained (unknown) tokens proceed.
                    if (missing.isEmpty()) onSuccess()
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(isLoading = false, errorMessage = mapError(throwable))
                }
        }
    }

    /** Skips the missing-scope warning and connects anyway (fine-grained/partial tokens). */
    fun connectAnyway(onSuccess: () -> Unit) {
        uiState = uiState.copy(isLoading = false)
        onSuccess()
    }

    private fun mapError(throwable: Throwable): String = when (throwable) {
        is HttpException -> when (throwable.code()) {
            401 -> "Invalid or expired token. Generate a new one on GitHub."
            403 -> "Token doesn't have the required scope, or the rate limit was hit."
            404 -> "Couldn't reach that GitHub account."
            else -> "GitHub returned an error (${throwable.code()})."
        }
        is IOException -> "Network error — check your connection and try again."
        else -> "${throwable::class.simpleName}: ${throwable.message ?: "Something went wrong. Please try again."}"
    }
}
