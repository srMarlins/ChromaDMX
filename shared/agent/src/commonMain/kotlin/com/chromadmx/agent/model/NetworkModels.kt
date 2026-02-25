package com.chromadmx.agent.model

import kotlinx.serialization.Serializable

/**
 * Status snapshot of a discovered DMX node.
 */
@Serializable
data class NodeStatusResult(
    val id: String,
    val name: String,
    val ip: String,
    val universes: List<Int>,
    val isOnline: Boolean,
    val firmwareVersion: String = "",
    val packetsSent: Long = 0L
)

/**
 * Result of a network diagnostic test on a DMX node.
 */
@Serializable
data class DiagnosticResult(
    val nodeId: String,
    val latencyMs: Float,
    val packetLossPercent: Float,
    val isReachable: Boolean,
    val details: String = ""
)
