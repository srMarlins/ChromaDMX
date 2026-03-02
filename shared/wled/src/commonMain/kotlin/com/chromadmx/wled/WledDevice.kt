package com.chromadmx.wled

import kotlinx.serialization.Serializable

@Serializable
data class WledSegmentState(
    val id: Int = 0,
    val start: Int = 0,
    val stop: Int = 0,
    val len: Int = 0,
    val grp: Int = 1,
    val spc: Int = 0,
    val col: List<List<Int>> = listOf(listOf(255, 160, 0)),
    val fx: Int = 0,
    val sx: Int = 128,
    val ix: Int = 128,
    val pal: Int = 0,
    val on: Boolean = true,
    val bri: Int = 255,
)

@Serializable
data class WledLedInfo(
    val count: Int,
    val rgbw: Boolean = false,
    val wv: Int = 0,
    val cct: Boolean = false,
    val maxpwr: Int = 0,
    val maxseg: Int = 1,
    val lc: Int = 1,
    val seglc: List<Int> = emptyList(),
)

@Serializable
data class WledDeviceInfo(
    val ver: String,
    val vid: Long,
    val leds: WledLedInfo,
    val name: String,
    val udpport: Int = 21324,
    val arch: String = "",
    val freeheap: Long = 0,
    val mac: String = "",
)

@Serializable
data class WledState(
    val on: Boolean = true,
    val bri: Int = 128,
    val transition: Int = 7,
    val ps: Int = -1,
    val seg: List<WledSegmentState> = emptyList(),
)

@Serializable
data class WledFullState(
    val state: WledState,
    val info: WledDeviceInfo,
)

data class WledDevice(
    val ipAddress: String,
    val name: String,
    val macAddress: String = "",
    val totalLeds: Int = 0,
    val segments: List<WledSegmentState> = emptyList(),
    val firmwareVersion: String = "",
    val isOnline: Boolean = false,
    val lastSeenMs: Long = 0L,
)
