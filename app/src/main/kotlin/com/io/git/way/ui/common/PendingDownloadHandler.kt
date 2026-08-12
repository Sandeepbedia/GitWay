package com.io.git.way.ui.common

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme

/**
 * Shared UI for a [DownloadResult] sitting in session state: offers Install (APKs),
 * Save to Downloads (SAF create-document), and Share (ACTION_SEND via FileProvider).
 * Screens that trigger downloads call this with the current pending download; it owns
 * the SAF launcher and the install/share intents.
 */
@Composable
fun PendingDownloadHandler(
    pending: DownloadResult?,
    downloading: Boolean,
    downloadError: String?,
    onClear: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null && pending != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(pending.bytes) }
            onClear()
        }
    }

    if (downloadError != null) {
        AlertDialog(
            onDismissRequest = onClearError,
            title = { Text("Download failed") },
            text = { Text(downloadError) },
            confirmButton = { TextButton(onClick = onClearError) { Text("OK") } }
        )
    }

    if (pending != null) {
        val isApk = pending.isApk
        AlertDialog(
            onDismissRequest = onClear,
            title = { Text(if (isApk) "Ready to install" else "Download complete") },
            text = {
                Column {
                    Text(pending.fileName, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (isApk) "This is an APK built by GitHub Actions. Install it now, save it to Downloads, or share it."
                        else "Save it to Downloads or share it with another app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            },
            confirmButton = {
                if (isApk) {
                    TextButton(
                        onClick = {
                            val file = ApkInstaller.writeToCache(context, pending.fileName, pending.bytes)
                            val error = ApkInstaller.installApk(context, file)
                            if (error == null) onClear()
                        }
                    ) { Text("Install") }
                }
                TextButton(onClick = { saveLauncher.launch(pending.fileName) }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { shareBytes(context, pending.fileName, pending.bytes) }) { Text("Share") }
                TextButton(onClick = onClear) { Text("Cancel") }
            }
        )
    }

    if (downloading) {
        AlertDialog(
            onDismissRequest = { /* download can't be dismissed mid-flight */ },
            title = { Text("Downloading…") },
            text = {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    CircularProgressIndicator()
                    Text(
                        "Fetching from GitHub…",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            },
            confirmButton = {}
        )
    }
}

private fun shareBytes(context: android.content.Context, fileName: String, bytes: ByteArray) {
    val file = ApkInstaller.writeToCache(context, fileName, bytes)
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Share $fileName")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
