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
package com.io.git.way.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.io.git.way.domain.model.AppUpdateInfo
import com.io.git.way.ui.theme.GlassPrimaryButton
import com.io.git.way.ui.theme.GlassSecondaryButton
import com.io.git.way.ui.theme.RepoDialogSurface
import com.io.git.way.ui.theme.RepoPurple
import com.io.git.way.ui.theme.RepoPurpleDark
import com.io.git.way.ui.theme.RepoPurpleLight
import com.io.git.way.ui.theme.RepoTextPrimary
import com.io.git.way.ui.theme.RepoTextSecondary

/** Shown when [com.io.git.way.domain.repository.GitHubRepository.checkForUpdate] finds
 * a newer GitHub release than the installed app. [onUpdate] should open the APK
 * download (or the release page, if no APK asset was attached); [onDismiss] just
 * closes it for this session — there's no "don't ask again", since a manual PAT-based
 * push tool is exactly the kind of app people want to know is current. */
@Composable
fun UpdateAvailableDialog(
    update: AppUpdateInfo,
    isDownloading: Boolean = false,
    downloadError: String? = null,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(RepoDialogSurface, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.linearGradient(listOf(RepoPurpleLight, RepoPurple, RepoPurpleDark)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = RepoTextPrimary)
                }
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        "Update available",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = RepoTextPrimary
                    )
                    Text(
                        update.versionTag,
                        style = MaterialTheme.typography.bodySmall,
                        color = RepoTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                update.releaseTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = RepoTextPrimary
            )
            Text(
                update.releaseNotes,
                style = MaterialTheme.typography.bodySmall,
                color = RepoTextSecondary,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState())
            )

            Spacer(modifier = Modifier.height(22.dp))

            if (isDownloading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        "Downloading update…",
                        style = MaterialTheme.typography.bodySmall,
                        color = RepoTextSecondary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                if (downloadError != null) {
                    Text(
                        downloadError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassSecondaryButton(
                        text = "Later",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    GlassPrimaryButton(
                        text = "Update",
                        onClick = onUpdate,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
