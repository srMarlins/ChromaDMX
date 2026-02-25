package com.chromadmx.agent.tools

import com.chromadmx.agent.controller.NodeSummary
import com.chromadmx.agent.model.DiagnosticResult
import com.chromadmx.agent.model.NodeStatusResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkToolsTest {
    private val controller = FakeNetworkController()

    @Test
    fun scanNetworkReturnsNodeList() = runTest {
        controller.nodes = listOf(
            NodeSummary(id = "node-1", ip = "192.168.1.100", name = "Front Bar", universes = listOf(0)),
            NodeSummary(id = "node-2", ip = "192.168.1.101", name = "Back Truss", universes = listOf(1, 2))
        )
        val tool = ScanNetworkTool(controller)
        val result = tool.execute(ScanNetworkTool.Args())
        assertContains(result, "2 nodes")
        assertContains(result, "Front Bar")
        assertContains(result, "Back Truss")
    }

    @Test
    fun scanNetworkEmptyReturnsMessage() = runTest {
        controller.nodes = emptyList()
        val tool = ScanNetworkTool(controller)
        val result = tool.execute(ScanNetworkTool.Args())
        assertContains(result, "0 nodes")
    }

    @Test
    fun getNodeStatusReturnsInfo() = runTest {
        controller.nodeStatus = NodeStatusResult(
            id = "node-1",
            name = "Front Bar",
            ip = "192.168.1.100",
            universes = listOf(0),
            isOnline = true,
            firmwareVersion = "1.0.3",
            packetsSent = 12345L
        )
        val tool = GetNodeStatusTool(controller)
        val result = tool.execute(GetNodeStatusTool.Args(nodeId = "node-1"))
        assertContains(result, "Front Bar")
        assertContains(result, "online")
    }

    @Test
    fun getNodeStatusNotFoundReturnsError() = runTest {
        controller.nodeStatus = null
        val tool = GetNodeStatusTool(controller)
        val result = tool.execute(GetNodeStatusTool.Args(nodeId = "unknown"))
        assertContains(result, "not found")
    }

    @Test
    fun configureNodeSuccess() = runTest {
        controller.configureResult = true
        val tool = ConfigureNodeTool(controller)
        val result = tool.execute(ConfigureNodeTool.Args(nodeId = "node-1", universe = 2, startAddress = 1))
        assertContains(result, "Configured")
        assertEquals("node-1", controller.lastConfiguredNodeId)
        assertEquals(2, controller.lastConfiguredUniverse)
    }

    @Test
    fun configureNodeFailure() = runTest {
        controller.configureResult = false
        val tool = ConfigureNodeTool(controller)
        val result = tool.execute(ConfigureNodeTool.Args(nodeId = "node-1", universe = 2, startAddress = 1))
        assertContains(result, "Failed")
    }

    @Test
    fun diagnoseConnectionReturnsResult() = runTest {
        controller.diagnosticResult = DiagnosticResult(
            nodeId = "node-1",
            latencyMs = 2.5f,
            packetLossPercent = 0.1f,
            isReachable = true,
            details = "Connection stable"
        )
        val tool = DiagnoseConnectionTool(controller)
        val result = tool.execute(DiagnoseConnectionTool.Args(nodeId = "node-1"))
        assertContains(result, "reachable")
        assertContains(result, "2.5")
    }

    @Test
    fun diagnoseConnectionNotFound() = runTest {
        controller.diagnosticResult = null
        val tool = DiagnoseConnectionTool(controller)
        val result = tool.execute(DiagnoseConnectionTool.Args(nodeId = "unknown"))
        assertContains(result, "not found")
    }
}
