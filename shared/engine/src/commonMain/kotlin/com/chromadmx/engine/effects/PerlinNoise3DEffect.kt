package com.chromadmx.engine.effects

import com.chromadmx.core.EffectParams
import com.chromadmx.core.model.BeatState
import com.chromadmx.core.model.Color
import com.chromadmx.core.model.Vec3
import com.chromadmx.engine.effect.SpatialEffect
import com.chromadmx.engine.util.ColorUtils
import com.chromadmx.engine.util.PerlinNoise

/**
 * 3D Perlin noise animated over time.
 *
 * Samples the noise field at each fixture's 3D position, scaled by [scale],
 * with the time axis scrolling at [speed].  The noise value is mapped onto
 * a color palette.
 *
 * **Params:**
 * - `scale`   (Float)       — spatial frequency (higher = more detail). Default: 1.0.
 * - `speed`   (Float)       — time scroll speed. Default: 0.5.
 * - `palette` (List<Color>) — color palette mapped across noise range. Default: black->white.
 */
class PerlinNoise3DEffect : SpatialEffect {

    override val id: String = ID
    override val name: String = "Perlin Noise 3D"

    private data class Context(
        val scale: Float,
        val zOffset: Float,
        val palette: List<Color>
    )

    override fun prepare(params: EffectParams, time: Float, beat: BeatState): Any {
        val scale = params.getFloat("scale", 1.0f)
        val speed = params.getFloat("speed", 0.5f)
        val palette = params.getColorList("palette", DEFAULT_PALETTE)

        // Scale animation time by BPM ratio (1.0x at 120 BPM baseline)
        val beatTime = if (beat.bpm > 0f) time * (beat.bpm / 120f) else time
        val zOffset = beatTime * speed

        return Context(scale, zOffset, palette)
    }

    override fun compute(pos: Vec3, context: Any?): Color {
        val ctx = context as? Context ?: return Color.BLACK

        val noiseVal = PerlinNoise.noise01(
            pos.x * ctx.scale,
            pos.y * ctx.scale,
            pos.z * ctx.scale + ctx.zOffset
        )

        return ColorUtils.samplePalette(ctx.palette, noiseVal.coerceIn(0f, 1f))
    }

    companion object {
        const val ID = "perlin-noise-3d"
        val DEFAULT_PALETTE = listOf(Color.BLACK, Color.WHITE)
    }
}
