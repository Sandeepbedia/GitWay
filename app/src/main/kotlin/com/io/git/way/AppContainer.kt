package com.io.git.way

import android.content.Context
import com.io.git.way.data.local.TokenManager
import com.io.git.way.data.remote.RetrofitProvider
import com.io.git.way.data.repository.GitHubRepositoryImpl
import com.io.git.way.domain.repository.GitHubRepository

/**
 * Small manual dependency container (no DI framework yet). Wires TokenManager ->
 * Retrofit -> GitHubRepositoryImpl once per process and exposes the interface only.
 */
class AppContainer(context: Context) {
    private val tokenManager = TokenManager(context)
    private val apiService = RetrofitProvider.create(tokenManager)

    val gitHubRepository: GitHubRepository = GitHubRepositoryImpl(tokenManager, apiService)
}
