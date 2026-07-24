package com.io.git.way.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** A repository's card accent — a colour + a representative icon (PRD "left accent bar... colour based on repository language"). */
data class RepoVisual(val color: Color, val icon: ImageVector)

private val languageVisuals: Map<String, RepoVisual> = mapOf(
    "kotlin" to RepoVisual(GlassBlobPurple, Icons.Filled.Code),
    "java" to RepoVisual(Color(0xFFF08A3C), Icons.Filled.Coffee),
    "python" to RepoVisual(GlassBlobBlue, Icons.Filled.DataObject),
    "javascript" to RepoVisual(Color(0xFFE0C93E), Icons.Filled.Language),
    "typescript" to RepoVisual(GlassBlobBlue, Icons.Filled.Language),
    "c++" to RepoVisual(GlassBlobPink, Icons.Filled.Memory),
    "c" to RepoVisual(Color(0xFF6F86D6), Icons.Filled.Memory),
    "rust" to RepoVisual(Color(0xFFE0622A), Icons.Filled.Bolt),
    "go" to RepoVisual(GlassBlobTeal, Icons.Filled.Bolt),
    "html" to RepoVisual(Color(0xFFE0622A), Icons.Filled.Brush),
    "css" to RepoVisual(GlassBlobBlue, Icons.Filled.Brush),
    "shell" to RepoVisual(Color(0xFF3EAA6A), Icons.Filled.Terminal),
    "dart" to RepoVisual(GlassBlobTeal, Icons.Filled.PhoneAndroid)
)

/** Fallback palette cycled by a stable hash so unknown/null-language repos still look distinct. */
private val fallbackPalette = listOf(
    RepoVisual(GlassBlobPurple, Icons.Filled.Code),
    RepoVisual(GlassBlobBlue, Icons.Filled.Folder),
    RepoVisual(Color(0xFF3EAA6A), Icons.Filled.Description),
    RepoVisual(Color(0xFFE0A23C), Icons.Filled.Bolt),
    RepoVisual(GlassBlobPink, Icons.Filled.DataObject),
    RepoVisual(GlassBlobTeal, Icons.Filled.Memory)
)

/** Resolves a stable [RepoVisual] for a repo — same language always maps to the same look,
 * and repos with no language still get a consistent (not random-per-recomposition) look. */
fun repoVisualFor(name: String, language: String?): RepoVisual {
    val known = language?.lowercase()?.let { languageVisuals[it] }
    if (known != null) return known
    val index = Math.floorMod(name.hashCode(), fallbackPalette.size)
    return fallbackPalette[index]
}

/** Short capsule label shown on the card — the repo's language, or "Misc" when GitHub reports none. */
fun repoLanguageLabel(language: String?): String = language?.takeIf { it.isNotBlank() } ?: "Misc"
