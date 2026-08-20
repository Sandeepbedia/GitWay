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

package com.io.git.way.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ---------------------------------------------------------------------------
 * Theme helpers
 * ---------------------------------------------------------------------------
 */

private val MaterialTheme.isDarkSurface: Boolean
    @Composable
    get() = colorScheme.background.luminance() < 0.5f

/**
 * ---------------------------------------------------------------------------
 * Liquid Glass Background
 * ---------------------------------------------------------------------------
 */

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = MaterialTheme.isDarkSurface

    val backdrop = if (dark) {
        Brush.verticalGradient(
            listOf(
                RepoBgGradientTop,
                RepoBgGradientMid,
                RepoBgGradientBottom
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                RepoBgGradientTopLight,
                RepoBgGradientBottomLight
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backdrop)
    ) {
        content()
    }
}

/**
 * ---------------------------------------------------------------------------
 * Glass Scaffold
 * ---------------------------------------------------------------------------
 */

@Composable
fun GlassScaffold(
    title: String,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val dark = MaterialTheme.isDarkSurface

    val titleColor =
        if (dark) Color.White
        else MaterialTheme.colorScheme.onBackground

    val bodyColor =
        if (dark) RepoTextPrimary
        else RepoTextPrimaryLight

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.navigationBars
                .only(WindowInsetsSides.Bottom),

            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = title,
                                fontWeight = FontWeight.SemiBold,
                                color = titleColor
                            )

                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = titleColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },

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
                CompositionLocalProvider(
                    LocalContentColor provides bodyColor
                ) {
                    content(padding)
                }
            }
        )
    }
}

/**
 * ---------------------------------------------------------------------------
 * Glass Treatment
 * ---------------------------------------------------------------------------
 */

@Composable
private fun glassTreatment(
    selected: Boolean = false
): GlassTreatment {
    val dark = MaterialTheme.isDarkSurface

    val border = when {
        selected && dark -> RepoBorderSelected
        selected && !dark -> RepoBorderSelectedLight
        dark -> RepoBorderNormal
        else -> RepoBorderNormalLight
    }

    return GlassTreatment(
        fillTop = if (dark) RepoCardSurface else RepoCardSurfaceLight,
        fillBottom = if (dark) RepoCardSurface else RepoCardSurfaceLight,
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

/**
 * ---------------------------------------------------------------------------
 * Glass Card
 * ---------------------------------------------------------------------------
 */

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    padding: Dp = 16.dp,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val treatment = glassTreatment(selected)

    Column(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.035f),
                spotColor = Color.Black.copy(alpha = 0.035f)
            )
            .clip(shape)
            .background(treatment.fillTop)
            .border(
                width = 1.dp,
                color = treatment.borderTop.copy(
                    alpha = if (selected) 0.90f else 0.55f
                ),
                shape = shape
            )
            .padding(padding)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides treatment.content
        ) {
            content()
        }
    }
}

/**
 * ---------------------------------------------------------------------------
 * Clickable Glass Card
 * ---------------------------------------------------------------------------
 */

@Composable
fun GlassClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    padding: Dp = 16.dp,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val treatment = glassTreatment(selected)

    Column(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.035f),
                spotColor = Color.Black.copy(alpha = 0.035f)
            )
            .clip(shape)
            .background(treatment.fillTop)
            .border(
                width = 1.dp,
                color = treatment.borderTop.copy(
                    alpha = if (selected) 0.90f else 0.55f
                ),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(padding)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides treatment.content
        ) {
            content()
        }
    }
}

