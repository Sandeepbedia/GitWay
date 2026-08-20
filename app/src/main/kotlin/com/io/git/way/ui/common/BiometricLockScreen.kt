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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.io.git.way.ui.theme.GlassBlobPurple
import com.io.git.way.ui.theme.GlassPrimaryButton
import com.io.git.way.ui.theme.LiquidGlassBackground
import kotlinx.coroutines.launch

/** Shown in place of the whole app while Security > Fingerprint Lock is on and the
 * session hasn't been unlocked yet this launch/resume — prompts immediately on first
 * appearance, with a manual retry button if the user dismisses or fails. */
@Composable
fun BiometricLockScreen(onUnlocked: () -> Unit) {
    val activity = LocalContext.current as FragmentActivity
    val scope = rememberCoroutineScope()
    var isPrompting by remember { mutableStateOf(false) }
    var wasCancelled by remember { mutableStateOf(false) }

    fun promptNow() {
        if (isPrompting) return
        isPrompting = true
        scope.launch {
            // runCatching so a rare prompt exception (already active prompt, hardware
            // hiccup) can never leave the lock screen stuck in the "prompting" state —
            // worst case the user just taps Try again.
            val success = runCatching {
                activity.authenticateWithBiometrics(
                    title = "Unlock Git Way",
                    subtitle = "Use your fingerprint, face, or device password to continue"
                )
            }.getOrDefault(false)
            isPrompting = false
            wasCancelled = !success
            if (success) onUnlocked()
        }
    }

    LaunchedEffect(Unit) { promptNow() }

    LiquidGlassBackground {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = null,
                    tint = GlassBlobPurple,
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    "Git Way is locked",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    "Unlock with your fingerprint, face, or device password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                )
                if (wasCancelled) {
                    GlassPrimaryButton(text = "Try again", onClick = { promptNow() })
                }
            }
        }
    }
}
