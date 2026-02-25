package com.chromadmx.engine.effect

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EffectRegistryTest {

    private class StubEffect(override val id: String, override val name: String) : SpatialEffect {
        override fun compute(pos: Vec3, context: Any?): Color = Color.BLACK
    }

    @Test
    fun registerAndLookUp() {
        val registry = EffectRegistry()
        val effect = StubEffect("test-1", "Test 1")

        registry.register(effect)

        assertNotNull(registry.get("test-1"))
        assertEquals("Test 1", registry.get("test-1")!!.name)
    }

    @Test
    fun unknownIdReturnsNull() {
        val registry = EffectRegistry()
        assertNull(registry.get("nonexistent"))
    }

    @Test
    fun registerOverwritesPreviousEntry() {
        val registry = EffectRegistry()
        registry.register(StubEffect("dup", "First"))
        registry.register(StubEffect("dup", "Second"))

        assertEquals(1, registry.size)
        assertEquals("Second", registry.get("dup")!!.name)
    }

    @Test
    fun idsReturnsAllRegisteredIds() {
        val registry = EffectRegistry()
        registry.register(StubEffect("a", "A"))
        registry.register(StubEffect("b", "B"))

        val ids = registry.ids()
        assertTrue(ids.contains("a"))
        assertTrue(ids.contains("b"))
        assertEquals(2, ids.size)
    }

    @Test
    fun unregisterRemovesEffect() {
        val registry = EffectRegistry()
        registry.register(StubEffect("rem", "Remove Me"))

        assertTrue(registry.unregister("rem"))
        assertNull(registry.get("rem"))
        assertEquals(0, registry.size)
    }

    @Test
    fun allReturnsEffectsList() {
        val registry = EffectRegistry()
        registry.register(StubEffect("x", "X"))
        registry.register(StubEffect("y", "Y"))

        val all = registry.all()
        assertEquals(2, all.size)
    }
}
