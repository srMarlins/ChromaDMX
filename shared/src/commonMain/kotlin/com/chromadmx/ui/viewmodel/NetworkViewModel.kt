package com.chromadmx.ui.viewmodel

import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.model.DmxNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun startScan() {
        nodeDiscovery.start()
        _isScanning.value = true
    }

    fun stopScan() {
        nodeDiscovery.stop()
        _isScanning.value = false
    }

    fun triggerPoll() {
        scope.launch {
            nodeDiscovery.sendPoll()
        }
    }

    /** Cancel all coroutines launched by this ViewModel. */
    fun onCleared() {
        scope.coroutineContext[Job]?.cancel()
    }
}
