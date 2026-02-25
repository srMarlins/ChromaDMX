package com.chromadmx.agent.controller

import com.chromadmx.core.model.Fixture3D
import com.chromadmx.engine.effect.EffectStack
import com.chromadmx.engine.pipeline.EffectEngine
import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.output.DmxOutputService
import com.chromadmx.tempo.clock.BeatClock

/**
 * Real [StateController] bridging to the engine, tempo, and networking modules.
 *
 * Provides read-only snapshots of the current system state.
 */
class RealStateController(
    private val effectEngine: EffectEngine,
    private val beatClock: BeatClock,
    private val nodeDiscovery: NodeDiscovery,
    private val dmxOutputService: DmxOutputService,
    private val fixturesProvider: () -> List<Fixture3D> = { emptyList() },
) : StateController {

    override fun getEngineState(): EngineStateSnapshot {
        val stack = effectEngine.effectStack
        return EngineStateSnapshot(
            isRunning = effectEngine.isRunning,
            layerCount = stack.layerCount,
            masterDimmer = stack.masterDimmer,
            fixtureCount = fixturesProvider().size,
            fps = 0f, // TODO: Wire frame timing measurement from engine
            effectIds = stack.layers
                .filter { it.enabled }
                .map { it.effect.id }
        )
    }

    override fun getBeatState(): BeatStateSnapshot {
        val state = beatClock.beatState.value
        return BeatStateSnapshot(
            bpm = state.bpm,
            beatPhase = state.beatPhase,
            barPhase = state.barPhase,
            isRunning = beatClock.isRunning.value,
            source = beatClock::class.simpleName ?: "Unknown"
        )
    }

    override fun getNetworkState(): NetworkStateSnapshot {
        val nodes = nodeDiscovery.nodeList
        val totalUniverses = nodes.flatMap { it.universes }.distinct().size
        return NetworkStateSnapshot(
            nodeCount = nodes.size,
            totalUniverses = totalUniverses,
            isOutputActive = dmxOutputService.isRunning,
            protocol = "Art-Net",
            frameRate = if (dmxOutputService.isRunning) DmxOutputService.DEFAULT_FRAME_RATE_HZ else 0
        )
    }
}
