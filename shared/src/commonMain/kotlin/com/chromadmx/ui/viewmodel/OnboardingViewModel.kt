package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.pregen.PreGenerationService
import com.chromadmx.core.model.Genre
import com.chromadmx.core.persistence.FileStorage
import com.chromadmx.engine.preset.PresetLibrary
import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.core.model.DmxNode
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import com.chromadmx.ui.onboarding.OnboardingStep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Genre entry for the Vibe Check step.
 *
 * @property id Machine-readable genre key (e.g. "techno").
 * @property displayName Human-readable label.
 * @property color ARGB color int used for the tile accent.
 * @property genre The core [Genre] enum value for preset filtering.
 */
data class GenreOption(
    val id: String,
    val displayName: String,
    val color: Long,
    val genre: Genre? = null,
)

/**
 * ViewModel managing the 5-step onboarding flow.
 *
 * Owns the current [OnboardingStep], orchestrates network discovery,
 * simulated fixture loading, genre selection, preset generation via
 * [PresetLibrary], and persists the "first launch" flag via [FileStorage].
 *
 * On repeat launches, [performRepeatLaunchCheck] runs a quick network
 * health check and tracks node changes for mascot alerts.
 *
 * @param scope Coroutine scope for async work.
 * @param nodeDiscovery Art-Net node discovery service.
 * @param fileStorage Persistent key-value storage.
 * @param presetLibrary Preset library for listing/loading genre presets.
 * @param preGenService Optional pre-generation service for batch scene creation.
 */
