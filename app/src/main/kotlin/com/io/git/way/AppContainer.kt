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
package com.io.git.way

import android.content.Context
import com.io.git.way.data.local.BiometricLockManager
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

    val biometricLockManager: BiometricLockManager by lazy { BiometricLockManager(context) }
}
