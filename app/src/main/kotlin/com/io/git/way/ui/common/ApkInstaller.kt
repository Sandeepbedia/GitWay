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

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/** A completed download waiting for the UI to decide what to do with it — install
 * (when [isApk]), save via SAF, or share. */
data class DownloadResult(
    val fileName: String,
    val bytes: ByteArray,
    val isApk: Boolean
)

/**
 * Downloads → device helpers: write bytes to the app cache, extract an APK from an
 * Actions-artifact zip archive, and hand it to the system package installer via
 * FileProvider. No `MANAGE_EXTERNAL_STORAGE` needed — everything stays in app storage.
 */
object ApkInstaller {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /** Downloads arbitrary bytes (e.g. a release APK from its browser_download_url —
     * codeload / objects.githubusercontent.com, which needs no auth). */
    suspend fun downloadBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Download failed (HTTP ${response.code}).")
            response.body?.bytes() ?: throw IOException("Download returned no data.")
        }
    }

    /** Writes [bytes] to a file in the cache dir (used for zips, logs, APKs). */
    fun writeToCache(context: Context, fileName: String, bytes: ByteArray): File {
        val dir = File(context.cacheDir, "downloads").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    /** Extracts the first `*.apk` entry from a zip archive into [targetDir]. */
    fun extractApkFromZip(zipBytes: ByteArray, targetDir: File = File(System.getProperty("java.io.tmpdir"), "gitway-apk").apply { mkdirs() }): File? {
        targetDir.mkdirs()
        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                    val safeName = entry.name.substringAfterLast('/')
                    val out = File(targetDir, safeName)
                    FileOutputStream(out).use { fos ->
                        zip.copyTo(fos)
                    }
                    return out
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return null
    }

    /** Hands an APK file to the system package installer. Returns an error string on
     * failure, or null when the install intent was launched successfully. */
    fun installApk(context: Context, apkFile: File): String? {
        if (!apkFile.exists() || apkFile.length() == 0L) return "APK file is empty or missing."
        return try {
            val uri: Uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resolve = context.packageManager.resolveActivity(intent, 0)
            if (resolve == null) {
                "No app found that can install APKs on this device."
            } else {
                context.startActivity(intent)
                null
            }
        } catch (e: Exception) {
            "Couldn't open the installer: ${e.message}"
        }
    }
}
