/*
 * Git Way
 * Copyright (C) 2026 Sandeep Bedia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
    "kotlin" to RepoVisual(LangKotlin, Icons.Filled.Code),
    "java" to RepoVisual(LangJava, Icons.Filled.Coffee),
    "python" to RepoVisual(LangPython, Icons.Filled.DataObject),
    "javascript" to RepoVisual(LangJavaScript, Icons.Filled.Language),
    "typescript" to RepoVisual(LangJavaScript, Icons.Filled.Language),
    "c++" to RepoVisual(LangCpp, Icons.Filled.Memory),
    "c" to RepoVisual(LangCpp, Icons.Filled.Memory),
    "rust" to RepoVisual(LangRust, Icons.Filled.Bolt),
    "go" to RepoVisual(LangGo, Icons.Filled.Bolt),
    "html" to RepoVisual(LangJava, Icons.Filled.Brush),
    "css" to RepoVisual(LangGo, Icons.Filled.Brush),
    "shell" to RepoVisual(LangPython, Icons.Filled.Terminal),
    "dart" to RepoVisual(LangMisc, Icons.Filled.PhoneAndroid),
    "misc" to RepoVisual(LangMisc, Icons.Filled.Memory)
)

/** Fallback palette cycled by a stable hash so unknown/null-language repos still look distinct. */
private val fallbackPalette = listOf(
    RepoVisual(LangKotlin, Icons.Filled.Code),
    RepoVisual(LangGo, Icons.Filled.Folder),
    RepoVisual(LangPython, Icons.Filled.Description),
    RepoVisual(LangJava, Icons.Filled.Bolt),
    RepoVisual(LangCpp, Icons.Filled.DataObject),
    RepoVisual(LangMisc, Icons.Filled.Memory)
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
