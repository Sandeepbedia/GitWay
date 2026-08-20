/*
 * GitWay — premium dark-mode color system.
 *
 * Dark mode follows the MIUI/HyperOS-inspired premium spec: a near-black
 * #0B0C0F base with layered surfaces (#15171C cards, #1D2026 elevated),
 * soft #292C33 borders and a purple hero accent — never pure black except
 * in AMOLED mode. Light mode keeps the Bravo neumorphic porcelain family
 * (#E7EBF0 background, white cards).
 *
 * All semantic names are kept so every screen picks up the new look
 * automatically — only the values changed.
 */
package com.io.git.way.ui.theme

import androidx.compose.ui.graphics.Color

// ── Accent hierarchy ─────────────────────────────────────────────────
// Primary — Bravo hero purple (Home gradient)
val HyperPrimary = Color(0xFF7F5CF0)
val HyperPrimaryDark = Color(0xFF6A3DFF)      // pressed / emphasis on dark surfaces (pill bottom)
val HyperPrimaryLight = Color(0xFFB18CFF)     // containers, tints (home gradient end)
val HyperPrimaryDisabled = Color(0xFFCFC0FF)

// Secondary — teal, used for links, info states, secondary actions (Library gradient)
val HyperCyan = Color(0xFF12C2A9)
val HyperAuroraPurple = Color(0xFF4D6BFE)     // tertiary — tags, highlights (Settings gradient)
val HyperAuroraPurpleLight = Color(0xFF6BC5FF)
val HyperPink = Color(0xFFFF4E8E)             // Dashboard gradient

val RepoSuccess = Color(0xFF32D583)
val RepoWarning = Color(0xFFFFB020)
val RepoDanger = Color(0xFFFF5B5B)
val RepoInfo = HyperCyan
val RepoSecondary = HyperCyan
val RepoSecondaryLight = Color(0xFF67E8C4)
val RepoSecondaryDark = Color(0xFF0E8F7C)

// ── Dark mode — premium layered surfaces on a near-black base ──
// #0B0C0F base with subtle depth gradient (pure black reserved for AMOLED).
val RepoBgPrimary = Color(0xFF0B0C0F)
val RepoBgGradientTop = Color(0xFF0E1014)
val RepoBgGradientMid = Color(0xFF0B0C0F)
val RepoBgGradientBottom = Color(0xFF08090C)

// section < card < elevated < dialog — layered surfaces instead of outlines
val RepoSectionSurface = Color(0xFF0F1116)
val RepoCardSurface = Color(0xFF15171C)
val RepoElevatedSurface = Color(0xFF1D2026)
val RepoDialogSurface = Color(0xFF23262E)
val RepoBottomNavSurface = Color(0xF015171C)
val RepoSearchBarSurface = Color(0xE615171C)
val RepoFloatingButtonSurface = Color(0xFF1D2026)

// ── Light mode — Bravo neumorphic porcelain with a faint cool-gray tone ─
val RepoBgPrimaryLight = Color(0xFFE7EBF0)
val RepoBgGradientTopLight = Color(0xFFF3F5F8)
val RepoBgGradientBottomLight = Color(0xFFE4E8EE)
val RepoSectionSurfaceLight = Color(0xFFE7EBF0)
val RepoCardSurfaceLight = Color(0xFFFFFFFF)
val RepoElevatedSurfaceLight = Color(0xFFDDE2E9)
val RepoSearchBarSurfaceLight = Color(0xFFFFFFFF)
val RepoBottomNavSurfaceLight = Color(0xF5FFFFFF)

// AMOLED
val AmoledBlack = Color(0xFF000000)
val AmoledSurface = Color(0xFF0E0E10)

// Neumorphic soft shadows (Bravo — light mode)
val GlassHighlight = Color(0xFFFFFFFF)
val GlassShadowDark = Color(0xFFA3B1C6)
val RepoBorderNormal = Color(0xFF292C33)
val RepoBorderGlass = Color(0x26FFFFFF)
val RepoBorderSelected = HyperPrimary
val RepoBorderNormalLight = Color(0x14A3B1C6)
val RepoBorderSelectedLight = HyperPrimaryLight

// Accent gradients — restrained, used only for active controls.
val RepoPurple = HyperPrimary
val RepoPurpleLight = HyperPrimaryLight
val RepoPurpleDark = HyperPrimaryDark
val RepoPurpleGradientStart = Color(0xFF9D6CFF)   // pill top
val RepoPurpleGradientEnd = Color(0xFF6A3DFF)     // pill bottom
val RepoPillGradientLight = Color(0xFFE9E2FF)

val HyperPrimaryGradient = listOf(HyperPrimary, HyperPrimaryLight)
val HyperAuroraGradient = listOf(HyperPrimary, HyperPink)
val HyperDarkPremiumGradient = listOf(RepoBgPrimary, RepoCardSurface, RepoElevatedSurface)

// Legacy names retained for source compatibility.
val GlassBlobBlue = HyperPrimary
val GlassBlobPurple = HyperAuroraPurple
val GlassBlobTeal = HyperCyan
val GlassBlobPink = HyperPink

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
val RepoTextPrimary = Color(0xFFF7F7F8)
val RepoTextSecondary = Color(0xFFA6A9B0)
val RepoTextMuted = Color(0xFF767B85)
val RepoTextDisabled = Color(0xFF4A4E56)
val RepoTextHint = Color(0xFF767B85)

// Text — light mode
val RepoTextPrimaryLight = Color(0xFF1A1D26)
val RepoTextSecondaryLight = Color(0xFF4E5563)
val RepoTextMutedLight = Color(0xFF8A92A1)

// Icons
val RepoIconDefault = Color(0xFFF7F7F8)
val RepoIconInactive = Color(0xFF767B85)
val RepoIconAccent = HyperPrimary

// Theme aliases
val RepoPrimary = HyperPrimary
val RepoPrimaryLight = HyperPrimaryLight
val RepoAuroraPurple = HyperAuroraPurple

// Diff status colors
val DiffAddedGreen = RepoSuccess
val DiffModifiedYellow = RepoWarning
val DiffRemovedRed = RepoDanger