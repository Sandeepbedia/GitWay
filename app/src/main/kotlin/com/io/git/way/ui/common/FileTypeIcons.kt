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
package com.io.git.way.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.io.git.way.ui.theme.GlassBlobBlue
import com.io.git.way.ui.theme.GlassBlobPink
import com.io.git.way.ui.theme.GlassBlobPurple
import com.io.git.way.ui.theme.GlassBlobTeal

/** File-manager style extension -> icon/colour lookup, purely cosmetic. */
object FileTypeIcons {

    private val codeExtensions = setOf(
        "kt", "kts", "java", "py", "js", "ts", "tsx", "jsx", "c", "cpp", "h", "hpp",
        "cs", "go", "rs", "rb", "php", "swift", "sh", "gradle"
    )
    private val dataExtensions = setOf("json", "xml", "yaml", "yml", "toml", "csv", "sql")

    /** Every raster/vector image format Git Way recognises for icon/colour, the "Images"
     * type filter, and the file-metadata label — one list so those three never drift out
     * of sync with each other again. */
    private val imageExtensions = setOf(
        // Common raster formats
        "png", "jpg", "jpeg", "jpe", "jfif", "webp", "bmp", "dib",
        // Vector
        "svg", "svgz",
        // Apple / HEIF family
        "heic", "heif",
        // Next-gen web formats
        "avif",
        // TIFF
        "tif", "tiff",
        // Icons/cursors
        "ico", "icns", "cur",
        // Legacy/rare bitmap formats still seen in older Android/asset repos
        "pbm", "pgm", "ppm", "pnm", "xbm", "xpm", "tga",
        // RAW camera formats (occasionally checked into design/asset folders)
        "raw", "cr2", "nef", "arw", "dng", "orf", "rw2"
    )
    private val gifExtensions = setOf("gif")
    private val videoExtensions = setOf("mp4", "mov", "mkv", "avi", "webm", "3gp", "m4v")
    private val audioExtensions = setOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "opus")
    private val docExtensions = setOf("md", "txt", "rst")
    private val pdfExtensions = setOf("pdf")

    /** Single source of truth for "is this file an image" — used by the icon/colour/label
     * lookups below AND by [com.io.git.way.ui.common.GitWaySessionViewModel]'s Images type
     * filter, so a format recognised here is never missed by the filter (or vice versa). */
    fun isImage(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in imageExtensions || ext in gifExtensions
    }
    private val configExtensions = setOf("properties", "cfg", "ini", "env", "lock")

    fun iconFor(fileName: String): ImageVector = when (fileName.substringAfterLast('.', "").lowercase()) {
        in codeExtensions -> Icons.Filled.Code
        in dataExtensions -> Icons.Filled.DataObject
        in imageExtensions -> Icons.Filled.Image
        in gifExtensions -> Icons.Filled.Gif
        in videoExtensions -> Icons.Filled.MovieFilter
        in audioExtensions -> Icons.Filled.MusicNote
        in docExtensions -> Icons.Filled.TextSnippet
        in pdfExtensions -> Icons.Filled.PictureAsPdf
        in configExtensions -> Icons.Filled.Settings
        "" -> Icons.Filled.InsertDriveFile
        else -> Icons.Filled.Description
    }

    fun colorFor(fileName: String): Color = when (fileName.substringAfterLast('.', "").lowercase()) {
        in codeExtensions -> GlassBlobBlue
        in dataExtensions -> GlassBlobPurple
        in imageExtensions, in gifExtensions -> GlassBlobPink
        in videoExtensions -> GlassBlobPink
        in audioExtensions -> GlassBlobTeal
        in pdfExtensions -> GlassBlobPink
        else -> GlassBlobTeal
    }

    /** Human readable "12.4 KB" / "3.2 MB" style size, for the file-manager list. */
    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.1f GB", gb)
    }

    /** Human "AndroidManifest.xml -> XML Document" style label (PRD §6/§7). */
    fun typeLabel(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower == ".gitignore" -> "Git Ignore File"
            lower == ".gitattributes" -> "Git Attributes File"
            lower.endsWith(".gradle.kts") -> "Kotlin Script"
            lower.endsWith(".gradle") -> "Gradle Script"
            lower.endsWith(".pro") -> "ProGuard Rules"
            lower.endsWith(".kt") -> "Kotlin File"
            lower.endsWith(".kts") -> "Kotlin Script"
            lower.endsWith(".java") -> "Java File"
            lower.endsWith(".xml") -> "XML Document"
            lower.endsWith(".json") -> "JSON Document"
            lower.endsWith(".md") || lower.endsWith(".markdown") -> "Markdown Document"
            lower.endsWith(".yml") || lower.endsWith(".yaml") -> "YAML Document"
            lower.endsWith(".properties") -> "Properties File"
            lower.endsWith(".toml") -> "TOML Document"
            lower.endsWith(".zip") || lower.endsWith(".jar") || lower.endsWith(".aar") -> "Archive"
            lower.endsWith(".pdf") -> "PDF Document"
            fileName.substringAfterLast('.', "") in imageExtensions -> "Image"
            fileName.substringAfterLast('.', "") in gifExtensions -> "GIF Image"
            fileName.substringAfterLast('.', "") in videoExtensions -> "Video"
            fileName.substringAfterLast('.', "") in audioExtensions -> "Audio"
            fileName.substringAfterLast('.', "").isEmpty() -> "File"
            else -> "${fileName.substringAfterLast('.').uppercase()} File"
        }
    }

    /** Short badge text ("XML", "KTS", "PRO") shown next to a file row. */
    fun badgeFor(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".gradle.kts") -> "KTS"
            lower == ".gitignore" -> "GIT"
            else -> fileName.substringAfterLast('.', "").uppercase().take(5)
        }
    }

    /** Wired custom SVG icon (res/raw, e.g. kt.svg) for a filename, if a dedicated one
     * exists — these are the hand-picked file-manager icons (Kotlin, Java, XML, Gradle,
     * LICENSE, ...). Returns null when nothing was drawn specifically for that file, so
     * callers fall back to the generic Material icon from [iconFor]. */
    fun iconResFor(fileName: String): Int? {
        val lower = fileName.lowercase()
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            lower == "gradlew" -> com.io.git.way.R.raw.gradlew
            lower == "license" || lower == "license.md" || lower == "license.txt" ||
                lower == "licence" || lower == "licence.md" || lower == "licence.txt" ->
                com.io.git.way.R.raw.licence
            lower == "pubspec.yaml" || lower == "pubspec.yml" -> com.io.git.way.R.raw.flutter
            lower == ".env.example" || lower == "env.example" -> com.io.git.way.R.raw.env_example
            ext == "kts" -> com.io.git.way.R.raw.kts
            ext == "kt" -> com.io.git.way.R.raw.kt
            ext == "java" -> com.io.git.way.R.raw.java
            ext == "xml" -> com.io.git.way.R.raw.xml
            ext == "json" -> com.io.git.way.R.raw.json
            ext == "md" || ext == "markdown" -> com.io.git.way.R.raw.md
            ext == "gradle" -> com.io.git.way.R.raw.gradle
            ext == "properties" -> com.io.git.way.R.raw.properties
            ext == "pro" -> com.io.git.way.R.raw.pro
            ext == "bat" -> com.io.git.way.R.raw.bt
            ext == "jpg" || ext == "jpeg" || ext == "jpe" || ext == "jfif" -> com.io.git.way.R.raw.jpg
            ext == "png" -> com.io.git.way.R.raw.png
            ext == "webp" -> com.io.git.way.R.raw.webp
            ext == "dart" -> com.io.git.way.R.raw.flutter
            ext == "7z" -> com.io.git.way.R.raw.sevenz
            ext == "class" -> com.io.git.way.R.raw.javaclass
            else -> null
        }
    }
}
