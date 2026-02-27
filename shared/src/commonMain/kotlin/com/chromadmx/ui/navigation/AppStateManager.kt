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

    init {
        scope.launch {
            val fixtures = allFixtures().first()
            if (fixtures.isNotEmpty()) {
                _currentScreen.value = AppScreen.Stage
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

    fun navigateTo(screen: AppScreen) {
        backStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateBack() {
        val previous = backStack.removeLastOrNull() ?: return
        _currentScreen.value = previous
    }

    fun completeSetup() {
        backStack.clear()
        _currentScreen.value = AppScreen.Stage
        scope.launch {
            setSetupCompleted(true)
        }
    }
}
