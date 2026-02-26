package com.chromadmx.ui.navigation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Basic smoke tests for [AppStateManager] using the new [AppScreen] model.
 * More thorough scenarios are in [AppNavigationTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppStateManagerTest {

    private fun emptyFixtures() = MutableStateFlow<List<com.chromadmx.core.model.Fixture3D>>(emptyList())

    @Test
    fun defaultScreenIsSetupWhenNoFixtures() = runTest {
        val manager = AppStateManager(
            allFixtures = { emptyFixtures() },
            setSetupCompleted = {},
            scope = this,
        )
        advanceUntilIdle()
        assertIs<AppScreen.Setup>(manager.currentScreen.first())
    }

    @Test
    fun navigateToAndBack() = runTest {
        val manager = AppStateManager(
            allFixtures = { emptyFixtures() },
            setSetupCompleted = {},
            scope = this,
        )
        advanceUntilIdle()

        manager.navigateTo(AppScreen.Settings)
        assertEquals(AppScreen.Settings, manager.currentScreen.first())

        manager.navigateBack()
        assertIs<AppScreen.Setup>(manager.currentScreen.first())
    }

    @Test
    fun completeSetupGoesToStage() = runTest {
        val manager = AppStateManager(
            allFixtures = { emptyFixtures() },
            setSetupCompleted = {},
            scope = this,
        )
        advanceUntilIdle()

        manager.completeSetup()
        advanceUntilIdle()
        assertIs<AppScreen.Stage>(manager.currentScreen.first())
    }
}
