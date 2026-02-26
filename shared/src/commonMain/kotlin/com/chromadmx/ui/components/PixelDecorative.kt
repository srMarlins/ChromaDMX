package com.chromadmx.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.ChromaAnimations
import com.chromadmx.ui.theme.PixelDesign
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── 1. BlinkingCursor ────────────────────────────────────────────────

/**
 * A blinking block cursor character (`█`) that pulses its alpha.
 *
 * Uses [ChromaAnimations.cursorBlink] for the 500 ms blink cycle.
 * When reduced motion is active, the cursor is displayed at full opacity
 * with no animation.
 *
 * @param modifier Modifier applied to the underlying [Text].
 */
@Composable
fun BlinkingCursor(modifier: Modifier = Modifier) {
    val reduceMotion = PixelDesign.reduceMotion
    val color = PixelDesign.colors.primary

    val alpha = if (reduceMotion) {
        ChromaAnimations.Reduced.STATIC_CURSOR_ALPHA
    } else {
        val config = ChromaAnimations.cursorBlink
        val transition = rememberInfiniteTransition(label = "cursorBlink")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(config.durationMillis, easing = config.easing),
                repeatMode = config.repeatMode,
            ),
            label = "cursorAlpha",
        )
        animated
    }

    Text(
        text = "\u2588", // █ block character
        color = color.copy(alpha = alpha),
        modifier = modifier,
    )
}

// ── 2. PixelStar ─────────────────────────────────────────────────────

/**
 * A 4-pointed star shape that rotates and twinkles.
 *
 * Uses [ChromaAnimations.starRotate] (4 s cycle) for rotation and
 * [ChromaAnimations.starTwinkle] (1 s cycle) for brightness pulsing.
 *
 * Reduced motion: static rotation (0 deg) and fixed alpha (0.7).
 *
 * @param modifier Modifier applied to the [Canvas].
 * @param color    Fill color. Defaults to [PixelDesign.colors.tertiary].
 * @param size     Overall size of the star canvas. Defaults to 16.dp.
 */
@Composable
fun PixelStar(
    modifier: Modifier = Modifier,
    color: Color = PixelDesign.colors.tertiary,
    size: Dp = 16.dp,
) {
    val reduceMotion = PixelDesign.reduceMotion

    val rotation: Float
    val alpha: Float

    if (reduceMotion) {
        rotation = ChromaAnimations.Reduced.STATIC_STAR_ROTATION
        alpha = ChromaAnimations.Reduced.STATIC_STAR_ALPHA
    } else {
        val transition = rememberInfiniteTransition(label = "pixelStar")

        val rotateConfig = ChromaAnimations.starRotate
        val animatedRotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(rotateConfig.durationMillis, easing = rotateConfig.easing),
                repeatMode = rotateConfig.repeatMode,
            ),
            label = "starRotation",
        )

        val twinkleConfig = ChromaAnimations.starTwinkle
        val animatedAlpha by transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(twinkleConfig.durationMillis, easing = twinkleConfig.easing),
                repeatMode = twinkleConfig.repeatMode,
            ),
            label = "starTwinkle",
        )

        rotation = animatedRotation
        alpha = animatedAlpha
    }

    Canvas(modifier = modifier.size(size)) {
        val center = this.center
        val outerRadius = this.size.minDimension / 2f
        val innerRadius = outerRadius * 0.4f

        rotate(rotation, pivot = center) {
            val path = Path()
            for (i in 0 until 8) {
                val radius = if (i % 2 == 0) outerRadius else innerRadius
                val angle = (i * 45f - 90f) * (PI.toFloat() / 180f)
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color.copy(alpha = alpha))
        }
    }
}

// ── 3. PixelSparkles ─────────────────────────────────────────────────

/**
 * Orbiting sparkle particles that circle within a container.
 *
 * Renders 6 small circles that orbit in a circular path with sinusoidal
 * size oscillation and an outer glow halo. Uses [ChromaAnimations.sparkleOrbit]
 * for the 3 s orbit cycle.
 *
 * Reduced motion: renders nothing (empty canvas).
 *
 * @param modifier      Modifier applied to the [Canvas].
 * @param containerSize Size of the sparkle container. Defaults to 48.dp.
 */
