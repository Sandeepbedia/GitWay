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
package com.io.git.way.ui.screens.splash

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.io.git.way.BuildConfig
import com.io.git.way.R
import com.io.git.way.ui.theme.HyperCyan
import com.io.git.way.ui.theme.HyperPink
import com.io.git.way.ui.theme.HyperPrimary
import com.io.git.way.ui.theme.HyperPrimaryDark
import com.io.git.way.ui.theme.LiquidGlassBackground
import com.io.git.way.ui.theme.RepoBgPrimary
import com.io.git.way.ui.theme.RepoBorderGlass
import com.io.git.way.ui.theme.RepoBorderNormalLight
import com.io.git.way.ui.theme.RepoElevatedSurface
import com.io.git.way.ui.theme.RepoPurple
import com.io.git.way.ui.theme.RepoPurpleDark
import com.io.git.way.ui.theme.RepoPurpleLight
import com.io.git.way.ui.theme.RepoTextSecondary
import com.io.git.way.ui.theme.RepoTextSecondaryLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Screen 1: Git Way branding over the liquid glass background.
 *
 * Premium choreography, all driven by the app's own purple palette so the first
 * thing anyone sees on-device matches the identity everywhere else:
 *
 *  1. Ambient aurora orbs drift slowly behind everything (dark mode only shows
 *     them at full strength; light mode keeps them faint).
 *  2. The logo mark springs in with EaseOutBack over a breathing glow, wrapped
 *     by a slow comet-tail orbit ring that doubles as the "working" cue.
 *  3. A light sheen sweeps across the mark periodically (glass feel).
 *  4. "Git Way" lands letter-by-letter with a rising fade, tagline follows.
 *  5. A slim gradient loading line + version pill anchor the bottom.
 *  6. The whole center block fades/scales out before [onFinished] fires, so the
 *     hand-off to the next destination never feels like a hard cut.
 *
 * Respects the system "remove animations" accessibility setting
 * (ANIMATOR_DURATION_SCALE == 0): every stage snaps to its final state and the
 * screen finishes quickly instead of animating for two seconds.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val reducedMotion = rememberReducedMotion()

    // ── Entrance animatables ────────────────────────────────────────────
    val markScale = remember { Animatable(if (reducedMotion) 1f else 0.55f) }
    val markAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val tagline = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val bottom = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val exit = remember { Animatable(0f) }
    val letters = remember { "Git Way".map { Animatable(if (reducedMotion) 1f else 0f) } }

    // Master timeline: mark springs in → letters/tagline/bottom are already
    // running on their own delays → hold → exit fade → hand off.
    LaunchedEffect(Unit) {
        if (reducedMotion) {
            delay(600)
            exit.snapTo(1f)
            onFinished()
            return@LaunchedEffect
        }
        launch { markAlpha.animateTo(1f, tween(340)) }
        markScale.animateTo(1f, tween(640, easing = EaseOutBack))
        delay(1080)
        exit.animateTo(1f, tween(320, easing = EaseInCubic))
        onFinished()
    }
    LaunchedEffect(Unit) {
        if (!reducedMotion) delay(720)
        tagline.animateTo(1f, tween(430))
    }
    LaunchedEffect(Unit) {
        if (!reducedMotion) delay(900)
        bottom.animateTo(1f, tween(450))
    }
    LaunchedEffect(Unit) {
        if (reducedMotion) return@LaunchedEffect
        letters.forEachIndexed { index, anim ->
            launch {
                delay(340L + index * 45L)
                anim.animateTo(1f, tween(380, easing = EaseOutCubic))
            }
        }
    }

    LiquidGlassBackground {
        AuroraOrbs(dark = dark, reducedMotion = reducedMotion)

        // Center block — exits as one unit so nothing pops when navigation moves on.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = 1f - exit.value
                    val s = 1f - 0.05f * exit.value
                    scaleX = s
                    scaleY = s
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    MarkGlow(reducedMotion = reducedMotion)
                    OrbitRing(dark = dark, reducedMotion = reducedMotion)
                    LogoMark(
                        markScale = markScale.value,
                        markAlpha = markAlpha.value,
                        reducedMotion = reducedMotion
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Letter-by-letter title — every glyph is laid out from frame one
                // (only alpha/translation animate), so the row never reflows.
                Row {
                    "Git Way".forEachIndexed { index, char ->
                        Text(
                            text = char.toString(),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                brush = Brush.linearGradient(
                                    if (dark) {
                                        listOf(RepoPurpleLight, RepoPurple, HyperPink)
                                    } else {
                                        listOf(HyperPrimaryDark, HyperPrimary, HyperPink)
                                    }
                                )
                            ),
                            modifier = Modifier.graphicsLayer {
                                alpha = letters[index].value
                                translationY = (1f - letters[index].value) * 18f
                            }
                        )
                    }
                }

                Text(
                    text = "Update GitHub from your pocket",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (dark) RepoTextSecondary else RepoTextSecondaryLight,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .graphicsLayer {
                            alpha = tagline.value
                            translationY = (1f - tagline.value) * 12f
                        }
                )
            }
        }

        // Bottom anchor — quiet loading line + version pill.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp)
                .graphicsLayer { alpha = bottom.value },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoadingLine(dark = dark, reducedMotion = reducedMotion)
            Spacer(modifier = Modifier.height(14.dp))
            VersionPill(dark = dark)
        }
    }
}

/** True when the user disabled system animations (accessibility "remove animations"). */
@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}

/**
 * Soft colored glows drifting behind everything. Radial gradients fading to
 * transparent do the "blur" work — no RenderEffect cost, works identically on
 * API 26+. Three hues from the accent hierarchy keep it on-brand, not rainbow.
 */
