package com.chromadmx.core.persistence

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    private val settings = MapSettings()
    private val repository = SettingsRepository(settings)

    @Test
    fun testMasterDimmer() = runTest {
        assertEquals(1.0f, repository.masterDimmer.first())
        repository.setMasterDimmer(0.5f)
        assertEquals(0.5f, repository.masterDimmer.first())
    }

    @Test
    fun testIsSimulation() = runTest {
        assertFalse(repository.isSimulation.first())
        repository.setSimulation(true)
        assertTrue(repository.isSimulation.first())
    }

    @Test
    fun testActivePresetId() = runTest {
        assertNull(repository.activePresetId.first())
        repository.setActivePresetId("preset-123")
        assertEquals("preset-123", repository.activePresetId.first())
        repository.setActivePresetId(null)
        assertNull(repository.activePresetId.first())
    }

    @Test
    fun testThemePreference() = runTest {
        assertEquals("Dark", repository.themePreference.first())
        repository.setThemePreference("Neon")
        assertEquals("Neon", repository.themePreference.first())
    }

    @Test
    fun testTransportMode() = runTest {
        assertEquals("Real", repository.transportMode.first())
        repository.setTransportMode("Simulated")
        assertEquals("Simulated", repository.transportMode.first())
    }
}