@Composable
fun PixelSparkles(
    modifier: Modifier = Modifier,
    containerSize: Dp = 48.dp,
) {
    val reduceMotion = PixelDesign.reduceMotion

    if (reduceMotion) {
        // Empty box — no sparkles rendered
        Box(modifier = modifier.size(containerSize))
        return
    }

    val color = PixelDesign.colors.tertiary
    val config = ChromaAnimations.sparkleOrbit
    val transition = rememberInfiniteTransition(label = "pixelSparkles")

    val orbitProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(config.durationMillis, easing = config.easing),
            repeatMode = config.repeatMode,
        ),
        label = "sparkleOrbit",
    )

    val particleCount = 6

    Canvas(modifier = modifier.size(containerSize)) {
        val center = this.center
        val orbitRadius = this.size.minDimension / 2f * 0.7f

        for (i in 0 until particleCount) {
            val phaseOffset = i.toFloat() / particleCount
            val angle = (orbitProgress + phaseOffset) * 2f * PI.toFloat()

            val px = center.x + orbitRadius * cos(angle)
            val py = center.y + orbitRadius * sin(angle)

            // Size oscillates sinusoidally per particle
            val sizeOscillation = (sin((orbitProgress + phaseOffset) * 4f * PI.toFloat()) + 1f) / 2f
            val particleRadius = 1.5f + sizeOscillation * 1.5f

            // Outer glow halo
            drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = particleRadius * 3f,
                center = Offset(px, py),
            )

            // Inner particle
            drawCircle(
                color = color,
                radius = particleRadius,
                center = Offset(px, py),
            )
        }
    }
}

// ── 4. ScanlineOverlay ───────────────────────────────────────────────

/**
 * A full-screen CRT-style scanline overlay.
 *
 * Draws horizontal lines every 4 px with a slow vertical drift animation
 * ([ChromaAnimations.scanlineDrift], 8 s cycle). Adjusts line color based
 * on [PixelDesign.isDarkTheme]:
 * - Dark theme: primary color at 3 % alpha
 * - Light theme: dark lines at 2 % alpha
 *
 * Reduced motion: static scanlines with no drift.
 *
 * Apply this composable as an overlay on top of your content:
 * ```kotlin
 * Box {
 *     MainContent()
 *     ScanlineOverlay()
 * }
 * ```
 */
@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier) {
    val reduceMotion = PixelDesign.reduceMotion
    val isDark = PixelDesign.isDarkTheme
    val primaryColor = PixelDesign.colors.primary

    val lineColor = if (isDark) {
        primaryColor.copy(alpha = 0.03f)
    } else {
        Color.Black.copy(alpha = 0.02f)
    }

    val offset: Float = if (reduceMotion) {
        0f
    } else {
        val config = ChromaAnimations.scanlineDrift
        val transition = rememberInfiniteTransition(label = "scanlineDrift")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(config.durationMillis, easing = config.easing),
                repeatMode = config.repeatMode,
            ),
            label = "scanlineOffset",
        )
        animated
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val lineSpacing = 4.dp.toPx()
        // Animate offset across one full line-spacing period
        val yOffset = offset * lineSpacing
        var y = yOffset % lineSpacing
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += lineSpacing
        }
    }
}

// ── 5. PixelEnchantedDivider ─────────────────────────────────────────

/**
 * A horizontal divider with a shimmer gradient sweep.
 *
 * Draws a 2 dp line with a gradient that sweeps from primary -> secondary ->
 * tertiary -> primary, animated left-to-right over a 2 s cycle
 * ([ChromaAnimations.shimmerSweep]).
 *
 * Reduced motion: static gradient (no animation).
 *
 * @param modifier Modifier applied to the [Canvas].
 */
