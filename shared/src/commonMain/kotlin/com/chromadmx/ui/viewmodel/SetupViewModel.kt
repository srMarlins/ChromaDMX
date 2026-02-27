package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.pregen.PreGenerationService
import com.chromadmx.core.model.Genre
import com.chromadmx.core.model.toKnownNode
import com.chromadmx.core.persistence.FixtureStore
import com.chromadmx.core.persistence.NetworkStateStore
import com.chromadmx.core.persistence.SettingsStore
import com.chromadmx.networking.FixtureDiscovery
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import com.chromadmx.simulation.vision.SimulatedScanRunner
import com.chromadmx.vision.calibration.ScanState
import com.chromadmx.ui.state.GenreOption
import com.chromadmx.ui.state.SetupEvent
import com.chromadmx.ui.state.SetupStep
import com.chromadmx.ui.state.SetupUiState
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
 * @param preGenerationService Service for batch-generating genre presets.
 * @param scope Coroutine scope for async work.
 */
class SetupViewModel(
    private val fixtureDiscovery: FixtureDiscovery,
    private val fixtureStore: FixtureStore,
    private val settingsStore: SettingsStore,
    private val networkStateRepository: NetworkStateStore? = null,
    private val preGenerationService: PreGenerationService? = null,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(
        SetupUiState(
            availableGenres = GENRES.toImmutableList(),
        )
    )
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    init {
        // Auto-start scan
        startScan()

        // Auto-advance from SPLASH after delay
        scope.launch {
            delay(SPLASH_DURATION_MS)
            if (_state.value.currentStep == SetupStep.SPLASH) {
                advance()
            }
        }

        // Collect discovered nodes into UI state
        scope.launch {
            fixtureDiscovery.discoveredNodes.collect { nodes ->
                _state.update { it.copy(discoveredNodes = nodes.toImmutableList()) }
            }
        }
        scope.launch {
            fixtureDiscovery.isScanning.collect { scanning ->
                _state.update { it.copy(isScanning = scanning) }
            }
        }

        // Collect generation progress into UI state
        preGenerationService?.let { service ->
            scope.launch {
                service.progress.collect { progress ->
                    val fraction = if (progress.total > 0) {
                        progress.current.toFloat() / progress.total.toFloat()
                    } else {
                        0f
                    }
                    _state.update {
                        it.copy(
                            isGenerating = progress.isRunning,
                            generationProgress = fraction,
                            matchingPresetCount = progress.current,
                        )
                    }
                }
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
            is SetupEvent.StartFixtureScan -> startFixtureScan()
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
        scanJob = scope.launch {
            delay(SCAN_DURATION_MS)
            if (_state.value.discoveredNodes.isEmpty()) {
                fixtureDiscovery.stopScan()
            }
        }
    }

    /**
     * Enter simulation mode: stop scanning, set flag, jump to fixture scan.
     */
    private fun enterSimulationMode() {
        fixtureDiscovery.stopScan()
        scanJob?.cancel()
        val rig = SimulatedFixtureRig(_state.value.selectedRigPreset)
        _state.update {
            it.copy(
                currentStep = SetupStep.FIXTURE_SCAN,
                isSimulationMode = true,
                isScanning = false,
                simulationFixtureCount = rig.fixtureCount,
            )
        }
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
     * Confirm genre selection, launch preset generation, and advance to the next step.
     *
     * Generation runs asynchronously while the Stage Preview step is shown,
     * so the user sees progress feedback as presets are created.
     */
    private fun confirmGenre() {
        val genre = _state.value.selectedGenre
        if (genre != null && preGenerationService != null) {
            val genreName = genre.genre?.name ?: genre.id
            scope.launch {
                try {
                    preGenerationService.generate(genreName, count = GENRE_PRESET_COUNT)
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            isGenerating = false,
                            generationError = e.message ?: "Generation failed",
                        )
                    }
                }
            }
        }
        advance()
    }

    // -- Stage Preview --

    /**
     * Skip the stage preview and advance to COMPLETE.
     */
    private fun skipStagePreview() {
        advance()
    }

    // -- Fixture Scan --

    private fun startFixtureScan() {
        if (!_state.value.isSimulationMode) return
        if (_state.value.selectedRigPreset != RigPreset.PIXEL_BAR_V) return

        _state.update { it.copy(isScanningFixtures = true) }

        scope.launch {
            val runner = SimulatedScanRunner()

            // Collect active fixtures for flash animation
            val flashJob = scope.launch {
                runner.activeFixtures.collect { active ->
                    _state.update { it.copy(scanActiveFixtures = active.toImmutableSet()) }
                }
            }

            try {
                val result = runner.runScan()
                _state.update {
                    it.copy(
                        isScanningFixtures = false,
                        scanComplete = result != null,
                        scanActiveFixtures = persistentSetOf(),
                    )
                }
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        isScanningFixtures = false,
                        scanComplete = false,
                        scanActiveFixtures = persistentSetOf(),
                    )
                }
            } finally {
                flashJob.cancel()
            }
        }
    }

    // -- Persistence --

    /**
     * Mark setup as complete, persist simulation fixtures if in sim mode,
     * and save settings.
     */
    private fun persistSetupComplete() {
        scope.launch {
            withContext(Dispatchers.IO) {
                settingsStore.setSetupCompleted(true)

                if (_state.value.isSimulationMode) {
                    settingsStore.setIsSimulation(true)

                    // Save simulated fixtures
                    val rig = SimulatedFixtureRig(_state.value.selectedRigPreset)
                    fixtureStore.saveAll(rig.fixtures)
                }

                // Persist the current node topology for comparison on next launch
                val currentNodes = _state.value.discoveredNodes
                if (networkStateRepository != null && currentNodes.isNotEmpty()) {
                    val knownNodes = currentNodes.take(MAX_PERSISTED_NODES).map { it.toKnownNode() }
                    networkStateRepository.saveKnownNodes(knownNodes)
                }
            }
        }
    }

    // -- Repeat Launch --

    /**
     * Perform a topology comparison on repeat launches.
     *
     * Waits for the quick scan to settle, then compares discovered nodes
     * against the persisted known nodes. If nodes were added or removed,
     * sets [SetupUiState.networkChangedSinceLastLaunch] so the caller
     * (typically [ChromaDmxApp]) can trigger a mascot alert.
     *
     * When no [NetworkStateRepository] is available the check completes
     * immediately without flagging any change.
     */
    private fun performRepeatLaunchCheck() {
        scope.launch {
            if (networkStateRepository == null) {
                _state.update { it.copy(repeatLaunchCheckComplete = true) }
                return@launch
            }

            // Let the quick scan settle before comparing
            delay(REPEAT_SCAN_DURATION_MS)
            fixtureDiscovery.stopScan()

            val currentNodes = _state.value.discoveredNodes
            val diff = networkStateRepository.detectTopologyChanges(currentNodes)
            val changed = diff.newNodes.isNotEmpty() || diff.lostNodes.isNotEmpty()

            _state.update {
                it.copy(
                    networkChangedSinceLastLaunch = changed,
                    addedNodeCount = diff.newNodes.size,
                    removedNodeCount = diff.lostNodes.size,
                    repeatLaunchCheckComplete = true,
                )
            }

            // Update the persisted known nodes with the current scan result
            val knownNodes = currentNodes.take(MAX_PERSISTED_NODES).map { it.toKnownNode() }
            networkStateRepository.saveKnownNodes(knownNodes)
        }
    }

    fun onCleared() {
        scope.coroutineContext[Job]?.cancel()
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

        /** Number of presets to generate per genre. */
        const val GENRE_PRESET_COUNT = 4

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
