package com.io.git.way.ui.screens.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import com.io.git.way.ui.theme.BottomNavTab
import com.io.git.way.ui.theme.GlassFloatingBottomNav
import com.io.git.way.ui.theme.LiquidGlassBackground

/**
 * Overview tab — reachable from the floating bottom nav. Intentionally left blank
 * for now (placeholder); the nav destination exists so Repositories isn't the only tab.
 *
 * The floating nav is drawn as an overlay on top of the screen (not in Scaffold's
 * `bottomBar` slot) — `bottomBar` reserves its own layout row and pushes content up
 * above it, which is exactly the "doesn't reach the very bottom" look. Overlaying it
 * instead lets the background and content extend the full height of the screen, with
 * only the nav's own translucent card floating above the gesture area.
 */
@Composable
fun OverviewScreen(
    onNavigateRepositories: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Overview coming soon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        GlassFloatingBottomNav(
            selected = BottomNavTab.OVERVIEW,
            onSelect = { tab ->
                when (tab) {
                    BottomNavTab.OVERVIEW -> Unit
                    BottomNavTab.REPOSITORIES -> onNavigateRepositories()
                    BottomNavTab.PROFILE -> onNavigateProfile()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