@Composable
fun PixelEnchantedDivider(modifier: Modifier = Modifier) {
    val reduceMotion = PixelDesign.reduceMotion
    val primary = PixelDesign.colors.primary
    val secondary = PixelDesign.colors.secondary
    val tertiary = PixelDesign.colors.tertiary

    val shimmerProgress: Float = if (reduceMotion) {
        0f
    } else {
        val config = ChromaAnimations.shimmerSweep
        val transition = rememberInfiniteTransition(label = "enchantedDivider")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(config.durationMillis, easing = config.easing),
                repeatMode = config.repeatMode,
            ),
            label = "shimmerSweep",
        )
        animated
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        val gradientColors = listOf(primary, secondary, tertiary, primary)

        // Sweep the gradient start/end across the width
        val sweepWidth = size.width * 1.5f
        val startX = -size.width * 0.5f + shimmerProgress * sweepWidth
        val endX = startX + size.width

        val brush = Brush.linearGradient(
            colors = gradientColors,
            start = Offset(startX, 0f),
            end = Offset(endX, 0f),
        )

        drawRect(
            brush = brush,
            size = size,
        )
    }
}

// ── 6. PixelLoadingSpinner ───────────────────────────────────────────

/**
 * Size options for [PixelLoadingSpinner].
 */
enum class SpinnerSize(val dp: Dp) {
    Small(24.dp),
    Medium(40.dp),
    Large(64.dp),
}

/**
 * A rotating sweep-gradient arc with orbiting sparkle dots.
 *
 * The arc rotates continuously while 4 small sparkle dots orbit around
 * the perimeter. Uses [ChromaAnimations.sparkleOrbit] for dot orbits.
 *
 * Reduced motion: static arc at a fixed position with no dot animation.
 *
 * @param modifier    Modifier applied to the [Canvas].
 * @param spinnerSize Size of the spinner. Defaults to [SpinnerSize.Medium].
 * @param color       Arc and sparkle color. Defaults to [PixelDesign.colors.primary].
 */
@Composable
fun PixelLoadingSpinner(
    modifier: Modifier = Modifier,
    spinnerSize: SpinnerSize = SpinnerSize.Medium,
    color: Color = PixelDesign.colors.primary,
) {
    val reduceMotion = PixelDesign.reduceMotion
    val sizeDp = spinnerSize.dp

    val arcRotation: Float
    val sparkleProgress: Float

    if (reduceMotion) {
        arcRotation = 0f
        sparkleProgress = 0f
    } else {
        val transition = rememberInfiniteTransition(label = "loadingSpinner")

        // Arc rotation — reuse starRotate (4s, linear, restart) for smooth 360 spin
        val rotateConfig = ChromaAnimations.starRotate
        val animatedRotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(rotateConfig.durationMillis, easing = rotateConfig.easing),
                repeatMode = rotateConfig.repeatMode,
            ),
            label = "spinnerRotation",
        )

        // Sparkle orbits
        val orbitConfig = ChromaAnimations.sparkleOrbit
        val animatedOrbit by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(orbitConfig.durationMillis, easing = orbitConfig.easing),
                repeatMode = orbitConfig.repeatMode,
            ),
            label = "sparkleOrbitProgress",
        )

        arcRotation = animatedRotation
        sparkleProgress = animatedOrbit
    }

    Canvas(modifier = modifier.size(sizeDp)) {
        val center = this.center
        val radius = this.size.minDimension / 2f
        val strokeWidth = radius * 0.15f
        val arcRadius = radius - strokeWidth

        // Draw sweep arc
        rotate(arcRotation, pivot = center) {
            drawArc(
                color = color.copy(alpha = 0.3f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
            )

            // Bright sweep arc (270 degree sweep)
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
            )
        }

        // Draw 4 orbiting sparkle dots
        if (!reduceMotion) {
            val sparkleCount = 4
            val sparkleRadius = strokeWidth * 0.4f
            val orbitR = radius * 0.85f

            for (i in 0 until sparkleCount) {
                val phaseOffset = i.toFloat() / sparkleCount
                val angle = (sparkleProgress + phaseOffset) * 2f * PI.toFloat()

                val sx = center.x + orbitR * cos(angle)
                val sy = center.y + orbitR * sin(angle)

                // Glow halo
                drawCircle(
                    color = color.copy(alpha = 0.25f),
                    radius = sparkleRadius * 2.5f,
                    center = Offset(sx, sy),
                )

                // Sparkle dot
                drawCircle(
                    color = color,
                    radius = sparkleRadius,
                    center = Offset(sx, sy),
                )
            }
        }
    }
}
