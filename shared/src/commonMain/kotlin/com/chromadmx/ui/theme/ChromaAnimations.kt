package com.chromadmx.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Centralized animation presets for the ChromaDMX pixel design system.
 *
 * Provides two categories of animation specs:
 * - **Spring presets** for interactive transitions (button press, panel slide, etc.)
 * - **Tween presets** for continuous/looping animations (glow, sparkle, shimmer, etc.)
 *
 * All presets respect the system reduced-motion preference via [ChromaAnimations.resolve],
 * which returns either the full-motion or reduced-motion variant.
 *
 * Usage:
 * ```kotlin
 * // Spring animation:
 * val offset by animateDpAsState(
 *     targetValue = if (expanded) 200.dp else 0.dp,
 *     animationSpec = ChromaAnimations.resolve().panelSlide,
 * )
 *
 * // Tween (infinite) animation:
 * val config = ChromaAnimations.resolve().glowPulse
 * val transition = rememberInfiniteTransition()
 * val glow by transition.animateFloat(
 *     initialValue = 0f,
 *     targetValue = 1f,
 *     animationSpec = infiniteRepeatable(
 *         animation = tween(config.durationMillis, easing = config.easing),
 *         repeatMode = config.repeatMode,
 *     ),
 * )
 * ```
 */
object ChromaAnimations {

    // ── Spring Presets (interactive) ─────────────────────────────────────

    /** Button/toggle press — low bounce, medium stiffness. */
    val buttonPress: SpringSpec<Float> = spring(
        dampingRatio = 0.75f,
        stiffness = 1500f,
    )

    /** Overlays, panels — no bounce, medium-low stiffness. */
    val panelSlide: SpringSpec<Float> = spring(
        dampingRatio = 1.0f,
        stiffness = 400f,
    )

    /** Mascot, playful elements — medium bounce, low stiffness. */
    val mascotBounce: SpringSpec<Float> = spring(
        dampingRatio = 0.5f,
        stiffness = 200f,
    )

    /** Beat-reactive snaps — no bounce, high stiffness. */
    val beatPulse: SpringSpec<Float> = spring(
        dampingRatio = 1.0f,
        stiffness = 10000f,
    )

    /** Card expand/collapse — low bounce, medium-low stiffness. */
    val cardExpand: SpringSpec<Float> = spring(
        dampingRatio = 0.75f,
        stiffness = 400f,
    )

    /** Drag release snap-back — medium bounce, medium stiffness. */
    val dragReturn: SpringSpec<Float> = spring(
        dampingRatio = 0.5f,
        stiffness = 1500f,
    )

    // ── Tween Presets (continuous / looping) ─────────────────────────────

    /**
     * Configuration holder for infinite/repeating tween animations.
     *
     * Callers use these values with `rememberInfiniteTransition().animateFloat()`:
     * ```
     * val config = ChromaAnimations.glowPulse
     * infiniteRepeatable(
     *     animation = tween(config.durationMillis, easing = config.easing),
     *     repeatMode = config.repeatMode,
     * )
     * ```
     */
    data class TweenConfig(
        val durationMillis: Int,
        val easing: androidx.compose.animation.core.Easing,
        val repeatMode: RepeatMode,
    )

    /** Border glow breathing — 1.5s, ease in/out, reversing. */
    val glowPulse = TweenConfig(
        durationMillis = 1500,
        easing = FastOutSlowInEasing,
        repeatMode = RepeatMode.Reverse,
    )

    /** Sparkle particle orbits — 3s, linear, infinite loop. */
    val sparkleOrbit = TweenConfig(
        durationMillis = 3000,
        easing = LinearEasing,
        repeatMode = RepeatMode.Restart,
    )

    /** Divider shimmer gradient sweep — 2s, linear, infinite loop. */
    val shimmerSweep = TweenConfig(
        durationMillis = 2000,
        easing = LinearEasing,
        repeatMode = RepeatMode.Restart,
    )

    /** Blinking cursor alpha — 500ms, linear, reversing. */
    val cursorBlink = TweenConfig(
        durationMillis = 500,
        easing = LinearEasing,
        repeatMode = RepeatMode.Reverse,
    )

    /** Scanline vertical scroll — 8s, linear, infinite loop. */
    val scanlineDrift = TweenConfig(
        durationMillis = 8000,
        easing = LinearEasing,
        repeatMode = RepeatMode.Restart,
    )

