package com.chromadmx.ui.viewmodel

import com.chromadmx.agent.LightingAgent
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
    private val agent: LightingAgent,
    private val scope: CoroutineScope,
) {
    val nodes: StateFlow<Map<String, DmxNode>> = nodeDiscovery.nodes

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _activeAlert = MutableStateFlow<NetworkAlert?>(null)
    val activeAlert: StateFlow<NetworkAlert?> = _activeAlert.asStateFlow()

    init {
        scope.launch {
            monitorNodeChanges()
        }
    }

    private suspend fun monitorNodeChanges() {
        var prevNodes = emptyMap<String, DmxNode>()
        nodes.collect { currentNodes ->
            if (prevNodes.isEmpty() && currentNodes.isNotEmpty()) {
                // First time nodes are found
                _activeAlert.value = NetworkAlert(
                    message = "All nodes healthy ✓",
                    type = NetworkAlertType.ALL_HEALTHY
                )
            } else {
                // Detect new nodes
                val newNodes = currentNodes.keys - prevNodes.keys
                newNodes.firstOrNull()?.let { nodeId ->
                    val node = currentNodes[nodeId]!!
                    _activeAlert.value = NetworkAlert(
                        message = "New node found: ${node.shortName.ifEmpty { "Node" }}! Auto-configuring...",
                        type = NetworkAlertType.NEW_NODE,
                        nodeId = nodeId
                    )
                }

                // Detect disconnected nodes (removed from registry)
                val removedNodes = prevNodes.keys - currentNodes.keys
                removedNodes.firstOrNull()?.let { nodeId ->
                    val node = prevNodes[nodeId]!!
                    _activeAlert.value = NetworkAlert(
                        message = "Node ${node.shortName.ifEmpty { "Node" }} disconnected — diagnose?",
                        type = NetworkAlertType.DISCONNECTED,
                        nodeId = nodeId
                    )
                }
            }
            prevNodes = currentNodes
        }
    }

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

    fun diagnoseNode(nodeId: String) {
        scope.launch {
            agent.dispatchTool("diagnoseConnection", "{\"nodeId\": \"$nodeId\"}")
        }
        _activeAlert.value = null
    }

    fun dismissAlert() {
        _activeAlert.value = null
    }

    /** Cancel all coroutines launched by this ViewModel. */
    fun onCleared() {
        scope.coroutineContext[Job]?.cancel()
    }
}

/**
 * Programmatic alert from the network service.
 */
data class NetworkAlert(
    val message: String,
    val type: NetworkAlertType,
    val nodeId: String? = null,
)

enum class NetworkAlertType {
    DISCONNECTED,
    NEW_NODE,
    ALL_HEALTHY,
}
