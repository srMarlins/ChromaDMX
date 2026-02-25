package com.chromadmx.ui.viewmodel

import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.model.DmxNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Network screen.
 *
 * Exposes discovered DMX nodes and provides scan control.
 */
class NetworkViewModel(
    private val nodeDiscovery: NodeDiscovery,
    private val scope: CoroutineScope,
) {
    val nodes: StateFlow<Map<String, DmxNode>> = nodeDiscovery.nodes

    val isScanning: Boolean get() = nodeDiscovery.isRunning

    fun startScan() {
        nodeDiscovery.start()
    }

    fun stopScan() {
        nodeDiscovery.stop()
    }

    fun triggerPoll() {
        scope.launch {
            nodeDiscovery.sendPoll()
        }
    }
}
