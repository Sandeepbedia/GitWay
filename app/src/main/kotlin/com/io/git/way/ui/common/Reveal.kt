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
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

/**
 * Git Way's shared entrance animation (ported from the PhoneIDE/LSP project-tree
 * reveal system): a row or section rises/slides in, fades in and settles from a
 * slightly smaller scale, each one a beat after the one before it.
 *
 * Everything is painted in a graphics layer, so it costs no recomposition and no
 * relayout — it cannot move a tap target out from under a finger, only draw it.
 *
 * Usage: `val reveal = rememberReveal()` once per screen/panel, then
 * `Modifier.revealIn(reveal, key = "row-path", order = index)` on each thing that
 * should enter.
 */
class RevealController internal constructor(
    internal val trigger: Any,
    internal val reduceMotion: Boolean,
) {
    /** Keys that have already played for this trigger, so scrolling back does not replay them. */
    internal val played = HashSet<Any>()
}

/**
 * @param trigger changing it replays every entrance; the first composition always plays once.
 */
@Composable
fun rememberReveal(trigger: Any = Unit): RevealController {
    val context = LocalContext.current
    val reduce = remember(context) { animationsDisabled(context) }
    return remember(trigger, reduce) { RevealController(trigger, reduce) }
}

/** Shape and timing of one entrance. */
data class RevealStyle(
    val riseDp: Float = 36f,
    val slideXDp: Float = 0f,
    val startScale: Float = 0.94f,
    val durationMs: Int = 620,
    val staggerMs: Long = 70L,
    /** Items past this position start together instead of waiting their turn. */
    val maxStaggered: Int = 7,
) {
    companion object {
        /** Cards and sections on a full screen. */
        val Section = RevealStyle()

        /** Rows in a list or tree: shorter travel, quicker, tighter stagger — used for
         * the Repository Explorer's file-tree rows. */
        val Row = RevealStyle(riseDp = 0f, slideXDp = -18f, startScale = 1f, durationMs = 380, staggerMs = 22L, maxStaggered = 14)

        /** Items in a popup menu. */
        val Menu = RevealStyle(riseDp = 8f, startScale = 0.97f, durationMs = 300, staggerMs = 28L, maxStaggered = 10)

        /** Content inside a sheet that is itself sliding in. */
        val Sheet = RevealStyle(riseDp = 14f, startScale = 0.98f, durationMs = 420, staggerMs = 45L, maxStaggered = 5)
    }
}

/** Soft landing: quick off the mark, long gentle settle — motion that arrives and then rests. */
private val RevealEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

/**
 * Apply the entrance to this element.
 *
 * @param key identifies the element for [RevealController.played]; must be stable.
 * @param order position in the stagger (0 = first).
 * @param enabled false leaves the element untouched — used for list rows beyond the first screenful.
 */
@Composable
fun Modifier.revealIn(
    controller: RevealController,
    key: Any,
    order: Int,
    style: RevealStyle = RevealStyle.Section,
    enabled: Boolean = true,
): Modifier {
    val skip = !enabled || controller.reduceMotion
    // Hidden from the very first frame unless this element already played — no flash of the
    // finished layout before the entrance begins, and a replay resets in the frame it triggers.
    val progress = remember(controller) {
        Animatable(if (skip || key in controller.played) 1f else 0f)
    }

    LaunchedEffect(controller) {
        if (skip || key in controller.played) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        controller.played += key
        progress.snapTo(0f)
        if (order < style.maxStaggered) delay(order * style.staggerMs)
        progress.animateTo(1f, tween(style.durationMs, easing = RevealEasing))
    }

    if (skip) return this
    return graphicsLayer {
        val p = progress.value
        alpha = (p * 1.35f).coerceAtMost(1f) // fades in a little ahead of the motion settling
        translationY = (1f - p) * style.riseDp.dp.toPx()
        translationX = (1f - p) * style.slideXDp.dp.toPx()
        val scale = style.startScale + (1f - style.startScale) * p
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Counter that goes up when the app comes back from the background after a real absence, so a
 * screen can replay its entrance on reopen. Short trips — a permission dialog, the system file
 * picker, the notification shade — leave it alone: replaying then would blink the screen.
 */
@Composable
fun rememberReopenEpoch(replayAfterMs: Long = 5_000L): Int {
    var epoch by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        var stoppedAt = 0L
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> stoppedAt = System.currentTimeMillis()
                Lifecycle.Event.ON_START -> {
                    if (stoppedAt != 0L && System.currentTimeMillis() - stoppedAt >= replayAfterMs) epoch++
                    stoppedAt = 0L
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return epoch
}

/** True when the system animation scale is 0 — "Remove animations" in accessibility settings. */
private fun animationsDisabled(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)
