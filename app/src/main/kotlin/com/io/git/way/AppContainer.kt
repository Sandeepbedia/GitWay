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

class AppContainer(context: Context) {
    val tokenManager = TokenManager(context)

    private val apiService by lazy { RetrofitProvider.create(tokenManager) }

    val gitHubRepository: GitHubRepository by lazy {
        GitHubRepositoryImpl(tokenManager, apiService)
    }

    val biometricLockManager: BiometricLockManager by lazy { BiometricLockManager(context) }
}
