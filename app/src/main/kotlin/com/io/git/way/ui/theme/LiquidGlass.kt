package com.io.git.way.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
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

/** Background gradient per spec: a single consistent dark palette (#151520 → #11131C →
 * #09090B), no primary/tertiary colour wash. Purple only ever appears as an accent on
 * top of this — buttons, active tab, selection, icons — never as part of the backdrop
 * itself, which is what was causing the background/card mismatch. */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = MaterialTheme.isDarkSurface
    val backdrop = if (dark) {
        Brush.verticalGradient(listOf(RepoBgGradientTop, RepoBgGradientMid, RepoBgGradientBottom))
    } else {
        Brush.verticalGradient(listOf(RepoBgGradientTopLight, RepoBgGradientBottomLight))
    }

    Box(
        modifier
            .fillMaxSize()
            .background(backdrop)
    ) {
        content()
    }
}

/** Scaffold pre-wired with the liquid glass background + a transparent, frosted top bar.
 * Also resolves [LocalContentColor] for the whole content area to the theme's real text
 * colour — without this, any bare `Text(...)` with no explicit `color=` (i.e. anything
 * not inside a [GlassCard]) fell back to the Compose default content colour, which reads
 * as near-invisible on the dark background.
 *
 * Bottom inset: only the *content* padding includes the navigation-bar/gesture-area
 * height — the [LiquidGlassBackground] behind it is unaffected and still paints all the
 * way to the true bottom edge. That's what keeps things like the "Sync updated project
 * from device" button (or any other bottom-docked action) clear of the gesture handle,
 * without bringing back a solid bar behind it. */
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
    val bodyColor = if (dark) RepoTextPrimary else RepoTextPrimaryLight

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
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
            content = { padding ->
                CompositionLocalProvider(LocalContentColor provides bodyColor) {
                    content(padding)
            }
        }
    )
    }
}

/** Shared glass fill/border colours + a resolved, always-legible content colour.
 * Per spec: cards are a FLAT single surface colour (no gradient into primary/tertiary)
 * with a flat hairline border — purple is reserved for [selected] state or accents
 * elsewhere, never the card's resting border. */
@Composable
private fun glassTreatment(selected: Boolean = false): GlassTreatment {
    val dark = MaterialTheme.isDarkSurface
    val scheme = MaterialTheme.colorScheme
    val border = when {
        selected && dark -> RepoBorderSelected
        selected && !dark -> RepoBorderSelectedLight
        dark -> RepoBorderNormal
        else -> RepoBorderNormalLight
    }
    return GlassTreatment(
        fillTop = scheme.surface,
        fillBottom = scheme.surface,
        borderTop = border,
        borderBottom = border,
        content = if (dark) RepoTextPrimary else RepoTextPrimaryLight
    )
}

private data class GlassTreatment(
    val fillTop: Color,
    val fillBottom: Color,
    val borderTop: Color,
    val borderBottom: Color,
    val content: Color
)

/** A frosted glass panel: flat surface fill + a soft hairline border (purple only when [selected]). */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    padding: Dp = 16.dp,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val t = glassTreatment(selected)

    Column(
        modifier
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.18f)
            )
            .clip(shape)
            .background(t.fillTop)
            .border(1.dp, t.borderTop, shape)
            .padding(padding)
    ) {
        CompositionLocalProvider(LocalContentColor provides t.content) {
            content()
        }
    }
}

/** Same flat treatment as [GlassCard] but clickable, for list rows / tappable tiles. */
@Composable
fun GlassClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    padding: Dp = 16.dp,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val t = glassTreatment(selected)

    Column(
        modifier
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.18f)
            )
            .clip(shape)
            .background(t.fillTop)
            .border(1.dp, t.borderTop, shape)
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
        Brush.horizontalGradient(listOf(RepoPurpleGradientStart, RepoPurple, RepoPurpleGradientEnd))
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
            .shadow(8.dp, shape, clip = false)
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
    val shape = RoundedCornerShape(18.dp)
    val fill = if (dark) RepoElevatedSurface else RepoElevatedSurfaceLight
    val textColor = if (enabled) RepoPurple else (if (dark) RepoTextDisabled else RepoTextMutedLight)

    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(7.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.14f), spotColor = Color.Black.copy(alpha = 0.14f))
            .clip(shape)
            .background(fill)
            .border(1.dp, RepoPurple.copy(alpha = if (enabled) 0.55f else 0.15f), shape)
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
    val shape = RoundedCornerShape(50)
    val fill = when {
        selected -> RepoPurple
        dark -> RepoElevatedSurface
        else -> RepoElevatedSurfaceLight
    }
    val border = when {
        selected -> RepoPurple
        dark -> RepoBorderNormal
        else -> RepoBorderNormalLight
    }
    val textColor = when {
        selected -> RepoTextPrimary
        dark -> RepoTextMuted
        else -> RepoTextMutedLight
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

/** Small circular glass button used for header actions (filter, add, theme, etc.). */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable () -> Unit
) {
    val dark = MaterialTheme.isDarkSurface
    val fill = if (dark) RepoElevatedSurface else RepoElevatedSurfaceLight
    val border = if (dark) RepoBorderNormal else RepoBorderNormalLight
    val contentColor = if (dark) RepoIconDefault else RepoTextPrimaryLight

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "iconButtonScale")

    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(8.dp, CircleShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, border, CircleShape)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/** The main FAB: radial purple gradient per spec, used for primary create/add actions. */
