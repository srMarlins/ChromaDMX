package com.chromadmx.engine.effect

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.BlendMode
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EffectStackTest {

    /** A trivial effect that always returns a fixed color. */
    private class ConstantEffect(
        override val id: String,
        override val name: String,
        private val color: Color
    ) : SpatialEffect {
        override fun compute(pos: Vec3, time: Float, beat: BeatState, params: EffectParams): Color = color
    }

    private val origin = Vec3.ZERO
    private val beat = BeatState.IDLE

    @Test
    fun emptyStackReturnsBlack() {
        val stack = EffectStack()
        val result = stack.evaluate(origin, 0f, beat)
        assertEquals(Color.BLACK, result)
    }

    @Test
    fun singleLayerNormalBlend() {
        val stack = EffectStack(
            layers = listOf(
                EffectLayer(
                    effect = ConstantEffect("red", "Red", Color.RED),
                    blendMode = BlendMode.NORMAL,
                    opacity = 1.0f
                )
            )
        )
        val result = stack.evaluate(origin, 0f, beat)
        assertEquals(Color.RED, result)
    }

    @Test
    fun masterDimmerScalesOutput() {
        val stack = EffectStack(
            layers = listOf(
                EffectLayer(
                    effect = ConstantEffect("white", "White", Color.WHITE),
                    blendMode = BlendMode.NORMAL,
                    opacity = 1.0f
                )
            ),
            masterDimmer = 0.5f
        )
        val result = stack.evaluate(origin, 0f, beat)
        assertEquals(0.5f, result.r, 0.001f)
        assertEquals(0.5f, result.g, 0.001f)
        assertEquals(0.5f, result.b, 0.001f)
    }

    @Test
    fun disabledLayerIsSkipped() {
        val stack = EffectStack(
            layers = listOf(
                EffectLayer(
                    effect = ConstantEffect("red", "Red", Color.RED),
                    blendMode = BlendMode.NORMAL,
                    opacity = 1.0f,
                    enabled = false
                )
            )
        )
        val result = stack.evaluate(origin, 0f, beat)
        assertEquals(Color.BLACK, result)
    }

    @Test
    fun additiveBlendingCombinesLayers() {
        val stack = EffectStack(
            layers = listOf(
                EffectLayer(
                    effect = ConstantEffect("red", "Red", Color.RED),
                    blendMode = BlendMode.NORMAL,
                    opacity = 1.0f
                ),
                EffectLayer(
                    effect = ConstantEffect("green", "Green", Color.GREEN),
                    blendMode = BlendMode.ADDITIVE,
                    opacity = 1.0f
                )
            )
        )
        val result = stack.evaluate(origin, 0f, beat)
        // RED + GREEN = YELLOW (1,1,0)
        assertEquals(1.0f, result.r, 0.001f)
        assertEquals(1.0f, result.g, 0.001f)
        assertEquals(0.0f, result.b, 0.001f)
    }

    @Test
    fun opacityReducesLayerContribution() {
        val stack = EffectStack(
            layers = listOf(
                EffectLayer(
                    effect = ConstantEffect("white", "White", Color.WHITE),
                    blendMode = BlendMode.NORMAL,
                    opacity = 0.5f
                )
            )
        )
        val result = stack.evaluate(origin, 0f, beat)
        // NORMAL blend: base=BLACK, overlay=WHITE, opacity=0.5 => 0.5 grey
        assertEquals(0.5f, result.r, 0.001f)
        assertEquals(0.5f, result.g, 0.001f)
        assertEquals(0.5f, result.b, 0.001f)
    }

    @Test
    fun masterDimmerClamps() {
        val stack = EffectStack(masterDimmer = 2.0f)
        assertEquals(1.0f, stack.masterDimmer)

        stack.masterDimmer = -0.5f
        assertEquals(0.0f, stack.masterDimmer)
    }

    @Test
    fun addAndRemoveLayers() {
        val stack = EffectStack()
        assertEquals(0, stack.layerCount)

        stack.addLayer(EffectLayer(effect = ConstantEffect("a", "A", Color.RED)))
        assertEquals(1, stack.layerCount)

        stack.addLayer(EffectLayer(effect = ConstantEffect("b", "B", Color.BLUE)))
        assertEquals(2, stack.layerCount)

        stack.removeLayerAt(0)
        assertEquals(1, stack.layerCount)
        assertEquals("b", stack.layers[0].effect.id)

        stack.clearLayers()
        assertEquals(0, stack.layerCount)
    }

    @Test
    fun multiplyBlendDarkensResult() {
        val halfGrey = Color(0.5f, 0.5f, 0.5f)
        val stack = EffectStack(
            layers = listOf(
                EffectLayer(
                    effect = ConstantEffect("base", "Base", Color.WHITE),
                    blendMode = BlendMode.NORMAL,
                    opacity = 1.0f
                ),
                EffectLayer(
                    effect = ConstantEffect("mult", "Mult", halfGrey),
                    blendMode = BlendMode.MULTIPLY,
                    opacity = 1.0f
                )
            )
        )
        val result = stack.evaluate(origin, 0f, beat)
        // WHITE * 0.5 grey = 0.5 grey
        assertEquals(0.5f, result.r, 0.001f)
        assertEquals(0.5f, result.g, 0.001f)
        assertEquals(0.5f, result.b, 0.001f)
    }
}
