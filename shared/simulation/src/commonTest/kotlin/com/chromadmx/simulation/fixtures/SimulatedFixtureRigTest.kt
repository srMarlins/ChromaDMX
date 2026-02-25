package com.chromadmx.simulation.fixtures

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SimulatedFixtureRigTest {

    // ------------------------------------------------------------------ //
    //  Small DJ rig                                                       //
    // ------------------------------------------------------------------ //

    @Test
    fun smallDj_has8Fixtures() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        assertEquals(8, rig.fixtureCount)
    }

    @Test
    fun smallDj_uses1Universe() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        assertEquals(1, rig.universeCount)
        assertTrue(rig.universeIds.contains(0))
    }

    @Test
    fun smallDj_24TotalChannels() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        assertEquals(24, rig.totalChannels)
    }

    @Test
    fun smallDj_fixturesAreInLine() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        // All fixtures should be at the same height (z=2.5m)
        for (fixture in rig.fixtures) {
            assertEquals(2.5f, fixture.position.z, "Fixture ${fixture.fixture.name} should be at z=2.5m")
        }
    }

    @Test
    fun smallDj_fixturesSpanWidth() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val xPositions = rig.fixtures.map { it.position.x }
        val minX = xPositions.min()
        val maxX = xPositions.max()
        // Should span from -3.5 to +3.5 (7m width)
        assertEquals(-3.5f, minX)
        assertEquals(3.5f, maxX)
    }

    @Test
    fun smallDj_channelsAreContiguous() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        var expectedStart = 0
        for (fixture in rig.fixtures) {
            assertEquals(expectedStart, fixture.fixture.channelStart)
            expectedStart += fixture.fixture.channelCount
        }
    }

    @Test
    fun smallDj_allInSameGroup() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        assertEquals(1, rig.groupIds.size)
        assertTrue(rig.groupIds.contains("dj-truss"))
        assertEquals(8, rig.fixturesInGroup("dj-truss").size)
    }

    @Test
    fun smallDj_eachFixtureHas3Channels() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        for (fixture in rig.fixtures) {
            assertEquals(3, fixture.fixture.channelCount, "Fixture ${fixture.fixture.name} should have 3 channels")
        }
    }

    // ------------------------------------------------------------------ //
    //  Truss rig                                                          //
    // ------------------------------------------------------------------ //

    @Test
    fun trussRig_has30Fixtures() {
        val rig = SimulatedFixtureRig(RigPreset.TRUSS_RIG)
        assertEquals(30, rig.fixtureCount)
    }

    @Test
    fun trussRig_uses2Universes() {
        val rig = SimulatedFixtureRig(RigPreset.TRUSS_RIG)
        assertEquals(2, rig.universeCount)
    }

    @Test
    fun trussRig_720TotalChannels() {
        val rig = SimulatedFixtureRig(RigPreset.TRUSS_RIG)
        assertEquals(720, rig.totalChannels)
    }

    @Test
    fun trussRig_eachBarHas24Channels() {
        val rig = SimulatedFixtureRig(RigPreset.TRUSS_RIG)
        for (fixture in rig.fixtures) {
            assertEquals(24, fixture.fixture.channelCount, "Bar ${fixture.fixture.name} should have 24 channels")
        }
    }

    @Test
    fun trussRig_hasTwoGroups() {
        val rig = SimulatedFixtureRig(RigPreset.TRUSS_RIG)
        assertEquals(2, rig.groupIds.size)
        assertTrue(rig.groupIds.contains("front-truss"))
        assertTrue(rig.groupIds.contains("rear-truss"))
    }

    @Test
    fun trussRig_frontTrussAt3m() {
        val rig = SimulatedFixtureRig(RigPreset.TRUSS_RIG)
        val frontFixtures = rig.fixturesInGroup("front-truss")
        assertEquals(15, frontFixtures.size)
        for (fixture in frontFixtures) {
            assertEquals(3.0f, fixture.position.z)
        }
    }

    @Test
    fun trussRig_rearTrussAt4m() {
        val rig = SimulatedFixtureRig(RigPreset.TRUSS_RIG)
        val rearFixtures = rig.fixturesInGroup("rear-truss")
        assertEquals(15, rearFixtures.size)
        for (fixture in rearFixtures) {
            assertEquals(4.0f, fixture.position.z)
        }
    }

    @Test
    fun trussRig_noChannelOverlap() {
        val rig = SimulatedFixtureRig(RigPreset.TRUSS_RIG)
        // Group by universe and check no overlapping channel ranges
        for (universeId in rig.universeIds) {
            val uniFixtures = rig.fixturesOnUniverse(universeId)
            val ranges = uniFixtures.map {
                it.fixture.channelStart until (it.fixture.channelStart + it.fixture.channelCount)
            }
            // Check no ranges overlap
            for (i in ranges.indices) {
                for (j in i + 1 until ranges.size) {
                    val overlap = ranges[i].first < ranges[j].last && ranges[j].first < ranges[i].last
                    assertTrue(!overlap,
                        "Fixtures on universe $universeId have overlapping channels: " +
                            "${ranges[i]} and ${ranges[j]}")
                }
            }
        }
    }

    @Test
    fun trussRig_noChannelExceeds512() {
        val rig = SimulatedFixtureRig(RigPreset.TRUSS_RIG)
        for (fixture in rig.fixtures) {
            val end = fixture.fixture.channelStart + fixture.fixture.channelCount
            assertTrue(end <= 512,
                "Fixture ${fixture.fixture.name} on universe ${fixture.fixture.universeId} " +
                    "exceeds 512 channels: ends at $end")
        }
    }

    // ------------------------------------------------------------------ //
    //  Festival stage rig                                                 //
    // ------------------------------------------------------------------ //

    @Test
    fun festivalStage_has108Fixtures() {
        val rig = SimulatedFixtureRig(RigPreset.FESTIVAL_STAGE)
        assertEquals(108, rig.fixtureCount)
    }

    @Test
    fun festivalStage_usesMultipleUniverses() {
        val rig = SimulatedFixtureRig(RigPreset.FESTIVAL_STAGE)
        assertTrue(rig.universeCount >= 2, "Festival stage should use at least 2 universes")
    }

    @Test
    fun festivalStage_hasMultipleGroups() {
        val rig = SimulatedFixtureRig(RigPreset.FESTIVAL_STAGE)
        assertTrue(rig.groupIds.size >= 5, "Festival stage should have at least 5 groups")
    }

    @Test
    fun festivalStage_hasGroundPars() {
        val rig = SimulatedFixtureRig(RigPreset.FESTIVAL_STAGE)
        val groundPars = rig.fixturesInGroup("ground-pars")
        assertEquals(16, groundPars.size)
        for (fixture in groundPars) {
            assertEquals(0.3f, fixture.position.z)
        }
    }

    @Test
    fun festivalStage_hasMovingHeads() {
        val rig = SimulatedFixtureRig(RigPreset.FESTIVAL_STAGE)
        val movers = rig.fixturesInGroup("mid-truss-movers")
        assertEquals(8, movers.size)
        for (fixture in movers) {
            assertEquals(16, fixture.fixture.channelCount)
        }
    }

    @Test
    fun festivalStage_hasStrobes() {
        val rig = SimulatedFixtureRig(RigPreset.FESTIVAL_STAGE)
        val strobes = rig.fixturesInGroup("high-truss-strobes")
        assertEquals(8, strobes.size)
        for (fixture in strobes) {
            assertEquals(2, fixture.fixture.channelCount)
        }
    }

    @Test
    fun festivalStage_hasSideTowers() {
        val rig = SimulatedFixtureRig(RigPreset.FESTIVAL_STAGE)
        val leftSide = rig.fixturesInGroup("side-tower-left")
        val rightSide = rig.fixturesInGroup("side-tower-right")
        assertEquals(4, leftSide.size)
        assertEquals(4, rightSide.size)
    }

    @Test
    fun festivalStage_noChannelOverlap() {
        val rig = SimulatedFixtureRig(RigPreset.FESTIVAL_STAGE)
        for (universeId in rig.universeIds) {
            val uniFixtures = rig.fixturesOnUniverse(universeId)
            val ranges = uniFixtures.map {
                it.fixture.channelStart until (it.fixture.channelStart + it.fixture.channelCount)
            }
            for (i in ranges.indices) {
                for (j in i + 1 until ranges.size) {
                    val overlap = ranges[i].first < ranges[j].last && ranges[j].first < ranges[i].last
                    assertTrue(!overlap,
                        "Fixtures on universe $universeId have overlapping channels: " +
                            "${ranges[i]} and ${ranges[j]}")
                }
            }
        }
    }

    @Test
    fun festivalStage_noChannelExceeds512() {
        val rig = SimulatedFixtureRig(RigPreset.FESTIVAL_STAGE)
        for (fixture in rig.fixtures) {
            val end = fixture.fixture.channelStart + fixture.fixture.channelCount
            assertTrue(end <= 512,
                "Fixture ${fixture.fixture.name} on universe ${fixture.fixture.universeId} " +
                    "exceeds 512 channels: ends at $end")
        }
    }

    @Test
    fun festivalStage_mixedHeights() {
        val rig = SimulatedFixtureRig(RigPreset.FESTIVAL_STAGE)
        val heights = rig.fixtures.map { it.position.z }.distinct().sorted()
        assertTrue(heights.size >= 4, "Festival stage should have at least 4 different heights, got: $heights")
    }

    // ------------------------------------------------------------------ //
    //  Utility methods                                                    //
    // ------------------------------------------------------------------ //

    @Test
    fun findFixture_existingId_returnsFixture() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        val fixture = rig.findFixture("dj-par-0")
        assertNotNull(fixture)
        assertEquals("DJ PAR 1", fixture.fixture.name)
    }

    @Test
    fun findFixture_nonExistentId_returnsNull() {
        val rig = SimulatedFixtureRig(RigPreset.SMALL_DJ)
        assertNull(rig.findFixture("nonexistent"))
    }

    @Test
    fun fromName_validName_createsRig() {
        val rig = SimulatedFixtureRig.fromName("SMALL_DJ")
        assertNotNull(rig)
        assertEquals(RigPreset.SMALL_DJ, rig.preset)
    }

    @Test
    fun fromName_caseInsensitive() {
        val rig = SimulatedFixtureRig.fromName("small_dj")
        assertNotNull(rig)
        assertEquals(RigPreset.SMALL_DJ, rig.preset)
    }

    @Test
    fun fromName_invalidName_returnsNull() {
        assertNull(SimulatedFixtureRig.fromName("nonexistent"))
    }

    @Test
    fun fixturesOnUniverse_correctFiltering() {
        val rig = SimulatedFixtureRig(RigPreset.TRUSS_RIG)
        val uni0 = rig.fixturesOnUniverse(0)
        val uni1 = rig.fixturesOnUniverse(1)

        // All fixtures should be on either universe 0 or 1
        assertEquals(rig.fixtureCount, uni0.size + uni1.size)
        assertTrue(uni0.isNotEmpty())
        assertTrue(uni1.isNotEmpty())
    }

    // ------------------------------------------------------------------ //
    //  Unique fixture IDs                                                 //
    // ------------------------------------------------------------------ //

    @Test
    fun allPresets_haveUniqueFixtureIds() {
        for (preset in RigPreset.entries) {
            val rig = SimulatedFixtureRig(preset)
            val ids = rig.fixtures.map { it.fixture.fixtureId }
            assertEquals(ids.size, ids.toSet().size,
                "$preset has duplicate fixture IDs: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}")
        }
    }
}
