package com.chromadmx.wled

import com.chromadmx.core.db.ChromaDmxDatabase

class WledRepository(private val db: ChromaDmxDatabase) {
    private val queries = db.wledQueries

    fun getAllDevices(): List<WledDevice> {
        return queries.selectAllDevices().executeAsList().map { row ->
            WledDevice(
                ipAddress = row.ip_address,
                name = row.name,
                macAddress = row.mac_address,
                totalLeds = row.total_leds.toInt(),
                firmwareVersion = row.firmware_version,
                lastSeenMs = row.last_seen_ms,
            )
        }
    }

    fun upsertDevice(device: WledDevice, universe: Int = -1) {
        queries.upsertDevice(
            ip_address = device.ipAddress,
            name = device.name,
            mac_address = device.macAddress,
            total_leds = device.totalLeds.toLong(),
            firmware_version = device.firmwareVersion,
            assigned_universe = universe.toLong(),
            last_seen_ms = device.lastSeenMs,
        )
    }

    fun deleteDevice(ip: String) { queries.deleteDevice(ip) }
    fun deviceCount(): Long { return queries.deviceCount().executeAsOne() }
}
