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
