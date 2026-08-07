/*
 * Git Way
 * Copyright (C) 2026 Sandeep Bedia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.io.git.way.ui.common

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Shows the system biometric prompt and suspends until the user succeeds or backs out —
 * used by the Profile "Security > Fingerprint Lock" toggle and the app-launch lock
 * screen. Requires a [FragmentActivity] (what [com.io.git.way.MainActivity] now is)
 * since [BiometricPrompt] is built on the Fragment lifecycle, not plain Activities.
 */
suspend fun FragmentActivity.authenticateWithBiometrics(
    title: String,
    subtitle: String
): Boolean = suspendCancellableCoroutine { continuation ->
    val executor = ContextCompat.getMainExecutor(this)
    val prompt = BiometricPrompt(
        this,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // User cancelled, hit "use PIN" and backed out, hardware error, etc. —
                // any of these just means "not unlocked", not a crash-worthy failure.
                if (continuation.isActive) continuation.resume(false)
            }

            override fun onAuthenticationFailed() {
                // A single non-matching fingerprint/face — the system prompt itself
                // handles the retry UI, nothing to do here.
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .setNegativeButtonText("Cancel")
        .build()

    continuation.invokeOnCancellation { prompt.cancelAuthentication() }
    prompt.authenticate(promptInfo)
}
