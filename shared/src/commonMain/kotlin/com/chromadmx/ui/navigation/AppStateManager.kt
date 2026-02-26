package com.chromadmx.ui.navigation

import com.chromadmx.core.model.Fixture3D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manages app-level navigation between the 4 main screens.
 *
 * Init logic: if fixtures exist in the repository, start at Stage.
 * Otherwise start at Setup.
 *
 * The primary constructor accepts function references so that both real
 * repositories and test fakes can be supplied without shared interfaces.
 */
class AppStateManager(
    private val allFixtures: () -> Flow<List<Fixture3D>>,
    private val setSetupCompleted: suspend (Boolean) -> Unit,
    private val scope: CoroutineScope,
) {
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Setup)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val backStack = mutableListOf<AppScreen>()

    // Legacy bridge: kept in sync manually (no background collector).
    private val _legacyState = MutableStateFlow<AppState>(AppState.Onboarding)

    /**
     * Bridge: exposes current navigation as a [StateFlow] of [AppState]
     * for legacy composables that still expect [AppState].
     * Will be removed when [AppState] is deleted.
     */
    val currentState: StateFlow<AppState> = _legacyState.asStateFlow()

    init {
        scope.launch {
            val fixtures = allFixtures().first()
            if (fixtures.isNotEmpty()) {
                setScreen(AppScreen.Stage)
            }
        }
    }

    /**
     * Convenience constructor that extracts the needed functions from
     * [com.chromadmx.core.persistence.SettingsRepository] and
     * [com.chromadmx.core.persistence.FixtureRepository].
     */
    constructor(
        settingsRepository: com.chromadmx.core.persistence.SettingsRepository,
        fixtureRepository: com.chromadmx.core.persistence.FixtureRepository,
        scope: CoroutineScope,
    ) : this(
        allFixtures = { fixtureRepository.allFixtures() },
        setSetupCompleted = { settingsRepository.setSetupCompleted(it) },
        scope = scope,
    )

    // ----- Internal helper -----

    private fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
        _legacyState.value = screen.toAppState()
    }

    // ----- New AppScreen-based API -----

    fun navigateTo(screen: AppScreen) {
        backStack.add(_currentScreen.value)
        setScreen(screen)
    }

    fun navigateBack() {
        val previous = backStack.removeLastOrNull() ?: return
        setScreen(previous)
    }

    fun completeSetup() {
        backStack.clear()
        setScreen(AppScreen.Stage)
        scope.launch {
            setSetupCompleted(true)
        }
    }

    // ----- Legacy AppState-based API (for ChromaDmxApp.kt) -----
    // These will be removed when AppState.kt is deleted.

    /**
     * Legacy constructor for code that passes [isFirstLaunch] directly.
     * Uses an empty fixture flow and a no-op settings callback.
     */
    @Suppress("unused")
    constructor(isFirstLaunch: Boolean) : this(
        allFixtures = {
            kotlinx.coroutines.flow.flowOf(
                if (isFirstLaunch) emptyList() else listOf(
                    Fixture3D(
                        fixture = com.chromadmx.core.model.Fixture(
                            fixtureId = "_legacy",
                            name = "_legacy",
                            channelStart = 0,
                            channelCount = 0,
                            universeId = 0,
                        ),
                        position = com.chromadmx.core.model.Vec3.ZERO,
                    )
                )
            )
        },
        setSetupCompleted = {},
        scope = CoroutineScope(kotlinx.coroutines.SupervisorJob()),
    )

    /**
     * Legacy navigation accepting [AppState].
     */
    fun navigateTo(state: AppState) {
        navigateTo(state.toAppScreen())
    }

    /**
     * Legacy alias for [completeSetup].
     */
    fun completeOnboarding() {
        completeSetup()
    }
}

// ----- Mapping helpers -----

private fun AppScreen.toAppState(): AppState = when (this) {
    AppScreen.Setup -> AppState.Onboarding
    AppScreen.Stage -> AppState.StagePreview
    AppScreen.Settings -> AppState.Settings
    AppScreen.Provisioning -> AppState.BleProvisioning
}

private fun AppState.toAppScreen(): AppScreen = when (this) {
    AppState.Onboarding -> AppScreen.Setup
    AppState.StagePreview -> AppScreen.Stage
    AppState.Settings -> AppScreen.Settings
    AppState.BleProvisioning -> AppScreen.Provisioning
    is AppState.RigSelection -> AppScreen.Settings // Best available mapping
}
