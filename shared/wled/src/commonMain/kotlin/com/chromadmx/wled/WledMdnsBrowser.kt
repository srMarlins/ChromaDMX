package com.chromadmx.wled

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for mDNS/DNS-SD browsing of WLED devices on the local network.
 *
 * Platform implementations use NsdManager (Android) or NWBrowser (iOS).
 * The browser discovers `_wled._tcp.` services and exposes them as [WledDevice] entries.
 */
interface WledMdnsBrowser {
    /** Start browsing for `_wled._tcp.` services on the local network. */
    fun startBrowse()

    /** Stop browsing and release platform resources. */
    fun stopBrowse()

    /** Currently discovered WLED devices, updated as services appear/disappear. */
    val discoveredDevices: StateFlow<List<WledDevice>>
}
