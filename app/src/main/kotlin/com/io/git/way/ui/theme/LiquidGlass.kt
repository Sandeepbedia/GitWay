package com.io.git.way.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass design system for Git Way — a small set of reusable building blocks that
 * give every screen a frosted, translucent "glass" look: soft moving colour blobs behind
 * the content, frosted cards with a light specular border, and gradient pill buttons.
 * There is no true backdrop-blur-of-content on API < 31, so instead we blur the blob
 * layer itself and let translucent surfaces sit on top of it — this reads as glass at
 * every API level (minSdk 26) without any extra dependency.
 */

/** Animated, blurred colour blobs drifting behind the screen content — the "liquid" part. */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    val transition = rememberInfiniteTransition(label = "liquidGlassBlobs")
    val driftA by transition.animateFloatDrift(9000)
    val driftB by transition.animateFloatDrift(12000)
    val driftC by transition.animateFloatDrift(15500)

    val blobAlpha = if (dark) 0.5f else 0.35f

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            Modifier
                .size(260.dp)
                .align(Alignment.TopStart)
                .offset(x = (-70).dp + (driftA * 50).dp, y = (-60).dp + (driftB * 30).dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(listOf(GlassBlobBlue.copy(alpha = blobAlpha), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = (80).dp - (driftC * 40).dp, y = (10).dp + (driftA * 40).dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(listOf(GlassBlobPurple.copy(alpha = blobAlpha), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier
                .size(280.dp)
                .align(Alignment.BottomCenter)
                .offset(x = (driftB * 60 - 30).dp, y = (70).dp - (driftC * 30).dp)
                .blur(95.dp)
                .background(
                    Brush.radialGradient(listOf(GlassBlobTeal.copy(alpha = blobAlpha), Color.Transparent)),
                    CircleShape
                )
        )
        content()
    }
}

@Composable
private fun InfiniteTransition.animateFloatDrift(durationMs: Int) =
    animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift$durationMs"
    )

/** Scaffold pre-wired with the liquid glass background + a transparent, frosted top bar. */
@Composable
fun GlassScaffold(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = navigationIcon,
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            },
            bottomBar = bottomBar,
            content = content
        )
    }
}

/** A frosted glass panel: translucent gradient fill + a soft specular border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    val fillTop = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.55f)
    val fillBottom = if (dark) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.22f)
    val borderTop = if (dark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f)
    val borderBottom = if (dark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.25f)

    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(fillTop, fillBottom)))
            .border(1.dp, Brush.verticalGradient(listOf(borderTop, borderBottom)), shape)
            .padding(padding)
    ) {
        content()
    }
}

/** Same frosted treatment as [GlassCard] but clickable, for list rows / tappable tiles. */
@Composable
fun GlassClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    val fillTop = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.55f)
    val fillBottom = if (dark) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.22f)
    val borderTop = if (dark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f)
    val borderBottom = if (dark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.25f)

    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(fillTop, fillBottom)))
            .border(1.dp, Brush.verticalGradient(listOf(borderTop, borderBottom)), shape)
            .clickable(onClick = onClick)
            .padding(padding)
    ) {
        content()
    }
}

/** Primary call-to-action: a gradient glass pill, used for Continue / Upload / Connect actions. */
@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(scheme.primary, scheme.tertiary))
    } else {
        Brush.horizontalGradient(
            listOf(scheme.onSurface.copy(alpha = 0.10f), scheme.onSurface.copy(alpha = 0.10f))
        )
    }
    val textColor = if (enabled) Color.White else scheme.onSurface.copy(alpha = 0.35f)

    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(brush)
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.35f else 0.08f), shape)
            .then(
                if (enabled && !loading) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                leadingIcon?.invoke()
                Text(text, color = textColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Secondary action: outlined glass pill (frosted, no fill gradient). */
@Composable
fun GlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val dark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val fill = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.35f)

    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, scheme.primary.copy(alpha = if (enabled) 0.55f else 0.15f), shape)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            leadingIcon?.invoke()
            Text(
                text,
                color = if (enabled) scheme.primary else scheme.onSurface.copy(alpha = 0.35f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Small frosted pill used for chips/badges/filters. */
@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val fill = if (selected) scheme.primary.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f)
    val border = if (selected) scheme.primary else Color.White.copy(alpha = 0.4f)
    val textColor = if (selected) Color.White else scheme.onSurface

    Row(
        modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leadingIcon?.invoke()
        Text(text, style = MaterialTheme.typography.labelMedium, color = textColor, fontWeight = FontWeight.Medium)
    }
}
