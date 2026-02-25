package com.chromadmx.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import com.chromadmx.agent.controller.StateController
import com.chromadmx.core.model.BuiltInProfiles
import kotlinx.serialization.Serializable

class GetEngineStateTool(private val controller: StateController) : SimpleTool<GetEngineStateTool.Args>(
    argsSerializer = Args.serializer(),
    name = "getEngineState",
    description = "Get the current effect engine state: running status, active layers, fixture count, master dimmer, FPS, and active effect IDs."
) {
    @Serializable
    class Args

    override suspend fun execute(args: Args): String {
        val state = controller.getEngineState()
        val runStr = if (state.isRunning) "running" else "stopped"
        val profileList = BuiltInProfiles.all().joinToString(", ") { "${it.profileId} (${it.type})" }
        return "Engine: $runStr, ${state.layerCount} layers, " +
            "${state.fixtureCount} fixtures, dimmer=${state.masterDimmer}, " +
            "fps=${state.fps}. Active effects: ${state.effectIds.joinToString(", ").ifEmpty { "none" }}. " +
            "Available profiles: $profileList"
    }
}

class GetBeatStateTool(private val controller: StateController) : SimpleTool<GetBeatStateTool.Args>(
    argsSerializer = Args.serializer(),
    name = "getBeatState",
    description = "Get the current beat/tempo state: BPM, beat phase, bar phase, running status, and tempo source."
) {
    @Serializable
    class Args

    override suspend fun execute(args: Args): String {
        val state = controller.getBeatState()
        val runStr = if (state.isRunning) "running" else "stopped"
        return "Beat: $runStr, ${state.bpm} BPM, " +
            "beatPhase=${state.beatPhase}, barPhase=${state.barPhase}, " +
            "source=${state.source}"
    }
}

class GetNetworkStateTool(private val controller: StateController) : SimpleTool<GetNetworkStateTool.Args>(
    argsSerializer = Args.serializer(),
    name = "getNetworkState",
    description = "Get the current DMX network state: node count, universes, output status, protocol, and frame rate."
) {
    @Serializable
    class Args

    override suspend fun execute(args: Args): String {
        val state = controller.getNetworkState()
        val outputStr = if (state.isOutputActive) "active" else "inactive"
        return "Network: ${state.nodeCount} nodes, ${state.totalUniverses} universes, " +
            "output=$outputStr, protocol=${state.protocol}, frameRate=${state.frameRate}Hz"
    }
}
