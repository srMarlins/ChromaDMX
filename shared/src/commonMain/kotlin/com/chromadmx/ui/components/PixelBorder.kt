package com.chromadmx.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.ChromaAnimations
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelShape

// --------------------------------------------------------------------------
// Shared drawing helpers
// --------------------------------------------------------------------------

/**
 * Build a chamfered-corner (octagonal) [Path] that matches [PixelShape]
 * but inset by [inset] pixels on every side.
 */
private fun chamferPath(size: Size, chamferPx: Float, inset: Float = 0f): Path {
    val w = size.width
    val h = size.height
    // Adjust chamfer for the inset — the diagonal cut shrinks proportionally
    val c = (chamferPx - inset).coerceAtLeast(0f)
    val i = inset

    return Path().apply {
        moveTo(c + i, i)
        lineTo(w - c - i, i)
        lineTo(w - i, c + i)
        lineTo(w - i, h - c - i)
        lineTo(w - c - i, h - i)
        lineTo(c + i, h - i)
        lineTo(i, h - c - i)
        lineTo(i, c + i)
        close()
    }
}

/** The pixel-perfect stroke style used for all border layers. */
private val pixelStroke: (Float) -> Stroke = { width ->
    Stroke(
        width = width,
        cap = StrokeCap.Square,
        join = StrokeJoin.Miter
    )
}

// --------------------------------------------------------------------------
// Core three-layer draw routine
// --------------------------------------------------------------------------

/**
 * Draws the three-layer pixel border:
 * 1. **Outer glow** — wide semi-transparent stroke
 * 2. **Main border** — visible border at 60 % opacity
 * 3. **Inner shadow** — subtle depth at 15 % black
 */
private fun DrawScope.drawPixelBorderLayers(
    borderColor: Color,
    glowColor: Color,
    glowAlpha: Float,
    chamferPx: Float,
    borderWidth: Float,
    glowWidth: Float,
) {
    // Layer 1 — outer glow (wide, semi-transparent)
    val outerPath = chamferPath(size, chamferPx, inset = 0f)
    drawPath(
        path = outerPath,
        color = glowColor.copy(alpha = glowAlpha),
        style = pixelStroke(glowWidth)
    )

    // Layer 2 — main border (60 % opacity)
    val mainInset = (glowWidth - borderWidth) / 2f
    val mainPath = chamferPath(size, chamferPx, inset = mainInset.coerceAtLeast(0f))
    drawPath(
        path = mainPath,
        color = borderColor.copy(alpha = 0.6f),
        style = pixelStroke(borderWidth)
    )

    // Layer 3 — inner shadow (subtle depth)
    val innerInset = mainInset + borderWidth
    val innerPath = chamferPath(size, chamferPx, inset = innerInset.coerceAtLeast(0f))
    drawPath(
        path = innerPath,
        color = Color.Black.copy(alpha = 0.15f),
        style = pixelStroke(borderWidth * 0.5f)
    )
}

// --------------------------------------------------------------------------
// Public Modifier extensions
// --------------------------------------------------------------------------

/**
 * Standard static pixel border with three layers:
 * outer glow, main border, and inner shadow.
 *
 * Uses [PixelShape]-compatible chamfered corners so clipping and border
 * outlines always match.
 *
 * @param color     Main border color. Defaults to `PixelDesign.colors.outline`.
 * @param glowColor Outer glow color. Defaults to `PixelDesign.colors.glow`.
 * @param chamfer   Chamfer (corner cut) size — should match the [PixelShape] used
 *                  on the same element.
 * @param borderWidth Width of the main border stroke.
 * @param glowWidth   Width of the outer glow stroke.
 * @param glowAlpha   Alpha of the outer glow (0..1). Defaults to 0.20.
 */
@Composable
fun Modifier.pixelBorder(
    color: Color = PixelDesign.colors.outline,
    glowColor: Color = PixelDesign.colors.glow,
    chamfer: Dp = 6.dp,
    borderWidth: Dp = 2.dp,
    glowWidth: Dp = 6.dp,
    glowAlpha: Float = 0.20f,
): Modifier {
    val density = LocalDensity.current
    val chamferPx = with(density) { chamfer.toPx() }
    val borderWidthPx = with(density) { borderWidth.toPx() }
    val glowWidthPx = with(density) { glowWidth.toPx() }

    return this.drawWithCache {
        onDrawBehind {
            drawPixelBorderLayers(
                borderColor = color,
                glowColor = glowColor,
                glowAlpha = glowAlpha,
                chamferPx = chamferPx,
                borderWidth = borderWidthPx,
                glowWidth = glowWidthPx,
            )
        }
    }
}

