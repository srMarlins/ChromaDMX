package com.chromadmx.ui.viewmodel

import com.chromadmx.networking.discovery.NodeDiscovery
import com.chromadmx.networking.model.DmxNode
import com.chromadmx.ui.screen.network.NodeHealth
import com.chromadmx.ui.screen.network.nodeHealth
import com.chromadmx.ui.util.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * States for the mascot animations.
 */
enum class MascotAnimation {
    IDLE, THINKING, HAPPY, ALERT, CONFUSED, DANCING
}

/**
 * Data for a mascot alert bubble.
 */
data class MascotAlert(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

/**
 * ViewModel managing mascot state and diagnostic alerts.
 */
class MascotViewModel(
    private val nodeDiscovery: NodeDiscovery,
    private val scope: CoroutineScope,
    private val onDiagnose: (String) -> Unit = {}
) {
    private val _animation = MutableStateFlow(MascotAnimation.IDLE)
    val animation: StateFlow<MascotAnimation> = _animation.asStateFlow()

    private val _currentAlert = MutableStateFlow<MascotAlert?>(null)
    val currentAlert: StateFlow<MascotAlert?> = _currentAlert.asStateFlow()

    private var previousNodes = emptyMap<String, DmxNode>()

    init {
        scope.launch {
            nodeDiscovery.nodes.collectLatest { nodes ->
                monitorNodes(nodes)
                previousNodes = nodes
            }
        }
    }

    private fun monitorNodes(nodes: Map<String, DmxNode>) {
        val now = currentTimeMillis()

        // Check for new nodes
        val newNodes = nodes.filterKeys { it !in previousNodes }
        if (newNodes.isNotEmpty()) {
            triggerAlert(
                MascotAlert("New node found! Auto-configuring..."),
                MascotAnimation.HAPPY
            )
            return
        }

        // Check for disconnected nodes
        val disconnectedNodes = nodes.filter { (key, node) ->
            val prevNode = previousNodes[key]
            val prevHealth = prevNode?.let { nodeHealth(it, now) }
            val currentHealth = nodeHealth(node, now)
            prevHealth != NodeHealth.OFFLINE && currentHealth == NodeHealth.OFFLINE
        }

        if (disconnectedNodes.isNotEmpty()) {
            val node = disconnectedNodes.values.first()
            val nodeName = node.shortName.ifEmpty { "Node" }
            triggerAlert(
                MascotAlert(
                    message = "$nodeName disconnected — diagnose?",
                    actionLabel = "DIAGNOSE",
                    onAction = { onDiagnose(node.ipAddress) }
                ),
                MascotAnimation.ALERT
            )
            return
        }

        // Check if all nodes became healthy
        val wereAnyOffline = previousNodes.values.any { nodeHealth(it, now) == NodeHealth.OFFLINE }
        val areAllOnline = nodes.isNotEmpty() && nodes.values.all { nodeHealth(it, now) == NodeHealth.ONLINE }

        if (wereAnyOffline && areAllOnline) {
            triggerAlert(
                MascotAlert("All nodes healthy ✓"),
                MascotAnimation.HAPPY
            )
        }
    }

    private fun triggerAlert(alert: MascotAlert, animation: MascotAnimation) {
        _currentAlert.value = alert
        _animation.value = animation

        // Auto-clear alert after some time if no action
        if (alert.onAction == null) {
            scope.launch {
                kotlinx.coroutines.delay(5000)
                if (_currentAlert.value == alert) {
                    dismissAlert()
                }
            }
        }
    }

    fun dismissAlert() {
        _currentAlert.value = null
        _animation.value = MascotAnimation.IDLE
    }

    fun triggerDiagnosis(ip: String) {
        onDiagnose(ip)
    }
}
