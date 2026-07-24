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
