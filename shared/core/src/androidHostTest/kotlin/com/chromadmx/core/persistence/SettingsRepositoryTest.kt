package com.chromadmx.core.persistence

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.chromadmx.core.db.ChromaDmxDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsRepositoryTest {

    private lateinit var db: ChromaDmxDatabase
    private lateinit var repo: SettingsRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChromaDmxDatabase.Schema.create(driver)
        db = ChromaDmxDatabase(driver)
        repo = SettingsRepository(db)
    }

    @Test
    fun defaultMasterDimmer() = runTest {
        assertEquals(1.0f, repo.masterDimmer.first())
    }

    @Test
    fun setAndGetMasterDimmer() = runTest {
        repo.setMasterDimmer(0.5f)
        assertEquals(0.5f, repo.masterDimmer.first())
    }

    @Test
    fun defaultIsSimulation() = runTest {
        assertFalse(repo.isSimulation.first())
    }

    @Test
    fun toggleSimulation() = runTest {
        repo.setIsSimulation(true)
        assertTrue(repo.isSimulation.first())
    }

    @Test
    fun defaultActivePresetId() = runTest {
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
    fun defaultThemePreference() = runTest {
        assertEquals("MatchaDark", repo.themePreference.first())
    }

    @Test
    fun setAndGetThemePreference() = runTest {
        repo.setThemePreference("CyberNeon")
        assertEquals("CyberNeon", repo.themePreference.first())
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
    fun defaultSetupCompleted() = runTest {
        assertFalse(repo.setupCompleted.first())
    }

    @Test
    fun setSetupCompleted() = runTest {
        repo.setSetupCompleted(true)
        assertTrue(repo.setupCompleted.first())
    }
}
