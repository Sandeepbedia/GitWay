/*
 * GitWay — Material 3 ColorScheme mapped from the Bravo-inspired palette
 * defined in Color.kt: purple hero accent with teal secondary, porcelain
 * neumorphic light surfaces and charcoal neumorphic dark surfaces.
 *
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
package com.io.git.way.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = HyperPrimary,
    onPrimary = Color.White,
    primaryContainer = RepoPillGradientLight,
    onPrimaryContainer = HyperPrimaryDark,
    secondary = RepoSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F5EF),
    onSecondaryContainer = RepoSecondaryDark,
    tertiary = HyperAuroraPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E5FF),
    onTertiaryContainer = Color(0xFF3440B8),
    background = RepoBgPrimaryLight,
    surface = RepoCardSurfaceLight,
    surfaceVariant = RepoElevatedSurfaceLight,
    onBackground = RepoTextPrimaryLight,
    onSurface = RepoTextPrimaryLight,
    onSurfaceVariant = RepoTextSecondaryLight,
    outline = RepoTextMutedLight,
    error = RepoDanger,
    onError = Color.White,
    errorContainer = Color(0xFFFFE0DE),
    onErrorContainer = Color(0xFF8E0F09)
)

// A single consistent dark palette end to end: background, cards, and every elevated
// surface all come from the same charcoal neumorphic family (#23262C / #383C46 / #40454F),
// with purple reserved strictly for accents (buttons, active tab, selection, icons).
private val DarkColorScheme = darkColorScheme(
    primary = HyperPrimaryLight,
    onPrimary = Color(0xFF2A1A70),
    primaryContainer = Color(0xFF4B379E),
    onPrimaryContainer = Color(0xFFE9E2FF),
    secondary = RepoSecondaryLight,
    onSecondary = Color(0xFF003A33),
    secondaryContainer = Color(0xFF005B50),
    onSecondaryContainer = Color(0xFFC5F5EA),
    tertiary = HyperAuroraPurpleLight,
    onTertiary = Color(0xFF00344F),
    tertiaryContainer = Color(0xFF1A4A75),
    onTertiaryContainer = Color(0xFFCFE6FF),
    background = RepoBgPrimary,
    surface = RepoCardSurface,
    surfaceVariant = RepoElevatedSurface,
    onBackground = RepoTextPrimary,
    onSurface = RepoTextPrimary,
    onSurfaceVariant = RepoTextSecondary,
    outline = RepoTextMuted,
    error = RepoDanger,
    onError = Color.White,
    errorContainer = Color(0xFF5C130E),
    onErrorContainer = Color(0xFFFFDAD6)
)

// True-black background for OLED screens, same card/surface family as Dark so cards
// still read as "elevated" rather than invisible against pure black.
private val AmoledColorScheme = DarkColorScheme.copy(
    background = AmoledBlack,
    surface = RepoCardSurface
)

/**
 * Git Way theme wrapper. Resolves [themeMode] (System/Light/Dark/AMOLED) into a
 * Material 3 ColorScheme, following system light/dark mode when SYSTEM is selected.
 */
@Composable
fun GitWayTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == AppThemeMode.AMOLED -> AmoledColorScheme
        useDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDark
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        // Material3's Scaffold uses a Transparent container here (for the glass look),
        // and contentColorFor(Transparent) doesn't match any themed colour, so it falls
        // back to whatever LocalContentColor already is. Without setting it here, that
        // fallback is Compose's hard-coded default (black) — invisible on a dark
        // background. Every screen's text now correctly inherits onBackground instead.
        CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground) {
            content()
        }
    }
}