@Composable
fun GlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "fabScale")
    val brush = Brush.radialGradient(listOf(RepoPurpleLight, RepoPurple, RepoPurpleDark))

    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = RepoPurple.copy(alpha = 0.4f),
                spotColor = RepoPurple.copy(alpha = 0.4f)
            )
            .clip(CircleShape)
            .background(brush)
            .clickable(onClick = onClick, interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            content()
        }
    }
}

/** Large rounded search field with a glass background and an animated focus border. */
@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search"
) {
    val dark = MaterialTheme.isDarkSurface
    val shape = RoundedCornerShape(30.dp)
    var focused by remember { mutableStateOf(false) }

    val restingBorder = if (dark) RepoBorderNormal else RepoBorderNormalLight
    val borderColor by animateColorAsState(
        targetValue = if (focused) RepoPurple.copy(alpha = 0.85f) else restingBorder,
        label = "searchBorder"
    )
    val fill = if (dark) RepoSearchBarSurface else RepoSearchBarSurfaceLight
    val contentColor = if (dark) RepoTextPrimary else RepoTextPrimaryLight
    val hintColor = if (dark) RepoTextHint else RepoTextMutedLight

    Box(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(4.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.14f), spotColor = Color.Black.copy(alpha = 0.14f))
            .clip(shape)
            .background(fill)
            .border(1.5.dp, borderColor, shape)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = RepoIconAccent)
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, color = hintColor)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
                    cursorBrush = SolidColor(RepoPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                )
            }
            if (value.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Clear",
                    tint = hintColor,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onValueChange("") }
                )
            }
        }
    }
}

/** Shimmering placeholder card shown while the repository list is loading, instead of a spinner. */
@Composable
fun GlassSkeletonCard(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.isDarkSurface
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmer by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    val base = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.4f)
    val shape = RoundedCornerShape(28.dp)

    Row(
        modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(shape)
            .background(base)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = shimmer * 0.3f))
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(120.dp, 14.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = shimmer * 0.3f)))
            Box(Modifier.size(80.dp, 10.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = shimmer * 0.2f)))
        }
    }
}

/** The three primary destinations behind the floating bottom nav. */
enum class BottomNavTab(val label: String) {
    OVERVIEW("Overview"),
    REPOSITORIES("Repositories"),
    PROFILE("Profile")
}

/** Elevated floating navigation dock with a compact, expressive active-tab pill. */
@Composable
fun GlassFloatingBottomNav(
    selected: BottomNavTab,
    onSelect: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = MaterialTheme.isDarkSurface
    val shape = RoundedCornerShape(34.dp)
    val fill = if (dark) RepoBottomNavSurface else RepoBottomNavSurfaceLight
    val border = if (dark) RepoBorderNormal else RepoBorderNormalLight

    Box(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        Row(
            modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(72.dp)
                .shadow(18.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
                .clip(shape)
                .background(fill)
                .border(1.dp, border, shape)
                .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavTab.entries.forEach { tab ->
            val isSelected = tab == selected
            // Unequal, animated weights — the selected pill grows to fit its label and
            // the inactive tabs shrink to icon-only width, instead of three fixed equal
            // thirds with dead space around a short icon.
            val weight by animateFloatAsState(
                targetValue = if (isSelected) 1.6f else 1f,
                label = "navItemWeight"
            )
            BottomNavItem(
                tab = tab,
                isSelected = isSelected,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(weight)
            )
        }
    }
}
}

@Composable
private fun BottomNavItem(
    tab: BottomNavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = MaterialTheme.isDarkSurface
    val shape = RoundedCornerShape(50)
    val pillAlpha by animateFloatAsState(if (isSelected) 1f else 0f, label = "navPillAlpha")
    val contentColor = when {
        isSelected -> Color.White
        dark -> RepoIconInactive
        else -> RepoTextMutedLight
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "navItemScale")

    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (isSelected) 6.dp else 0.dp, shape, clip = false)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource
            )
            .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        RepoPillGradientLight.copy(alpha = pillAlpha),
                        RepoPurpleGradientStart.copy(alpha = pillAlpha),
                        RepoPurple.copy(alpha = pillAlpha)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        Icon(bottomNavIcon(tab), contentDescription = tab.label, tint = contentColor, modifier = Modifier.size(20.dp))
        if (isSelected) {
            Text(
                tab.label,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

private fun bottomNavIcon(tab: BottomNavTab) = when (tab) {
    BottomNavTab.OVERVIEW -> Icons.Filled.Home
    BottomNavTab.REPOSITORIES -> Icons.Filled.Folder
    BottomNavTab.PROFILE -> Icons.Filled.Person
}
