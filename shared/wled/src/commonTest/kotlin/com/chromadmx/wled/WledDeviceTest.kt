package com.chromadmx.wled

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WledDeviceTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parseDeviceInfoFromJson() {
        val raw = """{"ver":"0.14.0","vid":2312080,"leds":{"count":60,"rgbw":false,"wv":0,"cct":false,"maxpwr":850,"maxseg":16,"lc":1,"seglc":[60]},"name":"WLED","udpport":21324,"arch":"esp32","freeheap":180000,"mac":"AA:BB:CC:DD:EE:FF"}"""
        val info = json.decodeFromString<WledDeviceInfo>(raw)
        assertEquals("WLED", info.name)
        assertEquals(60, info.leds.count)
        assertFalse(info.leds.rgbw)
        assertEquals("AA:BB:CC:DD:EE:FF", info.mac)
    }

    @Test
    fun parseFullStateFromJson() {
        val raw = """{"state":{"on":true,"bri":128,"transition":7,"ps":-1,"seg":[{"id":0,"start":0,"stop":60,"len":60,"col":[[255,0,0]],"fx":0,"sx":128,"ix":128,"on":true,"bri":255}]},"info":{"ver":"0.14.0","vid":2312080,"leds":{"count":60,"rgbw":false,"maxseg":16,"lc":1,"seglc":[60]},"name":"Desk Strip","udpport":21324,"mac":"AA:BB:CC:DD:EE:FF"}}"""
        val full = json.decodeFromString<WledFullState>(raw)
        assertEquals("Desk Strip", full.info.name)
        assertEquals(1, full.state.seg.size)
        assertEquals(60, full.state.seg[0].len)
    }

    @Test
    fun wledDeviceDefaultValues() {
        val device = WledDevice(ipAddress = "192.168.1.100", name = "Test")
        assertEquals("", device.macAddress)
        assertEquals(0, device.totalLeds)
        assertFalse(device.isOnline)
    }
}
