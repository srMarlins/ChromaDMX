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
        // Optimization: Early exit if opacity is effectively zero
        if (opacity <= 0f) return base

        val op = opacity.coerceIn(0f, 1f)

        // Optimization: For NORMAL mode at full opacity, return overlay directly (clamped)
        if (mode == BlendMode.NORMAL && op >= 1f) {
            return overlay.clamped()
        }

        // Calculate blended components directly to avoid intermediate Color allocations
        val r: Float
        val g: Float
        val b: Float

        when (mode) {
            BlendMode.NORMAL -> {
                r = overlay.r
                g = overlay.g
                b = overlay.b
            }
            BlendMode.ADDITIVE -> {
                r = base.r + overlay.r
                g = base.g + overlay.g
                b = base.b + overlay.b
            }
            BlendMode.MULTIPLY -> {
                r = base.r * overlay.r
                g = base.g * overlay.g
                b = base.b * overlay.b
            }
            BlendMode.OVERLAY -> {
                r = overlayChannel(base.r, overlay.r)
                g = overlayChannel(base.g, overlay.g)
                b = overlayChannel(base.b, overlay.b)
            }
        }

        // Mix between base and blended result by opacity and clamp directly
        // This avoids creating an intermediate Color object and calling .clamped()
        return Color(
            (base.r + (r - base.r) * op).coerceIn(0f, 1f),
            (base.g + (g - base.g) * op).coerceIn(0f, 1f),
            (base.b + (b - base.b) * op).coerceIn(0f, 1f)
        )
    }

    /**
     * Per-channel overlay formula:
     *   if base < 0.5 -> 2 * base * overlay
     *   else           -> 1 - 2 * (1 - base) * (1 - overlay)
     */
    private fun overlayChannel(base: Float, overlay: Float): Float =
        if (base < 0.5f) 2f * base * overlay
        else 1f - 2f * (1f - base) * (1f - overlay)
}
