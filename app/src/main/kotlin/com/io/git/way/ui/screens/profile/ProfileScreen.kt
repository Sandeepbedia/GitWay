package com.io.git.way.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
 * Profile tab — reachable from the floating bottom nav. Intentionally left blank
 * for now (placeholder); the nav destination exists so it isn't the only missing tab.
 *
 * See [com.io.git.way.ui.screens.overview.OverviewScreen] for why the nav is an overlay
 * rather than Scaffold's `bottomBar` slot.
 */
@Composable
fun ProfileScreen(
    onNavigateOverview: () -> Unit,
    onNavigateRepositories: () -> Unit
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
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Profile coming soon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        GlassFloatingBottomNav(
            selected = BottomNavTab.PROFILE,
            onSelect = { tab ->
                when (tab) {
                    BottomNavTab.OVERVIEW -> onNavigateOverview()
                    BottomNavTab.REPOSITORIES -> onNavigateRepositories()
                    BottomNavTab.PROFILE -> Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
