package com.chromadmx.ui.integration

import com.chromadmx.core.model.DmxNode
import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.core.persistence.FixtureStore
import com.chromadmx.core.persistence.SettingsStore
import com.chromadmx.networking.ConnectionState
import com.chromadmx.networking.DmxTransport
import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.ui.navigation.AppScreen
import com.chromadmx.ui.navigation.AppStateManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Integration tests for the navigation flow spanning AppStateManager,
 * fixture discovery, and settings persistence.
 *
 * Verifies the full user journey: first launch → Setup → discover → configure → Stage,
 * repeat launch → straight to Stage, and Settings round-trip.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NavigationIntegrationTest {

    // -- Fakes --

    private class FakeFixtureStore : FixtureStore {
        private val _fixtures = MutableStateFlow<List<Fixture3D>>(emptyList())
        var savedFixtures: List<Fixture3D> = emptyList()
            private set

        override fun allFixtures(): Flow<List<Fixture3D>> = _fixtures
        override fun saveFixture(fixture: Fixture3D) {
            savedFixtures = savedFixtures + fixture
            _fixtures.value = savedFixtures
        }
        override fun saveAll(fixtures: List<Fixture3D>) {
            savedFixtures = fixtures
            _fixtures.value = fixtures
        }
        override fun deleteFixture(fixtureId: String) {
            savedFixtures = savedFixtures.filter { it.fixture.fixtureId != fixtureId }
            _fixtures.value = savedFixtures
        }
        override fun deleteAll() {
            savedFixtures = emptyList()
            _fixtures.value = emptyList()
        }
    }

    private class FakeSettingsStore : SettingsStore {
        private val _masterDimmer = MutableStateFlow(1.0f)
        private val _themePreference = MutableStateFlow("MatchaDark")
        private val _isSimulation = MutableStateFlow(false)
        private val _transportMode = MutableStateFlow("Real")
        private val _activePresetId = MutableStateFlow<String?>(null)
        private val _setupCompleted = MutableStateFlow(false)

        override val masterDimmer: Flow<Float> = _masterDimmer
        override val themePreference: Flow<String> = _themePreference
        override val isSimulation: Flow<Boolean> = _isSimulation
        override val transportMode: Flow<String> = _transportMode
        override val activePresetId: Flow<String?> = _activePresetId
        override val setupCompleted: Flow<Boolean> = _setupCompleted

        override suspend fun setMasterDimmer(value: Float) { _masterDimmer.value = value }
        override suspend fun setThemePreference(value: String) { _themePreference.value = value }
        override suspend fun setIsSimulation(value: Boolean) { _isSimulation.value = value }
        override suspend fun setTransportMode(value: String) { _transportMode.value = value }
        override suspend fun setActivePresetId(value: String?) { _activePresetId.value = value }
        override suspend fun setSetupCompleted(value: Boolean) { _setupCompleted.value = value }
    }

    private class FakeFixtureDiscovery : FixtureDiscovery {
        private val _discoveredNodes = MutableStateFlow<List<DmxNode>>(emptyList())
        override val discoveredNodes: StateFlow<List<DmxNode>> = _discoveredNodes.asStateFlow()

        private val _isScanning = MutableStateFlow(false)
        override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

        var startScanCount = 0
            private set

        override fun startScan() {
            startScanCount++
            _isScanning.value = true
        }

        override fun stopScan() {
            _isScanning.value = false
        }

        fun emitNodes(nodes: List<DmxNode>) {
            _discoveredNodes.value = nodes
        }
    }

    // -- Helpers --

    private fun testFixture(id: String = "fix-1"): Fixture3D = Fixture3D(
        fixture = Fixture(
            fixtureId = id,
            name = "Test Fixture $id",
            channelStart = 0,
            channelCount = 3,
            universeId = 0,
        ),
        position = Vec3.ZERO,
    )

    private fun testNode(): DmxNode = DmxNode(
        ipAddress = "192.168.1.100",
        macAddress = "de:ad:be:ef:00:01",
        shortName = "TestNode-1",
        longName = "Test Art-Net Node 1",
        numPorts = 1,
        universes = listOf(0),
    )

    // -- Tests --

    @Test
    fun firstLaunchStartsAtSetup() = runTest {
        val fixtureStore = FakeFixtureStore()
        val settingsStore = FakeSettingsStore()

        val manager = AppStateManager(
            allFixtures = { fixtureStore.allFixtures() },
            setSetupCompleted = { settingsStore.setSetupCompleted(it) },
            scope = this,
        )
        advanceUntilIdle()

        assertIs<AppScreen.Setup>(manager.currentScreen.first())
    }

    @Test
    fun returnLaunchWithFixturesStartsAtStage() = runTest {
        val fixtureStore = FakeFixtureStore()
        val settingsStore = FakeSettingsStore()

        // Pre-populate with fixtures (simulating returning user)
        fixtureStore.saveAll(listOf(testFixture("fix-1"), testFixture("fix-2")))

        val manager = AppStateManager(
            allFixtures = { fixtureStore.allFixtures() },
            setSetupCompleted = { settingsStore.setSetupCompleted(it) },
            scope = this,
        )
        advanceUntilIdle()

        assertIs<AppScreen.Stage>(manager.currentScreen.first())
    }

    @Test
    fun completeSetupNavigatesToStageAndPersists() = runTest {
        val fixtureStore = FakeFixtureStore()
        val settingsStore = FakeSettingsStore()

        val manager = AppStateManager(
            allFixtures = { fixtureStore.allFixtures() },
            setSetupCompleted = { settingsStore.setSetupCompleted(it) },
            scope = this,
        )
        advanceUntilIdle()
        assertIs<AppScreen.Setup>(manager.currentScreen.first())

        // User completes setup
        manager.completeSetup()
        advanceUntilIdle()

        assertIs<AppScreen.Stage>(manager.currentScreen.first())
        assertTrue(settingsStore.setupCompleted.first(), "Setup completed should be persisted")
    }

    @Test
    fun settingsRoundTrip() = runTest {
        val fixtureStore = FakeFixtureStore()
        fixtureStore.saveAll(listOf(testFixture()))

        val manager = AppStateManager(
            allFixtures = { fixtureStore.allFixtures() },
            setSetupCompleted = {},
            scope = this,
        )
        advanceUntilIdle()
        assertIs<AppScreen.Stage>(manager.currentScreen.first())

        // Navigate to Settings
        manager.navigateTo(AppScreen.Settings)
        assertIs<AppScreen.Settings>(manager.currentScreen.first())

        // Navigate back to Stage
        manager.navigateBack()
        assertIs<AppScreen.Stage>(manager.currentScreen.first())
    }

    @Test
    fun settingsToProvisioningAndBack() = runTest {
        val fixtureStore = FakeFixtureStore()
        fixtureStore.saveAll(listOf(testFixture()))

        val manager = AppStateManager(
            allFixtures = { fixtureStore.allFixtures() },
            setSetupCompleted = {},
            scope = this,
        )
        advanceUntilIdle()

        // Stage -> Settings -> Provisioning -> back to Settings -> back to Stage
        manager.navigateTo(AppScreen.Settings)
        assertIs<AppScreen.Settings>(manager.currentScreen.first())

        manager.navigateTo(AppScreen.Provisioning)
        assertIs<AppScreen.Provisioning>(manager.currentScreen.first())

        manager.navigateBack()
        assertIs<AppScreen.Settings>(manager.currentScreen.first())

        manager.navigateBack()
        assertIs<AppScreen.Stage>(manager.currentScreen.first())
    }

    @Test
    fun completeSetupClearsBackStack() = runTest {
        val fixtureStore = FakeFixtureStore()

        val manager = AppStateManager(
            allFixtures = { fixtureStore.allFixtures() },
            setSetupCompleted = {},
            scope = this,
        )
        advanceUntilIdle()

        // Push Settings onto backStack before completing setup
        manager.navigateTo(AppScreen.Settings)
        assertIs<AppScreen.Settings>(manager.currentScreen.first())

        // Complete setup — should clear backStack and go to Stage
        manager.completeSetup()
        advanceUntilIdle()
        assertIs<AppScreen.Stage>(manager.currentScreen.first())

        // navigateBack should NOT go to Settings (backStack was cleared)
        manager.navigateBack()
        // Should still be on Stage since backStack is empty
        assertIs<AppScreen.Stage>(manager.currentScreen.first())
    }

    @Test
    fun fullFlowFirstLaunchToStage() = runTest {
        val fixtureStore = FakeFixtureStore()
        val settingsStore = FakeSettingsStore()
        val discovery = FakeFixtureDiscovery()

        // 1. First launch → Setup
        val manager = AppStateManager(
            allFixtures = { fixtureStore.allFixtures() },
            setSetupCompleted = { settingsStore.setSetupCompleted(it) },
            scope = this,
        )
        advanceUntilIdle()
        assertIs<AppScreen.Setup>(manager.currentScreen.first())

        // 2. Discovery finds nodes (simulating SetupViewModel behavior)
        discovery.startScan()
        discovery.emitNodes(listOf(testNode()))
        assertEquals(1, discovery.startScanCount)
        assertEquals(1, discovery.discoveredNodes.first().size)

        // 3. User saves fixtures
        fixtureStore.saveAll(listOf(testFixture()))
        assertEquals(1, fixtureStore.allFixtures().first().size)

        // 4. Complete setup → Stage
        manager.completeSetup()
        advanceUntilIdle()
        assertIs<AppScreen.Stage>(manager.currentScreen.first())

        // 5. Verify persistence
        assertTrue(settingsStore.setupCompleted.first())

        // 6. Simulate app restart with existing fixtures → straight to Stage
        val manager2 = AppStateManager(
            allFixtures = { fixtureStore.allFixtures() },
            setSetupCompleted = { settingsStore.setSetupCompleted(it) },
            scope = this,
        )
        advanceUntilIdle()
        assertIs<AppScreen.Stage>(manager2.currentScreen.first())
    }
}
