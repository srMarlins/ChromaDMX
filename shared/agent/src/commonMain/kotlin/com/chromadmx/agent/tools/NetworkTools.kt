package com.chromadmx.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.chromadmx.agent.controller.NetworkController
import kotlinx.serialization.Serializable

class ScanNetworkTool(private val controller: NetworkController) : SimpleTool<ScanNetworkTool.Args>(
    argsSerializer = Args.serializer(),
    name = "scanNetwork",
    description = "Scan the local network for DMX Art-Net/sACN nodes. Returns a list of discovered nodes with their IPs and universes."
) {
    @Serializable
    class Args

    override suspend fun execute(args: Args): String {
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

class GetNodeStatusTool(private val controller: NetworkController) : SimpleTool<GetNodeStatusTool.Args>(
    argsSerializer = Args.serializer(),
    name = "getNodeStatus",
    description = "Get detailed status of a specific DMX node including online status, firmware version, and packet count."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The node ID to query (from scanNetwork results)")
        val nodeId: String
    )

    override suspend fun execute(args: Args): String {
        val status = controller.getNodeStatus(args.nodeId)
            ?: return "Node '${args.nodeId}' not found. Run scanNetwork first."
        val onlineStr = if (status.isOnline) "online" else "offline"
        return "Node '${status.name}' (${status.ip}): $onlineStr, " +
            "universes=${status.universes}, firmware=${status.firmwareVersion}, " +
            "packets_sent=${status.packetsSent}"
    }
}

class ConfigureNodeTool(private val controller: NetworkController) : SimpleTool<ConfigureNodeTool.Args>(
    argsSerializer = Args.serializer(),
    name = "configureNode",
    description = "Configure a DMX node's universe assignment and start address."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The node ID to configure")
        val nodeId: String,
        @property:LLMDescription("Universe number to assign (0-32767)")
        val universe: Int,
        @property:LLMDescription("DMX start address (1-512)")
        val startAddress: Int
    )

    override suspend fun execute(args: Args): String {
        val success = controller.configureNode(args.nodeId, args.universe, args.startAddress)
        return if (success) {
            "Configured node '${args.nodeId}': universe=${args.universe}, startAddress=${args.startAddress}"
        } else {
            "Failed to configure node '${args.nodeId}'. Check that the node is online and accessible."
        }
    }
}

class DiagnoseConnectionTool(private val controller: NetworkController) : SimpleTool<DiagnoseConnectionTool.Args>(
    argsSerializer = Args.serializer(),
    name = "diagnoseConnection",
    description = "Run a diagnostic test on a DMX node's connection. Reports latency, packet loss, and reachability."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The node ID to diagnose")
        val nodeId: String
    )

    override suspend fun execute(args: Args): String {
        val result = controller.diagnoseConnection(args.nodeId)
            ?: return "Node '${args.nodeId}' not found. Run scanNetwork first."
        val reachableStr = if (result.isReachable) "reachable" else "unreachable"
        return "Diagnostic for '${args.nodeId}': $reachableStr, " +
            "latency=${result.latencyMs}ms, packetLoss=${result.packetLossPercent}%. " +
            result.details
    }
}
