package com.chromadmx.agent.controller

import kotlinx.serialization.Serializable

/**
 * Abstraction for querying current system state for agent tools.
 */
interface StateController {
    /** Snapshot of the current effect engine state. */
    fun getEngineState(): EngineStateSnapshot

    /** Snapshot of the current beat/tempo state. */
    fun getBeatState(): BeatStateSnapshot

    /** Snapshot of the current network/DMX output state. */
    fun getNetworkState(): NetworkStateSnapshot
}

/**
 * Summary of the effect engine's current state.
 */
@Serializable
data class EngineStateSnapshot(
    val isRunning: Boolean,
    val layerCount: Int,
    val masterDimmer: Float,
    val fixtureCount: Int,
    val fps: Float,
    val effectIds: List<String>
)

/**
 * Summary of the current beat/tempo state.
 */
@Serializable
data class BeatStateSnapshot(
    val bpm: Float,
    val beatPhase: Float,
    val barPhase: Float,
    val isRunning: Boolean,
    val source: String
)

/**
 * Summary of the current network/DMX output state.
 */
@Serializable
data class NetworkStateSnapshot(
    val nodeCount: Int,
    val totalUniverses: Int,
    val isOutputActive: Boolean,
    val protocol: String,
    val frameRate: Int
)
