package com.chromadmx.ui.state

import androidx.compose.runtime.Immutable
import com.chromadmx.core.model.Genre
import com.chromadmx.networking.model.DmxNode
import com.chromadmx.simulation.fixtures.RigPreset

/**
 * Genre entry for the Vibe Check step.
 */
@Immutable
data class GenreOption(
    val id: String,
    val displayName: String,
    val color: Long,
    val genre: Genre? = null,
)

/**
 * UI State for the Onboarding/Setup flow.
 */
@Immutable
data class SetupUiState(
    val currentStep: SetupStep = SetupStep.SPLASH,
    val discoveredNodes: List<DmxNode> = emptyList(),
    val isScanning: Boolean = false,
    val isSimulationMode: Boolean = false,
    val selectedRigPreset: RigPreset = RigPreset.SMALL_DJ,
    val simulationFixtureCount: Int = 0,
    val fixturesLoadedCount: Int = 0,
    val selectedGenre: GenreOption? = null,
    val availableGenres: List<GenreOption> = emptyList(),
    val matchingPresetCount: Int = 0,
    val isGenerating: Boolean = false,
    val generationProgress: Float = 0f,
    val generationError: String? = null,
    val networkChangedSinceLastLaunch: Boolean = false,
    val repeatLaunchCheckComplete: Boolean = false
)

/**
 * Steps in the setup flow.
 */
enum class SetupStep {
    SPLASH,
    NETWORK_DISCOVERY,
    FIXTURE_SCAN,
    VIBE_CHECK,
    STAGE_PREVIEW,
    COMPLETE
}

/**
 * Events for the Setup flow.
 */
sealed interface SetupEvent {
    data object Start : SetupEvent
    data object Advance : SetupEvent
    data object SkipToComplete : SetupEvent
    data object EnterSimulationMode : SetupEvent
    data object RetryNetworkScan : SetupEvent
    data class SelectRigPreset(val preset: RigPreset) : SetupEvent
    data class SelectGenre(val genre: GenreOption) : SetupEvent
    data object ConfirmGenre : SetupEvent
    data object SkipStagePreview : SetupEvent
    data object PerformRepeatLaunchCheck : SetupEvent
}
