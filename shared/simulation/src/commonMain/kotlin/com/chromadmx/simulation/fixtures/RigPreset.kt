package com.chromadmx.simulation.fixtures

import com.chromadmx.simulation.network.NetworkProfile

/**
 * Predefined fixture rig layouts for testing.
 *
 * Each preset represents a realistic venue configuration with
 * fixtures at realistic 3D positions (in meters, relative to a
 * center point at the stage front).
 *
 * @property networkProfile Default network behavior profile for this rig.
 */
enum class RigPreset(val networkProfile: NetworkProfile = NetworkProfile.Stable) {
    /**
     * Small DJ setup: 8 RGB PAR fixtures in a line.
     * Typical bar/club scenario with a single truss at 2.5m height.
     * Total: 8 fixtures, 24 DMX channels, 1 universe.
     */
    SMALL_DJ,

    /**
     * Truss rig: 30 pixel bars (8 pixels each) on two trusses.
     * Theater/mid-size venue with front truss at z=3.0m and
     * rear truss at z=4.0m.
     * Total: 30 fixtures (240 pixels), 720 DMX channels, 2 universes.
     */
    TRUSS_RIG,

    /**
     * Festival stage: 100+ fixtures across multiple heights.
     * Large outdoor stage with ground bars, mid trusses, and high trusses.
     * Total: 108 fixtures, mix of PAR, pixel bar, moving head, and strobe.
     */
    FESTIVAL_STAGE,

    /**
     * Pixel Bar V: 8 vertical 24-pixel RGB bars in a V-formation.
     * Performer at the tip, 4 bars per side fanning out.
     * DMX addresses scrambled to test vision-based position mapping.
     * Total: 8 fixtures (192 pixels), 576 DMX channels, 2 universes.
     */
    PIXEL_BAR_V,

    /**
     * Desk strip rig: 3 WLED pixel strips (60 pixels each) for a streamer desk.
     * Back-lower, back-upper, and desk-under strips.
     * Total: 3 strips (180 pixels), 540 DMX channels, 2 universes.
     */
    DESK_STRIP,

    /**
     * Room accent rig: 4 WLED pixel strips (75 pixels each) on ceiling/floor edges.
     * Ceiling-back, ceiling-right, floor-left, and floor-front strips.
     * Total: 4 strips (300 pixels), 900 DMX channels, 2 universes.
     */
    ROOM_ACCENT,

    /**
     * Wall panels rig: 9 hexagonal RGB panels in a honeycomb pattern (Nanoleaf-style).
     * Each panel is 1 RGB fixture (3 channels) on the wall.
     * Total: 9 panels, 27 DMX channels, 1 universe.
     */
    WALL_PANELS
}
