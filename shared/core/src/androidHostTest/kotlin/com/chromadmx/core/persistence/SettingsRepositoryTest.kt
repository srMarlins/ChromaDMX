package com.chromadmx.core.persistence

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalSettingsApi::class)
class SettingsRepositoryTest {

    private lateinit var repo: SettingsRepository

    @BeforeTest
    fun setup() {
        val settings = MapSettings().toFlowSettings()
        repo = SettingsRepository(settings)
    }

    @Test
    fun defaultMasterDimmer() = runTest {
        assertEquals(1f, repo.masterDimmer.first())
    }

    @Test
    fun setAndGetMasterDimmer() = runTest {
        repo.setMasterDimmer(0.5f)
        assertEquals(0.5f, repo.masterDimmer.first())
    }

    @Test
    fun defaultTheme() = runTest {
        assertEquals("MatchaDark", repo.themePreference.first())
    }

    @Test
    fun setAndGetTheme() = runTest {
        repo.setThemePreference("CyberNeon")
        assertEquals("CyberNeon", repo.themePreference.first())
    }

    @Test
    fun defaultSimulationFalse() = runTest {
        assertFalse(repo.isSimulation.first())
    }

    @Test
    fun toggleSimulation() = runTest {
        repo.setIsSimulation(true)
        assertTrue(repo.isSimulation.first())
    }

    @Test
    fun defaultTransportMode() = runTest {
        assertEquals("Real", repo.transportMode.first())
    }

    @Test
    fun setTransportMode() = runTest {
        repo.setTransportMode("Simulated")
        assertEquals("Simulated", repo.transportMode.first())
    }

    @Test
    fun activePresetIdNullByDefault() = runTest {
        assertNull(repo.activePresetId.first())
    }

    @Test
    fun setAndClearActivePresetId() = runTest {
        repo.setActivePresetId("preset-1")
        assertEquals("preset-1", repo.activePresetId.first())

        repo.setActivePresetId(null)
        assertNull(repo.activePresetId.first())
    }

    @Test
    fun defaultSetupCompleted() = runTest {
        assertFalse(repo.setupCompleted.first())
    }

    @Test
    fun setSetupCompleted() = runTest {
        repo.setSetupCompleted(true)
        assertTrue(repo.setupCompleted.first())
    }
}
