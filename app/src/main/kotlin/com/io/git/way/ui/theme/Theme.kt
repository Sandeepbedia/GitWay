/*
 * GitWay — Material 3 ColorScheme mapped from the ASUS OS 3.0 "ROG Red"
 * palette defined in Color.kt: opaque near-black surfaces with #FF003C ROG red
 * in dark mode, and porcelain #F5F5F7 surfaces with #E60035 in light mode.
 * Every Material surface role (dim/bright and the container ladder) is mapped
 * so menus, sheets, dialogs and cards all follow the same ASUS layering.
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = AsusLightPrimary,
    onPrimary = AsusLightOnPrimary,
    primaryContainer = AsusLightPrimaryContainer,
    onPrimaryContainer = AsusLightOnPrimaryContainer,
    inversePrimary = AsusLightPrimaryPressed,
    secondary = AsusLightPrimaryPressed,
    onSecondary = AsusLightOnPrimary,
    secondaryContainer = AsusLightSecondaryCard,
    onSecondaryContainer = AsusLightPrimaryPressed,
    tertiary = AsusLightTertiary,
    onTertiary = AsusLightOnPrimary,
    tertiaryContainer = AsusLightPrimaryContainer,
    onTertiaryContainer = AsusLightPrimaryPressed,
    background = AsusLightBackground,
    onBackground = AsusLightTextPrimary,
    surface = AsusLightSurface,
    onSurface = AsusLightTextPrimary,
    surfaceVariant = AsusLightCard,
    onSurfaceVariant = AsusLightTextSecondary,
    surfaceTint = AsusLightSurface,
    surfaceDim = AsusLightBackground,
    surfaceBright = AsusLightCard,
    surfaceContainerLowest = AsusLightCard,
    surfaceContainerLow = AsusLightCard,
    surfaceContainer = AsusLightDialog,
    surfaceContainerHigh = AsusLightDialog,
    surfaceContainerHighest = AsusLightElevated,
    inverseSurface = AsusLightInverseSurface,
    inverseOnSurface = AsusLightCard,
    outline = AsusLightInputBorder,
    outlineVariant = AsusLightDivider,
    scrim = AsusLightScrim,
    error = AsusLightError,
    onError = AsusLightOnPrimary,
    errorContainer = AsusLightErrorContainer,
    onErrorContainer = AsusLightError
)

private val DarkColorScheme = darkColorScheme(
    primary = AsusDarkPrimary,
    onPrimary = AsusDarkOnPrimary,
    primaryContainer = AsusDarkPrimaryContainer,
    onPrimaryContainer = AsusDarkOnPrimaryContainer,
    inversePrimary = AsusDarkOnPrimaryContainer,
    secondary = AsusDarkOnPrimaryContainer,
    onSecondary = AsusDarkBackground,
    secondaryContainer = AsusDarkDialog,
    onSecondaryContainer = AsusDarkTextPrimary,
    tertiary = AsusDarkTertiary,
    onTertiary = AsusDarkBackground,
    tertiaryContainer = AsusDarkPopup,
    onTertiaryContainer = AsusDarkTertiary,
    background = AsusDarkBackground,
    onBackground = AsusDarkTextPrimary,
    surface = AsusDarkSurface,
    onSurface = AsusDarkTextPrimary,
    surfaceVariant = AsusDarkCard,
    onSurfaceVariant = AsusDarkTextSecondary,
    surfaceTint = AsusDarkSurface,
    surfaceDim = AsusDarkBackground,
    surfaceBright = AsusDarkDialog,
    surfaceContainerLowest = AsusDarkBackground,
    surfaceContainerLow = AsusDarkSecondaryCard,
    surfaceContainer = AsusDarkPopup,
    surfaceContainerHigh = AsusDarkDialog,
    surfaceContainerHighest = AsusDarkDialog,
    inverseSurface = AsusDarkInverseSurface,
    inverseOnSurface = AsusDarkBackground,
    outline = AsusDarkInputBorder,
    outlineVariant = AsusDarkDivider,
    scrim = AsusDarkScrim,
    error = AsusDarkError,
    onError = AsusDarkBackground,
    errorContainer = AsusDarkErrorContainer,
    onErrorContainer = AsusDarkError
)

private val AmoledColorScheme = DarkColorScheme.copy(
    background = AmoledBlack,
    surface = AmoledSurface,
    surfaceDim = AmoledBlack,
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerLow = AmoledBlack,
    surfaceContainer = AsusDarkSecondaryCard,
    surfaceContainerHigh = AsusDarkCard,
    surfaceContainerHighest = AsusDarkCard
)

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
        CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground) {
            content()
        }
    }
}
