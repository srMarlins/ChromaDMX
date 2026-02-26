package com.chromadmx.networking.ble

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-level BLE provisioning workflow orchestrator.
 *
 * Encapsulates the full provisioning state machine:
 *   IDLE -> SCANNING -> CONNECTING -> READING_CONFIG -> WRITING_CONFIG -> VERIFYING -> SUCCESS
 *
 * Any state may transition to ERROR. ERROR and SUCCESS transition back to IDLE via [reset].
 *
 * This class contains the **business logic** for provisioning and is fully testable
 * in commonMain. Platform-specific BLE operations are delegated to [BleScanner] and
 * [BleProvisioner], which can be mocked in tests.
 *
 * @param scanner     Platform BLE scanner for discovering nodes
 * @param provisioner Platform BLE provisioner for GATT read/write operations
 */
class BleProvisioningService(
    private val scanner: BleScanner,
    private val provisioner: BleProvisioner,
) {
    private val _state = MutableStateFlow(ProvisioningState.IDLE)

    /** Current state of the provisioning workflow. */
    val state: StateFlow<ProvisioningState> = _state.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)

    /** Human-readable error message from the most recent failure. */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lastProvisionedConfig = MutableStateFlow<NodeConfig?>(null)

    /** Configuration that was last successfully written and verified. */
    val lastProvisionedConfig: StateFlow<NodeConfig?> = _lastProvisionedConfig.asStateFlow()

    /** Discovered BLE nodes (delegated from scanner). */
    val discoveredNodes: StateFlow<List<BleNode>> = scanner.discoveredNodes

    /** Whether scanning is active (delegated from scanner). */
    val isScanning: StateFlow<Boolean> = scanner.isScanning

    /**
     * Begin scanning for ChromaDMX BLE nodes.
     *
     * Transitions to [ProvisioningState.SCANNING].
     *
     * @return true if scanning was started, false if BLE is not available
     */
    fun startScanning(): Boolean {
        return try {
            _state.value = ProvisioningState.SCANNING
            _errorMessage.value = null
            scanner.startScan()
            true
        } catch (e: UnsupportedOperationException) {
            _state.value = ProvisioningState.ERROR
            _errorMessage.value = "BLE scanning not supported on this platform"
            false
        }
    }

    /**
     * Stop the active BLE scan.
     *
     * Transitions back to [ProvisioningState.IDLE] if currently scanning.
     */
    fun stopScanning() {
        try {
            scanner.stopScan()
        } catch (_: UnsupportedOperationException) {
            // Ignore — scan was never started
        }
        if (_state.value == ProvisioningState.SCANNING) {
            _state.value = ProvisioningState.IDLE
        }
    }

    /**
     * Execute the full provisioning workflow for the given node and config.
     *
     * Performs:
     * 1. Connect to the node via GATT
     * 2. Read the current configuration from the node
     * 3. Validate and write the new configuration
     * 4. Read config back for verification
     * 5. Disconnect
     *
     * State transitions are emitted to [state] throughout the process.
     *
     * @param node   The BLE node to provision
     * @param config The configuration to write
     * @return The verified configuration read back from the node, or null on failure
     */
    suspend fun provision(node: BleNode, config: NodeConfig): NodeConfig? {
        // Validate config before starting
        val errors = config.validate()
        if (errors.isNotEmpty()) {
            _state.value = ProvisioningState.ERROR
            _errorMessage.value = errors.first()
            return null
        }

        _errorMessage.value = null

        try {
            // Step 1: Connect
            _state.value = ProvisioningState.CONNECTING
            val connected = provisioner.connect(node.deviceId)
            if (!connected) {
                _state.value = ProvisioningState.ERROR
                _errorMessage.value = "Failed to connect to ${node.displayName}"
                return null
            }

            // Step 2: Read current config (optional, may be null on fresh node)
            _state.value = ProvisioningState.READING_CONFIG
            // We still attempt the read but don't fail if it returns null
            provisioner.readConfig()

            // Step 3: Write new config
            _state.value = ProvisioningState.WRITING_CONFIG
            val written = provisioner.writeConfig(config)
            if (!written) {
                _state.value = ProvisioningState.ERROR
                _errorMessage.value = "Failed to write configuration to ${node.displayName}"
                safeDisconnect()
                return null
            }

            // Step 4: Verify by reading back
            _state.value = ProvisioningState.VERIFYING
            // Short delay to let the node process the write
            delay(VERIFY_DELAY_MS)
            val verified = provisioner.readConfig()
            if (verified == null) {
                _state.value = ProvisioningState.ERROR
                _errorMessage.value = "Failed to verify configuration on ${node.displayName}"
                safeDisconnect()
                return null
            }

            // Step 5: Disconnect and report success
            safeDisconnect()
            _state.value = ProvisioningState.SUCCESS
            _lastProvisionedConfig.value = verified
            return verified
        } catch (e: CancellationException) {
            safeDisconnect()
            throw e
        } catch (e: UnsupportedOperationException) {
            _state.value = ProvisioningState.ERROR
            _errorMessage.value = "BLE provisioning not supported on this platform"
            safeDisconnect()
            return null
        } catch (e: Exception) {
            _state.value = ProvisioningState.ERROR
            _errorMessage.value = e.message ?: "Unknown provisioning error"
            safeDisconnect()
            return null
        }
    }

    /**
     * Reset the state machine back to IDLE.
     *
     * Should be called after SUCCESS or ERROR to prepare for the next operation.
     */
    fun reset() {
        _state.value = ProvisioningState.IDLE
        _errorMessage.value = null
        _lastProvisionedConfig.value = null
    }

    /**
     * Disconnect from the currently connected node, ignoring any errors.
     */
    private suspend fun safeDisconnect() {
        try {
            provisioner.disconnect()
        } catch (_: Exception) {
            // Ignore disconnect errors during cleanup
        }
    }

    companion object {
        /** Delay before verification read to allow the node to process the write. */
        internal const val VERIFY_DELAY_MS: Long = 200L
    }
}
