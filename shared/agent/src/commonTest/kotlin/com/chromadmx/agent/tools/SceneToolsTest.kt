package com.chromadmx.agent.tools

import com.chromadmx.agent.scene.Scene
import com.chromadmx.agent.scene.SceneStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains

class SceneToolsTest {
    private val controller = FakeEngineController()
    private val sceneStore = SceneStore()

    @Test
    fun setEffectAppliesEffectToLayer() = runTest {
        val tool = SetEffectTool(controller)
        val result = tool.execute(SetEffectTool.Args(layer = 0, effectId = "solid_color", params = mapOf("r" to 1.0f)))
        assertContains(result, "solid_color")
        assertEquals("solid_color", controller.lastSetEffectId)
    }

    @Test
    fun setEffectWithEmptyParams() = runTest {
        val tool = SetEffectTool(controller)
        val result = tool.execute(SetEffectTool.Args(layer = 2, effectId = "rainbow_sweep_3d"))
        assertContains(result, "rainbow_sweep_3d")
        assertContains(result, "layer 2")
    }

    @Test
    fun setBlendModeUpdatesLayer() = runTest {
        val tool = SetBlendModeTool(controller)
        val result = tool.execute(SetBlendModeTool.Args(layer = 0, mode = "ADDITIVE"))
        assertContains(result, "ADDITIVE")
        assertEquals("ADDITIVE", controller.lastBlendMode)
    }

    @Test
    fun setMasterDimmerClampsAboveOne() = runTest {
        val tool = SetMasterDimmerTool(controller)
        tool.execute(SetMasterDimmerTool.Args(value = 1.5f))
        assertEquals(1.0f, controller.lastMasterDimmer)
    }

    @Test
    fun setMasterDimmerClampsBelowZero() = runTest {
        val tool = SetMasterDimmerTool(controller)
        tool.execute(SetMasterDimmerTool.Args(value = -0.5f))
        assertEquals(0.0f, controller.lastMasterDimmer)
    }

    @Test
    fun setMasterDimmerNormalValue() = runTest {
        val tool = SetMasterDimmerTool(controller)
        val result = tool.execute(SetMasterDimmerTool.Args(value = 0.75f))
        assertEquals(0.75f, controller.lastMasterDimmer)
        assertContains(result, "0.75")
    }

    @Test
    fun setColorPaletteStoresColors() = runTest {
        val tool = SetColorPaletteTool(controller)
        tool.execute(SetColorPaletteTool.Args(colors = listOf("#FF0000", "#00FF00")))
        assertEquals(2, controller.lastPalette.size)
        assertEquals("#FF0000", controller.lastPalette[0])
    }

    @Test
    fun setTempoMultiplierUpdates() = runTest {
        val tool = SetTempoMultiplierTool(controller)
        tool.execute(SetTempoMultiplierTool.Args(multiplier = 2.0f))
        assertEquals(2.0f, controller.lastTempoMultiplier)
    }

    @Test
    fun createSceneSavesToStore() = runTest {
        val tool = CreateSceneTool(controller, sceneStore)
        val result = tool.execute(CreateSceneTool.Args(name = "Test Scene"))
        val saved = sceneStore.load("Test Scene")
        assertTrue(saved != null)
        assertContains(result, "Test Scene")
    }

    @Test
    fun loadSceneRestoresFromStore() = runTest {
        sceneStore.save(Scene(name = "Saved", masterDimmer = 0.5f))
        val tool = LoadSceneTool(controller, sceneStore)
        val result = tool.execute(LoadSceneTool.Args(name = "Saved"))
        assertContains(result, "Saved")
        assertEquals(0.5f, controller.lastMasterDimmer)
    }

    @Test
    fun loadSceneReturnsErrorForMissing() = runTest {
        val tool = LoadSceneTool(controller, sceneStore)
        val result = tool.execute(LoadSceneTool.Args(name = "Nope"))
        assertContains(result, "not found")
    }
}
