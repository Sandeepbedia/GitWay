package com.io.git.way.ui.theme

import androidx.compose.ui.graphics.Color

// Git Way brand palette — developer-tool feel, GitHub-adjacent blues/greens.
val GitBlue80 = Color(0xFFA9C7FF)
val GitBlueGrey80 = Color(0xFFBFC6DC)
val GitGreen80 = Color(0xFFA0D9A6)

val GitBlue40 = Color(0xFF2F5FCB)
val GitBlueGrey40 = Color(0xFF48566E)
val GitGreen40 = Color(0xFF2E7D4F)

val AmoledBlack = Color(0xFF000000)
val AmoledSurface = Color(0xFF0A0A0A)

val DiffAddedGreen = Color(0xFF2EA043)
val DiffModifiedYellow = Color(0xFFD29922)
val DiffRemovedRed = Color(0xFFDA3633)

// --- Liquid Glass palette: soft, saturated blobs that float behind frosted surfaces. ---
val GlassBlobBlue = Color(0xFF3E7BFA)
val GlassBlobPurple = Color(0xFF9B5CF6)
val GlassBlobTeal = Color(0xFF2CD9C5)
val GlassBlobPink = Color(0xFFF6618C)

val GlassHighlight = Color(0xFFFFFFFF)
val GlassShadowDark = Color(0xFF000000)

// ============================================================================
// Repository Screen — Premium Color System v2
// Premium AMOLED + GitHub + Linear + Material 3 Expressive. Purple is an ACCENT
// only (active tab, FAB, buttons, selection, icons) — never a background wash —
// so every dark surface reads as one consistent palette instead of clashing
// grey-background / pure-black-card / random-purple-bleed.
// ============================================================================

// --- Background (dark) ---
val RepoBgPrimary = Color(0xFF09090B)
val RepoBgGradientTop = Color(0xFF151520)
val RepoBgGradientMid = Color(0xFF11131C)
val RepoBgGradientBottom = Color(0xFF09090B)

// --- Surfaces (dark) ---
val RepoCardSurface = Color(0xFF171923)
val RepoElevatedSurface = Color(0xFF1D2030)
val RepoDialogSurface = Color(0xFF202437)
val RepoBottomNavSurface = Color(0x8C121522)
val RepoSearchBarSurface = Color(0xFF151824)
val RepoFloatingButtonSurface = Color(0xFF1B1F2D)

// --- Borders (dark) ---
val RepoBorderNormal = Color(0x14FFFFFF)   // #FFFFFF14
val RepoBorderSelected = Color(0x66B388FF) // #B388FF66
val RepoBorderGlass = Color(0x1FFFFFFF)    // #FFFFFF1F

// --- Background / surfaces / borders (light counterpart — spec only defines dark,
// this keeps the same structure + accent for the Light theme option) ---
val RepoBgPrimaryLight = Color(0xFFF5F4FA)
val RepoBgGradientTopLight = Color(0xFFFCFBFF)
val RepoBgGradientBottomLight = Color(0xFFEFEEF6)
val RepoCardSurfaceLight = Color(0xFFFFFFFF)
val RepoElevatedSurfaceLight = Color(0xFFFFFFFF)
val RepoSearchBarSurfaceLight = Color(0xFFFFFFFF)
val RepoBottomNavSurfaceLight = Color(0x8CFFFFFF)
val RepoBorderNormalLight = Color(0x14000000)
val RepoBorderSelectedLight = Color(0x668B5CF6)

// --- Primary accent (purple identity) ---
val RepoPurple = Color(0xFF8B5CF6)
val RepoPurpleLight = Color(0xFFA78BFA)
val RepoPurpleDark = Color(0xFF6D28D9)
val RepoPurpleGradientStart = Color(0xFFB388FF)
val RepoPurpleGradientEnd = Color(0xFF6D28D9)
val RepoPillGradientLight = Color(0xFFD8C5FF)

// --- Secondary accents ---
val RepoSuccess = Color(0xFF22C55E)
val RepoInfo = Color(0xFF06B6D4)
val RepoWarning = Color(0xFFF59E0B)
val RepoDanger = Color(0xFFEF4444)

// --- Repository language colors ---
val LangKotlin = Color(0xFF8B5CF6)
val LangJava = Color(0xFFF97316)
val LangCpp = Color(0xFF3B82F6)
val LangPython = Color(0xFF22C55E)
val LangJavaScript = Color(0xFFFACC15)
val LangMisc = Color(0xFF14B8A6)
val LangRust = Color(0xFFF97316)
val LangGo = Color(0xFF38BDF8)

// --- Text ---
val RepoTextPrimary = Color(0xFFFFFFFF)
val RepoTextSecondary = Color(0xFFB7BDD3)
val RepoTextMuted = Color(0xFF8B92A8)
val RepoTextDisabled = Color(0xFF666D84)
val RepoTextHint = Color(0xFF7B8195)

val RepoTextPrimaryLight = Color(0xFF17181F)
val RepoTextSecondaryLight = Color(0xFF5A5F73)
val RepoTextMutedLight = Color(0xFF888EA3)

// --- Icons ---
val RepoIconDefault = Color(0xFFE6E8EF)
val RepoIconInactive = Color(0xFF9096AB)
val RepoIconAccent = Color(0xFFB388FF)
