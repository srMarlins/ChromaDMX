package com.chromadmx.agent.tools

import com.chromadmx.agent.controller.StateController

/**
 * Tool: Get the current effect engine state.
 */
class GetEngineStateTool(private val controller: StateController) {

    fun execute(): String {
        val state = controller.getEngineState()
        val runStr = if (state.isRunning) "running" else "stopped"
        return "Engine: $runStr, ${state.layerCount} layers, " +
            "${state.fixtureCount} fixtures, dimmer=${state.masterDimmer}, " +
            "fps=${state.fps}. Active effects: ${state.effectIds.joinToString(", ").ifEmpty { "none" }}"
    }
}

/**
 * Tool: Get the current beat/tempo state.
 */
class GetBeatStateTool(private val controller: StateController) {

    fun execute(): String {
        val state = controller.getBeatState()
        val runStr = if (state.isRunning) "running" else "stopped"
        return "Beat: $runStr, ${state.bpm} BPM, " +
            "beatPhase=${state.beatPhase}, barPhase=${state.barPhase}, " +
            "source=${state.source}"
    }
}

/**
 * Tool: Get the current network/DMX output state.
 */
class GetNetworkStateTool(private val controller: StateController) {

    fun execute(): String {
        val state = controller.getNetworkState()
        val outputStr = if (state.isOutputActive) "active" else "inactive"
        return "Network: ${state.nodeCount} nodes, ${state.totalUniverses} universes, " +
            "output=$outputStr, protocol=${state.protocol}, frameRate=${state.frameRate}Hz"
    }
}
