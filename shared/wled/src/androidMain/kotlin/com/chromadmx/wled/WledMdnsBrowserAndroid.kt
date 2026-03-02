package com.chromadmx.wled

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Android implementation of [WledMdnsBrowser] using [NsdManager].
 *
 * Discovers `_wled._tcp.` services via DNS-SD and resolves each to extract
 * the device IP address and name.
 */
class WledMdnsBrowserAndroid(context: Context) : WledMdnsBrowser {

    companion object {
        private const val SERVICE_TYPE = "_wled._tcp."
    }

    private val nsdManager: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _discoveredDevices = MutableStateFlow<List<WledDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<WledDevice>> = _discoveredDevices.asStateFlow()

    private var isActive = false

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {
            isActive = true
        }

        override fun onDiscoveryStopped(serviceType: String) {
            isActive = false
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            nsdManager.resolveService(serviceInfo, resolveListener())
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            val name = serviceInfo.serviceName
            _discoveredDevices.update { it.filter { d -> d.name != name } }
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            isActive = false
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            // Best-effort cleanup — nothing to do
        }
    }

    private fun resolveListener() = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Silently ignore — device will be retried on next browse cycle
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            val ip = serviceInfo.host?.hostAddress ?: return
            val name = serviceInfo.serviceName
            val device = WledDevice(
                ipAddress = ip,
                name = name,
                isOnline = true,
                lastSeenMs = currentTimeMillis(),
            )
            _discoveredDevices.update { current ->
                val existingIndex = current.indexOfFirst { it.ipAddress == ip }
                if (existingIndex >= 0) {
                    current.toMutableList().also { it[existingIndex] = device }
                } else {
                    current + device
                }
            }
        }
    }

    override fun startBrowse() {
        if (isActive) return
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    override fun stopBrowse() {
        if (!isActive) return
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (_: IllegalArgumentException) {
            // Listener was not registered — safe to ignore
        }
        isActive = false
    }

    /** Platform time — separated for clarity. */
    private fun currentTimeMillis(): Long = System.currentTimeMillis()
}