@Composable
private fun AuroraOrbs(dark: Boolean, reducedMotion: Boolean) {
    val baseAlpha = if (dark) 0.16f else 0.09f
    val transition = rememberInfiniteTransition(label = "aurora")
    val driftX by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse),
        label = "driftX"
    )
    val driftY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8600, easing = LinearEasing), RepeatMode.Reverse),
        label = "driftY"
    )
    val dx = if (reducedMotion) 0f else driftX
    val dy = if (reducedMotion) 0f else driftY

    Box(modifier = Modifier.fillMaxSize()) {
        Orb(
            modifier = Modifier
                .offset(x = (-78 + 40 * dx).dp, y = (-96 + 30 * dy).dp)
                .size(320.dp),
            color = HyperPrimary,
            alpha = baseAlpha
        )
        Orb(
            modifier = Modifier
                .offset(x = (140 + 44 * dx).dp, y = (420 - 36 * dy).dp)
                .size(300.dp),
            color = HyperPink,
            alpha = baseAlpha * 0.8f
        )
        Orb(
            modifier = Modifier
                .offset(x = (-60 + 26 * dy).dp, y = (380 + 30 * dx).dp)
                .size(220.dp),
            color = HyperCyan,
            alpha = baseAlpha * 0.55f
        )
    }
}

@Composable
private fun Orb(modifier: Modifier, color: Color, alpha: Float) {
    Box(
        modifier = modifier.drawBehind {
            drawCircle(
                Brush.radialGradient(
                    listOf(color.copy(alpha = alpha), Color.Transparent)
                )
            )
        }
    )
}

/** Breathing purple halo sitting directly behind the mark. */
@Composable
private fun MarkGlow(reducedMotion: Boolean) {
    val transition = rememberInfiniteTransition(label = "glow")
    val breath by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "breath"
    )
    val glow = if (reducedMotion) 0.85f else breath
    Box(
        modifier = Modifier
            .size(176.dp)
            .graphicsLayer { this.alpha = glow }
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(RepoPurple.copy(alpha = 0.42f), Color.Transparent)
                    )
                )
            }
    )
}

/** Slow comet-tail ring around the mark — reads as "alive / working" without a spinner. */
@Composable
private fun OrbitRing(dark: Boolean, reducedMotion: Boolean) {
    val transition = rememberInfiniteTransition(label = "orbit")
    val rawAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing)),
        label = "orbitAngle"
    )
    val angle = if (reducedMotion) 0f else rawAngle
    Canvas(modifier = Modifier.size(150.dp)) {
        rotate(degrees = angle) {
            drawArc(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.55f to (if (dark) RepoPurpleLight else HyperPrimary).copy(alpha = 0.85f),
                    1f to Color.Transparent
                ),
                startAngle = 0f,
                sweepAngle = 330f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * The brand tile: deep-dark rounded square (like the app icon, premium-fied) so
 * the multicolour Git Way logo pops against it — glass sheen on top, colored
 * elevation shadow, hairline border, and a periodic light sweep clipped to the
 * tile shape. The sweep uses keyframes so it plays for ~850ms then rests ~2.5s.
 *
 * The launcher foreground PNG is drawn at native tile size and then scaled up:
 * adaptive-icon assets keep a large safe-zone margin around the artwork, and
 * zooming past that margin is what makes the logo actually fill the tile.
 */
@Composable
private fun LogoMark(markScale: Float, markAlpha: Float, reducedMotion: Boolean) {
    val shape = RoundedCornerShape(26.dp)
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3400
                0f at 0
                1f at 850
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Box(
        modifier = Modifier
            .size(96.dp)
            .graphicsLayer {
                scaleX = markScale
                scaleY = markScale
                alpha = markAlpha
            }
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = RepoPurple.copy(alpha = 0.45f),
                spotColor = RepoPurpleDark.copy(alpha = 0.50f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(RepoElevatedSurface, RepoBgPrimary))
            )
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape),
        contentAlignment = Alignment.Center
    ) {
        // The real Git Way mark (launcher foreground asset).
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = "Git Way logo",
            modifier = Modifier
                .size(96.dp)
                .scale(1.8f)
        )

        // Static top sheen — the "glass" half of liquid glass.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.20f), Color.Transparent)
                    )
                )
        )

        // Periodic light sweep — clipped by the parent's clip(), so no overflow.
        if (!reducedMotion) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val bandWidth = size.width * 0.42f
                        val x = (-bandWidth) + (size.width + 2 * bandWidth) * shimmer
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.32f),
                                    Color.Transparent
                                ),
                                startX = x,
                                endX = x + bandWidth
                            )
                        )
                    }
            )
        }
    }
}

/** Slim indeterminate line — a soft gradient segment travelling along a quiet track. */
@Composable
private fun LoadingLine(dark: Boolean, reducedMotion: Boolean) {
    val trackColor = if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)
    val transition = rememberInfiniteTransition(label = "loadLine")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "travel"
    )
    val travel = if (reducedMotion) 0.35f else t

    Box(
        modifier = Modifier
            .width(132.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .offset(x = 88.dp * travel)
                .width(44.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, RepoPurple, Color.Transparent)
                    )
                )
        )
    }
}

/** Small pill anchoring the build identity — quiet, out of the way, real info. */
@Composable
private fun VersionPill(dark: Boolean) {
    val pillBackground =
        if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.85f)
    val pillBorder =
        if (dark) RepoBorderGlass else RepoBorderNormalLight
    val textColor =
        if (dark) RepoTextSecondary else RepoTextSecondaryLight

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(pillBackground)
            .border(0.5.dp, pillBorder, CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(HyperPrimary)
        )
        Text(
            text = "v${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_CODE}",
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
