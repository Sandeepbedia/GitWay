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
package com.io.git.way.domain.model

/**
 * Result of validating a Personal Access Token: the authenticated user plus the
 * classic-token scopes GitHub returned in the `X-OAuth-Scopes` response header.
 *
 * [grantedScopes] is empty when GitHub sent no scope header at all — that happens
 * with fine-grained tokens (which don't expose scopes) and with classic tokens
 * that carry no scopes; the UI then shows a "couldn't verify" warning instead of
 * claiming anything is granted or missing.
 */
data class TokenValidationResult(
    val user: GitUser,
    val grantedScopes: Set<String>
)
