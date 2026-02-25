package com.chromadmx.engine.util

import kotlin.math.floor

/**
 * Improved Perlin noise (Ken Perlin, 2002) implemented in pure Kotlin.
 *
 * Deterministic: the same (x, y, z) always produces the same value in
 * approximately the range -1..1 (typically ~-0.7..0.7 for 3D).
 *
 * Reference: https://mrl.cs.nyu.edu/~perlin/noise/
 */
object PerlinNoise {

    /* ------------------------------------------------------------------ */
    /*  Permutation table (doubled to avoid wrapping)                      */
    /* ------------------------------------------------------------------ */

    private val p = intArrayOf(
        151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,
        140,36,103,30,69,142,8,99,37,240,21,10,23,190,6,148,
        247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,
        57,177,33,88,237,149,56,87,174,20,125,136,171,168,68,175,
        74,165,71,134,139,48,27,166,77,146,158,231,83,111,229,122,
        60,211,133,230,220,105,92,41,55,46,245,40,244,102,143,54,
        65,25,63,161,1,216,80,73,209,76,132,187,208,89,18,169,
        200,196,135,130,116,188,159,86,164,100,109,198,173,186,3,64,
        52,217,226,250,124,123,5,202,38,147,118,126,255,82,85,212,
        207,206,59,227,47,16,58,17,182,189,28,42,223,183,170,213,
        119,248,152,2,44,154,163,70,221,153,101,155,167,43,172,9,
        129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,104,
        218,246,97,228,251,34,242,193,238,210,144,12,191,179,162,241,
        81,51,145,235,249,14,239,107,49,192,214,31,181,199,106,157,
        184,84,204,176,115,121,50,45,127,4,150,254,138,236,205,93,
        222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
    )

    private val perm = IntArray(512) { p[it and 255] }

    /* ------------------------------------------------------------------ */
    /*  Public API                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * 3D Perlin noise.
     *
     * @return A value approximately in -1..1.
     */
    fun noise(x: Float, y: Float, z: Float): Float {
        // Unit cube containing the point
        val xi = floor(x).toInt() and 255
        val yi = floor(y).toInt() and 255
        val zi = floor(z).toInt() and 255

        // Relative position inside the cube
        val xf = x - floor(x)
        val yf = y - floor(y)
        val zf = z - floor(z)

        // Fade curves
        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)

        // Hash coordinates of the 8 cube corners
        val a  = perm[xi] + yi
        val aa = perm[a] + zi
        val ab = perm[a + 1] + zi
        val b  = perm[xi + 1] + yi
        val ba = perm[b] + zi
        val bb = perm[b + 1] + zi

        // Trilinear interpolation of gradient dot products
        return lerp(w,
            lerp(v,
                lerp(u, grad(perm[aa], xf, yf, zf),
                        grad(perm[ba], xf - 1f, yf, zf)),
                lerp(u, grad(perm[ab], xf, yf - 1f, zf),
                        grad(perm[bb], xf - 1f, yf - 1f, zf))
            ),
            lerp(v,
                lerp(u, grad(perm[aa + 1], xf, yf, zf - 1f),
                        grad(perm[ba + 1], xf - 1f, yf, zf - 1f)),
                lerp(u, grad(perm[ab + 1], xf, yf - 1f, zf - 1f),
                        grad(perm[bb + 1], xf - 1f, yf - 1f, zf - 1f))
            )
        )
    }

    /**
     * Normalized 3D Perlin noise remapped to 0..1.
     */
    fun noise01(x: Float, y: Float, z: Float): Float =
        (noise(x, y, z) + 1f) * 0.5f

    /* ------------------------------------------------------------------ */
    /*  Internal helpers                                                    */
    /* ------------------------------------------------------------------ */

    /** Quintic fade curve: 6t^5 - 15t^4 + 10t^3 */
    private fun fade(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)

    private fun lerp(t: Float, a: Float, b: Float): Float = a + t * (b - a)

    /**
     * Gradient function: select one of 12 gradient directions based on
     * the low 4 bits of [hash].
     */
    private fun grad(hash: Int, x: Float, y: Float, z: Float): Float {
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = when {
            h < 4  -> y
            h == 12 || h == 14 -> x
            else   -> z
        }
        return (if (h and 1 == 0) u else -u) +
               (if (h and 2 == 0) v else -v)
    }
}
