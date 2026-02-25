package com.chromadmx.agent.controller

import com.chromadmx.agent.model.DiagnosticResult
import com.chromadmx.agent.model.NodeStatusResult
import com.chromadmx.networking.discovery.NodeDiscovery

/**
 * Real [NetworkController] bridging to the networking module.
 *
 * Translates agent tool calls into [NodeDiscovery] operations.
 */
class RealNetworkController(
    private val nodeDiscovery: NodeDiscovery,
) : NetworkController {

    override suspend fun scanNetwork(): List<NodeSummary> {
        // Trigger a poll if discovery is running
        if (nodeDiscovery.isRunning) {
            nodeDiscovery.sendPoll()
        }

        return nodeDiscovery.nodeList.map { node ->
            NodeSummary(
                id = node.nodeKey,
                ip = node.ipAddress,
                name = node.shortName.ifEmpty { node.longName },
                universes = node.universes
            )
        }
    }

    override suspend fun getNodeStatus(nodeId: String): NodeStatusResult? {
        val node = nodeDiscovery.nodeList.find { it.nodeKey == nodeId } ?: return null
        return NodeStatusResult(
            id = node.nodeKey,
            name = node.shortName.ifEmpty { node.longName },
            ip = node.ipAddress,
            universes = node.universes,
            isOnline = true, // If in the list, it was recently seen
            firmwareVersion = node.firmwareVersion.toString(),
            packetsSent = 0L // Not tracked at this level
        )
    }

    override suspend fun configureNode(nodeId: String, universe: Int, startAddress: Int): Boolean {
        // Art-Net node configuration requires ArtAddress packets
        // This is a placeholder until full ArtAddress support is implemented
        val node = nodeDiscovery.nodeList.find { it.nodeKey == nodeId }
        return node != null
    }

    override suspend fun diagnoseConnection(nodeId: String): DiagnosticResult? {
        val node = nodeDiscovery.nodeList.find { it.nodeKey == nodeId } ?: return null
        return DiagnosticResult(
            nodeId = node.nodeKey,
            latencyMs = 0f, // Would need actual ping measurement
            packetLossPercent = 0f,
            isReachable = true,
            details = "Node '${node.shortName}' is in the discovery list (last seen recently)"
        )
    }
}
