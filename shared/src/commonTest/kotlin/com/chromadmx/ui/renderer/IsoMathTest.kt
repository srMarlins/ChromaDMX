package com.chromadmx.ui.renderer

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.ui.state.IsoAngle
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IsoMathTest {

    private val tolerance = 0.05f

    private fun assertApprox(expected: Float, actual: Float, msg: String = "") {
        assertTrue(
            abs(expected - actual) < tolerance,
            "$msg expected=$expected actual=$actual diff=${abs(expected - actual)}",
        )
    }

    // -----------------------------------------------------------------------
    // worldToIso round-trips with isoToWorld at each IsoAngle
    // -----------------------------------------------------------------------

    @Test
    fun worldToIso_roundTrip_ZERO() {
        roundTripTest(IsoAngle.ZERO, 3f, 5f)
    }

    @Test
    fun worldToIso_roundTrip_FORTY_FIVE() {
        roundTripTest(IsoAngle.FORTY_FIVE, 3f, 5f)
    }

    @Test
    fun worldToIso_roundTrip_NINETY() {
        roundTripTest(IsoAngle.NINETY, 3f, 5f)
    }

    private fun roundTripTest(angle: IsoAngle, wx: Float, wy: Float) {
        // Project world (wx, wy, 0) to screen, then invert
        val screen = IsoMath.worldToIso(wx, wy, 0f, angle)
        val (rx, ry) = IsoMath.isoToWorld(screen.x, screen.y, angle)
        assertApprox(wx, rx, "roundTrip($angle) x")
        assertApprox(wy, ry, "roundTrip($angle) y")
    }

    @Test
    fun worldToIso_roundTrip_negative_coords() {
        for (angle in IsoAngle.entries) {
            val screen = IsoMath.worldToIso(-4f, -7f, 0f, angle)
            val (rx, ry) = IsoMath.isoToWorld(screen.x, screen.y, angle)
            assertApprox(-4f, rx, "negative roundTrip($angle) x")
            assertApprox(-7f, ry, "negative roundTrip($angle) y")
        }
    }

    @Test
    fun worldToIso_roundTrip_origin() {
        for (angle in IsoAngle.entries) {
            val screen = IsoMath.worldToIso(0f, 0f, 0f, angle)
            val (rx, ry) = IsoMath.isoToWorld(screen.x, screen.y, angle)
            assertApprox(0f, rx, "origin roundTrip($angle) x")
            assertApprox(0f, ry, "origin roundTrip($angle) y")
        }
    }

    // -----------------------------------------------------------------------
    // worldToIso produces expected offsets for known inputs
    // -----------------------------------------------------------------------

    @Test
    fun worldToIso_origin_isZero() {
        for (angle in IsoAngle.entries) {
            val offset = IsoMath.worldToIso(0f, 0f, 0f, angle)
            assertApprox(0f, offset.x, "origin screenX ($angle)")
            assertApprox(0f, offset.y, "origin screenY ($angle)")
        }
    }

    @Test
    fun worldToIso_z_shifts_up() {
        // For any angle, increasing z should decrease screenY (move up)
        for (angle in IsoAngle.entries) {
            val withoutZ = IsoMath.worldToIso(5f, 5f, 0f, angle)
            val withZ = IsoMath.worldToIso(5f, 5f, 10f, angle)
            assertTrue(
                withZ.y < withoutZ.y,
                "z=10 should have smaller screenY than z=0 at $angle",
            )
            // x should be the same
            assertApprox(withoutZ.x, withZ.x, "z should not affect screenX at $angle")
        }
    }

    @Test
    fun worldToIso_vec3_overload_matches_xyz() {
        val pos = Vec3(2f, 3f, 1f)
        for (angle in IsoAngle.entries) {
            val a = IsoMath.worldToIso(pos, angle)
            val b = IsoMath.worldToIso(pos.x, pos.y, pos.z, angle)
            assertApprox(a.x, b.x, "Vec3 overload screenX ($angle)")
            assertApprox(a.y, b.y, "Vec3 overload screenY ($angle)")
        }
    }

    // -----------------------------------------------------------------------
    // sortFixturesByDepth
    // -----------------------------------------------------------------------

    private fun fixture(x: Float, y: Float, z: Float, id: String = "f"): Fixture3D =
        Fixture3D(
            fixture = Fixture(
                fixtureId = id,
                name = id,
                channelStart = 1,
                channelCount = 3,
                universeId = 0,
            ),
            position = Vec3(x, y, z),
        )

    @Test
    fun sortFixturesByDepth_backToFront() {
        // Fixture far away in +x,+y should be drawn first (earlier in list)
        val near = fixture(0f, 0f, 0f, "near")
        val far = fixture(10f, 10f, 0f, "far")
        val mid = fixture(5f, 5f, 0f, "mid")
        val fixtures = listOf(near, mid, far)

        for (angle in IsoAngle.entries) {
            val sorted = IsoMath.sortFixturesByDepth(fixtures, angle)
            // The "far" fixture should appear before "near" in the sorted list
            val farIdx = sorted.indexOfFirst { it.value.fixture.fixtureId == "far" }
            val nearIdx = sorted.indexOfFirst { it.value.fixture.fixtureId == "near" }
            assertTrue(
                farIdx < nearIdx,
                "far should be drawn before near at $angle (farIdx=$farIdx, nearIdx=$nearIdx)",
            )
        }
    }

    @Test
    fun sortFixturesByDepth_singleFixture() {
        val single = listOf(fixture(1f, 2f, 3f))
        val sorted = IsoMath.sortFixturesByDepth(single, IsoAngle.FORTY_FIVE)
        assertEquals(1, sorted.size)
        assertEquals(0, sorted[0].index)
    }

    @Test
    fun sortFixturesByDepth_emptyList() {
        val sorted = IsoMath.sortFixturesByDepth(emptyList(), IsoAngle.FORTY_FIVE)
        assertTrue(sorted.isEmpty())
    }

    @Test
    fun sortFixturesByDepth_samePosition() {
        val fixtures = List(5) { fixture(3f, 3f, 0f, "f$it") }
        val sorted = IsoMath.sortFixturesByDepth(fixtures, IsoAngle.FORTY_FIVE)
        assertEquals(5, sorted.size)
        // All original indices should be present
        assertEquals(setOf(0, 1, 2, 3, 4), sorted.map { it.index }.toSet())
    }

    @Test
    fun sortFixturesByDepth_preservesOriginalIndices() {
        val fixtures = listOf(
            fixture(0f, 0f, 0f, "a"),
            fixture(10f, 10f, 0f, "b"),
            fixture(5f, 5f, 0f, "c"),
        )
        val sorted = IsoMath.sortFixturesByDepth(fixtures, IsoAngle.FORTY_FIVE)
        // Verify that original indices are correctly paired with fixtures
        for (indexed in sorted) {
            assertEquals(
                fixtures[indexed.index].fixture.fixtureId,
                indexed.value.fixture.fixtureId,
            )
        }
    }

    // -----------------------------------------------------------------------
    // toRadians extension
    // -----------------------------------------------------------------------

    @Test
    fun toRadians_valuesInExpectedRange() {
        // All angles should produce positive radians less than PI/2
        for (angle in IsoAngle.entries) {
            val rad = angle.toRadians()
            assertTrue(rad > 0f, "$angle.toRadians() should be positive")
            assertTrue(rad < 1.6f, "$angle.toRadians() should be less than ~PI/2")
        }
        // ZERO < FORTY_FIVE < NINETY
        assertTrue(IsoAngle.ZERO.toRadians() < IsoAngle.FORTY_FIVE.toRadians())
        assertTrue(IsoAngle.FORTY_FIVE.toRadians() < IsoAngle.NINETY.toRadians())
    }
}
