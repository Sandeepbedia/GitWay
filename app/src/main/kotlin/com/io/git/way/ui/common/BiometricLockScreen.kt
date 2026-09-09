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

import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

    // Bug fix: this screen is a single long-lived composable instance for as long as the
    // app stays locked — MainActivity just flips isUnlocked back to false and recomposes
    // the *same* BiometricLockScreen rather than leaving/re-entering composition. A
    // LaunchedEffect(Unit) here only ever fires once for that instance's lifetime, so the
    // old code only auto-prompted on the very first appearance.
    //
    // That first appearance is also the wrong moment on its own: MainActivity sets
    // isUnlocked = false from an ON_STOP observer (screen turning off), which recomposes
    // this screen and fires the effect *while the activity is stopped* — the system can't
    // show a BiometricPrompt over a non-resumed activity, so that attempt silently does
    // nothing (or errors), and nothing was left to retry it once the screen came back on.
    //
    // Listening for ON_RESUME instead covers both cases correctly: it fires once when the
    // activity first becomes resumed after cold start/re-lock (same moment as before, but
    // now guaranteed to be foregrounded), and it fires again every subsequent time the
    // screen is turned back on while this lock screen is still showing.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) promptNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LiquidGlassBackground {
        Box(
            Modifier
                .fillMaxSize()
                // MainActivity now overlays this screen on top of the still-mounted app
                // content instead of swapping it in, so — unlike a plain Box with no click
                // handling, which lets touches fall through to whatever's underneath at the
                // same position — every pointer event needs to be consumed here explicitly,
                // or a tap on an "empty" part of this lock screen could still reach and
                // trigger something in the (invisible, but still interactive) screen below.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
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
