package com.io.git.way

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.io.git.way.navigation.GitWayNavGraph
import com.io.git.way.ui.theme.AppThemeMode
import com.io.git.way.ui.theme.GitWayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
