/*
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

import androidx.compose.ui.graphics.Color

// Git Way brand palette — developer-tool feel, GitHub-adjacent blues/greens.
val GitBlue80 = Color(0xFFCBD5FF)
val GitBlueGrey80 = Color(0xFFC9D3E4)
val GitGreen80 = Color(0xFFA0F0D0)

val GitBlue40 = Color(0xFF5B4FE8)
val GitBlueGrey40 = Color(0xFF475569)
val GitGreen40 = Color(0xFF12B886)

val AmoledBlack = Color(0xFF000000)
val AmoledSurface = Color(0xFF0C0C12)

val DiffAddedGreen = Color(0xFF3FB876)
val DiffModifiedYellow = Color(0xFFE0A83E)
val DiffRemovedRed = Color(0xFFF0625E)

// --- Liquid Glass palette: soft, saturated blobs that float behind frosted surfaces. ---
val GlassBlobBlue = Color(0xFF5B4FE8)
val GlassBlobPurple = Color(0xFF8A6FF0)
val GlassBlobTeal = Color(0xFF19C3D6)
val GlassBlobPink = Color(0xFFB16CE0)

val GlassHighlight = Color(0xFFFFFFFF)
val GlassShadowDark = Color(0xFF000000)

// ============================================================================
// GitWay unified application palette — "Premium Model" v3
// Primary: royal indigo/violet, reserved for actions, active states, and the
//   signature gradient (pill nav, buttons, key accents).
// Secondary: refined electric teal — navigation highlights, links, info.
// Tertiary: emerald — success/status only, never decorative.
// Surfaces form one coherent tonal family per mode (a warm-neutral graphite in
// dark, a soft indigo-tinted porcelain in light) so cards read as "elevated"
// rather than mismatched, and every accent keeps full contrast on top.
// ============================================================================

// Dark surfaces — graphite family with a faint violet undertone, not flat navy.
val RepoBgPrimary = Color(0xFF0B0C12)
val RepoBgGradientTop = Color(0xFF17151F)
val RepoBgGradientMid = Color(0xFF121017)
val RepoBgGradientBottom = Color(0xFF0B0C12)
val RepoCardSurface = Color(0xFF1A1826)
val RepoElevatedSurface = Color(0xFF231F33)
val RepoDialogSurface = Color(0xFF2A2440)
val RepoBottomNavSurface = Color(0xF01A1826)
val RepoSearchBarSurface = Color(0xFF191725)
val RepoFloatingButtonSurface = Color(0xFF2A2440)

// Light surfaces — porcelain with a whisper of indigo, not stark white.
val RepoBgPrimaryLight = Color(0xFFF5F5FB)
val RepoBgGradientTopLight = Color(0xFFFFFFFF)
val RepoBgGradientBottomLight = Color(0xFFECEBF8)
val RepoCardSurfaceLight = Color(0xFFFFFFFF)
val RepoElevatedSurfaceLight = Color(0xFFF0EFFA)
val RepoSearchBarSurfaceLight = Color(0xFFFFFFFF)
val RepoBottomNavSurfaceLight = Color(0xF5FFFFFF)

// Borders
val RepoBorderNormal = Color(0x24FFFFFF)
val RepoBorderSelected = Color(0x8A6D5EF0)
val RepoBorderGlass = Color(0x33FFFFFF)
val RepoBorderNormalLight = Color(0x1F171235)
val RepoBorderSelectedLight = Color(0x8A6D5EF0)

// Primary / secondary / tertiary
val RepoPurple = Color(0xFF6D5EF0)
val RepoPurpleLight = Color(0xFF9285F5)
val RepoPurpleDark = Color(0xFF4E3FD6)
val RepoPurpleGradientStart = Color(0xFF9285F5)
val RepoPurpleGradientEnd = Color(0xFF5B4FE8)
val RepoPillGradientLight = Color(0xFFE3DFFC)

val RepoSecondary = Color(0xFF19C3D6)
val RepoSecondaryLight = Color(0xFF4FE0EF)
val RepoSecondaryDark = Color(0xFF0E96A8)
val RepoSuccess = Color(0xFF2FCB80)
val RepoInfo = Color(0xFF19C3D6)
val RepoWarning = Color(0xFFF0A93E)
val RepoDanger = Color(0xFFF0625E)

// Repository language colors — kept distinct from primary so cards don't camouflage
// against the accent, tuned to the same saturation/lightness family for cohesion.
val LangKotlin = Color(0xFF9285F5)
val LangJava = Color(0xFFF0975C)
val LangCpp = Color(0xFF5AA3F5)
val LangPython = Color(0xFF2FCB80)
val LangJavaScript = Color(0xFFEFCB5C)
val LangMisc = Color(0xFF2FD1B8)
val LangRust = Color(0xFFF0975C)
val LangGo = Color(0xFF4FCBEF)

// Text — dark mode
val RepoTextPrimary = Color(0xFFF7F6FC)
val RepoTextSecondary = Color(0xFFBFBAD4)
val RepoTextMuted = Color(0xFF8B85A6)
val RepoTextDisabled = Color(0xFF615C78)
val RepoTextHint = Color(0xFF7B7595)

// Text — light mode
val RepoTextPrimaryLight = Color(0xFF1C1730)
val RepoTextSecondaryLight = Color(0xFF524C6B)
val RepoTextMutedLight = Color(0xFF7B7591)

// Icons
val RepoIconDefault = Color(0xFFEBE9F5)
val RepoIconInactive = Color(0xFF9B95B5)
val RepoIconAccent = Color(0xFF9285F5)