/**
 * Animated glowing pixel border.
 *
 * The outer glow pulses between [minAlpha] and [maxAlpha] over a 1.5 s cycle,
 * giving interactive elements a subtle "breathing" effect.
 *
 * @param color       Main border color.
 * @param glowColor   Outer glow color.
 * @param chamfer     Chamfer size (should match the element's [PixelShape]).
 * @param borderWidth Width of the main border stroke.
 * @param glowWidth   Width of the outer glow stroke.
 * @param minAlpha    Minimum glow alpha during the pulse.
 * @param maxAlpha    Maximum glow alpha during the pulse.
 * @param durationMs  Full cycle duration in milliseconds.
 */
@Composable
fun Modifier.pixelBorderGlowing(
    color: Color = PixelDesign.colors.outline,
    glowColor: Color = PixelDesign.colors.glow,
    chamfer: Dp = 6.dp,
    borderWidth: Dp = 2.dp,
    glowWidth: Dp = 8.dp,
    minAlpha: Float = 0.15f,
    maxAlpha: Float = 0.50f,
    durationMs: Int = 1500,
): Modifier {
    val density = LocalDensity.current
    val chamferPx = with(density) { chamfer.toPx() }
    val borderWidthPx = with(density) { borderWidth.toPx() }
    val glowWidthPx = with(density) { glowWidth.toPx() }

    val reduceMotion = PixelDesign.reduceMotion

    val glowAlpha = if (reduceMotion) {
        ChromaAnimations.Reduced.STATIC_GLOW_ALPHA
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "pixelGlow")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = minAlpha,
            targetValue = maxAlpha,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "glowAlpha",
        )
        animatedAlpha
    }

    return this.drawWithCache {
        onDrawBehind {
            drawPixelBorderLayers(
                borderColor = color,
                glowColor = glowColor,
                glowAlpha = glowAlpha,
                chamferPx = chamferPx,
                borderWidth = borderWidthPx,
                glowWidth = glowWidthPx,
            )
        }
    }
}

/**
 * Active / focused pixel border with a brighter, wider glow.
 *
 * Suitable for selection states, focus rings, and active indicators.
 *
 * @param color       Main border color.
 * @param glowColor   Outer glow color.
 * @param chamfer     Chamfer size.
 * @param borderWidth Width of the main border stroke.
 * @param glowWidth   Width of the wider active glow stroke.
 * @param glowAlpha   Glow alpha — higher than the default for emphasis.
 */
@Composable
fun Modifier.pixelBorderActive(
    color: Color = PixelDesign.colors.outline,
    glowColor: Color = PixelDesign.colors.glow,
    chamfer: Dp = 6.dp,
    borderWidth: Dp = 2.dp,
    glowWidth: Dp = 10.dp,
    glowAlpha: Float = 0.50f,
): Modifier {
    val density = LocalDensity.current
    val chamferPx = with(density) { chamfer.toPx() }
    val borderWidthPx = with(density) { borderWidth.toPx() }
    val glowWidthPx = with(density) { glowWidth.toPx() }

    return this.drawWithCache {
        onDrawBehind {
            drawPixelBorderLayers(
                borderColor = color,
                glowColor = glowColor,
                glowAlpha = glowAlpha,
                chamferPx = chamferPx,
                borderWidth = borderWidthPx,
                glowWidth = glowWidthPx,
            )
        }
    }
}

// --------------------------------------------------------------------------
// Legacy compatibility
// --------------------------------------------------------------------------

/**
 * Legacy pixel border — retained for backward compatibility with existing call sites.
 *
 * This overload preserves the old `(width, color, pixelSize)` signature so that
 * callers that have not yet been migrated continue to compile. It delegates to
 * the new three-layer system with sensible defaults.
 */
fun Modifier.pixelBorder(
    width: Dp = 4.dp,
    color: Color = Color.White,
    pixelSize: Dp = 4.dp,
): Modifier = composed {
    val glowColor = PixelDesign.colors.glow
    val density = LocalDensity.current
    val chamferPx = with(density) { 6.dp.toPx() }
    val borderWidthPx = with(density) { width.toPx() }
    val glowWidthPx = with(density) { (width + 4.dp).toPx() }

    this.drawWithCache {
        onDrawBehind {
            drawPixelBorderLayers(
                borderColor = color,
                glowColor = glowColor,
                glowAlpha = 0.20f,
                chamferPx = chamferPx,
                borderWidth = borderWidthPx,
                glowWidth = glowWidthPx,
            )
        }
    }
}
