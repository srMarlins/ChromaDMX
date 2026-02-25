package com.chromadmx.agent.tools

import com.chromadmx.agent.controller.NetworkController
import com.chromadmx.agent.controller.NodeSummary
import com.chromadmx.agent.model.DiagnosticResult
import com.chromadmx.agent.model.NodeStatusResult

/**
 * Fake [NetworkController] for testing network tools.
 */
class FakeNetworkController : NetworkController {
    var nodes: List<NodeSummary> = emptyList()
    var nodeStatus: NodeStatusResult? = null
    var configureResult: Boolean = true
    var diagnosticResult: DiagnosticResult? = null

    var lastConfiguredNodeId: String = ""
    var lastConfiguredUniverse: Int = -1
    var lastConfiguredStartAddress: Int = -1

    override suspend fun scanNetwork(): List<NodeSummary> = nodes

    override suspend fun getNodeStatus(nodeId: String): NodeStatusResult? = nodeStatus

    override suspend fun configureNode(nodeId: String, universe: Int, startAddress: Int): Boolean {
        lastConfiguredNodeId = nodeId
        lastConfiguredUniverse = universe
        lastConfiguredStartAddress = startAddress
        return configureResult
    }

    override suspend fun diagnoseConnection(nodeId: String): DiagnosticResult? = diagnosticResult
}
