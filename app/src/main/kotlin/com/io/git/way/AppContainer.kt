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

    // Deferred: building the OkHttpClient + Retrofit involves real work (TLS setup,
    // interceptor construction) that has no reason to happen synchronously during
    // Application.onCreate, before any screen even asks for network access. `by lazy`
    // pushes that cost to the first actual API call instead of app cold start.
    private val apiService by lazy { RetrofitProvider.create(tokenManager) }

    val gitHubRepository: GitHubRepository by lazy {
        GitHubRepositoryImpl(tokenManager, apiService)
    }
}
