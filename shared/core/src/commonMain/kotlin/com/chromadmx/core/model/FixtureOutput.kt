package com.chromadmx.core.model

/**
 * Multi-channel output for a single fixture.
 *
 * Extends beyond simple RGB color to include pan/tilt movement,
 * gobo wheel selection, focus, zoom, and strobe rate.
 *
 * Null values mean "no opinion" — when blending, a null field defers
 * to the other layer's value (or the default).
 *
 * @property color       RGB color output.
 * @property pan         Pan position, 0.0-1.0 normalized. Null = no opinion.
 * @property tilt        Tilt position, 0.0-1.0 normalized. Null = no opinion.
 * @property gobo        Gobo wheel slot index. Null = no opinion.
 * @property focus       Focus position, 0.0-1.0. Null = no opinion.
 * @property zoom        Zoom position, 0.0-1.0. Null = no opinion.
 * @property strobeRate  Strobe rate, 0.0 (off) to 1.0 (max). Null = no opinion.
 */
data class FixtureOutput(
    val color: Color = Color.BLACK,
    val pan: Float? = null,
    val tilt: Float? = null,
    val gobo: Int? = null,
    val focus: Float? = null,
    val zoom: Float? = null,
    val strobeRate: Float? = null
) {
    /**
     * Blend this output with [other] using the given [mode] and [opacity].
     *
     * For color: uses the standard blend mode math.
     * For nullable float channels (pan, tilt, focus, zoom, strobeRate):
     *   - NORMAL / MULTIPLY / OVERLAY: overlay replaces base if non-null.
     *   - ADDITIVE: overlay value is added as an offset to the base (clamped to 0-1).
     * For gobo: overlay replaces base if non-null (no additive blending for integer slots).
     *
     * Null values in the overlay are ignored (base value is kept).
     */
    fun blendWith(other: FixtureOutput, mode: BlendMode, opacity: Float): FixtureOutput {
        val op = opacity.coerceIn(0f, 1f)

        // Blend color using existing blend math
        val blendedColor = com.chromadmx.core.util.ColorBlending.blend(
            base = this.color,
            overlay = other.color,
            mode = mode,
            opacity = op
        )

        return FixtureOutput(
            color = blendedColor,
            pan = blendFloat(this.pan, other.pan, mode, op),
            tilt = blendFloat(this.tilt, other.tilt, mode, op),
            gobo = if (other.gobo != null && op > 0f) other.gobo else this.gobo,
            focus = blendFloat(this.focus, other.focus, mode, op),
            zoom = blendFloat(this.zoom, other.zoom, mode, op),
            strobeRate = blendFloat(this.strobeRate, other.strobeRate, mode, op)
        )
    }

    /**
     * Blend only the movement channels (pan, tilt, gobo, focus, zoom, strobeRate)
     * from [other] onto this output, preserving this output's color.
     *
     * Used by the engine when compositing movement layers onto a color result,
     * since movement effects should not affect color.
     */
    fun blendMovementOnly(other: FixtureOutput, mode: BlendMode, opacity: Float): FixtureOutput {
        val op = opacity.coerceIn(0f, 1f)

        return copy(
            pan = blendFloat(this.pan, other.pan, mode, op),
            tilt = blendFloat(this.tilt, other.tilt, mode, op),
            gobo = if (other.gobo != null && op > 0f) other.gobo else this.gobo,
            focus = blendFloat(this.focus, other.focus, mode, op),
            zoom = blendFloat(this.zoom, other.zoom, mode, op),
            strobeRate = blendFloat(this.strobeRate, other.strobeRate, mode, op)
        )
    }

    companion object {
        /** Default output: black, no movement, no gobo. */
        val DEFAULT = FixtureOutput()

        /**
         * Blend a nullable float channel.
         *
         * If the overlay is null, the base is returned unchanged.
         * For ADDITIVE mode, the overlay is added as an offset.
         * For other modes, the overlay replaces the base, lerped by opacity.
         */
        internal fun blendFloat(
            base: Float?,
            overlay: Float?,
            mode: BlendMode,
            opacity: Float
        ): Float? {
            if (overlay == null) return base
            val b = base ?: 0f

            return when (mode) {
                BlendMode.ADDITIVE -> {
                    // Add overlay offset scaled by opacity
                    (b + overlay * opacity).coerceIn(0f, 1f)
                }
                else -> {
                    // NORMAL, MULTIPLY, OVERLAY: lerp from base to overlay
                    (b + (overlay - b) * opacity).coerceIn(0f, 1f)
                }
            }
        }
    }
}