/**
 * ---------------------------------------------------------------------------
 * Primary Button
 * ---------------------------------------------------------------------------
 */

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
        Brush.horizontalGradient(
            listOf(
                RepoPurpleGradientStart,
                RepoPurple,
                RepoPurpleGradientEnd
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                scheme.onSurface.copy(alpha = 0.10f),
                scheme.onSurface.copy(alpha = 0.10f)
            )
        )
    }

    val textColor =
        if (enabled) Color.White
        else scheme.onSurface.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(brush)
            .border(
                width = 1.dp,
                color = Color.White.copy(
                    alpha = if (enabled) 0.35f else 0.08f
                ),
                shape = shape
            )
            .then(
                if (enabled && !loading) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                leadingIcon?.invoke()

                Text(
                    text = text,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * ---------------------------------------------------------------------------
 * Secondary Button
 * ---------------------------------------------------------------------------
 */

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

    val fill =
        if (dark) RepoElevatedSurface
        else RepoElevatedSurfaceLight

    val textColor =
        if (enabled) RepoPurple
        else if (dark) RepoTextDisabled
        else RepoTextMutedLight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = 7.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            )
            .clip(shape)
            .background(fill)
            .border(
                width = 1.dp,
                color = RepoPurple.copy(
                    alpha = if (enabled) 0.55f else 0.15f
                ),
                shape = shape
            )
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leadingIcon?.invoke()

            Text(
                text = text,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * ---------------------------------------------------------------------------
 * Glass Chip
 * ---------------------------------------------------------------------------
 */

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
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(
                horizontal = 14.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leadingIcon?.invoke()

        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * ---------------------------------------------------------------------------
 * Glass Icon Button
 * ---------------------------------------------------------------------------
 */

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable () -> Unit
) {
    val dark = MaterialTheme.isDarkSurface

    val fill =
        if (dark) RepoElevatedSurface
        else RepoElevatedSurfaceLight

    val border =
        if (dark) RepoBorderNormal
        else RepoBorderNormalLight

    val contentColor =
        if (dark) RepoIconDefault
        else RepoTextPrimaryLight

    val interactionSource =
        remember { MutableInteractionSource() }

    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        label = "iconButtonScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.18f)
            )
            .clip(CircleShape)
            .background(fill)
            .border(
                width = 1.dp,
                color = border,
                shape = CircleShape
            )
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            content()
        }
    }
}

/**
 * ---------------------------------------------------------------------------
 * Glass FAB
 * ---------------------------------------------------------------------------
 */

@Composable
fun GlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    content: @Composable () -> Unit
) {
    val interactionSource =
        remember { MutableInteractionSource() }

    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        label = "fabScale"
    )

    val brush = Brush.radialGradient(
        listOf(
            RepoPurpleLight,
            RepoPurple,
            RepoPurpleDark
        )
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = RepoPurple.copy(alpha = 0.4f),
                spotColor = RepoPurple.copy(alpha = 0.4f)
            )
            .clip(CircleShape)
            .background(brush)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White
        ) {
            content()
        }
    }
}

/**
 * ---------------------------------------------------------------------------
 * Search Field
 * ---------------------------------------------------------------------------
 */

@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search"
) {
    val dark = MaterialTheme.isDarkSurface
    val shape = RoundedCornerShape(30.dp)

    var focused by remember {
        mutableStateOf(false)
    }

    val restingBorder =
        if (dark) RepoBorderNormal
        else RepoBorderNormalLight

    val borderColor by animateColorAsState(
        targetValue =
            if (focused) {
                RepoPurple.copy(alpha = 0.85f)
            } else {
                restingBorder
            },
        label = "searchBorder"
    )

    val fill =
        if (dark) RepoSearchBarSurface
        else RepoSearchBarSurfaceLight

    val contentColor =
        if (dark) RepoTextPrimary
        else RepoTextPrimaryLight

    val hintColor =
        if (dark) RepoTextHint
        else RepoTextMutedLight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = 4.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            )
            .clip(shape)
            .background(fill)
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = shape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = RepoIconAccent
            )

            Box(
                modifier = Modifier.weight(1f)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = hintColor
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = contentColor
                    ),
                    cursorBrush = SolidColor(RepoPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            focused = it.isFocused
                        }
                )
            }

            if (value.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear",
                    tint = hintColor,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable {
                            onValueChange("")
                        }
                )
            }
        }
    }
}

/**
 * ---------------------------------------------------------------------------
 * Skeleton Card
 * ---------------------------------------------------------------------------
 */

@Composable
fun GlassSkeletonCard(
    modifier: Modifier = Modifier
) {
    val dark = MaterialTheme.isDarkSurface

    val transition =
        rememberInfiniteTransition(label = "shimmer")

    val shimmer by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    val scheme = MaterialTheme.colorScheme

    val base =
        if (dark) {
            scheme.surfaceVariant.copy(alpha = 0.58f)
        } else {
            scheme.surfaceVariant.copy(alpha = 0.9f)
        }

    val shape = RoundedCornerShape(28.dp)

    Row(
        modifier = modifier
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
                .background(
                    scheme.onSurface.copy(
                        alpha = shimmer * 0.10f
                    )
                )
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(120.dp, 14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        scheme.onSurface.copy(
                            alpha = shimmer * 0.10f
                        )
                    )
            )

            Box(
                Modifier
                    .size(80.dp, 10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        scheme.onSurface.copy(
                            alpha = shimmer * 0.07f
                        )
                    )
            )
        }
    }
}

