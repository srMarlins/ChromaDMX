package com.chromadmx.ui.navigation

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AppStateManagerTest {
    @Test
    fun firstLaunchStartsWithOnboarding() = runTest {
        val manager = AppStateManager(isFirstLaunch = true)
        val state = manager.currentState.first()
        assertIs<AppState.Onboarding>(state)
    }

    @Test
    fun repeatLaunchStartsWithStagePreview() = runTest {
        val manager = AppStateManager(isFirstLaunch = false)
        val state = manager.currentState.first()
        assertIs<AppState.StagePreview>(state)
    }

    @Test
    fun navigateToSettings() = runTest {
        val manager = AppStateManager(isFirstLaunch = false)
        manager.navigateTo(AppState.Settings)
        assertEquals(AppState.Settings, manager.currentState.first())
    }

    @Test
    fun navigateBackFromSettings() = runTest {
        val manager = AppStateManager(isFirstLaunch = false)
        manager.navigateTo(AppState.Settings)
        manager.navigateBack()
        assertIs<AppState.StagePreview>(manager.currentState.first())
    }

    @Test
    fun completeOnboardingGoesToStagePreview() = runTest {
        val manager = AppStateManager(isFirstLaunch = true)
        manager.completeOnboarding()
        assertIs<AppState.StagePreview>(manager.currentState.first())
    }
}
