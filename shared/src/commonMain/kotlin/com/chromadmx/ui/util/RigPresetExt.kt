package com.chromadmx.ui.util

import com.chromadmx.simulation.fixtures.RigPreset

/**
 * User-friendly display name for a [RigPreset].
 */
internal fun RigPreset.presetDisplayName(): String = when (this) {
    RigPreset.SMALL_DJ -> "Small DJ"
    RigPreset.TRUSS_RIG -> "Truss Rig"
    RigPreset.FESTIVAL_STAGE -> "Festival Stage"
    RigPreset.PIXEL_BAR_V -> "Pixel Bar V"
    RigPreset.DESK_STRIP -> "Desk Strip"
    RigPreset.ROOM_ACCENT -> "Room Accent"
    RigPreset.WALL_PANELS -> "Wall Panels"
}

/**
 * Short description for a [RigPreset].
 */
internal fun RigPreset.shortDescription(): String = when (this) {
    RigPreset.SMALL_DJ -> "8 RGB PARs, single truss"
    RigPreset.TRUSS_RIG -> "30 pixel bars, dual truss"
    RigPreset.FESTIVAL_STAGE -> "108 mixed fixtures"
    RigPreset.PIXEL_BAR_V -> "8 pixel bars, V-formation"
    RigPreset.DESK_STRIP -> "3 WLED strips, streamer desk"
    RigPreset.ROOM_ACCENT -> "4 WLED strips, room edges"
    RigPreset.WALL_PANELS -> "9 hex panels, honeycomb"
}
