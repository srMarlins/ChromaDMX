package com.chromadmx.vision.mapping

import com.chromadmx.core.model.Fixture
import com.chromadmx.core.model.Fixture3D
import com.chromadmx.core.model.Vec3
import com.chromadmx.vision.detection.Coord2D

/**
 * Unique identifier for a fixture in the spatial map.
 */
typealias FixtureId = String

/**
 * Result of the scan pipeline: camera-space positions for each fixture's pixels.
 *
 * For single-cell fixtures, the list has one entry (the centroid).
 * For multi-cell bars, the list contains interpolated positions for each pixel.
 */
data class SpatialMap(
    val fixturePositions: Map<FixtureId, List<Coord2D>>
)

/**
 * Builds a [SpatialMap] from scan results and converts camera coordinates
 * to 3D fixture positions.
 *
 * The scan pipeline produces 2D camera coordinates for each fixture. The user
 * then assigns a Z-height per fixture group. This builder combines both to
 * produce [Fixture3D] instances ready for the spatial effect engine.
 *
 * Camera coordinates are normalized: X and Y are mapped from pixel coordinates
 * to a [-1, 1] range based on the frame dimensions, with (0,0) at the center.
 */
class SpatialMapBuilder {

    private val entries = mutableMapOf<FixtureId, List<Coord2D>>()

    /**
     * Record a single-cell fixture's detected centroid.
     */
    fun addSingleCell(fixtureId: FixtureId, centroid: Coord2D) {
        entries[fixtureId] = listOf(centroid)
    }

    /**
     * Record a multi-cell fixture's interpolated pixel positions.
     *
     * @param fixtureId the fixture identifier
     * @param startEndpoint centroid of the first endpoint blob
     * @param endEndpoint centroid of the second endpoint blob
     * @param pixelCount total number of addressable pixels on the bar
     */
    fun addMultiCell(
        fixtureId: FixtureId,
        startEndpoint: Coord2D,
        endEndpoint: Coord2D,
        pixelCount: Int
    ) {
        entries[fixtureId] = PixelInterpolator.interpolate(startEndpoint, endEndpoint, pixelCount)
    }

    /**
     * Record pre-computed pixel positions for a fixture.
     */
    fun addPositions(fixtureId: FixtureId, positions: List<Coord2D>) {
        require(positions.isNotEmpty()) { "Positions list must not be empty" }
        entries[fixtureId] = positions.toList()
    }

    /**
     * Build the spatial map from all recorded entries.
     */
    fun build(): SpatialMap = SpatialMap(entries.toMap())

    /**
     * Convert camera-space 2D coordinates to 3D fixture positions.
     *
     * Camera coordinates are normalized to a [-1, 1] range based on frame
     * dimensions. The user-provided Z-height is assigned directly.
     *
     * For single-cell fixtures, the fixture position is the centroid.
     * For multi-cell fixtures, the fixture position is the center of the bar
     * (average of all pixel positions).
     *
     * @param spatialMap the camera-space spatial map
     * @param fixtures the base fixture definitions to augment with 3D positions
     * @param zHeights Z-height per fixture ID (user-assigned)
     * @param frameWidth camera frame width for normalization
     * @param frameHeight camera frame height for normalization
     * @return list of [Fixture3D] with computed spatial coordinates
     */
    companion object {
        fun toFixture3DList(
            spatialMap: SpatialMap,
            fixtures: Map<FixtureId, Fixture>,
            zHeights: Map<FixtureId, Float>,
            frameWidth: Int,
            frameHeight: Int
        ): List<Fixture3D> {
            return spatialMap.fixturePositions.mapNotNull { (fixtureId, positions) ->
                val fixture = fixtures[fixtureId] ?: return@mapNotNull null
                val z = zHeights[fixtureId] ?: 0f

                // Compute average position (center of bar for multi-cell)
                val avgX = positions.map { it.x }.average().toFloat()
                val avgY = positions.map { it.y }.average().toFloat()

                // Normalize to [-1, 1] range
                val normX = normalizeCoord(avgX, frameWidth)
                val normY = normalizeCoord(avgY, frameHeight)

                Fixture3D(
                    fixture = fixture,
                    position = Vec3(normX, normY, z)
                )
            }
        }

        /**
         * Convert the full set of per-pixel positions to 3D coordinates.
         *
         * This is useful for multi-cell bars where each pixel needs its own
         * 3D position for per-pixel spatial effects.
         *
         * @return map of fixture ID to list of Vec3 positions (one per pixel)
         */
        fun toPixelPositions3D(
            spatialMap: SpatialMap,
            zHeights: Map<FixtureId, Float>,
            frameWidth: Int,
            frameHeight: Int
        ): Map<FixtureId, List<Vec3>> {
            return spatialMap.fixturePositions.mapValues { (fixtureId, positions) ->
                val z = zHeights[fixtureId] ?: 0f
                positions.map { coord ->
                    Vec3(
                        x = normalizeCoord(coord.x, frameWidth),
                        y = normalizeCoord(coord.y, frameHeight),
                        z = z
                    )
                }
            }
        }

        /**
         * Normalize a pixel coordinate to [-1, 1] range.
         * 0 maps to -1, dimension/2 maps to 0, dimension maps to 1.
         */
        internal fun normalizeCoord(pixel: Float, dimension: Int): Float {
            return (pixel / dimension) * 2f - 1f
        }
    }
}
