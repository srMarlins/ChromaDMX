package com.chromadmx.wled

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS stub implementation of [WledMdnsBrowser].
 *
 * TODO: Implement using Network.framework NWBrowser to discover `_wled._tcp.` services.
 *  NWBrowser.init(for: .bonjourWithTXTRecord(type: "_wled._tcp.", domain: "local."), using: .tcp)
 *  Browse results provide NWEndpoint which can be resolved to IP addresses.
 */
class WledMdnsBrowserIos : WledMdnsBrowser {

    private val _discoveredDevices = MutableStateFlow<List<WledDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<WledDevice>> = _discoveredDevices.asStateFlow()

    override fun startBrowse() {
        // TODO: Implement with NWBrowser from Network.framework
    }

    override fun stopBrowse() {
        // TODO: Implement NWBrowser cancellation
    }
}
