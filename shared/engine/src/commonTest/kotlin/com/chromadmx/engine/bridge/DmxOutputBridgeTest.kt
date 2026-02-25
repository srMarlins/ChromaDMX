package com.chromadmx.engine.bridge

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.*
import com.chromadmx.engine.effect.EffectLayer
import com.chromadmx.engine.effects.SolidColorEffect
import com.chromadmx.engine.pipeline.EffectEngine
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DmxOutputBridgeTest {
    @Test
    fun bridgeReadsFromEngineAndProducesFrames() {
        val fixtures = listOf(
            Fixture3D(
                fixture = Fixture("f1", "Par 1", 0, 3, 0),
                position = Vec3.ZERO
            )
        )
        val engine = EffectEngine(TestScope(), fixtures)
        engine.effectStack.addLayer(
            EffectLayer(
                effect = SolidColorEffect(),
                params = EffectParams.EMPTY.with("color", Color.RED)
            )
        )
        engine.tick()

        val bridge = DmxBridge(fixtures, emptyMap())
        val colors = engine.colorOutput.let {
            it.swapRead()
            it.readSlot()
        }
        val result = bridge.convert(colors)

        assertTrue(result.containsKey(0))
        assertEquals(255, result[0]!![0].toInt() and 0xFF)
    }
}
