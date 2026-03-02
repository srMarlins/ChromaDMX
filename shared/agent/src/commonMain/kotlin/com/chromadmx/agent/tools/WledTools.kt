package com.chromadmx.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.chromadmx.wled.WledApiClient
import com.chromadmx.wled.WledDevice
import com.chromadmx.wled.WledDeviceRegistry
import kotlinx.serialization.Serializable

// -- Named color map --

private val NAMED_COLORS = mapOf(
    "red" to Triple(255, 0, 0),
    "green" to Triple(0, 255, 0),
    "blue" to Triple(0, 0, 255),
    "white" to Triple(255, 255, 255),
    "warm white" to Triple(255, 180, 100),
    "cool white" to Triple(200, 220, 255),
    "orange" to Triple(255, 120, 0),
    "purple" to Triple(128, 0, 255),
    "pink" to Triple(255, 50, 120),
    "yellow" to Triple(255, 255, 0),
    "cyan" to Triple(0, 255, 255),
    "sunset orange" to Triple(255, 80, 20),
)

// -- Helper functions --

/**
 * Resolve WLED devices from a query string.
 * Matches "all" for every device, or matches by device name or IP address (case-insensitive).
 */
internal fun resolveDevices(query: String, registry: WledDeviceRegistry): List<WledDevice> {
    val devices = registry.adoptedDevices.value
    if (query.equals("all", ignoreCase = true)) return devices
    return devices.filter { device ->
        device.name.equals(query, ignoreCase = true) ||
            device.ipAddress.equals(query, ignoreCase = true)
    }
}

/**
 * Parse a color string into an RGB triple.
 * Supports hex colors (e.g. "#FF0000") and named colors (e.g. "warm white").
 * Returns null if the color cannot be parsed.
 */
internal fun parseColor(color: String): Triple<Int, Int, Int>? {
    // Try named color first (case-insensitive)
    val named = NAMED_COLORS[color.lowercase().trim()]
    if (named != null) return named

    // Try hex format: #RRGGBB or RRGGBB
    val hex = color.trim().removePrefix("#")
    if (hex.length != 6) return null
    return try {
        val r = hex.substring(0, 2).toInt(16)
        val g = hex.substring(2, 4).toInt(16)
        val b = hex.substring(4, 6).toInt(16)
        Triple(r, g, b)
    } catch (_: NumberFormatException) {
        null
    }
}

// -- Tools --

class ListWledDevicesTool(
    private val registry: WledDeviceRegistry,
) : SimpleTool<ListWledDevicesTool.Args>(
    argsSerializer = Args.serializer(),
    name = "listWledDevices",
    description = "List all adopted WLED devices with their name, IP address, LED count, segment count, and online status."
) {
    @Serializable
    class Args

    override suspend fun execute(args: Args): String {
        val devices = registry.adoptedDevices.value
        if (devices.isEmpty()) {
            return "0 WLED devices adopted. Use discovery to find and adopt WLED devices."
        }
        val listing = devices.joinToString("\n") { d ->
            val status = if (d.isOnline) "online" else "offline"
            "  - ${d.name} (${d.ipAddress}): ${d.totalLeds} LEDs, ${d.segments.size} segments, $status"
        }
        return "${devices.size} WLED devices:\n$listing"
    }
}

class SetWledBrightnessTool(
    private val apiClient: WledApiClient,
    private val registry: WledDeviceRegistry,
) : SimpleTool<SetWledBrightnessTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setWledBrightness",
    description = "Set the master brightness of a WLED device (0-255). Use 'all' to target every adopted device."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Device name, IP address, or 'all' to target every device")
        val device: String,
        @property:LLMDescription("Brightness level from 0 (off) to 255 (full)")
        val brightness: Int,
    )

    override suspend fun execute(args: Args): String {
        val devices = resolveDevices(args.device, registry)
        if (devices.isEmpty()) {
            return "No WLED device found matching '${args.device}'. Use listWledDevices to see available devices."
        }
        val results = mutableListOf<Pair<String, Boolean>>()
        for (d in devices) {
            results.add(d.name to apiClient.setBrightness(d.ipAddress, args.brightness))
        }
        val successes = results.count { it.second }
        val failures = results.count { !it.second }
        return if (failures == 0) {
            "Set brightness to ${args.brightness} on ${successes} device(s)."
        } else {
            val failedNames = results.filter { !it.second }.joinToString(", ") { it.first }
            "Set brightness on $successes device(s). Failed on: $failedNames"
        }
    }
}

class SetWledColorTool(
    private val apiClient: WledApiClient,
    private val registry: WledDeviceRegistry,
) : SimpleTool<SetWledColorTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setWledColor",
    description = "Set the color of a WLED device. Accepts hex (#FF0000) or named colors (warm white, sunset orange, etc.)."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Device name, IP address, or 'all' to target every device")
        val device: String,
        @property:LLMDescription("Color as hex (#FF0000) or name (warm white, red, blue, etc.)")
        val color: String,
    )

    override suspend fun execute(args: Args): String {
        val rgb = parseColor(args.color)
            ?: return "Unknown color '${args.color}'. Use hex (#RRGGBB) or a named color: ${NAMED_COLORS.keys.joinToString(", ")}."

        val devices = resolveDevices(args.device, registry)
        if (devices.isEmpty()) {
            return "No WLED device found matching '${args.device}'. Use listWledDevices to see available devices."
        }
        val results = mutableListOf<Pair<String, Boolean>>()
        for (d in devices) {
            results.add(d.name to apiClient.setSegmentColor(d.ipAddress, 0, rgb.first, rgb.second, rgb.third))
        }
        val successes = results.count { it.second }
        val failures = results.count { !it.second }
        return if (failures == 0) {
            "Set color to ${args.color} on ${successes} device(s)."
        } else {
            val failedNames = results.filter { !it.second }.joinToString(", ") { it.first }
            "Set color on $successes device(s). Failed on: $failedNames"
        }
    }
}

class SetWledPowerTool(
    private val apiClient: WledApiClient,
    private val registry: WledDeviceRegistry,
) : SimpleTool<SetWledPowerTool.Args>(
    argsSerializer = Args.serializer(),
    name = "setWledPower",
    description = "Turn a WLED device on or off. Use 'all' to target every adopted device."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Device name, IP address, or 'all' to target every device")
        val device: String,
        @property:LLMDescription("true to turn on, false to turn off")
        val on: Boolean,
    )

    override suspend fun execute(args: Args): String {
        val devices = resolveDevices(args.device, registry)
        if (devices.isEmpty()) {
            return "No WLED device found matching '${args.device}'. Use listWledDevices to see available devices."
        }
        val state = if (args.on) "on" else "off"
        val results = mutableListOf<Pair<String, Boolean>>()
        for (d in devices) {
            results.add(d.name to apiClient.setPower(d.ipAddress, args.on))
        }
        val successes = results.count { it.second }
        val failures = results.count { !it.second }
        return if (failures == 0) {
            "Turned $state ${successes} device(s)."
        } else {
            val failedNames = results.filter { !it.second }.joinToString(", ") { it.first }
            "Turned $state $successes device(s). Failed on: $failedNames"
        }
    }
}
