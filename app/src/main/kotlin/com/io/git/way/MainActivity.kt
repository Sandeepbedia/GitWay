package com.io.git.way

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import com.io.git.way.domain.model.AppUpdateInfo
import com.io.git.way.navigation.GitWayNavGraph
import com.io.git.way.ui.common.ApkInstaller
import com.io.git.way.ui.common.BiometricLockScreen
import com.io.git.way.ui.components.UpdateAvailableDialog
import com.io.git.way.ui.theme.AppThemeMode
import com.io.git.way.ui.theme.GitWayTheme
import kotlinx.coroutines.launch

/** Repo this build checks for updates against — see docs/RELEASING.md and
 * .github/workflows/release.yml, which is what actually publishes them. */
private const val UPDATE_CHECK_OWNER = "Sandeepbedia"
private const val UPDATE_CHECK_REPO = "GitWay"

// FragmentActivity (a ComponentActivity subclass, so nothing else about the activity
// changes) rather than plain ComponentActivity — androidx.biometric's BiometricPrompt is
// built on the Fragment lifecycle and requires one, for Profile > Security's fingerprint
// app-lock.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )

        // enableEdgeToEdge() alone still applies its own default scrim colour to the
        // status/navigation bars (not literally transparent) — this is the actual "dark
        // strip" behind the 3-button/gesture nav area. Forcing both explicitly, plus
        // telling the window to fully own its own layout (not just let the system inset
        // content), is what removes it for real, on 3-button nav and gesture nav alike.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = AndroidColor.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = AndroidColor.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            GitWayRoot()
        }
    }
}

@Composable
private fun GitWayRoot() {
    // TODO: replace with persisted theme preference from DataStore (see PRD Theme System).
    var themeMode by remember { mutableStateOf(AppThemeMode.SYSTEM) }

    GitWayTheme(themeMode = themeMode, dynamicColor = true) {
        val context = LocalContext.current
        val navController = rememberNavController()
        var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
        var isDownloadingUpdate by remember { mutableStateOf(false) }
        var updateError by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        val biometricLockManager = remember {
            (context.applicationContext as GitWayApp).container.biometricLockManager
        }
        var isUnlocked by remember {
            mutableStateOf(!(biometricLockManager.isLockEnabled() && biometricLockManager.isBiometricAvailable()))
        }

        // Re-lock whenever the app leaves the foreground — Security > Fingerprint Lock
        // is meant to gate every resume, not just the very first cold-start launch.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP &&
                    biometricLockManager.isLockEnabled() &&
                    biometricLockManager.isBiometricAvailable()
                ) {
                    isUnlocked = false
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        // Fire-and-forget, once per process: never blocks navigation, never shown twice
        // in one session once dismissed. A failed/offline check just means no dialog —
        // it never surfaces as an error to the user.
        LaunchedEffect(Unit) {
            val gitHubRepository = (context.applicationContext as GitWayApp).container.gitHubRepository
            val versionName = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: return@LaunchedEffect

            gitHubRepository.checkForUpdate(UPDATE_CHECK_OWNER, UPDATE_CHECK_REPO, versionName)
                .onSuccess { update -> availableUpdate = update }
        }

        if (!isUnlocked) {
            BiometricLockScreen(onUnlocked = { isUnlocked = true })
        } else {
            Box {
                GitWayNavGraph(
                    navController = navController,
                    themeMode = themeMode,
                    onThemeModeChange = { themeMode = it }
                )

                availableUpdate?.let { update ->
                    UpdateAvailableDialog(
                        update = update,
                        isDownloading = isDownloadingUpdate,
                        downloadError = updateError,
                        onUpdate = {
                            val target = update.apkDownloadUrl
                            if (target != null) {
                                // Direct in-app download + install via the package installer.
                                isDownloadingUpdate = true
                                updateError = null
                                scope.launch {
                                    runCatching { ApkInstaller.downloadBytes(target) }
                                        .onSuccess { bytes ->
                                            val file = ApkInstaller.writeToCache(context, "gitway-update.apk", bytes)
                                            ApkInstaller.installApk(context, file)?.let { updateError = it }
                                            if (updateError == null) availableUpdate = null
                                        }
                                        .onFailure { updateError = it.message ?: "Update download failed." }
                                    isDownloadingUpdate = false
                                }
                            } else {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releasePageUrl)))
                                availableUpdate = null
                            }
                        },
                        onDismiss = { availableUpdate = null }
                    )
                }
            }
        }
    }
}
