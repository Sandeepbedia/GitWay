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
package com.io.git.way.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Secure multi-account GitHub PAT storage.
 *
 * Tokens are encrypted at rest. The active account is switched by changing which
 * encrypted token is exposed to the API interceptor; no token is ever stored in
 * normal SharedPreferences or logs.
 */
class TokenManager(context: Context) {

    private val _activeAccountFlow = MutableStateFlow<String?>(null)
    val activeAccountFlow: StateFlow<String?> = _activeAccountFlow

    private val appContext = context.applicationContext

    private val masterKey by lazy {
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            appContext,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    init {
        _activeAccountFlow.value = prefs.getString(KEY_ACTIVE_ACCOUNT, null)
    }

    /** Legacy/current active token API retained for the network layer. */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun hasToken(): Boolean = !getToken().isNullOrBlank()

    /**
     * Saves an account and makes it active.
     * The username is used only as an account identifier; the PAT stays encrypted.
     */
    fun saveAccount(username: String, token: String, avatarUrl: String? = null) {
        val normalized = username.trim()
        if (normalized.isBlank() || token.isBlank()) return

        val accounts = prefs.getStringSet(KEY_ACCOUNTS, emptySet()).orEmpty().toMutableSet()
        accounts.add(normalized)

        prefs.edit()
            .putStringSet(KEY_ACCOUNTS, accounts)
            .putString(accountTokenKey(normalized), token)
            .apply { if (!avatarUrl.isNullOrBlank()) putString(accountAvatarKey(normalized), avatarUrl) }
            .putString(KEY_ACTIVE_ACCOUNT, normalized)
            .putString(KEY_TOKEN, token)
            .apply()
        _activeAccountFlow.value = normalized
    }

    fun getAccountUsernames(): List<String> =
        prefs.getStringSet(KEY_ACCOUNTS, emptySet())
            .orEmpty()
            .toList()
            .sortedBy { it.lowercase() }

    fun getActiveAccount(): String? =
        prefs.getString(KEY_ACTIVE_ACCOUNT, null)

    fun getAccountAvatar(username: String): String? =
        prefs.getString(accountAvatarKey(username.trim()), null)

    fun switchAccount(username: String): Boolean {
        val normalized = username.trim()
        val token = prefs.getString(accountTokenKey(normalized), null)
            ?: return false

        prefs.edit()
            .putString(KEY_ACTIVE_ACCOUNT, normalized)
            .putString(KEY_TOKEN, token)
            .apply()
        _activeAccountFlow.value = normalized
        return true
    }

    /**
     * Removes one account. If it was active, another account becomes active
     * automatically when available; otherwise the active token is cleared.
     */
    fun removeAccount(username: String): Boolean {
        val normalized = username.trim()
        val accounts = prefs.getStringSet(KEY_ACCOUNTS, emptySet()).orEmpty().toMutableSet()
        if (!accounts.remove(normalized)) return false

        val active = getActiveAccount()
        val editor = prefs.edit()
            .putStringSet(KEY_ACCOUNTS, accounts)
            .remove(accountTokenKey(normalized))
            .remove(accountAvatarKey(normalized))

        if (active == normalized) {
            val next = accounts.firstOrNull()
            if (next != null) {
                val nextToken = prefs.getString(accountTokenKey(next), null)
                if (!nextToken.isNullOrBlank()) {
                    editor.putString(KEY_ACTIVE_ACCOUNT, next)
                        .putString(KEY_TOKEN, nextToken)
                } else {
                    editor.remove(KEY_ACTIVE_ACCOUNT).remove(KEY_TOKEN)
                }
            } else {
                editor.remove(KEY_ACTIVE_ACCOUNT).remove(KEY_TOKEN)
            }
        }

        editor.apply()
        _activeAccountFlow.value = if (active == normalized) accounts.firstOrNull() else active
        return true
    }

    /** Used by token validation so a failed add-account attempt can restore the old session. */
    fun restoreToken(token: String?) {
        val editor = prefs.edit()
        if (token.isNullOrBlank()) editor.remove(KEY_TOKEN)
        else editor.putString(KEY_TOKEN, token)
        editor.apply()
    }

    fun clearToken() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_ACTIVE_ACCOUNT)
            .remove(KEY_ACCOUNTS)
            .apply()
        _activeAccountFlow.value = null
    }

    private fun accountTokenKey(username: String): String =
        "$KEY_ACCOUNT_TOKEN_PREFIX${username.lowercase()}"

    private fun accountAvatarKey(username: String): String =
        "$KEY_ACCOUNT_AVATAR_PREFIX${username.lowercase()}"

    private companion object {
        const val PREFS_FILE_NAME = "git_way_secure_prefs"
        const val KEY_TOKEN = "github_pat"
        const val KEY_ACCOUNTS = "github_accounts"
        const val KEY_ACTIVE_ACCOUNT = "github_active_account"
        const val KEY_ACCOUNT_TOKEN_PREFIX = "github_account_token_"
        const val KEY_ACCOUNT_AVATAR_PREFIX = "github_account_avatar_"
    }
}