    /** Star brightness pulse — 1s, ease in/out, reversing. */
    val starTwinkle = TweenConfig(
        durationMillis = 1000,
        easing = FastOutSlowInEasing,
        repeatMode = RepeatMode.Reverse,
    )

    /** Star rotation — 4s, linear, infinite loop. */
    val starRotate = TweenConfig(
        durationMillis = 4000,
        easing = LinearEasing,
        repeatMode = RepeatMode.Restart,
    )

    // ── Reduced Motion Variants ──────────────────────────────────────────

    /**
     * Reduced-motion variants of all animation presets.
     *
     * When the system requests reduced motion:
     * - All springs become instant (very high stiffness, no bounce).
     * - Continuous/looping animations use static values (no movement).
     */
    object Reduced {

        /** Instant spring — very high stiffness, critically damped (no bounce). */
        val instantSpring: SpringSpec<Float> = spring(
            dampingRatio = 1.0f,
            stiffness = 10000f,
        )

        /**
         * Static glow — holds at 50% brightness, no animation.
         * Callers should check [PixelDesign.reduceMotion] and use this
         * fixed value instead of animating.
         */
        const val STATIC_GLOW_ALPHA = 0.5f

        /**
         * Static cursor — fully visible, no blink.
         */
        const val STATIC_CURSOR_ALPHA = 1.0f

        /**
         * Static star brightness — held at moderate brightness.
         */
        const val STATIC_STAR_ALPHA = 0.7f

        /**
         * Static star rotation — no rotation (0 degrees).
         */
        const val STATIC_STAR_ROTATION = 0f

        /**
         * Disabled tween config — zero duration placeholder.
         * Callers should not animate when reduced motion is active;
         * instead use the static constants above.
         */
        val disabled = TweenConfig(
            durationMillis = 0,
            easing = LinearEasing,
            repeatMode = RepeatMode.Restart,
        )
    }

    // ── Motion-Aware Resolution ──────────────────────────────────────────

    /**
     * Resolved animation set that respects the current reduced-motion preference.
     *
     * Use [resolve] in a composable context to get the correct set:
     * ```
     * val anims = ChromaAnimations.resolve()
     * animateDpAsState(target, animationSpec = anims.buttonPress)
     * ```
     */
    class Resolved(private val reduceMotion: Boolean) {

        // Springs
        val buttonPress: SpringSpec<Float>
            get() = if (reduceMotion) Reduced.instantSpring else ChromaAnimations.buttonPress

        val panelSlide: SpringSpec<Float>
            get() = if (reduceMotion) Reduced.instantSpring else ChromaAnimations.panelSlide

        val mascotBounce: SpringSpec<Float>
            get() = if (reduceMotion) Reduced.instantSpring else ChromaAnimations.mascotBounce

        val beatPulse: SpringSpec<Float>
            get() = if (reduceMotion) Reduced.instantSpring else ChromaAnimations.beatPulse

        val cardExpand: SpringSpec<Float>
            get() = if (reduceMotion) Reduced.instantSpring else ChromaAnimations.cardExpand

        val dragReturn: SpringSpec<Float>
            get() = if (reduceMotion) Reduced.instantSpring else ChromaAnimations.dragReturn

        // Tweens
        val glowPulse: TweenConfig
            get() = if (reduceMotion) Reduced.disabled else ChromaAnimations.glowPulse

        val sparkleOrbit: TweenConfig
            get() = if (reduceMotion) Reduced.disabled else ChromaAnimations.sparkleOrbit

        val shimmerSweep: TweenConfig
            get() = if (reduceMotion) Reduced.disabled else ChromaAnimations.shimmerSweep

        val cursorBlink: TweenConfig
            get() = if (reduceMotion) Reduced.disabled else ChromaAnimations.cursorBlink

        val scanlineDrift: TweenConfig
            get() = if (reduceMotion) Reduced.disabled else ChromaAnimations.scanlineDrift

        val starTwinkle: TweenConfig
            get() = if (reduceMotion) Reduced.disabled else ChromaAnimations.starTwinkle

        val starRotate: TweenConfig
            get() = if (reduceMotion) Reduced.disabled else ChromaAnimations.starRotate

        /** Whether continuous animations should be skipped entirely. */
        val shouldSkipContinuousAnimations: Boolean get() = reduceMotion
    }

    /**
     * Resolve animation presets respecting the current reduced-motion preference.
     *
     * Must be called from a [Composable] context — reads [PixelDesign.reduceMotion].
     */
    @Composable
    @ReadOnlyComposable
    fun resolve(): Resolved = Resolved(PixelDesign.reduceMotion)
}
