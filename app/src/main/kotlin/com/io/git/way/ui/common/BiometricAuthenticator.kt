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
