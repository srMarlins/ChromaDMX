package com.chromadmx.ui.viewmodel

import com.chromadmx.networking.ble.BleNode
import com.chromadmx.networking.ble.BleProvisioningService
import com.chromadmx.networking.ble.NodeConfig
import com.chromadmx.networking.ble.ProvisioningState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the BLE provisioning workflow.
 *
 * Thin layer between the UI and [BleProvisioningService], exposing reactive
 * state for Compose and delegating all provisioning tasks to the service.
 *
 * The provisioning flow:
 * 1. Start scan -> discover nodes
 * 2. Select a node
 * 3. Connect -> read existing config
 * 4. User fills in new config
 * 5. Write config -> verify -> success
 * 6. Node reboots and appears on Art-Net
 *
 * When [isBleAvailable] is false (e.g., platform does not support BLE or
 * the service is null), the UI should display a graceful "BLE not available"
 * message.
 *
 * @param service BLE provisioning service (may be null if BLE is unavailable)
 * @param scope   Coroutine scope for async operations
 */
class ProvisioningViewModel(
    private val service: BleProvisioningService?,
    private val scope: CoroutineScope,
) {
    /**
     * Whether BLE is available on this platform.
     * When false, all scan/provision operations are no-ops and the UI
     * should show a "BLE not available" message.
     */
    val isBleAvailable: Boolean = service != null

    // --- Scan state (delegated from service) ---

    /** Discovered BLE nodes from the scanner. */
    val discoveredNodes: StateFlow<List<BleNode>> =
        service?.discoveredNodes ?: MutableStateFlow(emptyList())

    /** Whether a BLE scan is currently active. */
    val isScanning: StateFlow<Boolean> =
        service?.isScanning ?: MutableStateFlow(false)

    // --- Provisioning state (delegated from service) ---

    /** Current state of the provisioning workflow. */
    val provisioningState: StateFlow<ProvisioningState> =
        service?.state ?: MutableStateFlow(ProvisioningState.IDLE)

    /** Human-readable error message from the most recent failed operation. */
    val errorMessage: StateFlow<String?> =
        service?.errorMessage ?: MutableStateFlow(null)

    /** Configuration that was last successfully written and verified. */
    val currentConfig: StateFlow<NodeConfig?> =
        service?.lastProvisionedConfig ?: MutableStateFlow(null)

    // --- Selection ---

    private val _selectedNode = MutableStateFlow<BleNode?>(null)

    /** The node currently selected for provisioning. */
    val selectedNode: StateFlow<BleNode?> = _selectedNode.asStateFlow()

    private var provisionJob: Job? = null

    // --- Scan controls ---

    /**
     * Start scanning for BLE nodes.
     * No-op if BLE is unavailable.
     */
    fun startScan() {
        if (!isBleAvailable) return
        service?.startScanning()
    }

    /**
     * Stop the active BLE scan.
     * No-op if BLE is unavailable or no scan is running.
     */
    fun stopScan() {
        if (!isBleAvailable) return
        service?.stopScanning()
    }

    // --- Node selection ---

    /**
     * Select a discovered node for provisioning.
     *
     * Clears any previous error and config state.
     */
    fun selectNode(node: BleNode) {
        _selectedNode.value = node
        service?.reset()
    }

    /**
     * Clear the current node selection.
     */
    fun clearSelection() {
        _selectedNode.value = null
        service?.reset()
    }

    // --- Provisioning ---

    /**
     * Run the full provisioning workflow for the selected node.
     *
     * Delegates the entire provisioning workflow to [BleProvisioningService].
     * State transitions are reflected in [provisioningState] and errors in
     * [errorMessage], both delegated from the service.
     *
     * @param config The configuration to write to the node
     */
    fun provision(config: NodeConfig) {
        val node = _selectedNode.value ?: return
        if (!isBleAvailable) return

        provisionJob?.cancel()
        provisionJob = scope.launch {
            service?.provision(node, config)
        }
    }

    /**
     * Reset the provisioning state back to IDLE and clear errors.
     * Useful after SUCCESS or ERROR states.
     */
    fun resetState() {
        provisionJob?.cancel()
        service?.reset()
    }

    /**
     * Clean up resources when the ViewModel is no longer needed.
     */
    fun onCleared() {
        provisionJob?.cancel()
        service?.stopScanning()
    }
}
