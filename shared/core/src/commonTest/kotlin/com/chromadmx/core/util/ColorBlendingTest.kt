package com.chromadmx.core.util

import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Color
import kotlin.test.Test
import kotlin.test.assertTrue

class ColorBlendingTest {

    /* ---- NORMAL ---- */

    @Test
    fun normalFullOpacityReturnsOverlay() {
        val result = ColorBlending.blend(Color.BLACK, Color.RED, BlendMode.NORMAL, 1f)
        assertColorApprox(Color.RED, result)
    }

    @Test
    fun normalZeroOpacityReturnsBase() {
        val result = ColorBlending.blend(Color.BLACK, Color.RED, BlendMode.NORMAL, 0f)
        assertColorApprox(Color.BLACK, result)
    }

    @Test
    fun normalHalfOpacityMixes() {
        val result = ColorBlending.blend(Color.BLACK, Color.WHITE, BlendMode.NORMAL, 0.5f)
        assertApprox(0.5f, result.r)
        assertApprox(0.5f, result.g)
        assertApprox(0.5f, result.b)
    }

    /* ---- ADDITIVE ---- */

    @Test
    fun additiveAddsAndClamps() {
        val base = Color(0.6f, 0.7f, 0.3f)
        val overlay = Color(0.5f, 0.5f, 0.5f)
        val result = ColorBlending.blend(base, overlay, BlendMode.ADDITIVE, 1f)
        // 0.6+0.5=1.1 clamped to 1.0
        assertApprox(1f, result.r)
        // 0.7+0.5=1.2 clamped to 1.0
        assertApprox(1f, result.g)
        assertApprox(0.8f, result.b)
    }

    @Test
    fun additiveBlackIsIdentity() {
        val base = Color(0.3f, 0.5f, 0.7f)
        val result = ColorBlending.blend(base, Color.BLACK, BlendMode.ADDITIVE, 1f)
        assertColorApprox(base, result)
    }

    /* ---- MULTIPLY ---- */

    @Test
    fun multiplyMath() {
        val base = Color(0.5f, 0.8f, 1f)
        val overlay = Color(0.5f, 0.5f, 0.5f)
        val result = ColorBlending.blend(base, overlay, BlendMode.MULTIPLY, 1f)
        assertApprox(0.25f, result.r)
        assertApprox(0.4f, result.g)
        assertApprox(0.5f, result.b)
    }

    @Test
    fun multiplyByWhiteIsIdentity() {
        val base = Color(0.3f, 0.5f, 0.7f)
        val result = ColorBlending.blend(base, Color.WHITE, BlendMode.MULTIPLY, 1f)
        assertColorApprox(base, result)
    }

    @Test
    fun multiplyByBlackIsBlack() {
        val base = Color(0.3f, 0.5f, 0.7f)
        val result = ColorBlending.blend(base, Color.BLACK, BlendMode.MULTIPLY, 1f)
        assertColorApprox(Color.BLACK, result)
    }

    /* ---- OVERLAY ---- */

    @Test
    fun overlayDarkBaseMultiplies() {
        // base < 0.5 => 2 * base * overlay
        val base = Color(0.2f, 0.2f, 0.2f)
        val overlay = Color(0.5f, 0.5f, 0.5f)
        val result = ColorBlending.blend(base, overlay, BlendMode.OVERLAY, 1f)
        // 2 * 0.2 * 0.5 = 0.2
        assertApprox(0.2f, result.r)
    }

    @Test
    fun overlayLightBaseScreens() {
        // base >= 0.5 => 1 - 2*(1-base)*(1-overlay)
        val base = Color(0.8f, 0.8f, 0.8f)
        val overlay = Color(0.6f, 0.6f, 0.6f)
        val result = ColorBlending.blend(base, overlay, BlendMode.OVERLAY, 1f)
        // 1 - 2*(0.2)*(0.4) = 1 - 0.16 = 0.84
        assertApprox(0.84f, result.r)
    }

    @Test
    fun overlayMidGrayIsNeutral() {
        // base=0.5 => 2*0.5*overlay = overlay  (for the < 0.5 branch, 0.5 is not < 0.5)
        // Actually 0.5 >= 0.5 so: 1 - 2*(0.5)*(1-overlay)
        val base = Color(0.5f, 0.5f, 0.5f)
        val overlay = Color(0.7f, 0.7f, 0.7f)
        val result = ColorBlending.blend(base, overlay, BlendMode.OVERLAY, 1f)
        // 1 - 2*0.5*0.3 = 1 - 0.3 = 0.7
        assertApprox(0.7f, result.r)
    }

    /* ---- opacity interaction ---- */

    @Test
    fun opacityIsClamped() {
        // opacity > 1 behaves as 1
        val result = ColorBlending.blend(Color.BLACK, Color.WHITE, BlendMode.NORMAL, 5f)
        assertColorApprox(Color.WHITE, result)
    }

    /* ---- helpers ---- */

    private fun assertApprox(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(
            kotlin.math.abs(expected - actual) < eps,
            "Expected ~$expected but got $actual"
        )
    }

    private fun assertColorApprox(expected: Color, actual: Color, eps: Float = 1e-4f) {
        assertApprox(expected.r, actual.r, eps)
        assertApprox(expected.g, actual.g, eps)
        assertApprox(expected.b, actual.b, eps)
    }
}
