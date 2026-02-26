package com.chromadmx.ui.viewmodel

import com.chromadmx.core.model.Genre
import com.chromadmx.core.persistence.FixtureStore
import com.chromadmx.core.persistence.SettingsStore
import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import com.chromadmx.ui.state.GenreOption
import com.chromadmx.ui.state.SetupEvent
import com.chromadmx.ui.state.SetupStep
import com.chromadmx.ui.state.SetupUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the setup/onboarding flow using the
 * Unidirectional Data Flow (UDF) pattern.
 *
 * Exposes a single [state] flow and a single [onEvent] entry point.
 * Dependencies use abstracted interfaces for testability:
 * [FixtureDiscovery], [FixtureStore], and [SettingsStore].
 *
 * Auto-starts fixture discovery on initialization and collects
 * discovered nodes into the UI state.
 *
 * @param fixtureDiscovery Abstracted discovery (real Art-Net or simulated).
 * @param fixtureStore Persistence for fixture data.
 * @param settingsStore Key-value settings persistence.
 * @param scope Coroutine scope for async work.
 */
class SetupViewModel(
    private val fixtureDiscovery: FixtureDiscovery,
    private val fixtureStore: FixtureStore,
    private val settingsStore: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(
        SetupUiState(
            availableGenres = GENRES,
        )
    )
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    init {
        // Auto-start scan
        startScan()

        // Collect discovered nodes into UI state
        scope.launch {
            fixtureDiscovery.discoveredNodes.collect { nodes ->
                _state.update { it.copy(discoveredNodes = nodes) }
            }
        }
        scope.launch {
            fixtureDiscovery.isScanning.collect { scanning ->
                _state.update { it.copy(isScanning = scanning) }
            }
        }
    }

    /**
     * Single entry point for all UI events.
     */
    fun onEvent(event: SetupEvent) {
        when (event) {
            is SetupEvent.Start -> { /* already started in init */ }
            is SetupEvent.Advance -> advance()
            is SetupEvent.SkipToComplete -> skipToComplete()
            is SetupEvent.EnterSimulationMode -> enterSimulationMode()
            is SetupEvent.RetryNetworkScan -> retryNetworkScan()
            is SetupEvent.SelectRigPreset -> selectRigPreset(event.preset)
            is SetupEvent.SelectGenre -> selectGenre(event.genre)
            is SetupEvent.ConfirmGenre -> confirmGenre()
            is SetupEvent.SkipStagePreview -> skipStagePreview()
            is SetupEvent.PerformRepeatLaunchCheck -> performRepeatLaunchCheck()
        }
    }

    // -- Step management --

    /**
     * Advance to the next step in the linear flow.
     * When entering STAGE_PREVIEW, persists setup state.
     * When entering COMPLETE from STAGE_PREVIEW, persists fixtures and settings.
     */
    private fun advance() {
        val steps = SetupStep.entries
        val currentIndex = steps.indexOf(_state.value.currentStep)
        if (currentIndex < 0 || currentIndex >= steps.lastIndex) return

        val nextStep = steps[currentIndex + 1]
        val currentStep = _state.value.currentStep

        // Persist when leaving STAGE_PREVIEW -> COMPLETE
        if (currentStep == SetupStep.STAGE_PREVIEW && nextStep == SetupStep.COMPLETE) {
            persistSetupComplete()
        }

        _state.update { it.copy(currentStep = nextStep) }
        onStepEntered(nextStep)
    }

    /**
     * Skip directly to COMPLETE, persisting settings.
     */
    private fun skipToComplete() {
        persistSetupComplete()
        _state.update { it.copy(currentStep = SetupStep.COMPLETE) }
    }

    /**
     * Called when a new step is entered; starts step-specific logic.
     */
    private fun onStepEntered(step: SetupStep) {
        when (step) {
            SetupStep.SPLASH -> { /* no-op, init handles scan */ }
            SetupStep.NETWORK_DISCOVERY -> { /* scan already running from init */ }
            SetupStep.FIXTURE_SCAN -> { /* wait for user to select rig */ }
            SetupStep.VIBE_CHECK -> { /* wait for user genre selection */ }
            SetupStep.STAGE_PREVIEW -> { /* preview shown */ }
            SetupStep.COMPLETE -> { /* done */ }
        }
    }

    // -- Network Discovery --

    private fun startScan() {
        scanJob?.cancel()
        fixtureDiscovery.startScan()
    }

    /**
     * Enter simulation mode: stop scanning, set flag, advance.
     */
    private fun enterSimulationMode() {
        fixtureDiscovery.stopScan()
        scanJob?.cancel()
        _state.update {
            it.copy(
                isSimulationMode = true,
                isScanning = false,
            )
        }
        advance()
    }

    /**
     * Retry the network scan by stopping then restarting.
     */
    private fun retryNetworkScan() {
        fixtureDiscovery.stopScan()
        scanJob?.cancel()
        startScan()
    }

    // -- Rig Preset --

    /**
     * Select a rig preset and update fixture count from the simulated rig.
     */
    private fun selectRigPreset(preset: RigPreset) {
        val rig = SimulatedFixtureRig(preset)
        _state.update {
            it.copy(
                selectedRigPreset = preset,
                simulationFixtureCount = rig.fixtureCount,
            )
        }
    }

    // -- Vibe Check --

    private fun selectGenre(genre: GenreOption) {
        _state.update { it.copy(selectedGenre = genre) }
    }

    /**
     * Confirm genre selection and advance to the next step.
     */
    private fun confirmGenre() {
        advance()
    }

    // -- Stage Preview --

    /**
     * Skip the stage preview and advance to COMPLETE.
     */
    private fun skipStagePreview() {
        advance()
    }

    // -- Persistence --

    /**
     * Mark setup as complete, persist simulation fixtures if in sim mode,
     * and save settings.
     */
    private fun persistSetupComplete() {
        scope.launch {
            settingsStore.setSetupCompleted(true)

            if (_state.value.isSimulationMode) {
                settingsStore.setIsSimulation(true)

                // Save simulated fixtures
                val rig = SimulatedFixtureRig(_state.value.selectedRigPreset)
                fixtureStore.saveAll(rig.fixtures)
            }
        }
    }

    // -- Repeat Launch --

    /**
     * Perform a quick network health check on repeat launches.
     * Sets [SetupUiState.repeatLaunchCheckComplete] when done.
     */
    private fun performRepeatLaunchCheck() {
        scope.launch {
            // Quick scan is already running from init, just mark check complete
            _state.update { it.copy(repeatLaunchCheckComplete = true) }
        }
    }

    companion object {
        /** Splash auto-advance delay. */
        const val SPLASH_DURATION_MS = 2_500L

        /** Network scan duration before showing results. */
        const val SCAN_DURATION_MS = 3_000L

        /** Settle time after nodes are found before auto-advancing. */
        const val NODE_SETTLE_MS = 3_000L

        /** Delay between each simulated fixture appearing. */
        const val FIXTURE_POP_DELAY_MS = 120L

        /** Stage preview auto-advance delay. */
        const val STAGE_PREVIEW_DURATION_MS = 2_000L

        /** Quick network scan duration on repeat launches. */
        const val REPEAT_SCAN_DURATION_MS = 2_000L

        /** Maximum number of nodes to persist (DoS prevention). */
        const val MAX_PERSISTED_NODES = 50

        /**
         * All eight genre options matching the core [Genre] enum.
         */
        val GENRES = listOf(
            GenreOption("techno", "Techno", 0xFFFF0040, Genre.TECHNO),
            GenreOption("house", "House", 0xFFFF8800, Genre.HOUSE),
            GenreOption("dnb", "Drum & Bass", 0xFFFFFF00, Genre.DNB),
            GenreOption("ambient", "Ambient", 0xFF0044FF, Genre.AMBIENT),
            GenreOption("hiphop", "Hip-Hop", 0xFFFF00FF, Genre.HIPHOP),
            GenreOption("pop", "Pop", 0xFF00FFAA, Genre.POP),
            GenreOption("rock", "Rock", 0xFFFF5500, Genre.ROCK),
            GenreOption("custom", "Custom", 0xFF6C63FF, Genre.CUSTOM),
        )
    }
}
