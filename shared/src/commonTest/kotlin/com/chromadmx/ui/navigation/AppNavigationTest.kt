package com.chromadmx.ui.navigation

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/* ------------------------------------------------------------------ */
/*  Fakes                                                              */
/* ------------------------------------------------------------------ */

/**
 * Fake [SettingsRepository] backed by [MutableStateFlow]s.
 * Only exposes the flows used by [AppStateManager].
 */
class FakeSettingsRepository {
    val masterDimmer = MutableStateFlow(1f)
    val themePreference = MutableStateFlow("MatchaDark")
    val isSimulation = MutableStateFlow(false)
    val transportMode = MutableStateFlow("Real")
    val activePresetId = MutableStateFlow<String?>(null)
    val setupCompleted = MutableStateFlow(false)

    private var _setupCompletedValue = false

    suspend fun setSetupCompleted(value: Boolean) {
        _setupCompletedValue = value
        setupCompleted.value = value
    }

    fun wasSetupCompletedSet(): Boolean = _setupCompletedValue
}

/**
 * Fake [FixtureRepository] exposing a [MutableStateFlow] for [allFixtures].
 */
class FakeFixtureRepository {
    val fixtures = MutableStateFlow<List<Fixture3D>>(emptyList())

    fun allFixtures(): Flow<List<Fixture3D>> = fixtures
}

/* ------------------------------------------------------------------ */
/*  Tests                                                              */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalCoroutinesApi::class)
class AppNavigationTest {

    private fun fixture(id: String = "f1"): Fixture3D = Fixture3D(
        fixture = Fixture(
            fixtureId = id,
            name = "Par $id",
            channelStart = 1,
            channelCount = 3,
            universeId = 0,
        ),
        position = Vec3.ZERO,
    )

    private fun createManager(
        scope: TestScope,
        fakeSettings: FakeSettingsRepository = FakeSettingsRepository(),
        fakeFixtures: FakeFixtureRepository = FakeFixtureRepository(),
    ): AppStateManager {
        return AppStateManager(
            allFixtures = { fakeFixtures.allFixtures() },
            setSetupCompleted = { fakeSettings.setSetupCompleted(it) },
            scope = scope,
        )
    }

    // ----- initial state tests -----

    @Test
    fun firstLaunchNoFixturesStartsAtSetup() = runTest {
        val manager = createManager(this)
        advanceUntilIdle()

        val screen = manager.currentScreen.first()
        assertIs<AppScreen.Setup>(screen)
    }

    @Test
    fun repeatLaunchWithFixturesStartsAtStage() = runTest {
        val fakeFixtures = FakeFixtureRepository()
        fakeFixtures.fixtures.value = listOf(fixture())

        val manager = createManager(this, fakeFixtures = fakeFixtures)
        advanceUntilIdle()

        val screen = manager.currentScreen.first()
        assertIs<AppScreen.Stage>(screen)
    }

    // ----- completeSetup -----

    @Test
    fun completeSetupNavigatesToStage() = runTest {
        val fakeSettings = FakeSettingsRepository()
        val manager = createManager(this, fakeSettings = fakeSettings)
        advanceUntilIdle()

        // Starts at Setup
        assertIs<AppScreen.Setup>(manager.currentScreen.first())

        manager.completeSetup()
        advanceUntilIdle()

        assertIs<AppScreen.Stage>(manager.currentScreen.first())
    }

    // ----- navigateTo / navigateBack -----

    @Test
    fun navigateToSettingsFromStage() = runTest {
        val fakeFixtures = FakeFixtureRepository()
        fakeFixtures.fixtures.value = listOf(fixture())
        val manager = createManager(this, fakeFixtures = fakeFixtures)
        advanceUntilIdle()

        manager.navigateTo(AppScreen.Settings)

        assertEquals(AppScreen.Settings, manager.currentScreen.first())
    }

    @Test
    fun navigateBackFromSettingsReturnsToStage() = runTest {
        val fakeFixtures = FakeFixtureRepository()
        fakeFixtures.fixtures.value = listOf(fixture())
        val manager = createManager(this, fakeFixtures = fakeFixtures)
        advanceUntilIdle()

        manager.navigateTo(AppScreen.Settings)
        manager.navigateBack()

        assertIs<AppScreen.Stage>(manager.currentScreen.first())
    }

    @Test
    fun navigateToProvisioningFromSettingsThenBack() = runTest {
        val fakeFixtures = FakeFixtureRepository()
        fakeFixtures.fixtures.value = listOf(fixture())
        val manager = createManager(this, fakeFixtures = fakeFixtures)
        advanceUntilIdle()

        // Stage -> Settings -> Provisioning
        manager.navigateTo(AppScreen.Settings)
        manager.navigateTo(AppScreen.Provisioning)

        assertIs<AppScreen.Provisioning>(manager.currentScreen.first())

        // Back -> Settings
        manager.navigateBack()
        assertIs<AppScreen.Settings>(manager.currentScreen.first())

        // Back -> Stage
        manager.navigateBack()
        assertIs<AppScreen.Stage>(manager.currentScreen.first())
    }

    @Test
    fun navigateBackWithEmptyStackDoesNothing() = runTest {
        val fakeFixtures = FakeFixtureRepository()
        fakeFixtures.fixtures.value = listOf(fixture())
        val manager = createManager(this, fakeFixtures = fakeFixtures)
        advanceUntilIdle()

        // Already at Stage with empty back-stack
        manager.navigateBack()

        // Should stay at Stage
        assertIs<AppScreen.Stage>(manager.currentScreen.first())
    }

    @Test
    fun completeSetupClearsBackStack() = runTest {
        val fakeSettings = FakeSettingsRepository()
        val manager = createManager(this, fakeSettings = fakeSettings)
        advanceUntilIdle()

        // Setup -> navigate to Settings (builds a back-stack entry)
        manager.navigateTo(AppScreen.Settings)
        assertEquals(AppScreen.Settings, manager.currentScreen.first())

        // Now complete setup — should clear back stack and go to Stage
        manager.completeSetup()
        advanceUntilIdle()

        assertIs<AppScreen.Stage>(manager.currentScreen.first())

        // navigateBack should stay at Stage since back-stack was cleared
        manager.navigateBack()
        assertIs<AppScreen.Stage>(manager.currentScreen.first())
    }
}
