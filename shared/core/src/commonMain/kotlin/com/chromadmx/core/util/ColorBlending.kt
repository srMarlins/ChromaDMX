package com.chromadmx.core.util

import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Color

/**
 * Pure-function color blending following standard compositing math.
 */
object ColorBlending {

    /**
     * Blend [overlay] onto [base] using the given [mode] and [opacity] (0-1).
     *
     * The result is always clamped to 0..1.
     */
    fun blend(
        base: Color,
        overlay: Color,
        mode: BlendMode = BlendMode.NORMAL,
        opacity: Float = 1f
    ): Color {
        val blended = when (mode) {
            BlendMode.NORMAL   -> overlay
            BlendMode.ADDITIVE -> additive(base, overlay)
            BlendMode.MULTIPLY -> multiply(base, overlay)
            BlendMode.OVERLAY  -> overlay(base, overlay)
        }

        // Mix between base and blended result by opacity
        val op = opacity.coerceIn(0f, 1f)
        return Color(
            base.r + (blended.r - base.r) * op,
            base.g + (blended.g - base.g) * op,
            base.b + (blended.b - base.b) * op
        ).clamped()
    }

    /* -- individual blend math ---------------------------------------- */

    private fun additive(base: Color, overlay: Color): Color =
        Color(base.r + overlay.r, base.g + overlay.g, base.b + overlay.b)

    private fun multiply(base: Color, overlay: Color): Color =
        Color(base.r * overlay.r, base.g * overlay.g, base.b * overlay.b)

    private fun overlay(base: Color, overlay: Color): Color = Color(
        overlayChannel(base.r, overlay.r),
        overlayChannel(base.g, overlay.g),
        overlayChannel(base.b, overlay.b)
    )

    /**
     * Per-channel overlay formula:
     *   if base < 0.5 -> 2 * base * overlay
     *   else           -> 1 - 2 * (1 - base) * (1 - overlay)
     */
    private fun overlayChannel(base: Float, overlay: Float): Float =
        if (base < 0.5f) 2f * base * overlay
        else 1f - 2f * (1f - base) * (1f - overlay)
}
