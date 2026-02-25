package com.chromadmx.agent.tools

import com.chromadmx.agent.controller.NetworkController
import kotlinx.serialization.Serializable

/**
 * Tool: Scan the network for DMX nodes.
 */
class ScanNetworkTool(private val controller: NetworkController) {

    suspend fun execute(): String {
        val nodes = controller.scanNetwork()
        if (nodes.isEmpty()) {
            return "Found 0 nodes on the network. Check that nodes are powered on and on the same subnet."
        }
        val listing = nodes.joinToString("\n") { node ->
            "  - ${node.name} (${node.ip}) [universes: ${node.universes.joinToString(",")}]"
        }
        return "Found ${nodes.size} nodes:\n$listing"
    }
}

/**
 * Tool: Get detailed status of a specific DMX node.
 */
class GetNodeStatusTool(private val controller: NetworkController) {
    @Serializable
    data class Args(val nodeId: String)

    suspend fun execute(args: Args): String {
        val status = controller.getNodeStatus(args.nodeId)
            ?: return "Node '${args.nodeId}' not found. Run scan first."
        val onlineStr = if (status.isOnline) "online" else "offline"
        return "Node '${status.name}' (${status.ip}): $onlineStr, " +
            "universes=${status.universes}, firmware=${status.firmwareVersion}, " +
            "packets_sent=${status.packetsSent}"
    }
}

/**
 * Tool: Configure a DMX node's universe and start address.
 */
class ConfigureNodeTool(private val controller: NetworkController) {
    @Serializable
    data class Args(val nodeId: String, val universe: Int, val startAddress: Int)

    suspend fun execute(args: Args): String {
        val success = controller.configureNode(args.nodeId, args.universe, args.startAddress)
        return if (success) {
            "Configured node '${args.nodeId}': universe=${args.universe}, startAddress=${args.startAddress}"
        } else {
            "Failed to configure node '${args.nodeId}'. Check that the node is online and accessible."
        }
    }
}

/**
 * Tool: Run a diagnostic test on a DMX node's connection.
 */
class DiagnoseConnectionTool(private val controller: NetworkController) {
    @Serializable
    data class Args(val nodeId: String)

    suspend fun execute(args: Args): String {
        val result = controller.diagnoseConnection(args.nodeId)
            ?: return "Node '${args.nodeId}' not found. Run scan first."
        val reachableStr = if (result.isReachable) "reachable" else "unreachable"
        return "Diagnostic for '${args.nodeId}': $reachableStr, " +
            "latency=${result.latencyMs}ms, packetLoss=${result.packetLossPercent}%. " +
            result.details
    }
}
