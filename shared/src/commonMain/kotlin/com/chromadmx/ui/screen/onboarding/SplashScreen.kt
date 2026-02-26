package com.chromadmx.ui.screen.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.NeonMagenta
import com.chromadmx.ui.theme.PixelFontFamily

/**
 * Animated splash screen shown as the first onboarding step.
 *
 * The logo "assembles" from offset/transparent to centered/opaque,
 * followed by the tagline fading in. Auto-advances via the ViewModel
 * after 2.5 seconds.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    // Logo assembly animation
    val logoAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "logo-alpha",
    )
    val logoOffsetY by animateFloatAsState(
        targetValue = if (visible) 0f else -40f,
        animationSpec = tween(durationMillis = 800),
        label = "logo-offset-y",
    )

    // Tagline fades in slightly after logo
    val taglineAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 500),
        label = "tagline-alpha",
    )

    // Subtitle "DMX" word slides in from right
    val dmxAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 300),
        label = "dmx-alpha",
    )
    val dmxOffsetX by animateFloatAsState(
        targetValue = if (visible) 0f else 60f,
        animationSpec = tween(durationMillis = 500, delayMillis = 300),
        label = "dmx-offset-x",
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // "Chroma" text - drops in from above
            Text(
                text = "Chroma",
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = PixelFontFamily),
                color = NeonCyan,
                modifier = Modifier
                    .alpha(logoAlpha)
                    .offset { IntOffset(0, logoOffsetY.toInt()) },
            )

            // "DMX" text - slides in from right
            Text(
                text = "DMX",
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = PixelFontFamily),
                color = NeonMagenta,
                modifier = Modifier
                    .alpha(dmxAlpha)
                    .offset { IntOffset(dmxOffsetX.toInt(), 0) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tagline fades in last
            Text(
                text = "Light your stage",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(taglineAlpha),
            )
        }
    }
}
