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
    FESTIVAL_STAGE
}
