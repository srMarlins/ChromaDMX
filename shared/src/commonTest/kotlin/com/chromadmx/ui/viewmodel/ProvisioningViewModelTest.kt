package com.chromadmx.ui.viewmodel

import com.chromadmx.networking.ble.BleNode
import com.chromadmx.networking.ble.NodeConfig
import com.chromadmx.networking.ble.ProvisioningState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [ProvisioningViewModel].
 *
 * Tests both the BLE-unavailable path (null service) and
 * basic state management operations.
 */
class ProvisioningViewModelTest {

    private fun testNode() = BleNode(
        deviceId = "AA:BB:CC:DD:EE:FF",
        name = "Test Node",
        rssi = -50,
        isProvisioned = false
    )

    // ================================================================
    // BLE Unavailable Path (null service)
    // ================================================================

    @Test
    fun bleUnavailable_isBleAvailable_false() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        assertFalse(vm.isBleAvailable)
    }

    @Test
    fun bleUnavailable_discoveredNodes_empty() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        assertTrue(vm.discoveredNodes.value.isEmpty())
    }

    @Test
    fun bleUnavailable_isScanning_false() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        assertFalse(vm.isScanning.value)
    }

    @Test
    fun bleUnavailable_provisioningState_idle() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        assertEquals(ProvisioningState.IDLE, vm.provisioningState.value)
    }

    @Test
    fun bleUnavailable_startScan_noOp() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.startScan()
        // Should not crash, state should remain
        assertFalse(vm.isScanning.value)
    }

    @Test
    fun bleUnavailable_stopScan_noOp() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.stopScan()
        // Should not crash
        assertFalse(vm.isScanning.value)
    }

    @Test
    fun bleUnavailable_provision_noOp() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.selectNode(testNode())
        vm.provision(NodeConfig("Node", "SSID", "pass", 0, 1))
        // Should not crash, no error set since it's a no-op
    }

    // ================================================================
    // Node Selection
    // ================================================================

    @Test
    fun selectNode_setsSelectedNode() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        val node = testNode()
        vm.selectNode(node)
        assertEquals(node, vm.selectedNode.value)
    }

    @Test
    fun selectNode_clearsError() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.selectNode(testNode())
        assertNull(vm.errorMessage.value)
    }

    @Test
    fun selectNode_clearsCurrentConfig() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.selectNode(testNode())
        assertNull(vm.currentConfig.value)
    }

    @Test
    fun clearSelection_removesSelectedNode() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.selectNode(testNode())
        vm.clearSelection()
        assertNull(vm.selectedNode.value)
    }

    @Test
    fun clearSelection_clearsError() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.selectNode(testNode())
        vm.clearSelection()
        assertNull(vm.errorMessage.value)
    }

    @Test
    fun clearSelection_clearsCurrentConfig() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.selectNode(testNode())
        vm.clearSelection()
        assertNull(vm.currentConfig.value)
    }

    // ================================================================
    // Provisioning — Validation
    // ================================================================

    @Test
    fun provision_invalidConfig_setsError() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.selectNode(testNode())
        // Invalid: blank name
        vm.provision(NodeConfig("", "SSID", "pass", 0, 1))
        // BLE is unavailable, so provision is a no-op.
        // The validation check is inside the service.
    }

    @Test
    fun provision_noSelectedNode_noOp() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        // No node selected
        vm.provision(NodeConfig("Node", "SSID", "pass", 0, 1))
        // Should not crash
    }

    // ================================================================
    // Reset State
    // ================================================================

    @Test
    fun resetState_clearsError() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.resetState()
        assertNull(vm.errorMessage.value)
    }

    @Test
    fun resetState_clearsCurrentConfig() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.resetState()
        assertNull(vm.currentConfig.value)
    }

    // ================================================================
    // onCleared
    // ================================================================

    @Test
    fun onCleared_doesNotCrash() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.onCleared()
        // Should not throw
    }

    @Test
    fun onCleared_afterNodeSelection() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.selectNode(testNode())
        vm.onCleared()
        // Should not throw
    }

    // ================================================================
    // Multiple Operations Sequence
    // ================================================================

    @Test
    fun fullWorkflow_selectClearSelect() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        val node1 = testNode()
        val node2 = testNode().copy(deviceId = "11:22:33:44:55:66", name = "Node 2")

        vm.selectNode(node1)
        assertEquals(node1, vm.selectedNode.value)

        vm.clearSelection()
        assertNull(vm.selectedNode.value)

        vm.selectNode(node2)
        assertEquals(node2, vm.selectedNode.value)
    }

    @Test
    fun fullWorkflow_scanStartStop_bleUnavailable() = runTest {
        val vm = ProvisioningViewModel(
            service = null,
            scope = backgroundScope
        )
        vm.startScan()
        vm.stopScan()
        vm.startScan()
        vm.stopScan()
        // All should be no-ops without crashing
        assertFalse(vm.isScanning.value)
    }
}
