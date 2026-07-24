package com.io.git.way.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass design system for Git Way — a small set of reusable building blocks that
 * give every screen a frosted, translucent "glass" look: soft colour blobs behind the
 * content, frosted cards with a light specular border, and gradient pill buttons.
 * There is no true backdrop-blur-of-content on API < 31, so instead we blur the blob
 * layer itself and let translucent surfaces sit on top of it — this reads as glass at
 * every API level (minSdk 26) without any extra dependency.
 *
 * The blobs are intentionally STATIC (no infinite drift animation) — a continuously
 * animating background was found distracting/battery-costly, so the "movement" was
 * removed and replaced with a fixed radial-gradient placement plus a subtle vertical
 * gradient wash behind everything for colour.
 *
 * [dark] is resolved from the actual resolved background luminance rather than
 * [androidx.compose.foundation.isSystemInDarkTheme], so it always matches the user's
 * chosen theme mode (System/Light/Dark/AMOLED) instead of the raw system setting —
 * this is also what was causing card text to pick the wrong (illegible) content colour.
 */
private val MaterialTheme.isDarkSurface: Boolean
    @Composable get() = colorScheme.background.luminance() < 0.5f

/** Softly gradient-tinted backdrop with a few fixed, blurred colour blobs — no motion. */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = MaterialTheme.isDarkSurface
    val scheme = MaterialTheme.colorScheme
    val blobAlpha = if (dark) 0.45f else 0.30f

    val backdrop = Brush.verticalGradient(
        listOf(
            scheme.background,
            (if (dark) GlassBlobPurple else GlassBlobBlue).copy(alpha = if (dark) 0.10f else 0.06f),
            scheme.background
        )
    )

    Box(
        modifier
            .fillMaxSize()
            .background(backdrop)
    ) {
        Box(
            Modifier
                .size(260.dp)
                .align(Alignment.TopStart)
                .offset(x = (-70).dp, y = (-60).dp)
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
                .offset(x = 60.dp, y = 20.dp)
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
                .offset(x = 0.dp, y = 60.dp)
                .blur(95.dp)
                .background(
                    Brush.radialGradient(listOf(GlassBlobTeal.copy(alpha = blobAlpha), Color.Transparent)),
                    CircleShape
                )
        )
        content()
    }
}

/** Scaffold pre-wired with the liquid glass background + a transparent, frosted top bar. */
@Composable
fun GlassScaffold(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val dark = MaterialTheme.isDarkSurface
    val titleColor = if (dark) Color.White else MaterialTheme.colorScheme.onBackground

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.SemiBold, color = titleColor) },
                    navigationIcon = navigationIcon,
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = titleColor,
                        navigationIconContentColor = titleColor,
                        actionIconContentColor = titleColor
                    )
                )
            },
            bottomBar = bottomBar,
            content = content
        )
    }
}

/** Shared glass fill/border colours + a resolved, always-legible content colour. */
@Composable
private fun glassTreatment(): GlassTreatment {
    val dark = MaterialTheme.isDarkSurface
    return GlassTreatment(
        fillTop = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.55f),
        fillBottom = if (dark) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.22f),
        borderTop = if (dark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f),
        borderBottom = if (dark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.25f),
        // The bug: cards never set a content colour, so Text()/Icon() inside them fell
        // back to whatever LocalContentColor happened to be further up the tree —
        // frequently a near-black default, illegible on a dark frosted card. Every
        // glass surface below now explicitly provides a colour guaranteed to contrast
        // with its own fill.
        content = if (dark) Color.White.copy(alpha = 0.94f) else MaterialTheme.colorScheme.onSurface
    )
}

private data class GlassTreatment(
    val fillTop: Color,
    val fillBottom: Color,
    val borderTop: Color,
    val borderBottom: Color,
    val content: Color
)

/** A frosted glass panel: translucent gradient fill + a soft specular border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val t = glassTreatment()

    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(t.fillTop, t.fillBottom)))
            .border(1.dp, Brush.verticalGradient(listOf(t.borderTop, t.borderBottom)), shape)
            .padding(padding)
    ) {
        CompositionLocalProvider(LocalContentColor provides t.content) {
            content()
        }
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
    val t = glassTreatment()

    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(t.fillTop, t.fillBottom)))
            .border(1.dp, Brush.verticalGradient(listOf(t.borderTop, t.borderBottom)), shape)
            .clickable(onClick = onClick)
            .padding(padding)
    ) {
        CompositionLocalProvider(LocalContentColor provides t.content) {
            content()
        }
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
    val dark = MaterialTheme.isDarkSurface
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val fill = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.35f)
    val textColor = if (enabled) scheme.primary else scheme.onSurface.copy(alpha = 0.35f)

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
            Text(text, color = textColor, fontWeight = FontWeight.SemiBold)
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
    val dark = MaterialTheme.isDarkSurface
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val fill = if (selected) scheme.primary.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f)
    val border = if (selected) scheme.primary else Color.White.copy(alpha = 0.4f)
    val textColor = when {
        selected -> Color.White
        dark -> Color.White.copy(alpha = 0.9f)
        else -> scheme.onSurface
    }

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
