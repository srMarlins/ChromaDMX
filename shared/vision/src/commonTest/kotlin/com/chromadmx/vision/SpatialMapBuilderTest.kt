package com.chromadmx.vision

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Vec3
import com.chromadmx.vision.detection.Coord2D
import com.chromadmx.vision.mapping.SpatialMapBuilder
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpatialMapBuilderTest {

    private val frameWidth = 640
    private val frameHeight = 480

    private fun fixture(id: String) = Fixture(
        fixtureId = id,
        name = "Test $id",
        channelStart = 1,
        channelCount = 3,
        universeId = 1
    )

    // -----------------------------------------------------------------------
    // Building SpatialMap
    // -----------------------------------------------------------------------

    @Test
    fun build_single_cell_fixture() {
        val builder = SpatialMapBuilder()
        builder.addSingleCell("f1", Coord2D(320f, 240f))
        val map = builder.build()

        assertEquals(1, map.fixturePositions.size)
        assertEquals(1, map.fixturePositions["f1"]!!.size)
        assertEquals(320f, map.fixturePositions["f1"]!![0].x, 0.01f)
        assertEquals(240f, map.fixturePositions["f1"]!![0].y, 0.01f)
    }

    @Test
    fun build_multi_cell_fixture() {
        val builder = SpatialMapBuilder()
        builder.addMultiCell("bar1", Coord2D(100f, 200f), Coord2D(500f, 200f), 5)
        val map = builder.build()

        assertEquals(1, map.fixturePositions.size)
        val positions = map.fixturePositions["bar1"]!!
        assertEquals(5, positions.size)
        assertEquals(100f, positions[0].x, 0.01f)
        assertEquals(500f, positions[4].x, 0.01f)
    }

    @Test
    fun build_multiple_fixtures() {
        val builder = SpatialMapBuilder()
        builder.addSingleCell("spot1", Coord2D(100f, 100f))
        builder.addSingleCell("spot2", Coord2D(500f, 350f))
        builder.addMultiCell("bar1", Coord2D(50f, 400f), Coord2D(600f, 400f), 10)
        val map = builder.build()

        assertEquals(3, map.fixturePositions.size)
        assertEquals(1, map.fixturePositions["spot1"]!!.size)
        assertEquals(1, map.fixturePositions["spot2"]!!.size)
        assertEquals(10, map.fixturePositions["bar1"]!!.size)
    }

    // -----------------------------------------------------------------------
    // Coordinate normalization
    // -----------------------------------------------------------------------

    @Test
    fun normalizeCoord_maps_center_to_zero() {
        val result = SpatialMapBuilder.normalizeCoord(320f, 640)
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun normalizeCoord_maps_origin_to_negative_one() {
        val result = SpatialMapBuilder.normalizeCoord(0f, 640)
        assertEquals(-1f, result, 0.01f)
    }

    @Test
    fun normalizeCoord_maps_max_to_positive_one() {
        val result = SpatialMapBuilder.normalizeCoord(640f, 640)
        assertEquals(1f, result, 0.01f)
    }

    // -----------------------------------------------------------------------
    // toFixture3DList
    // -----------------------------------------------------------------------

    @Test
    fun toFixture3DList_single_cell_at_center() {
        val builder = SpatialMapBuilder()
        builder.addSingleCell("f1", Coord2D(320f, 240f))
        val map = builder.build()

        val fixtures = mapOf("f1" to fixture("f1"))
        val zHeights = mapOf("f1" to 2.5f)

        val result = SpatialMapBuilder.toFixture3DList(map, fixtures, zHeights, frameWidth, frameHeight)
        assertEquals(1, result.size)

        val f3d = result[0]
        assertEquals("f1", f3d.fixture.fixtureId)
        assertEquals(0f, f3d.position.x, 0.01f)
        assertEquals(0f, f3d.position.y, 0.01f)
        assertEquals(2.5f, f3d.position.z, 0.01f)
    }

    @Test
    fun toFixture3DList_multi_cell_uses_average_position() {
        val builder = SpatialMapBuilder()
        // Bar from x=100 to x=500, y=240 (center)
        builder.addMultiCell("bar1", Coord2D(100f, 240f), Coord2D(500f, 240f), 5)
        val map = builder.build()

        val fixtures = mapOf("bar1" to fixture("bar1"))
        val zHeights = mapOf("bar1" to 1.0f)

        val result = SpatialMapBuilder.toFixture3DList(map, fixtures, zHeights, frameWidth, frameHeight)
        assertEquals(1, result.size)

        // Average X position: (100+200+300+400+500)/5 = 300
        // Normalized: (300/640)*2 - 1 = -0.0625
        val expectedNormX = SpatialMapBuilder.normalizeCoord(300f, frameWidth)
        assertEquals(expectedNormX, result[0].position.x, 0.01f)
        assertEquals(1.0f, result[0].position.z, 0.01f)
    }

    @Test
    fun toFixture3DList_skips_missing_fixtures() {
        val builder = SpatialMapBuilder()
        builder.addSingleCell("f1", Coord2D(320f, 240f))
        builder.addSingleCell("f2", Coord2D(100f, 100f))
        val map = builder.build()

        // Only f1 in the fixture map — f2 should be skipped
        val fixtures = mapOf("f1" to fixture("f1"))
        val zHeights = mapOf("f1" to 0f, "f2" to 0f)

        val result = SpatialMapBuilder.toFixture3DList(map, fixtures, zHeights, frameWidth, frameHeight)
        assertEquals(1, result.size)
        assertEquals("f1", result[0].fixture.fixtureId)
    }

    @Test
    fun toFixture3DList_uses_zero_z_when_not_specified() {
        val builder = SpatialMapBuilder()
        builder.addSingleCell("f1", Coord2D(320f, 240f))
        val map = builder.build()

        val fixtures = mapOf("f1" to fixture("f1"))
        val zHeights = emptyMap<String, Float>()  // no Z assigned

        val result = SpatialMapBuilder.toFixture3DList(map, fixtures, zHeights, frameWidth, frameHeight)
        assertEquals(0f, result[0].position.z, 0.01f)
    }

    // -----------------------------------------------------------------------
    // toPixelPositions3D
    // -----------------------------------------------------------------------

    @Test
    fun toPixelPositions3D_maps_each_pixel_independently() {
        val builder = SpatialMapBuilder()
        builder.addMultiCell("bar1", Coord2D(0f, 240f), Coord2D(640f, 240f), 3)
        val map = builder.build()

        val zHeights = mapOf("bar1" to 3.0f)
        val result = SpatialMapBuilder.toPixelPositions3D(map, zHeights, frameWidth, frameHeight)

        assertEquals(1, result.size)
        val positions = result["bar1"]!!
        assertEquals(3, positions.size)

        // First pixel at x=0: normalized to -1
        assertEquals(-1f, positions[0].x, 0.01f)
        // Middle pixel at x=320: normalized to 0
        assertEquals(0f, positions[1].x, 0.01f)
        // Last pixel at x=640: normalized to 1
        assertEquals(1f, positions[2].x, 0.01f)

        // All Z values should be 3.0
        positions.forEach { assertEquals(3.0f, it.z, 0.01f) }
    }

    // -----------------------------------------------------------------------
    // addPositions
    // -----------------------------------------------------------------------

    @Test
    fun addPositions_stores_pre_computed_list() {
        val builder = SpatialMapBuilder()
        val positions = listOf(
            Coord2D(10f, 20f),
            Coord2D(30f, 40f),
            Coord2D(50f, 60f)
        )
        builder.addPositions("custom", positions)
        val map = builder.build()

        assertEquals(3, map.fixturePositions["custom"]!!.size)
        assertEquals(10f, map.fixturePositions["custom"]!![0].x, 0.01f)
    }

    @Test
    fun addPositions_rejects_empty_list() {
        val builder = SpatialMapBuilder()
        try {
            builder.addPositions("empty", emptyList())
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}
