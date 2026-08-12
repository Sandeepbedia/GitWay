package com.io.git.way.data.local

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores whether the user has turned on biometric app-lock (Profile "Security" — PRD
 * "Fingerprint Lock"), and reports whether this device can even support it. Reuses the
 * same EncryptedSharedPreferences approach as [TokenManager] since this preference
 * gates access to the token/repos, not because the boolean itself is sensitive.
 */
class BiometricLockManager(context: Context) {

    private val appContext = context.applicationContext

    private val masterKey by lazy {
        MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    }

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            appContext,
            "git_way_security_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Whether this device has usable biometric hardware with at least one enrolled
     * fingerprint/face — the toggle in Settings should be disabled (with an explanation)
     * rather than silently failing later if this is false. */
    fun isBiometricAvailable(): Boolean {
        val manager = BiometricManager.from(appContext)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isLockEnabled(): Boolean = prefs.getBoolean(KEY_LOCK_ENABLED, false)

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    private companion object {
        const val KEY_LOCK_ENABLED = "biometric_lock_enabled"
    }
}
