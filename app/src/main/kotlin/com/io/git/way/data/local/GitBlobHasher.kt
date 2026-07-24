package com.io.git.way.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Computes the Git blob SHA-1 of a local file the same way `git hash-object` does:
 * sha1("blob {size}\0{content}"). Comparing this against GitHub's tree blob sha is the
 * cheap way to detect "Modified" without downloading remote file content (PRD1 §3.2).
 */
object GitBlobHasher {

    suspend fun hash(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        val header = "blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(header)
        digest.update(bytes)
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}
