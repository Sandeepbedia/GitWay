package com.io.git.way

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.io.git.way.navigation.GitWayNavGraph
import com.io.git.way.ui.theme.AppThemeMode
import com.io.git.way.ui.theme.GitWayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
        val navController = rememberNavController()
        GitWayNavGraph(
            navController = navController,
            themeMode = themeMode,
            onThemeModeChange = { themeMode = it }
        )
    }
}
