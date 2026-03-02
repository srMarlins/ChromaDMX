package com.chromadmx.wled

import kotlin.test.Test
import kotlin.test.assertEquals

class WledRepositoryTest {
    @Test
    fun wledDeviceRoundTrip() {
        val device = WledDevice(
            ipAddress = "192.168.1.100",
            name = "Test Strip",
            macAddress = "AA:BB:CC",
            totalLeds = 60,
            firmwareVersion = "0.14.0",
        )
        assertEquals("192.168.1.100", device.ipAddress)
        assertEquals(60, device.totalLeds)
    }
}
