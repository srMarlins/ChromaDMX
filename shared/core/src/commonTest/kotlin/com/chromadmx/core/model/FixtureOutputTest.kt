package com.chromadmx.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class FixtureOutputTest {

    @Test
    fun defaultFixtureOutputHasBlackColorAndNullChannels() {
        val output = FixtureOutput.DEFAULT
        assertEquals(Color.BLACK, output.color)
        assertNull(output.pan)
        assertNull(output.tilt)
        assertNull(output.gobo)
        assertNull(output.focus)
        assertNull(output.zoom)
        assertNull(output.strobeRate)
    }

    @Test
    fun constructorSetsAllFields() {
        val output = FixtureOutput(
            color = Color.RED,
            pan = 0.5f,
            tilt = 0.3f,
            gobo = 2,
            focus = 0.8f,
            zoom = 0.1f,
            strobeRate = 0.7f
        )
        assertEquals(Color.RED, output.color)
        assertEquals(0.5f, output.pan)
        assertEquals(0.3f, output.tilt)
        assertEquals(2, output.gobo)
        assertEquals(0.8f, output.focus)
        assertEquals(0.1f, output.zoom)
        assertEquals(0.7f, output.strobeRate)
    }

    /* ------------------------------------------------------------------ */
    /*  Blending — Normal mode                                             */
    /* ------------------------------------------------------------------ */

    @Test
    fun blendNormalReplacesNonNullValues() {
        val base = FixtureOutput(color = Color.RED, pan = 0.2f, tilt = 0.3f)
        val overlay = FixtureOutput(color = Color.BLUE, pan = 0.8f)

        val result = base.blendWith(overlay, BlendMode.NORMAL, 1.0f)
        assertEquals(Color.BLUE, result.color)
        assertEquals(0.8f, result.pan)
        // Tilt was null in overlay, so base value preserved
        assertEquals(0.3f, result.tilt)
    }

    @Test
    fun blendNormalWithZeroOpacityKeepsBase() {
        val base = FixtureOutput(color = Color.RED, pan = 0.2f)
        val overlay = FixtureOutput(color = Color.BLUE, pan = 0.8f)

        val result = base.blendWith(overlay, BlendMode.NORMAL, 0.0f)
        assertEquals(Color.RED, result.color)
        assertEquals(0.2f, result.pan)
    }

    @Test
    fun blendNormalWithHalfOpacityInterpolates() {
        val base = FixtureOutput(color = Color.BLACK, pan = 0.0f)
        val overlay = FixtureOutput(color = Color.WHITE, pan = 1.0f)

        val result = base.blendWith(overlay, BlendMode.NORMAL, 0.5f)
        assertEquals(0.5f, result.color.r, 0.01f)
        assertEquals(0.5f, result.pan!!, 0.01f)
    }

    /* ------------------------------------------------------------------ */
    /*  Blending — Additive mode                                           */
    /* ------------------------------------------------------------------ */

    @Test
    fun blendAdditiveAddsValues() {
        val base = FixtureOutput(color = Color.RED, pan = 0.3f)
        val overlay = FixtureOutput(color = Color.GREEN, pan = 0.2f)

        val result = base.blendWith(overlay, BlendMode.ADDITIVE, 1.0f)
        // Color: RED + GREEN additive = YELLOW (1,1,0)
        assertEquals(1.0f, result.color.r, 0.01f)
        assertEquals(1.0f, result.color.g, 0.01f)
        // Pan: 0.3 + 0.2 = 0.5
        assertEquals(0.5f, result.pan!!, 0.01f)
    }

    @Test
    fun blendAdditiveClampsPanToOne() {
        val base = FixtureOutput(pan = 0.8f)
        val overlay = FixtureOutput(pan = 0.5f)

        val result = base.blendWith(overlay, BlendMode.ADDITIVE, 1.0f)
        // 0.8 + 0.5 = 1.3, should clamp to 1.0
        assertEquals(1.0f, result.pan!!, 0.01f)
    }

    /* ------------------------------------------------------------------ */
    /*  Null handling                                                       */
    /* ------------------------------------------------------------------ */

    @Test
    fun nullOverlayPreservesBase() {
        val base = FixtureOutput(pan = 0.5f, tilt = 0.3f, gobo = 3)
        val overlay = FixtureOutput() // all nulls

        val result = base.blendWith(overlay, BlendMode.NORMAL, 1.0f)
        assertEquals(0.5f, result.pan)
        assertEquals(0.3f, result.tilt)
        assertEquals(3, result.gobo)
    }

    @Test
    fun nullBaseGetsOverlayValue() {
        val base = FixtureOutput() // all nulls
        val overlay = FixtureOutput(pan = 0.7f, gobo = 5)

        val result = base.blendWith(overlay, BlendMode.NORMAL, 1.0f)
        assertEquals(0.7f, result.pan)
        assertEquals(5, result.gobo)
    }

    @Test
    fun goboReplacesOnNonZeroOpacity() {
        val base = FixtureOutput(gobo = 1)
        val overlay = FixtureOutput(gobo = 5)

        val result = base.blendWith(overlay, BlendMode.NORMAL, 0.1f)
        // Gobo is replaced (not blended) when overlay is non-null and opacity > 0
        assertEquals(5, result.gobo)
    }

    @Test
    fun goboPreservesBaseWhenOverlayNull() {
        val base = FixtureOutput(gobo = 3)
        val overlay = FixtureOutput(gobo = null)

        val result = base.blendWith(overlay, BlendMode.NORMAL, 1.0f)
        assertEquals(3, result.gobo)
    }

    /* ------------------------------------------------------------------ */
    /*  Blend float internal helper edge cases                             */
    /* ------------------------------------------------------------------ */

    @Test
    fun blendFloatNullOverlayReturnsBase() {
        val result = FixtureOutput.blendFloat(0.5f, null, BlendMode.NORMAL, 1.0f)
        assertEquals(0.5f, result)
    }

    @Test
    fun blendFloatNullBaseWithOverlayInterpolatesFromZero() {
        val result = FixtureOutput.blendFloat(null, 1.0f, BlendMode.NORMAL, 0.5f)
        // null base treated as 0.0: lerp(0.0, 1.0, 0.5) = 0.5
        assertEquals(0.5f, result!!, 0.01f)
    }

    @Test
    fun blendFloatAdditiveWithNullBase() {
        val result = FixtureOutput.blendFloat(null, 0.3f, BlendMode.ADDITIVE, 1.0f)
        // null base treated as 0.0: 0.0 + 0.3 * 1.0 = 0.3
        assertEquals(0.3f, result!!, 0.01f)
    }

    /* ------------------------------------------------------------------ */
    /*  blendMovementOnly — preserves color                                */
    /* ------------------------------------------------------------------ */

    @Test
    fun blendMovementOnlyPreservesBaseColor() {
        val base = FixtureOutput(color = Color.RED, pan = 0.2f)
        val overlay = FixtureOutput(color = Color.BLUE, pan = 0.8f, tilt = 0.5f)

        val result = base.blendMovementOnly(overlay, BlendMode.NORMAL, 1.0f)
        // Color should be unchanged (RED, not BLUE)
        assertEquals(Color.RED, result.color)
        // Movement channels should blend
        assertEquals(0.8f, result.pan)
        assertEquals(0.5f, result.tilt)
    }

    @Test
    fun blendMovementOnlyWithAdditiveMode() {
        val base = FixtureOutput(color = Color.GREEN, pan = 0.3f, tilt = 0.2f)
        val overlay = FixtureOutput(color = Color.WHITE, pan = 0.2f, tilt = 0.3f)

        val result = base.blendMovementOnly(overlay, BlendMode.ADDITIVE, 1.0f)
        // Color should be preserved as GREEN
        assertEquals(Color.GREEN, result.color)
        // Movement channels should add
        assertEquals(0.5f, result.pan!!, 0.01f) // 0.3 + 0.2
        assertEquals(0.5f, result.tilt!!, 0.01f) // 0.2 + 0.3
    }

    @Test
    fun blendMovementOnlyWithZeroOpacityKeepsBase() {
        val base = FixtureOutput(color = Color.RED, pan = 0.2f, gobo = 3)
        val overlay = FixtureOutput(color = Color.BLUE, pan = 0.9f, gobo = 7)

        val result = base.blendMovementOnly(overlay, BlendMode.NORMAL, 0.0f)
        assertEquals(Color.RED, result.color)
        assertEquals(0.2f, result.pan)
        // Gobo: opacity is 0, so base preserved
        assertEquals(3, result.gobo)
    }

    @Test
    fun blendMovementOnlyNullOverlayPreservesBase() {
        val base = FixtureOutput(color = Color.BLUE, pan = 0.5f, gobo = 2)
        val overlay = FixtureOutput() // all movement nulls

        val result = base.blendMovementOnly(overlay, BlendMode.NORMAL, 1.0f)
        assertEquals(Color.BLUE, result.color)
        assertEquals(0.5f, result.pan)
        assertEquals(2, result.gobo)
    }

    @Test
    fun blendMovementOnlyHandlesAllChannels() {
        val base = FixtureOutput(
            color = Color.WHITE,
            pan = 0.1f,
            tilt = 0.2f,
            gobo = 1,
            focus = 0.3f,
            zoom = 0.4f,
            strobeRate = 0.5f
        )
        val overlay = FixtureOutput(
            color = Color.BLACK,
            pan = 0.9f,
            tilt = 0.8f,
            gobo = 7,
            focus = 0.7f,
            zoom = 0.6f,
            strobeRate = 0.5f
        )

        val result = base.blendMovementOnly(overlay, BlendMode.NORMAL, 1.0f)
        // Color preserved
        assertEquals(Color.WHITE, result.color)
        // All movement channels blended
        assertEquals(0.9f, result.pan)
        assertEquals(0.8f, result.tilt)
        assertEquals(7, result.gobo)
        assertEquals(0.7f, result.focus)
        assertEquals(0.6f, result.zoom)
        assertEquals(0.5f, result.strobeRate)
    }
}
