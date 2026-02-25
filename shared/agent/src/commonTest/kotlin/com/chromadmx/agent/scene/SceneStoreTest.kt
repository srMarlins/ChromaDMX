package com.chromadmx.agent.scene

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SceneStoreTest {
    @Test
    fun saveAndLoadScene() {
        val store = SceneStore()
        val scene = ScenePreset(
            name = "Techno Pulse",
            layers = listOf(
                EffectLayerConfig(
                    effectId = "radial_pulse_3d",
                    params = mapOf("speed" to 1.5f),
                    blendMode = "ADDITIVE",
                    opacity = 0.8f
                )
            ),
            masterDimmer = 0.9f,
            colorPalette = listOf("#FF0000", "#0000FF"),
            tempoMultiplier = 1.0f
        )
        store.save(scene)
        val loaded = store.load("Techno Pulse")
        assertNotNull(loaded)
        assertEquals("Techno Pulse", loaded.name)
        assertEquals(1, loaded.layers.size)
        assertEquals(0.9f, loaded.masterDimmer)
    }

    @Test
    fun loadNonexistentReturnsNull() {
        val store = SceneStore()
        assertNull(store.load("nope"))
    }

    @Test
    fun listScenesReturnsAllNames() {
        val store = SceneStore()
        val initialCount = store.list().size
        store.save(ScenePreset(name = "A"))
        store.save(ScenePreset(name = "B"))
        val names = store.list()
        assertTrue(names.contains("A"))
        assertTrue(names.contains("B"))
        assertEquals(initialCount + 2, names.size)
    }

    @Test
    fun saveOverwritesExisting() {
        val store = SceneStore()
        store.save(ScenePreset(name = "X", masterDimmer = 0.5f))
        store.save(ScenePreset(name = "X", masterDimmer = 1.0f))
        assertEquals(1.0f, store.load("X")!!.masterDimmer)
        assertEquals(1, store.list().size)
    }

    @Test
    fun deleteRemovesScene() {
        val store = SceneStore()
        val initialCount = store.list().size
        store.save(ScenePreset(name = "ToDelete"))
        assertTrue(store.delete("ToDelete"))
        assertNull(store.load("ToDelete"))
        assertEquals(initialCount, store.list().size)
    }

    @Test
    fun deleteBuiltInFails() {
        val store = SceneStore()
        val builtInName = BuiltInPresets.ALL.first().name
        val initialCount = store.list().size
        assertEquals(false, store.delete(builtInName), "Should not be able to delete built-in preset")
        assertNotNull(store.load(builtInName))
        assertEquals(initialCount, store.list().size)
    }

    @Test
    fun deleteNonexistentReturnsFalse() {
        val store = SceneStore()
        assertEquals(false, store.delete("nope"))
    }

    @Test
    fun layerConfigDefaultValues() {
        val layer = EffectLayerConfig()
        assertEquals("", layer.effectId)
        assertEquals(emptyMap(), layer.params)
        assertEquals("NORMAL", layer.blendMode)
        assertEquals(1.0f, layer.opacity)
    }
}