/**
 * ---------------------------------------------------------------------------
 * Bottom Navigation
 * ---------------------------------------------------------------------------
 */

enum class BottomNavTab(
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector
) {
    REPOSITORIES(
        label = "Repositories",
        icon = Icons.Outlined.Folder,
        iconSelected = Icons.Filled.Folder
    ),

    PROFILE(
        label = "Profile",
        icon = Icons.Outlined.Person,
        iconSelected = Icons.Filled.Person
    )
}

/**
 * ---------------------------------------------------------------------------
 * Bottom Navigation Metrics
 *
 * Normal icons intentionally stay close together.
 * Selected item expands only enough for icon + label.
 * ---------------------------------------------------------------------------
 */

private val NAV_ICON_SIZE = 21.dp
private val NAV_ITEM_HEIGHT = 48.dp

private val NAV_SELECTED_MIN_WIDTH = 112.dp
private val NAV_SELECTED_MAX_WIDTH = 148.dp

private val NAV_ICON_CELL_WIDTH = 42.dp

private val NAV_GAP = 3.dp

private val NAV_LABEL_STYLE = TextStyle(
    fontSize = 10.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.sp
)

/**
 * ---------------------------------------------------------------------------
 * Floating Bottom Nav
 * ---------------------------------------------------------------------------
 */

