package com.io.git.way.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.io.git.way.ui.theme.LiquidGlassBackground
import com.io.git.way.ui.theme.RepoPurple
import com.io.git.way.ui.theme.RepoPurpleDark
import com.io.git.way.ui.theme.RepoPurpleLight
import com.io.git.way.ui.theme.RepoTextPrimary
import com.io.git.way.ui.theme.RepoTextSecondary
import kotlinx.coroutines.delay

/** Screen 1: Git Way branding over the liquid glass background — same purple-gradient
 * mark and typography as the README banner, so the first thing anyone sees on-device
 * matches the app's identity everywhere else. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val markScale = remember { Animatable(0.6f) }
    val markAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        markScale.animateTo(1f, tween(520, easing = EaseOutBack))
    }
    LaunchedEffect(Unit) {
        markAlpha.animateTo(1f, tween(360))
    }
    LaunchedEffect(Unit) {
        delay(200)
        textAlpha.animateTo(1f, tween(420))
    }
    LaunchedEffect(Unit) {
        delay(1300)
        onFinished()
    }

    // Slow ambient pulse on the mark once it's settled — alive, not static, without
    // being distracting like the old infinite scale-bounce was.
    val ambientPulse by rememberInfinitePulse()

    LiquidGlassBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(markScale.value * ambientPulse)
                    .alpha(markAlpha.value)
                    .background(
                        Brush.linearGradient(listOf(RepoPurpleLight, RepoPurple, RepoPurpleDark)),
                        RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "</>",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha.value)
            ) {
                Text(
                    "Git Way",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = RepoTextPrimary
                )
                Text(
                    "Update GitHub from your pocket",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RepoTextSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // Small, quiet loading indicator anchored to the bottom instead of a spinner
        // fighting for attention with the mark — three dots, staggered pulse.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 56.dp)
                .alpha(textAlpha.value),
            horizontalArrangement = Arrangement.Center
        ) {
            LoadingDots()
        }
    }
}

@Composable
private fun rememberInfinitePulse(): androidx.compose.runtime.State<Float> {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "splashPulse")
    return transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )
}

@Composable
private fun LoadingDots() {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 150, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(alpha)
                    .background(RepoPurple, CircleShape)
            )
        }
    }
}
