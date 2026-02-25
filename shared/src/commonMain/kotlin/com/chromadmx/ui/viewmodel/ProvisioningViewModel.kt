package com.chromadmx.ui.viewmodel

import com.chromadmx.networking.ble.BleNode
import com.chromadmx.networking.ble.BleProvisioner
import com.chromadmx.networking.ble.BleScanner
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
 * Coordinates BLE scanning and provisioning through [BleScanner] and
 * [BleProvisioner], exposing reactive state for the UI layer.
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
 * the scanner/provisioner threw [UnsupportedOperationException]), the UI
 * should display a graceful "BLE not available" message.
 *
 * @param scanner     BLE scanner instance (may be null if BLE is unavailable)
 * @param provisioner BLE provisioner instance (may be null if BLE is unavailable)
 * @param scope       Coroutine scope for async operations
 */
class ProvisioningViewModel(
    private val scanner: BleScanner?,
    private val provisioner: BleProvisioner?,
    private val scope: CoroutineScope,
) {
    /**
     * Whether BLE is available on this platform.
     * When false, all scan/provision operations are no-ops and the UI
     * should show a "BLE not available" message.
     */
    val isBleAvailable: Boolean = scanner != null && provisioner != null

    // --- Scan state ---

    /** Discovered BLE nodes from the scanner. */
    val discoveredNodes: StateFlow<List<BleNode>> =
        scanner?.discoveredNodes ?: MutableStateFlow(emptyList())

    /** Whether a BLE scan is currently active. */
    val isScanning: StateFlow<Boolean> =
        scanner?.isScanning ?: MutableStateFlow(false)

    // --- Provisioning state ---

    /** Current state of the provisioning workflow. */
    val provisioningState: StateFlow<ProvisioningState> =
        provisioner?.state ?: MutableStateFlow(ProvisioningState.IDLE)

    // --- Selection ---

    private val _selectedNode = MutableStateFlow<BleNode?>(null)

    /** The node currently selected for provisioning. */
    val selectedNode: StateFlow<BleNode?> = _selectedNode.asStateFlow()

    // --- Current config read from node ---

    private val _currentConfig = MutableStateFlow<NodeConfig?>(null)

    /** Configuration currently stored on the selected node (read after connect). */
    val currentConfig: StateFlow<NodeConfig?> = _currentConfig.asStateFlow()

    // --- Error message ---

    private val _errorMessage = MutableStateFlow<String?>(null)

    /** Human-readable error message from the most recent failed operation. */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var provisionJob: Job? = null

    // --- Scan controls ---

    /**
     * Start scanning for BLE nodes.
     * No-op if BLE is unavailable.
     */
    fun startScan() {
        if (!isBleAvailable) return
        _errorMessage.value = null
        try {
            scanner?.startScan()
        } catch (e: UnsupportedOperationException) {
            _errorMessage.value = "BLE scanning not supported on this device"
        }
    }

    /**
     * Stop the active BLE scan.
     * No-op if BLE is unavailable or no scan is running.
     */
    fun stopScan() {
        if (!isBleAvailable) return
        try {
            scanner?.stopScan()
        } catch (_: UnsupportedOperationException) {
            // Silently ignore -- scan was never started
        }
    }

    // --- Node selection ---

    /**
     * Select a discovered node for provisioning.
     *
     * Clears any previous error and config state.
     */
    fun selectNode(node: BleNode) {
        _selectedNode.value = node
        _currentConfig.value = null
        _errorMessage.value = null
    }

    /**
     * Clear the current node selection.
     */
    fun clearSelection() {
        _selectedNode.value = null
        _currentConfig.value = null
        _errorMessage.value = null
    }

    // --- Provisioning ---

    /**
     * Run the full provisioning workflow for the selected node.
     *
     * Connects to the node, writes the given config, verifies it by
     * reading it back, then disconnects. State transitions are reflected
     * in [provisioningState].
     *
     * @param config The configuration to write to the node
     */
    fun provision(config: NodeConfig) {
        val node = _selectedNode.value ?: return
        if (!isBleAvailable) return

        // Validate before attempting
        val errors = config.validate()
        if (errors.isNotEmpty()) {
            _errorMessage.value = errors.first()
            return
        }

        provisionJob?.cancel()
        provisionJob = scope.launch {
            _errorMessage.value = null
            try {
                // Step 1: Connect
                val connected = provisioner!!.connect(node.deviceId)
                if (!connected) {
                    _errorMessage.value = "Failed to connect to ${node.displayName}"
                    return@launch
                }

                // Step 2: Write config
                val written = provisioner.writeConfig(config)
                if (!written) {
                    _errorMessage.value = "Failed to write configuration"
                    provisioner.disconnect()
                    return@launch
                }

                // Step 3: Verify by reading back
                val readBack = provisioner.readConfig()
                if (readBack == null) {
                    _errorMessage.value = "Failed to verify configuration"
                    provisioner.disconnect()
                    return@launch
                }

                // Step 4: Disconnect (node will reboot)
                provisioner.disconnect()

                _currentConfig.value = readBack
            } catch (e: UnsupportedOperationException) {
                _errorMessage.value = "BLE provisioning not yet implemented on this platform"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown provisioning error"
                try {
                    provisioner?.disconnect()
                } catch (_: Exception) {
                    // Ignore disconnect errors during error cleanup
                }
            }
        }
    }

    /**
     * Reset the provisioning state back to IDLE and clear errors.
     * Useful after SUCCESS or ERROR states.
     */
    fun resetState() {
        provisionJob?.cancel()
        _errorMessage.value = null
        _currentConfig.value = null
        // Note: actual state reset happens through the provisioner's StateFlow
        // For the stub, the state remains at whatever value it was
    }

    /**
     * Clean up resources when the ViewModel is no longer needed.
     */
    fun onCleared() {
        provisionJob?.cancel()
        try {
            scanner?.stopScan()
        } catch (_: Exception) {
            // Ignore
        }
    }
}