@Composable
fun GlassFloatingBottomNav(
    selected: BottomNavTab,
    onSelect: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = MaterialTheme.isDarkSurface

    val shape = RoundedCornerShape(24.dp)

    val fill =
        if (dark) RepoBottomNavSurface
        else RepoBottomNavSurfaceLight

    val border =
        if (dark) RepoBorderNormal
        else RepoBorderNormalLight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.18f)
                )
                .clip(shape)
                .background(fill)
                .border(
                    width = 1.dp,
                    color = border,
                    shape = shape
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                )
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val tabs = BottomNavTab.entries

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NAV_ITEM_HEIGHT),

                    /**
                     * This is intentional:
                     * icons stay close instead of being pushed
                     * to the extreme edges.
                     */
                    horizontalArrangement = Arrangement.Center,

                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, item ->

                        if (index > 0) {
                            androidx.compose.foundation.layout.Spacer(
                                modifier = Modifier.width(NAV_GAP)
                            )
                        }

                        NavPill(
                            item = item,
                            isSelected = item == selected,
                            // Every tab shows its label, so each pill is sized
                            // to its own content — icon + label.
                            itemWidth = measuredNavPillWidth(item.label),
                            onClick = {
                                onSelect(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * ---------------------------------------------------------------------------
 * Measure Selected Pill
 * ---------------------------------------------------------------------------
 *
 * Width is content based, so "Repositories" and "Profile"
 * don't get an unnecessarily huge selected section.
 */

@Composable
private fun measuredNavPillWidth(
    label: String
): Dp {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val labelWidthPx = remember(label) {
        textMeasurer.measure(
            text = AnnotatedString(label),
            style = NAV_LABEL_STYLE
        ).size.width
    }

    val labelWidth =
        with(density) {
            labelWidthPx.toDp()
        }

    val naturalWidth =
        12.dp +
            NAV_ICON_SIZE +
            7.dp +
            labelWidth +
            12.dp

    return naturalWidth.coerceIn(
        NAV_SELECTED_MIN_WIDTH,
        NAV_SELECTED_MAX_WIDTH
    )
}

/**
 * ---------------------------------------------------------------------------
 * Individual Nav Pill
 * ---------------------------------------------------------------------------
 *
 * IMPORTANT:
 *
 * - Every tab = icon + label.
 * - Selected tab gets the accent border/background + accent text/icon.
 * - Unselected tabs stay muted.
 * - Border/background animate with the width.
 * - Icon remains close to label.
 * - Press scale gives tactile feedback.
 * - Selected icon gets a subtle spring pop.
 */

@Composable
private fun RowScope.NavPill(
    item: BottomNavTab,
    isSelected: Boolean,
    itemWidth: Dp,
    onClick: () -> Unit
) {
    val dark = MaterialTheme.isDarkSurface

    val accent = RepoPurple

    val inactive =
        if (dark) {
            RepoIconInactive
        } else {
            RepoTextMutedLight
        }

    val targetWidth = itemWidth

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 520f
        ),
        label = "navWidth"
    )

    val interactionSource =
        remember { MutableInteractionSource() }

    val haptics =
        LocalHapticFeedback.current

    val isPressed by interactionSource
        .collectIsPressedAsState()

    /**
     * Press flash.
     *
     * Fast appearing highlight + slower fade.
     */
    val pressLevel by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isPressed) 70 else 350,
            easing = FastOutSlowInEasing
        ),
        label = "navPressLevel"
    )

    /**
     * Selected / inactive icon color — smooth morph on tap.
     */
    val iconTint by animateColorAsState(
        targetValue =
            if (isSelected) {
                accent
            } else {
                lerp(
                    inactive,
                    accent,
                    pressLevel * 0.85f
                )
            },
        animationSpec = tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing
        ),
        label = "navIconTint"
    )

    /**
     * Label color — accent when selected, muted otherwise, smooth morph.
     */
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) accent else inactive,
        animationSpec = tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing
        ),
        label = "navLabelColor"
    )

    /**
     * Selected icon bounce.
     */
    val iconScale by animateFloatAsState(
        targetValue =
            when {
                isSelected -> 1.12f
                isPressed -> 1.08f
                else -> 1f
            },
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 550f
        ),
        label = "navIconScale"
    )

    /**
     * Label pop — text scales with the tap so the whole pill reacts.
     */
    val labelScale by animateFloatAsState(
        targetValue =
            when {
                isSelected -> 1.06f
                isPressed -> 0.94f
                else -> 1f
            },
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 600f
        ),
        label = "navLabelScale"
    )

    /**
     * Pressed item slightly sinks.
     */
    val pillScale by animateFloatAsState(
        targetValue =
            if (isPressed) 0.94f
            else 1f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 700f
        ),
        label = "navPillScale"
    )

    /**
     * Selected border alpha.
     *
     * This makes the border itself feel like it is
     * appearing while the section expands.
     */
    val selectedBorderAlpha by animateFloatAsState(
        targetValue =
            if (isSelected) 0.75f
            else pressLevel * 0.55f,
        animationSpec = tween(
            durationMillis = 220
        ),
        label = "selectedBorderAlpha"
    )

    /**
     * Selected background alpha.
     *
     * No permanent heavy background on inactive items.
     */
    val selectedBackgroundAlpha by animateFloatAsState(
        targetValue =
            if (isSelected) 0.12f
            else pressLevel * 0.20f,
        animationSpec = tween(
            durationMillis = 220
        ),
        label = "selectedBackgroundAlpha"
    )

    /**
     * Border and background use exactly the same animated
     * container dimensions, so the section visually extends
     * together with the width.
     */
    val pillShape = RoundedCornerShape(17.dp)

    Column(
        modifier = Modifier
            .height(NAV_ITEM_HEIGHT)
            .width(animatedWidth)
            .graphicsLayer {
                scaleX = pillScale
                scaleY = pillScale
            }
            .clip(pillShape)
            .background(
                accent.copy(
                    alpha = selectedBackgroundAlpha
                )
            )
            .border(
                width = 1.dp,
                color = accent.copy(
                    alpha = selectedBorderAlpha
                ),
                shape = pillShape
            )
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = {
                    haptics.performHapticFeedback(
                        HapticFeedbackType.TextHandleMove
                    )

                    onClick()
                }
            ),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),

            /**
             * Keep icon + text compact.
             */
            horizontalArrangement = Arrangement.Center,

            verticalAlignment = Alignment.CenterVertically
        ) {
            /**
             * Icon container.
             *
             * This stays the same size in both states,
             * preventing the icon from jumping when label
             * appears.
             */
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                /**
                 * Soft selected glow.
                 */
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer {
                            alpha =
                                if (isSelected) {
                                    0.38f
                                } else {
                                    pressLevel * 0.55f
                                }
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                /**
                 * Actual icon.
                 */
                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                ) {
                    Icon(
                        imageVector =
                            if (isSelected) {
                                item.iconSelected
                            } else {
                                item.icon
                            },

                        contentDescription = item.label,

                        tint = iconTint,

                        modifier = Modifier.size(
                            NAV_ICON_SIZE
                        )
                    )
                }
            }

            /**
             * Label — always visible, muted when unselected, accent when
             * selected, with a springy scale pop on tap.
             */
            Text(
                text = item.label,

                textAlign = TextAlign.Center,

                color = labelColor,

                fontSize = NAV_LABEL_STYLE.fontSize,

                fontWeight =
                    NAV_LABEL_STYLE.fontWeight,

                letterSpacing =
                    NAV_LABEL_STYLE.letterSpacing,

                maxLines = 1,

                modifier = Modifier
                    .graphicsLayer {
                        scaleX = labelScale
                        scaleY = labelScale
                    }
                    .padding(
                        start = 2.dp
                    )
            )
        }
    }
}