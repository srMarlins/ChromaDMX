package com.chromadmx.agent.controller

import com.chromadmx.agent.model.DiagnosticResult
import com.chromadmx.agent.model.NodeStatusResult

/**
 * Abstraction over the DMX networking layer for agent tool operations.
 */
interface NetworkController {
    /** Scan the network and return a summary of discovered nodes. */
    suspend fun scanNetwork(): List<NodeSummary>

    /** Get detailed status of a specific node. */
    suspend fun getNodeStatus(nodeId: String): NodeStatusResult?

    /** Configure a node's universe and start address. */
    suspend fun configureNode(nodeId: String, universe: Int, startAddress: Int): Boolean

    /** Run a diagnostic test on a node's connection. */
    suspend fun diagnoseConnection(nodeId: String): DiagnosticResult?
}

/**
 * Summary of a discovered DMX node for tool output.
 */
data class NodeSummary(
    val id: String,
    val ip: String,
    val name: String,
    val universes: List<Int>
)