class OnboardingViewModel(
    private val scope: CoroutineScope,
    private val nodeDiscovery: NodeDiscovery,
    private val fileStorage: FileStorage,
    private val presetLibrary: PresetLibrary? = null,
    private val preGenService: PreGenerationService? = null,
) {
    // -- Current step --

    private val _currentStep = MutableStateFlow<OnboardingStep>(OnboardingStep.Splash)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    // -- Network discovery state --

    private val _discoveredNodes = MutableStateFlow<List<DmxNode>>(emptyList())
    val discoveredNodes: StateFlow<List<DmxNode>> = _discoveredNodes.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // -- Simulation state --

    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _selectedRigPreset = MutableStateFlow(RigPreset.SMALL_DJ)
    val selectedRigPreset: StateFlow<RigPreset> = _selectedRigPreset.asStateFlow()

    private val _simulationFixtureCount = MutableStateFlow(0)
    val simulationFixtureCount: StateFlow<Int> = _simulationFixtureCount.asStateFlow()

    private val _fixturesLoaded = MutableStateFlow(0)
    val fixturesLoaded: StateFlow<Int> = _fixturesLoaded.asStateFlow()

    // -- Vibe Check state --

    private val _selectedGenre = MutableStateFlow<GenreOption?>(null)
    val selectedGenre: StateFlow<GenreOption?> = _selectedGenre.asStateFlow()

    /**
     * All eight genre options matching the core [Genre] enum.
     * Includes Techno, House, DnB, Ambient, Hip-Hop, Pop, Rock, and Custom.
     */
    val genres: List<GenreOption> = listOf(
        GenreOption("techno", "Techno", 0xFFFF0040, Genre.TECHNO),
        GenreOption("house", "House", 0xFFFF8800, Genre.HOUSE),
        GenreOption("dnb", "Drum & Bass", 0xFFFFFF00, Genre.DNB),
        GenreOption("ambient", "Ambient", 0xFF0044FF, Genre.AMBIENT),
        GenreOption("hiphop", "Hip-Hop", 0xFFFF00FF, Genre.HIPHOP),
        GenreOption("pop", "Pop", 0xFF00FFAA, Genre.POP),
        GenreOption("rock", "Rock", 0xFFFF5500, Genre.ROCK),
        GenreOption("custom", "Custom", 0xFF6C63FF, Genre.CUSTOM),
    )

    // -- Repeat launch state --

    private val _networkChanged = MutableStateFlow(false)
    val networkChanged: StateFlow<Boolean> = _networkChanged.asStateFlow()

    private val _repeatLaunchComplete = MutableStateFlow(false)
    val repeatLaunchComplete: StateFlow<Boolean> = _repeatLaunchComplete.asStateFlow()

    // -- Preset count from vibe check --

    private val _matchingPresetCount = MutableStateFlow(0)
    val matchingPresetCount: StateFlow<Int> = _matchingPresetCount.asStateFlow()

    // -- Generation state --

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationProgress = MutableStateFlow(0f)
    val generationProgress: StateFlow<Float> = _generationProgress.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()

    // -- Internal jobs --

    private var splashJob: Job? = null
    private var scanJob: Job? = null
    private var settleJob: Job? = null
    private var fixtureLoadJob: Job? = null
    private var stagePreviewJob: Job? = null
    private var repeatLaunchJob: Job? = null
    private var generationJob: Job? = null

    // -- Persistent storage --

    /**
     * Check whether this is the first app launch.
     * Returns true if no "onboarding_complete" flag is found.
     */
    fun isFirstLaunch(): Boolean {
        val value = fileStorage.readFile(PREFS_PATH)
        return value != "true"
    }

    /**
     * Mark onboarding as complete so it won't show on next launch.
     */
    private fun markOnboardingComplete() {
        fileStorage.mkdirs("prefs")
        fileStorage.saveFile(PREFS_PATH, "true")
    }

    /**
     * Save the last known node IP addresses for repeat launch comparison.
     * Limits persisted nodes to [MAX_PERSISTED_NODES] to prevent DoS via
     * fake node flooding. Validates IP address format before persisting.
     */
    private fun saveLastKnownNodes(nodes: List<DmxNode>) {
        val nodeIps = nodes
            .take(MAX_PERSISTED_NODES)
            .map { it.ipAddress }
            .filter { isValidIpAddress(it) }
            .joinToString(",")
        fileStorage.mkdirs("prefs")
        fileStorage.saveFile(NODES_PATH, nodeIps)
    }

    /**
     * Read the last known node IP addresses from persistent storage.
     */
    fun getLastKnownNodes(): List<String> {
        val content = fileStorage.readFile(NODES_PATH) ?: return emptyList()
        return content.split(",").filter { it.isNotBlank() }
    }

    /**
     * Save the selected rig preset name for repeat launches.
     */
    private fun saveSelectedRigPreset(preset: RigPreset) {
        fileStorage.mkdirs("prefs")
        fileStorage.saveFile(RIG_PRESET_PATH, preset.name)
    }

    /**
     * Read the previously selected rig preset, if any.
     */
    fun getSavedRigPreset(): RigPreset? {
        val name = fileStorage.readFile(RIG_PRESET_PATH) ?: return null
        return RigPreset.entries.find { it.name == name }
    }

    // -- Step management --

    /**
     * Begin the onboarding flow from the Splash step.
     * Starts the auto-advance timer.
     */
    fun start() {
        _currentStep.value = OnboardingStep.Splash
        startSplashTimer()
    }

    /**
     * Advance to the next step in the linear flow.
     * If at VibeCheck, marks onboarding complete and moves to StagePreview.
     */
    fun advance() {
        val steps = OnboardingStep.steps
        val currentIndex = steps.indexOf(_currentStep.value)
        if (currentIndex < 0) return

        if (currentIndex < steps.lastIndex) {
            val nextStep = steps[currentIndex + 1]
            _currentStep.value = nextStep
            onStepEntered(nextStep)
        }
    }

    /**
     * Skip directly to Complete (e.g. from VibeCheck "Skip" button).
     */
    fun skipToComplete() {
        markOnboardingComplete()
        persistUserChoices()
        _currentStep.value = OnboardingStep.Complete
    }

    /**
     * Called when a step is entered; starts step-specific logic.
     */
    private fun onStepEntered(step: OnboardingStep) {
        when (step) {
            is OnboardingStep.Splash -> startSplashTimer()
            is OnboardingStep.NetworkDiscovery -> startNetworkScan()
            is OnboardingStep.FixtureScan -> startFixtureLoad()
            is OnboardingStep.VibeCheck -> { /* wait for user selection */ }
            is OnboardingStep.StagePreview -> {
                markOnboardingComplete()
                persistUserChoices()
                startStagePreviewTimer()
            }
            is OnboardingStep.Complete -> {
                /* Already marked complete in StagePreview or skipToComplete */
            }
        }
    }

    /**
     * Persist user choices (selected nodes, rig preset) for repeat launches.
     */
    private fun persistUserChoices() {
        val nodes = _discoveredNodes.value
        if (nodes.isNotEmpty()) {
            saveLastKnownNodes(nodes)
        }
        if (_isSimulationMode.value) {
            saveSelectedRigPreset(_selectedRigPreset.value)
        }
    }

    // -- Splash --

    private fun startSplashTimer() {
        splashJob?.cancel()
        splashJob = scope.launch {
            delay(SPLASH_DURATION_MS)
            if (_currentStep.value is OnboardingStep.Splash) {
                advance()
            }
        }
    }

    // -- Network Discovery --

    private fun startNetworkScan() {
        _isScanning.value = true
        _discoveredNodes.value = emptyList()

        scanJob?.cancel()
        scanJob = scope.launch {
            // Start actual discovery
            nodeDiscovery.start()

            // Collect discovered nodes for the scan duration
            val collectJob = launch {
                nodeDiscovery.nodes.collect { nodeMap ->
                    _discoveredNodes.value = nodeMap.values.toList()
                }
            }

            // Wait for scan period
            delay(SCAN_DURATION_MS)
            _isScanning.value = false

            // If nodes were found, wait a settle period then auto-advance
            val nodes = _discoveredNodes.value
            if (nodes.isNotEmpty()) {
                settleJob = launch {
                    delay(NODE_SETTLE_MS)
                    if (_currentStep.value is OnboardingStep.NetworkDiscovery) {
                        advance()
                    }
                }
            }
            // If no nodes found, user must tap "Simulation" or "Try Again"
        }
    }

    /**
     * Enter simulation mode (called when user taps "Virtual Stage").
     */
    fun enterSimulationMode() {
        nodeDiscovery.stop()
        scanJob?.cancel()
        settleJob?.cancel()
        _isScanning.value = false
        _isSimulationMode.value = true
        advance()
    }

    /**
     * Retry the network scan.
     */
    fun retryNetworkScan() {
        nodeDiscovery.stop()
        scanJob?.cancel()
        settleJob?.cancel()
        startNetworkScan()
    }

    // -- Fixture Scan --

    fun selectRigPreset(preset: RigPreset) {
        _selectedRigPreset.value = preset
        val rig = SimulatedFixtureRig(preset)
        _simulationFixtureCount.value = rig.fixtureCount
    }

    private fun startFixtureLoad() {
        if (_isSimulationMode.value) {
            val rig = SimulatedFixtureRig(_selectedRigPreset.value)
            _simulationFixtureCount.value = rig.fixtureCount
            _fixturesLoaded.value = 0

            fixtureLoadJob?.cancel()
            fixtureLoadJob = scope.launch {
                // Animate fixtures appearing one by one
                for (i in 1..rig.fixtureCount) {
                    if (!isActive) break
                    delay(FIXTURE_POP_DELAY_MS)
                    _fixturesLoaded.value = i
                }
            }
        }
        // For real hardware: placeholder, no-op for now
    }

    // -- Vibe Check --

    fun selectGenre(genre: GenreOption) {
        _selectedGenre.value = genre
        // Query preset library for matching presets
        if (presetLibrary != null && genre.genre != null) {
            val matching = presetLibrary.listPresets(genre.genre)
            _matchingPresetCount.value = matching.size
        }
    }

    /**
     * Confirm genre selection and trigger scene generation before advancing.
     */
    fun confirmGenre() {
        startGeneration()
    }

    /**
     * Trigger batch scene generation for the selected genre.
     *
     * If no [PreGenerationService] is available, sets an error message
     * and advances immediately (falling back to universal presets).
     * On success or failure, advances to the next step.
     */
    fun startGeneration() {
        val genre = _selectedGenre.value ?: return
        val service = preGenService ?: run {
            _generationError.value = "Agent unavailable \u2014 using universal presets"
            advance()
            return
        }
        _isGenerating.value = true
        _generationError.value = null
        generationJob?.cancel()
        generationJob = scope.launch {
            val progressJob = launch {
                service.progress.collect { p ->
                    if (p.total > 0) {
                        _generationProgress.value = p.current.toFloat() / p.total
                    }
                }
            }
            try {
                service.generate(genre.id, DEFAULT_SCENE_COUNT)
                _generationProgress.value = 1f
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _generationError.value = "Generation failed \u2014 using universal presets"
                }
            } finally {
                progressJob.cancel()
                _isGenerating.value = false
            }
            advance()
        }
    }

    // -- Stage Preview --

    private fun startStagePreviewTimer() {
        stagePreviewJob?.cancel()
        stagePreviewJob = scope.launch {
            delay(STAGE_PREVIEW_DURATION_MS)
            if (_currentStep.value is OnboardingStep.StagePreview) {
                advance()
            }
        }
    }

    /**
     * Skip the stage preview auto-advance and go to Complete immediately.
     */
    fun skipStagePreview() {
        stagePreviewJob?.cancel()
        advance()
    }

    // -- Repeat Launch Logic --

    /**
     * Called on repeat launches (not first launch).
     * Performs a quick network health check and compares against
     * last known nodes. Sets [networkChanged] if nodes differ.
     *
     * Returns the list of currently discovered node IPs when the
     * check completes.
     */
    fun performRepeatLaunchCheck() {
        repeatLaunchJob?.cancel()
        repeatLaunchJob = scope.launch {
            val previousNodes = getLastKnownNodes()

            // Quick scan
            nodeDiscovery.start()
            delay(REPEAT_SCAN_DURATION_MS)
            nodeDiscovery.stop()

            val currentNodes = nodeDiscovery.nodes.value.values.map { it.ipAddress }

            // Detect changes
            val changed = previousNodes.toSet() != currentNodes.toSet()
            _networkChanged.value = changed

            // Save new state
            if (currentNodes.isNotEmpty()) {
                saveLastKnownNodes(nodeDiscovery.nodes.value.values.toList())
            }

            _repeatLaunchComplete.value = true
        }
    }

    // -- Cleanup --

    fun onCleared() {
        splashJob?.cancel()
        scanJob?.cancel()
        settleJob?.cancel()
        fixtureLoadJob?.cancel()
        stagePreviewJob?.cancel()
        repeatLaunchJob?.cancel()
        generationJob?.cancel()
        nodeDiscovery.stop()
    }

    /**
     * Validate that a string looks like a valid IPv4 address (e.g. "192.168.1.1").
     * Rejects strings that are too long or don't match the expected format.
     */
    private fun isValidIpAddress(ip: String): Boolean {
        if (ip.length > 15) return false // max IPv4 length "XXX.XXX.XXX.XXX"
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            val num = part.toIntOrNull() ?: return false
            num in 0..255
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

        /** Preferences file path for first-launch flag. */
        const val PREFS_PATH = "prefs/onboarding.txt"

        /** File path for last known nodes. */
        const val NODES_PATH = "prefs/last_known_nodes.txt"

        /** File path for selected rig preset. */
        const val RIG_PRESET_PATH = "prefs/rig_preset.txt"

        /** Default number of scenes to generate per genre. */
        const val DEFAULT_SCENE_COUNT = 10
    }
}
