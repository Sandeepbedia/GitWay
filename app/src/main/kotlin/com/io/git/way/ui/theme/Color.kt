/*
 * GitWay — ASUS OS 3.0 "ROG Red" color system.
 *
 * Light mode: porcelain #F5F5F7 background, white cards, #E60035 ROG red.
 * Dark mode: near-black #080808 base with layered surfaces (#101010, #1B1B1B,
 * #252525) and the brighter #FF003C ROG red. There is deliberately no
 * translucency — every surface token is opaque, matching ASUS OS "solid" style.
 *
 * The AsusLight* / AsusDark* values below are the full ASUS OS 3.0 token table.
 * The legacy semantic names (Hyper*, Repo*) are kept and re-pointed at those
 * tokens, so every screen picks up the new look automatically.
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

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────
// ASUS OS 3.0 — Light
// ─────────────────────────────────────────────
val AsusLightBackground = Color(0xFFF5F5F7)
val AsusLightSurface = Color(0xFFFFFFFF)
val AsusLightCard = Color(0xFFFFFFFF)
val AsusLightSecondaryCard = Color(0xFFF0F0F2)
val AsusLightElevated = Color(0xFFEEEEF1)
val AsusLightDialog = Color(0xFFFFFFFF)
val AsusLightPrimary = Color(0xFFE60035)
val AsusLightPrimaryPressed = Color(0xFFB8002B)
val AsusLightPrimaryContainer = Color(0xFFFFE5EB)
val AsusLightOnPrimary = Color(0xFFFFFFFF)
val AsusLightOnPrimaryContainer = Color(0xFFB8002B)
val AsusLightTertiary = Color(0xFFFF5722)
val AsusLightTextPrimary = Color(0xFF171717)
val AsusLightTextSecondary = Color(0xFF666666)
val AsusLightTextTertiary = Color(0xFF888888)
val AsusLightTextDisabled = Color(0xFFAAAAAA)
val AsusLightBorder = Color(0xFFDDDDDD)
val AsusLightDivider = Color(0xFFE8E8E8)
val AsusLightInputBorder = Color(0xFFCCCCCC)
val AsusLightSuccess = Color(0xFF008F3D)
val AsusLightWarning = Color(0xFFB77900)
val AsusLightError = Color(0xFFD92D20)
val AsusLightErrorContainer = Color(0xFFFBE9EA)
val AsusLightInfo = Color(0xFF1769AA)
val AsusLightInverseSurface = Color(0xFF171717)
val AsusLightScrim = Color(0xFF000000)

// ─────────────────────────────────────────────
// ASUS OS 3.0 — Dark
// ─────────────────────────────────────────────
val AsusDarkBackground = Color(0xFF080808)
val AsusDarkSurface = Color(0xFF101010)
val AsusDarkCard = Color(0xFF1B1B1B)
val AsusDarkSecondaryCard = Color(0xFF202020)
val AsusDarkElevated = Color(0xFF252525)
val AsusDarkPopup = Color(0xFF292929)
val AsusDarkDialog = Color(0xFF303030)
val AsusDarkPrimary = Color(0xFFFF003C)
val AsusDarkPrimaryPressed = Color(0xFFC0002E)
val AsusDarkPrimaryContainer = Color(0xFF38101B)
val AsusDarkOnPrimary = Color(0xFFFFFFFF)
val AsusDarkOnPrimaryContainer = Color(0xFFFF476B)
val AsusDarkTertiary = Color(0xFFFF5722)
val AsusDarkTextPrimary = Color(0xFFFFFFFF)
val AsusDarkTextSecondary = Color(0xFFBDBDBD)
val AsusDarkTextTertiary = Color(0xFF888888)
val AsusDarkTextDisabled = Color(0xFF555555)
val AsusDarkBorder = Color(0xFF393939)
val AsusDarkDivider = Color(0xFF292929)
val AsusDarkInputBorder = Color(0xFF454545)
val AsusDarkSuccess = Color(0xFF00C853)
val AsusDarkWarning = Color(0xFFFFB300)
val AsusDarkError = Color(0xFFFF453A)
val AsusDarkErrorContainer = Color(0xFF38101B)
val AsusDarkInfo = Color(0xFF2196F3)
val AsusDarkInverseSurface = Color(0xFFFFFFFF)
val AsusDarkScrim = Color(0xFF000000)

// ─────────────────────────────────────────────
// Semantic aliases — ASUS OS 3.0
// ─────────────────────────────────────────────

// Primary — ROG red. Light uses the deeper #E60035, dark the brighter #FF003C.
val HyperPrimary = AsusLightPrimary
val HyperPrimaryDark = AsusLightPrimaryPressed
val HyperPrimaryLight = AsusDarkPrimary
val HyperPrimaryDisabled = AsusLightTextDisabled

// Secondary — ASUS info blue (links / informational states).
val HyperCyan = AsusLightInfo
val HyperAuroraPurple = AsusLightTertiary
val HyperAuroraPurpleLight = AsusDarkTertiary
val HyperPink = AsusDarkOnPrimaryContainer

// Status
val RepoSuccess = AsusDarkSuccess
val RepoWarning = AsusDarkWarning
val RepoDanger = AsusDarkError
val RepoInfo = HyperCyan
val RepoSecondary = HyperCyan
val RepoSecondaryLight = AsusDarkInfo
val RepoSecondaryDark = AsusLightInfo

// ── Layered dark surfaces — section < card < elevated < dialog ─────────────
val RepoBgPrimary = AsusDarkBackground
val RepoBgGradientTop = AsusDarkSurface
val RepoBgGradientMid = AsusDarkBackground
val RepoBgGradientBottom = AsusDarkBackground

val RepoSectionSurface = AsusDarkSurface
val RepoCardSurface = AsusDarkCard
val RepoElevatedSurface = AsusDarkElevated
val RepoDialogSurface = AsusDarkDialog
val RepoBottomNavSurface = AsusDarkCard
val RepoSearchBarSurface = AsusDarkSecondaryCard
val RepoFloatingButtonSurface = AsusDarkElevated

// ── Light surfaces — flat ASUS OS paper layers ──────────────────────────────
val RepoBgPrimaryLight = AsusLightBackground
val RepoBgGradientTopLight = AsusLightBackground
val RepoBgGradientBottomLight = AsusLightSecondaryCard
val RepoSectionSurfaceLight = AsusLightSecondaryCard
val RepoCardSurfaceLight = AsusLightCard
val RepoElevatedSurfaceLight = AsusLightElevated
val RepoSearchBarSurfaceLight = AsusLightSurface
val RepoBottomNavSurfaceLight = AsusLightSurface

// AMOLED
val AmoledBlack = Color(0xFF000000)
val AmoledSurface = AsusDarkSurface

// Borders
val GlassHighlight = Color(0xFFFFFFFF)
val GlassShadowDark = AsusLightBorder
val RepoBorderNormal = AsusDarkBorder
val RepoBorderGlass = Color(0x26FFFFFF)
val RepoBorderSelected = AsusDarkPrimary
val RepoBorderNormalLight = AsusLightBorder
val RepoBorderSelectedLight = AsusLightPrimary

// Accent gradients — red only, used for active controls.
val RepoPurple = HyperPrimary
val RepoPurpleLight = HyperPrimaryLight
val RepoPurpleDark = HyperPrimaryDark
val RepoPurpleGradientStart = AsusDarkOnPrimaryContainer
val RepoPurpleGradientEnd = AsusDarkPrimaryPressed
val RepoPillGradientLight = AsusLightPrimaryContainer

val HyperPrimaryGradient = listOf(AsusLightPrimary, AsusDarkPrimary)
val HyperAuroraGradient = listOf(AsusLightPrimary, AsusLightTertiary)
val HyperDarkPremiumGradient = listOf(AsusDarkBackground, AsusDarkCard, AsusDarkElevated)

// Legacy names retained for source compatibility.
val GlassBlobBlue = HyperPrimary
val GlassBlobPurple = AsusLightTertiary
val GlassBlobTeal = HyperCyan
val GlassBlobPink = AsusDarkOnPrimaryContainer

// Repository language colors — vivid, distinct, tuned for both surfaces.
val LangKotlin = Color(0xFF7F52FF)
val LangJava = Color(0xFFE8833E)
val LangCpp = Color(0xFF4F8FE0)
val LangPython = Color(0xFF22C55E)
val LangJavaScript = Color(0xFFE8C547)
val LangMisc = Color(0xFF16AFA0)
val LangRust = Color(0xFFDE7B3B)
val LangGo = Color(0xFF00ACD7)

// Text — dark mode
val RepoTextPrimary = AsusDarkTextPrimary
val RepoTextSecondary = AsusDarkTextSecondary
val RepoTextMuted = AsusDarkTextTertiary
val RepoTextDisabled = AsusDarkTextDisabled
val RepoTextHint = AsusDarkTextTertiary

// Text — light mode
val RepoTextPrimaryLight = AsusLightTextPrimary
val RepoTextSecondaryLight = AsusLightTextSecondary
val RepoTextMutedLight = AsusLightTextTertiary

// Icons
val RepoIconDefault = AsusDarkTextPrimary
val RepoIconInactive = AsusDarkTextTertiary
val RepoIconAccent = HyperPrimary

// Theme aliases
val RepoPrimary = HyperPrimary
val RepoPrimaryLight = HyperPrimaryLight
val RepoAuroraPurple = HyperAuroraPurple

// Diff status colors
val DiffAddedGreen = RepoSuccess
val DiffModifiedYellow = RepoWarning
val DiffRemovedRed = RepoDanger
