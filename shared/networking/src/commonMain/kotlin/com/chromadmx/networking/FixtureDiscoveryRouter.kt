package com.chromadmx.networking

import com.chromadmx.networking.model.DmxNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

/**
 * Routes fixture discovery to real or simulated backends based on [TransportMode].
 *
 * In [TransportMode.Mixed] mode, discovered nodes from both sources
 * are merged into a single list.
 */
class FixtureDiscoveryRouter(
    private val real: FixtureDiscovery,
    private val simulated: FixtureDiscovery,
    private val scope: CoroutineScope,
) : FixtureDiscovery {

    private val _mode = MutableStateFlow(TransportMode.Real)
    val mode: StateFlow<TransportMode> = _mode.asStateFlow()

    fun switchTo(mode: TransportMode) {
        _mode.value = mode
    }

    override fun startScan() {
        when (_mode.value) {
            TransportMode.Real -> real.startScan()
            TransportMode.Simulated -> simulated.startScan()
            TransportMode.Mixed -> { real.startScan(); simulated.startScan() }
        }
    }

    override fun stopScan() {
        real.stopScan()
        simulated.stopScan()
    }

    override val discoveredNodes: StateFlow<List<DmxNode>> by lazy {
        combine(_mode, real.discoveredNodes, simulated.discoveredNodes) { mode, realNodes, simNodes ->
            when (mode) {
                TransportMode.Real -> realNodes
                TransportMode.Simulated -> simNodes
                TransportMode.Mixed -> (realNodes + simNodes).distinctBy { it.nodeKey }
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    }

    override val isScanning: StateFlow<Boolean> by lazy {
        combine(_mode, real.isScanning, simulated.isScanning) { mode, realScanning, simScanning ->
            when (mode) {
                TransportMode.Real -> realScanning
                TransportMode.Simulated -> simScanning
                TransportMode.Mixed -> realScanning || simScanning
            }
        }.stateIn(scope, SharingStarted.Eagerly, false)
    }
}
