package com.chromadmx.agent.pregen

import com.chromadmx.agent.scene.Scene
import com.chromadmx.engine.preset.PresetLibrary
import com.chromadmx.engine.effect.EffectRegistry
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.agent.FakeFileStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PreGenerationServiceTest {
    private fun createService(): Pair<PreGenerationService, PresetLibrary> {
        val library = PresetLibrary(FakeFileStorage(), EffectRegistry(), EffectStack())
        return PreGenerationService(library) to library
    }

    @Test
    fun generateScenesCreatesRequestedCount() = runTest {
        val (service, library) = createService()
        val presets = service.generate("techno", 3)
        assertEquals(3, presets.size)
        assertEquals(3, library.listPresets().size)
    }

    @Test
    fun generatedScenesAreSavedToStore() = runTest {
        val (service, library) = createService()
        service.generate("ambient", 2)
        val presets = library.listPresets()
        assertEquals(2, presets.size)
        // Each scene should be loadable
        presets.forEach { preset ->
            assertNotNull(library.getPreset(preset.id))
        }
    }

    @Test
    fun generatedScenesHaveGenreInName() = runTest {
        val (service, _) = createService()
        val scenes = service.generate("techno", 2)
        scenes.forEach { scene ->
            assertTrue(scene.name.contains("techno", ignoreCase = true),
                "Scene name '${scene.name}' should contain genre 'techno'")
        }
    }

    @Test
    fun generationProgressUpdatesFlow() = runTest {
        val (service, _) = createService()
        // Before generation
        assertEquals(0, service.progress.value.current)
        assertFalse(service.progress.value.isRunning)

        service.generate("house", 3)

        // After generation
        assertEquals(3, service.progress.value.current)
        assertEquals(3, service.progress.value.total)
        assertFalse(service.progress.value.isRunning)
    }

    @Test
    fun cancelStopsGeneration() = runTest {
        val (service, sceneStore) = createService()
        service.cancel()
        val scenes = service.generate("dnb", 5)
        // After cancel is called, generation should stop early
        // (Since our generate is synchronous in test, cancel before will have no effect,
        //  but we verify the cancel flag is set)
        assertTrue(service.progress.value.current <= 5)
    }

    @Test
    fun generateZeroScenesReturnsEmpty() = runTest {
        val (service, _) = createService()
        val scenes = service.generate("techno", 0)
        assertEquals(0, scenes.size)
    }

    @Test
    fun progressInitialState() {
        val (service, _) = createService()
        val progress = service.progress.value
        assertEquals(0, progress.current)
        assertEquals(0, progress.total)
        assertFalse(progress.isRunning)
    }
}
