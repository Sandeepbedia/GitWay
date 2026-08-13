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
val GitBlue80 = Color(0xFFC7D2FE)
val GitBlueGrey80 = Color(0xFFC7D2E0)
val GitGreen80 = Color(0xFFA7F3D0)

val GitBlue40 = Color(0xFF4F46E5)
val GitBlueGrey40 = Color(0xFF475569)
val GitGreen40 = Color(0xFF16A34A)

val AmoledBlack = Color(0xFF000000)
val AmoledSurface = Color(0xFF0A0A0A)

val DiffAddedGreen = Color(0xFF2EA043)
val DiffModifiedYellow = Color(0xFFD29922)
val DiffRemovedRed = Color(0xFFDA3633)

// --- Liquid Glass palette: soft, saturated blobs that float behind frosted surfaces. ---
val GlassBlobBlue = Color(0xFF4F46E5)
val GlassBlobPurple = Color(0xFF6366F1)
val GlassBlobTeal = Color(0xFF06B6D4)
val GlassBlobPink = Color(0xFF8B5CF6)

val GlassHighlight = Color(0xFFFFFFFF)
val GlassShadowDark = Color(0xFF000000)

// ============================================================================
// GitWay unified application palette
// Primary: indigo/violet for actions and active states.
// Secondary: cyan for navigation/information.
// Tertiary: green for success/status.
// Surfaces stay neutral so accent colors remain readable.
// ============================================================================

// Dark surfaces
val RepoBgPrimary = Color(0xFF0F172A)
val RepoBgGradientTop = Color(0xFF172033)
val RepoBgGradientMid = Color(0xFF121C2D)
val RepoBgGradientBottom = Color(0xFF0F172A)
val RepoCardSurface = Color(0xFF182233)
val RepoElevatedSurface = Color(0xFF202D40)
val RepoDialogSurface = Color(0xFF243247)
val RepoBottomNavSurface = Color(0xEE182233)
val RepoSearchBarSurface = Color(0xFF172235)
val RepoFloatingButtonSurface = Color(0xFF243247)

// Light surfaces
val RepoBgPrimaryLight = Color(0xFFF6F8FB)
val RepoBgGradientTopLight = Color(0xFFFFFFFF)
val RepoBgGradientBottomLight = Color(0xFFEEF2F7)
val RepoCardSurfaceLight = Color(0xFFFFFFFF)
val RepoElevatedSurfaceLight = Color(0xFFF1F5F9)
val RepoSearchBarSurfaceLight = Color(0xFFFFFFFF)
val RepoBottomNavSurfaceLight = Color(0xF2FFFFFF)

// Borders
val RepoBorderNormal = Color(0x22FFFFFF)
val RepoBorderSelected = Color(0x805B5FEF)
val RepoBorderGlass = Color(0x2EFFFFFF)
val RepoBorderNormalLight = Color(0x220F172A)
val RepoBorderSelectedLight = Color(0x805B5FEF)

// Primary / secondary / tertiary
val RepoPurple = Color(0xFF6366F1)
val RepoPurpleLight = Color(0xFF818CF8)
val RepoPurpleDark = Color(0xFF4F46E5)
val RepoPurpleGradientStart = Color(0xFF818CF8)
val RepoPurpleGradientEnd = Color(0xFF4F46E5)
val RepoPillGradientLight = Color(0xFFE0E7FF)

val RepoSecondary = Color(0xFF06B6D4)
val RepoSecondaryLight = Color(0xFF22D3EE)
val RepoSecondaryDark = Color(0xFF0891B2)
val RepoSuccess = Color(0xFF22C55E)
val RepoInfo = Color(0xFF06B6D4)
val RepoWarning = Color(0xFFF59E0B)
val RepoDanger = Color(0xFFEF4444)

// Repository language colors
val LangKotlin = Color(0xFF7C83FF)
val LangJava = Color(0xFFF97316)
val LangCpp = Color(0xFF3B82F6)
val LangPython = Color(0xFF22C55E)
val LangJavaScript = Color(0xFFEAB308)
val LangMisc = Color(0xFF14B8A6)
val LangRust = Color(0xFFF97316)
val LangGo = Color(0xFF38BDF8)

// Text
val RepoTextPrimary = Color(0xFFF8FAFC)
val RepoTextSecondary = Color(0xFFB8C2D1)
val RepoTextMuted = Color(0xFF7F8B9D)
val RepoTextDisabled = Color(0xFF5B6676)
val RepoTextHint = Color(0xFF718096)
val RepoTextPrimaryLight = Color(0xFF172033)
val RepoTextSecondaryLight = Color(0xFF536174)
val RepoTextMutedLight = Color(0xFF7A8798)

// Icons
val RepoIconDefault = Color(0xFFE2E8F0)
val RepoIconInactive = Color(0xFF8995A7)
val RepoIconAccent = Color(0xFF818CF8)
