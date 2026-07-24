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
    private val imageExtensions = setOf("png", "jpg", "jpeg", "webp", "svg", "bmp", "heic")
    private val gifExtensions = setOf("gif")
    private val videoExtensions = setOf("mp4", "mov", "mkv", "avi", "webm")
    private val audioExtensions = setOf("mp3", "wav", "ogg", "flac", "m4a")
    private val docExtensions = setOf("md", "txt", "rst")
    private val pdfExtensions = setOf("pdf")
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
}
