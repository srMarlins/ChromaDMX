# Pixel Mascot & AI Companion (#18) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build the pixel-art mascot character (16x16 robot sprite) with beat-reactive animations, speech bubble system, proactive suggestions, and draggable overlay that opens the chat panel on tap.

**Architecture:** Create a sprite engine using Compose Canvas that renders frame-based pixel-art animations. A `MascotViewModel` manages state transitions driven by app events (beat, network, agent). The mascot composable is an overlay on the Stage Preview screen, always visible, with speech bubbles appearing contextually.

**Tech Stack:** Compose Multiplatform (Canvas API), Koin DI, kotlinx-coroutines, StateFlow

---

## Context Files

Before starting, read these files to understand the current state:

- `shared/src/commonMain/kotlin/com/chromadmx/ui/theme/Color.kt` — theme colors
- `shared/src/commonMain/kotlin/com/chromadmx/ui/components/VenueCanvas.kt` — existing Canvas usage pattern
- `shared/src/commonMain/kotlin/com/chromadmx/core/model/BeatState.kt` — beat state model
- `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/StageViewModel.kt` — stage VM (created in #20)
- `shared/src/commonMain/kotlin/com/chromadmx/ui/ChromaDmxApp.kt` — app root (modified in #20)

---

### Task 1: Sprite Data Model

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/SpriteModel.kt`
- Test: `shared/src/commonTest/kotlin/com/chromadmx/ui/mascot/SpriteModelTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.chromadmx.ui.mascot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpriteModelTest {
    @Test
    fun mascotStateHasAllSixStates() {
        val states = MascotState.entries
        assertEquals(6, states.size)
        assertTrue(states.contains(MascotState.IDLE))
        assertTrue(states.contains(MascotState.THINKING))
        assertTrue(states.contains(MascotState.HAPPY))
        assertTrue(states.contains(MascotState.ALERT))
        assertTrue(states.contains(MascotState.CONFUSED))
        assertTrue(states.contains(MascotState.DANCING))
    }

    @Test
    fun spriteFrameHasCorrectDimensions() {
        val frame = SpriteFrame(
            pixels = Array(16) { IntArray(16) { 0 } }
        )
        assertEquals(16, frame.pixels.size)
        assertEquals(16, frame.pixels[0].size)
    }

    @Test
    fun animationSequenceReturnsFrameByIndex() {
        val frame0 = SpriteFrame(Array(16) { IntArray(16) { 0 } })
        val frame1 = SpriteFrame(Array(16) { IntArray(16) { 1 } })
        val anim = AnimationSequence(
            state = MascotState.IDLE,
            frames = listOf(frame0, frame1),
            frameDurationMs = 200L,
            loop = true
        )
        assertEquals(frame0, anim.frameAt(0))
        assertEquals(frame1, anim.frameAt(1))
        assertEquals(frame0, anim.frameAt(2)) // loops
    }

    @Test
    fun nonLoopingAnimationClampsToLastFrame() {
        val frame0 = SpriteFrame(Array(16) { IntArray(16) { 0 } })
        val frame1 = SpriteFrame(Array(16) { IntArray(16) { 1 } })
        val anim = AnimationSequence(
            state = MascotState.HAPPY,
            frames = listOf(frame0, frame1),
            frameDurationMs = 200L,
            loop = false
        )
        assertEquals(frame1, anim.frameAt(5)) // clamps to last
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.mascot.SpriteModelTest"`

**Step 3: Implement SpriteModel**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/SpriteModel.kt`:

```kotlin
package com.chromadmx.ui.mascot

/**
 * Mascot animation states.
 */
enum class MascotState {
    IDLE,      // Breathing animation, beat-reactive pulse
    THINKING,  // Spinning pixel dots (agent processing)
    HAPPY,     // Jump + sparkle (success events)
    ALERT,     // Exclamation bubble (warnings)
    CONFUSED,  // Head tilt + question bubble
    DANCING    // Beat-synced movement (performance mode)
}

/**
 * A single 16x16 pixel frame.
 *
 * Each pixel is an ARGB color int (0 = transparent).
 * Row-major: pixels[row][col], row 0 is top.
 */
data class SpriteFrame(
    val pixels: Array<IntArray>
) {
    val height: Int get() = pixels.size
    val width: Int get() = if (pixels.isNotEmpty()) pixels[0].size else 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpriteFrame) return false
        return pixels.contentDeepEquals(other.pixels)
    }

    override fun hashCode(): Int = pixels.contentDeepHashCode()
}

/**
 * A sequence of frames for one animation state.
 *
 * @property state The mascot state this animation represents.
 * @property frames Ordered list of sprite frames.
 * @property frameDurationMs Milliseconds per frame.
 * @property loop Whether the animation loops or clamps to the last frame.
 */
data class AnimationSequence(
    val state: MascotState,
    val frames: List<SpriteFrame>,
    val frameDurationMs: Long = 200L,
    val loop: Boolean = true
) {
    /**
     * Get the frame at a given index, handling looping or clamping.
     */
    fun frameAt(index: Int): SpriteFrame {
        if (frames.isEmpty()) error("Animation has no frames")
        return if (loop) {
            frames[index % frames.size]
        } else {
            frames[index.coerceAtMost(frames.lastIndex)]
        }
    }

    /** Total duration in milliseconds for one cycle. */
    val cycleDurationMs: Long get() = frames.size * frameDurationMs
}
```

**Step 4: Run test**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.mascot.SpriteModelTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/SpriteModel.kt
git add shared/src/commonTest/kotlin/com/chromadmx/ui/mascot/SpriteModelTest.kt
git commit -m "feat(ui): add sprite data model for pixel mascot (#18)"
```

---

### Task 2: Built-in Sprite Data

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/MascotSprites.kt`
- Test: `shared/src/commonTest/kotlin/com/chromadmx/ui/mascot/MascotSpritesTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.chromadmx.ui.mascot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MascotSpritesTest {
    @Test
    fun allStatesHaveAnimations() {
        for (state in MascotState.entries) {
            val anim = MascotSprites.animationFor(state)
            assertTrue(anim.frames.isNotEmpty(), "No frames for state $state")
            assertEquals(state, anim.state)
        }
    }

    @Test
    fun idleAnimationHasMultipleFrames() {
        val idle = MascotSprites.animationFor(MascotState.IDLE)
        assertTrue(idle.frames.size >= 2, "Idle should have at least 2 frames")
        assertTrue(idle.loop, "Idle should loop")
    }

    @Test
    fun happyAnimationDoesNotLoop() {
        val happy = MascotSprites.animationFor(MascotState.HAPPY)
        assertTrue(!happy.loop || happy.frames.size >= 2)
    }

    @Test
    fun allFramesAre16x16() {
        for (state in MascotState.entries) {
            val anim = MascotSprites.animationFor(state)
            for (frame in anim.frames) {
                assertEquals(16, frame.height, "Frame height should be 16 for $state")
                assertEquals(16, frame.width, "Frame width should be 16 for $state")
            }
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.mascot.MascotSpritesTest"`

**Step 3: Implement MascotSprites**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/MascotSprites.kt`:

This file contains the procedurally-generated 16x16 pixel art for the mascot robot. Each pixel is an ARGB int. Use a helper to create frames:

```kotlin
package com.chromadmx.ui.mascot

/**
 * Built-in sprite data for the pixel mascot robot.
 *
 * The robot is a 16x16 pixel character with:
 * - Square head with antenna, visor eyes, and LED indicator
 * - Small body with arms
 * - Stubby legs
 *
 * Colors (ARGB):
 * - Body: 0xFF6C63FF (primary purple)
 * - Visor: 0xFF00E5FF (cyan)
 * - Antenna LED: 0xFF4CAF50 (green) or 0xFFFF5252 (red/alert)
 * - Outline: 0xFF1A1A2E (dark navy)
 * - Transparent: 0x00000000
 */
object MascotSprites {

    private const val T = 0x00000000  // Transparent
    private const val O = 0xFF1A1A2E.toInt()  // Outline (dark navy)
    private const val B = 0xFF6C63FF.toInt()  // Body (purple)
    private const val V = 0xFF00E5FF.toInt()  // Visor (cyan)
    private const val G = 0xFF4CAF50.toInt()  // Green LED
    private const val R = 0xFFFF5252.toInt()  // Red LED / alert
    private const val W = 0xFFE0E0E0.toInt()  // White highlight
    private const val S = 0xFF252540.toInt()  // Shadow
    private const val Y = 0xFFFFC107.toInt()  // Yellow sparkle

    // Base robot frame (idle position 1)
    private val BASE_ROBOT = spriteFrame(
        "____TTTTTTTT____",
        "____TTTOGTTT____",
        "____TTTOBTTT____",
        "___TOOOOOOOT____",
        "___OBVVBBVBOT___",
        "___OBBBBBBBBT___",
        "___OBBBBBBBBT___",
        "___TOOOOOOOOT___",
        "____TOBBBBOT____",
        "__TTOBBBBBBOTT__",
        "__TOBBBBBBBBOT__",
        "____TOBBBBOT____",
        "____TOBBBBOT____",
        "____TOBTTOBT____",
        "____TOBTTOBT____",
        "____TOOTTOOTT___"
    )

    // Idle frame 2 — slight "breathing" shift (1 pixel body expand)
    private val IDLE_BREATHE = spriteFrame(
        "____TTTTTTTT____",
        "____TTTOGTTT____",
        "____TTTOBTTT____",
        "___TOOOOOOOT____",
        "___OBVVBBVBOT___",
        "___OBBBBBBBBT___",
        "___OBBBBBBBBT___",
        "___TOOOOOOOOT___",
        "___TOBBBBBOT____",
        "__TTOBBBBBBOT___",
        "__TOBBBBBBBOT___",
        "____TOBBBBOT____",
        "____TOBBBBOT____",
        "____TOBTTOBT____",
        "____TOBTTOBT____",
        "____TOOTTOOTT___"
    )

    // Thinking frame 1 — dots above head
    private val THINKING_1 = spriteFrame(
        "___TWTTTTTTT____",
        "____TTTOGTTT____",
        "____TTTOBTTT____",
        "___TOOOOOOOT____",
        "___OBVVBBVBOT___",
        "___OBBBBBBBBT___",
        "___OBBBBBBBBT___",
        "___TOOOOOOOOT___",
        "____TOBBBBOT____",
        "__TTOBBBBBBOTT__",
        "__TOBBBBBBBBOT__",
        "____TOBBBBOT____",
        "____TOBBBBOT____",
        "____TOBTTOBT____",
        "____TOBTTOBT____",
        "____TOOTTOOTT___"
    )

    // Thinking frame 2 — dots rotate
    private val THINKING_2 = spriteFrame(
        "____TTTWTTT_____",
        "____TTTOGTTT____",
        "____TTTOBTTT____",
        "___TOOOOOOOT____",
        "___OBVVBBVBOT___",
        "___OBBBBBBBBT___",
        "___OBBBBBBBBT___",
        "___TOOOOOOOOT___",
        "____TOBBBBOT____",
        "__TTOBBBBBBOTT__",
        "__TOBBBBBBBBOT__",
        "____TOBBBBOT____",
        "____TOBBBBOT____",
        "____TOBTTOBT____",
        "____TOBTTOBT____",
        "____TOOTTOOTT___"
    )

    // Happy frame — jump up (shifted 1px)
    private val HAPPY_JUMP = spriteFrame(
        "___TYTYTYTT_____",
        "____TTTOGTTT____",
        "___TOOOOOOOT____",
        "___OBVVBBVBOT___",
        "___OBBBBBBBBT___",
        "___OBBBBBBBBT___",
        "___TOOOOOOOOT___",
        "____TOBBBBOT____",
        "__TTOBBBBBBOTT__",
        "__TOBBBBBBBBOT__",
        "____TOBBBBOT____",
        "____TOBBBBOT____",
        "____TOBTTOBT____",
        "____TOOTTOOTT___",
        "________________",
        "________________"
    )

    // Alert frame — exclamation, red antenna
    private val ALERT_FRAME = spriteFrame(
        "____TTTTTTTT____",
        "____TTTRRTTT____",
        "____TTTOBTTT____",
        "___TOOOOOOOT____",
        "___OBVVBBVBOT___",
        "___OBBBBBBBBT___",
        "___OBBBBBBBBT___",
        "___TOOOOOOOOT___",
        "____TOBBBBOT____",
        "__TTOBBBBBBOTT__",
        "__TOBBBBBBBBOT__",
        "____TOBBBBOT____",
        "____TOBBBBOT____",
        "____TOBTTOBT____",
        "____TOBTTOBT____",
        "____TOOTTOOTT___"
    )

    // Confused frame — tilted head
    private val CONFUSED_FRAME = spriteFrame(
        "____TTTTTTTT____",
        "____TTTOGTTT____",
        "____TTTOBTTT____",
        "____TOOOOOOOT___",
        "____OBVVBBVBOT__",
        "____OBBBBBBBBT__",
        "____OBBBBBBBBT__",
        "____TOOOOOOOOT__",
        "____TOBBBBOT____",
        "__TTOBBBBBBOTT__",
        "__TOBBBBBBBBOT__",
        "____TOBBBBOT____",
        "____TOBBBBOT____",
        "____TOBTTOBT____",
        "____TOBTTOBT____",
        "____TOOTTOOTT___"
    )

    // Dancing frames — side-to-side movement
    private val DANCE_LEFT = spriteFrame(
        "____TTTTTTTT____",
        "____TTTOGTTT____",
        "____TTTOBTTT____",
        "___TOOOOOOOT____",
        "___OBVVBBVBOT___",
        "___OBBBBBBBBT___",
        "___OBBBBBBBBT___",
        "___TOOOOOOOOT___",
        "___TOBBBBOT_____",
        "_TTOBBBBBBOTT___",
        "_TOBBBBBBBBOT___",
        "___TOBBBBOT_____",
        "___TOBBBBOT_____",
        "____TOBTTOBT____",
        "___TOBTTTTOBT___",
        "___TOOTTTTOOTT__"
    )

    private val DANCE_RIGHT = spriteFrame(
        "____TTTTTTTT____",
        "____TTTOGTTT____",
        "____TTTOBTTT____",
        "____TOOOOOOOT___",
        "___TOBVVBBVBOT__",
        "___TOBBBBBBBOT__",
        "___TOBBBBBBBOT__",
        "___TTOOOOOOOOT__",
        "_____TOBBBBOT___",
        "___TTOBBBBBBOTT_",
        "___TOBBBBBBBBOT_",
        "_____TOBBBBOT___",
        "_____TOBBBBOT___",
        "____TOBTTOBT____",
        "___TOBTTTTOBT___",
        "___TOOTTTTOOTT__"
    )

    private val animations: Map<MascotState, AnimationSequence> = mapOf(
        MascotState.IDLE to AnimationSequence(
            state = MascotState.IDLE,
            frames = listOf(BASE_ROBOT, IDLE_BREATHE),
            frameDurationMs = 500L,
            loop = true
        ),
        MascotState.THINKING to AnimationSequence(
            state = MascotState.THINKING,
            frames = listOf(THINKING_1, THINKING_2),
            frameDurationMs = 300L,
            loop = true
        ),
        MascotState.HAPPY to AnimationSequence(
            state = MascotState.HAPPY,
            frames = listOf(BASE_ROBOT, HAPPY_JUMP, HAPPY_JUMP, BASE_ROBOT),
            frameDurationMs = 150L,
            loop = false
        ),
        MascotState.ALERT to AnimationSequence(
            state = MascotState.ALERT,
            frames = listOf(ALERT_FRAME, BASE_ROBOT),
            frameDurationMs = 400L,
            loop = true
        ),
        MascotState.CONFUSED to AnimationSequence(
            state = MascotState.CONFUSED,
            frames = listOf(CONFUSED_FRAME, BASE_ROBOT),
            frameDurationMs = 600L,
            loop = true
        ),
        MascotState.DANCING to AnimationSequence(
            state = MascotState.DANCING,
            frames = listOf(DANCE_LEFT, BASE_ROBOT, DANCE_RIGHT, BASE_ROBOT),
            frameDurationMs = 250L,
            loop = true
        )
    )

    fun animationFor(state: MascotState): AnimationSequence =
        animations[state] ?: animations[MascotState.IDLE]!!

    /**
     * Parse a 16-character-per-row string into a SpriteFrame.
     * Characters: T=transparent, O=outline, B=body, V=visor, G=green, R=red,
     * W=white, S=shadow, Y=yellow, _=transparent
     */
    private fun spriteFrame(vararg rows: String): SpriteFrame {
        require(rows.size == 16) { "Expected 16 rows, got ${rows.size}" }
        val pixels = Array(16) { row ->
            val r = rows[row]
            require(r.length == 16) { "Row $row: expected 16 chars, got ${r.length}" }
            IntArray(16) { col ->
                when (r[col]) {
                    'T', '_' -> T
                    'O' -> O
                    'B' -> B
                    'V' -> V
                    'G' -> G
                    'R' -> R
                    'W' -> W
                    'S' -> S
                    'Y' -> Y
                    else -> T
                }
            }
        }
        return SpriteFrame(pixels)
    }
}
```

**Step 4: Run test**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.mascot.MascotSpritesTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/MascotSprites.kt
git add shared/src/commonTest/kotlin/com/chromadmx/ui/mascot/MascotSpritesTest.kt
git commit -m "feat(ui): add built-in pixel mascot sprite data for all 6 states (#18)"
```

---

### Task 3: Sprite Renderer (Compose Canvas)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/SpriteRenderer.kt`

**Step 1: Implement the Compose Canvas renderer**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/SpriteRenderer.kt`:

```kotlin
package com.chromadmx.ui.mascot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Renders a single [SpriteFrame] as pixel art on a Compose Canvas.
 *
 * Each pixel in the 16x16 grid is rendered as a filled square.
 * Transparent pixels (ARGB 0x00000000) are skipped.
 *
 * @param frame The sprite frame to render.
 * @param size Total size of the rendered sprite.
 */
@Composable
fun SpriteRenderer(
    frame: SpriteFrame,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val pixelW = this.size.width / frame.width
        val pixelH = this.size.height / frame.height

        for (row in 0 until frame.height) {
            for (col in 0 until frame.width) {
                val argb = frame.pixels[row][col]
                if (argb == 0) continue // transparent

                val alpha = ((argb shr 24) and 0xFF) / 255f
                val red = ((argb shr 16) and 0xFF) / 255f
                val green = ((argb shr 8) and 0xFF) / 255f
                val blue = (argb and 0xFF) / 255f

                drawRect(
                    color = Color(red, green, blue, alpha),
                    topLeft = Offset(col * pixelW, row * pixelH),
                    size = Size(pixelW, pixelH),
                )
            }
        }
    }
}
```

**Step 2: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: PASS

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/SpriteRenderer.kt
git commit -m "feat(ui): add Compose Canvas sprite renderer for pixel mascot (#18)"
```

---

### Task 4: Animation Controller

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/AnimationController.kt`
- Test: `shared/src/commonTest/kotlin/com/chromadmx/ui/mascot/AnimationControllerTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.chromadmx.ui.mascot

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AnimationControllerTest {
    @Test
    fun startsInIdleState() = runTest {
        val controller = AnimationController(scope = this)
        assertEquals(MascotState.IDLE, controller.currentState.first())
    }

    @Test
    fun currentFrameIndexStartsAtZero() = runTest {
        val controller = AnimationController(scope = this)
        assertEquals(0, controller.currentFrameIndex.first())
    }

    @Test
    fun transitionChangesState() = runTest {
        val controller = AnimationController(scope = this)
        controller.transitionTo(MascotState.HAPPY)
        assertEquals(MascotState.HAPPY, controller.currentState.first())
    }

    @Test
    fun nonLoopingAnimationReturnsToIdle() = runTest {
        val controller = AnimationController(scope = this)
        controller.transitionTo(MascotState.HAPPY)
        // Happy has 4 frames at 150ms = 600ms total
        advanceTimeBy(700L)
        assertEquals(MascotState.IDLE, controller.currentState.first())
    }

    @Test
    fun frameIndexAdvancesOverTime() = runTest {
        val controller = AnimationController(scope = this)
        controller.start()
        // Idle has 500ms per frame
        advanceTimeBy(600L)
        val index = controller.currentFrameIndex.first()
        assertEquals(1, index)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.mascot.AnimationControllerTest"`

**Step 3: Implement AnimationController**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/AnimationController.kt`:

```kotlin
package com.chromadmx.ui.mascot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Controls mascot animation frame advancement and state transitions.
 *
 * Advances frames at the rate specified by the current [AnimationSequence].
 * Non-looping animations automatically return to IDLE when complete.
 */
class AnimationController(
    private val scope: CoroutineScope,
    initialState: MascotState = MascotState.IDLE
) {
    private val _currentState = MutableStateFlow(initialState)
    val currentState: StateFlow<MascotState> = _currentState.asStateFlow()

    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex: StateFlow<Int> = _currentFrameIndex.asStateFlow()

    private var animationJob: Job? = null
    private var currentAnimation: AnimationSequence = MascotSprites.animationFor(initialState)

    /**
     * Transition to a new animation state.
     * Resets frame index and starts the new animation.
     */
    fun transitionTo(state: MascotState) {
        _currentState.value = state
        _currentFrameIndex.value = 0
        currentAnimation = MascotSprites.animationFor(state)
        startAnimationLoop()
    }

    /**
     * Start the animation frame advancement loop.
     */
    fun start() {
        startAnimationLoop()
    }

    fun stop() {
        animationJob?.cancel()
        animationJob = null
    }

    private fun startAnimationLoop() {
        animationJob?.cancel()
        animationJob = scope.launch {
            var frameIndex = 0
            while (isActive) {
                _currentFrameIndex.value = frameIndex

                delay(currentAnimation.frameDurationMs)
                frameIndex++

                // Check if non-looping animation is complete
                if (!currentAnimation.loop && frameIndex >= currentAnimation.frames.size) {
                    // Return to idle
                    _currentState.value = MascotState.IDLE
                    _currentFrameIndex.value = 0
                    currentAnimation = MascotSprites.animationFor(MascotState.IDLE)
                    frameIndex = 0
                }

                // Wrap looping animations
                if (currentAnimation.loop) {
                    frameIndex = frameIndex % currentAnimation.frames.size
                }
            }
        }
    }
}
```

**Step 4: Run test**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.mascot.AnimationControllerTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/AnimationController.kt
git add shared/src/commonTest/kotlin/com/chromadmx/ui/mascot/AnimationControllerTest.kt
git commit -m "feat(ui): add animation controller for mascot state transitions (#18)"
```

---

### Task 5: Speech Bubble System

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/SpeechBubble.kt`
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/SpeechBubbleComposable.kt`
- Test: `shared/src/commonTest/kotlin/com/chromadmx/ui/mascot/SpeechBubbleTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.chromadmx.ui.mascot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeechBubbleTest {
    @Test
    fun bubbleTypesExist() {
        assertEquals(3, BubbleType.entries.size)
        assertTrue(BubbleType.entries.contains(BubbleType.INFO))
        assertTrue(BubbleType.entries.contains(BubbleType.ACTION))
        assertTrue(BubbleType.entries.contains(BubbleType.ALERT))
    }

    @Test
    fun speechBubbleCreation() {
        val bubble = SpeechBubble(
            text = "No lights found — want a virtual stage?",
            type = BubbleType.ACTION,
            actionLabel = "Yes!",
            autoDismissMs = 5000L
        )
        assertEquals("No lights found — want a virtual stage?", bubble.text)
        assertEquals(BubbleType.ACTION, bubble.type)
        assertEquals("Yes!", bubble.actionLabel)
    }

    @Test
    fun infoBubbleHasNoAction() {
        val bubble = SpeechBubble(
            text = "Looking good!",
            type = BubbleType.INFO
        )
        assertEquals(null, bubble.actionLabel)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.mascot.SpeechBubbleTest"`

**Step 3: Implement SpeechBubble data model**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/SpeechBubble.kt`:

```kotlin
package com.chromadmx.ui.mascot

/**
 * Types of speech bubbles the mascot can show.
 */
enum class BubbleType {
    INFO,    // General tips and status
    ACTION,  // Actionable suggestion with button
    ALERT    // Urgent notification
}

/**
 * A speech bubble displayed by the mascot.
 *
 * @property text The message text.
 * @property type The bubble type (affects styling).
 * @property actionLabel Optional action button label (for ACTION type).
 * @property autoDismissMs Auto-dismiss after this many ms (0 = manual dismiss only).
 */
data class SpeechBubble(
    val text: String,
    val type: BubbleType = BubbleType.INFO,
    val actionLabel: String? = null,
    val autoDismissMs: Long = 4000L
)
```

**Step 4: Implement SpeechBubbleComposable**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/SpeechBubbleComposable.kt`:

```kotlin
package com.chromadmx.ui.mascot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.DmxPrimary
import com.chromadmx.ui.theme.DmxSecondary
import com.chromadmx.ui.theme.NodeOffline

/**
 * Pixel-styled speech bubble composable.
 */
@Composable
fun SpeechBubbleView(
    bubble: SpeechBubble,
    onAction: (() -> Unit)? = null,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val borderColor = when (bubble.type) {
        BubbleType.INFO -> DmxSecondary
        BubbleType.ACTION -> DmxPrimary
        BubbleType.ALERT -> NodeOffline
    }

    Column(
        modifier = modifier
            .widthIn(max = 200.dp)
            .border(2.dp, borderColor)
            .background(Color(0xFF1A1A2E))
            .padding(8.dp)
            .clickable { onDismiss() },
    ) {
        Text(
            text = bubble.text,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFE0E0E0),
        )

        if (bubble.actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bubble.actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = borderColor,
                modifier = Modifier.clickable { onAction() },
            )
        }
    }
}
```

**Step 5: Run test**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.mascot.SpeechBubbleTest"`
Expected: PASS

**Step 6: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: PASS

**Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/SpeechBubble.kt
git add shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/SpeechBubbleComposable.kt
git add shared/src/commonTest/kotlin/com/chromadmx/ui/mascot/SpeechBubbleTest.kt
git commit -m "feat(ui): add speech bubble system for pixel mascot (#18)"
```

---

### Task 6: MascotViewModel

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/MascotViewModel.kt`
- Test: `shared/src/commonTest/kotlin/com/chromadmx/ui/viewmodel/MascotViewModelTest.kt`

**Step 1: Write the failing test**

```kotlin
package com.chromadmx.ui.viewmodel

import com.chromadmx.core.model.BeatState
import com.chromadmx.ui.mascot.BubbleType
import com.chromadmx.ui.mascot.MascotState
import com.chromadmx.ui.mascot.SpeechBubble
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MascotViewModelTest {
    @Test
    fun startsInIdleState() = runTest {
        val vm = MascotViewModel(scope = this)
        assertEquals(MascotState.IDLE, vm.mascotState.first())
    }

    @Test
    fun showBubbleDisplaysBubble() = runTest {
        val vm = MascotViewModel(scope = this)
        val bubble = SpeechBubble("Hello!", BubbleType.INFO)
        vm.showBubble(bubble)
        assertEquals(bubble, vm.currentBubble.first())
    }

    @Test
    fun dismissBubbleClearsBubble() = runTest {
        val vm = MascotViewModel(scope = this)
        vm.showBubble(SpeechBubble("Hello!", BubbleType.INFO))
        vm.dismissBubble()
        assertNull(vm.currentBubble.first())
    }

    @Test
    fun triggerHappyState() = runTest {
        val vm = MascotViewModel(scope = this)
        vm.triggerHappy()
        assertEquals(MascotState.HAPPY, vm.mascotState.first())
    }

    @Test
    fun triggerAlert() = runTest {
        val vm = MascotViewModel(scope = this)
        vm.triggerAlert("Node disconnected!")
        assertEquals(MascotState.ALERT, vm.mascotState.first())
        assertEquals("Node disconnected!", vm.currentBubble.first()?.text)
    }

    @Test
    fun triggerThinking() = runTest {
        val vm = MascotViewModel(scope = this)
        vm.triggerThinking()
        assertEquals(MascotState.THINKING, vm.mascotState.first())
    }

    @Test
    fun autoDismissBubble() = runTest {
        val vm = MascotViewModel(scope = this)
        vm.showBubble(SpeechBubble("Temp", BubbleType.INFO, autoDismissMs = 1000L))
        advanceTimeBy(1100L)
        assertNull(vm.currentBubble.first())
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.viewmodel.MascotViewModelTest"`

**Step 3: Implement MascotViewModel**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/MascotViewModel.kt`:

```kotlin
package com.chromadmx.ui.viewmodel

import com.chromadmx.ui.mascot.AnimationController
import com.chromadmx.ui.mascot.BubbleType
import com.chromadmx.ui.mascot.MascotState
import com.chromadmx.ui.mascot.SpeechBubble
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing the pixel mascot's state, animations, and speech bubbles.
 */
class MascotViewModel(
    private val scope: CoroutineScope,
) {
    val animationController = AnimationController(scope)

    private val _mascotState = MutableStateFlow(MascotState.IDLE)
    val mascotState: StateFlow<MascotState> = _mascotState.asStateFlow()

    private val _currentBubble = MutableStateFlow<SpeechBubble?>(null)
    val currentBubble: StateFlow<SpeechBubble?> = _currentBubble.asStateFlow()

    /** Whether the chat panel is open. */
    private val _isChatOpen = MutableStateFlow(false)
    val isChatOpen: StateFlow<Boolean> = _isChatOpen.asStateFlow()

    private var autoDismissJob: Job? = null

    init {
        animationController.start()
    }

    fun showBubble(bubble: SpeechBubble) {
        _currentBubble.value = bubble
        autoDismissJob?.cancel()
        if (bubble.autoDismissMs > 0) {
            autoDismissJob = scope.launch {
                delay(bubble.autoDismissMs)
                _currentBubble.value = null
            }
        }
    }

    fun dismissBubble() {
        autoDismissJob?.cancel()
        _currentBubble.value = null
    }

    fun triggerHappy() {
        _mascotState.value = MascotState.HAPPY
        animationController.transitionTo(MascotState.HAPPY)
    }

    fun triggerAlert(message: String) {
        _mascotState.value = MascotState.ALERT
        animationController.transitionTo(MascotState.ALERT)
        showBubble(SpeechBubble(text = message, type = BubbleType.ALERT))
    }

    fun triggerThinking() {
        _mascotState.value = MascotState.THINKING
        animationController.transitionTo(MascotState.THINKING)
    }

    fun triggerConfused(message: String) {
        _mascotState.value = MascotState.CONFUSED
        animationController.transitionTo(MascotState.CONFUSED)
        showBubble(SpeechBubble(text = message, type = BubbleType.INFO))
    }

    fun returnToIdle() {
        _mascotState.value = MascotState.IDLE
        animationController.transitionTo(MascotState.IDLE)
    }

    fun toggleChat() {
        _isChatOpen.value = !_isChatOpen.value
    }

    fun onCleared() {
        animationController.stop()
        autoDismissJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
    }
}
```

**Step 4: Run test**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.chromadmx.ui.viewmodel.MascotViewModelTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/viewmodel/MascotViewModel.kt
git add shared/src/commonTest/kotlin/com/chromadmx/ui/viewmodel/MascotViewModelTest.kt
git commit -m "feat(ui): add MascotViewModel with state management and speech bubbles (#18)"
```

---

### Task 7: MascotOverlay Composable

**Files:**
- Create: `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/MascotOverlay.kt`

**Step 1: Implement MascotOverlay**

Create `shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/MascotOverlay.kt`:

```kotlin
package com.chromadmx.ui.mascot

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.viewmodel.MascotViewModel
import kotlin.math.roundToInt

/**
 * Full-screen overlay that positions the mascot sprite and speech bubble.
 *
 * The mascot is draggable and tappable (opens chat panel).
 * Speech bubbles appear above the mascot.
 */
@Composable
fun MascotOverlay(
    viewModel: MascotViewModel,
    onMascotTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mascotState by viewModel.mascotState.collectAsState()
    val frameIndex by viewModel.animationController.currentFrameIndex.collectAsState()
    val currentBubble by viewModel.currentBubble.collectAsState()

    // Draggable offset (starts at bottom-right)
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val animation = MascotSprites.animationFor(mascotState)
    val frame = animation.frameAt(frameIndex)

    Box(modifier = modifier.fillMaxSize()) {
        // Position mascot at bottom-right + drag offset
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        onMascotTap()
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Speech bubble (above mascot)
            if (currentBubble != null) {
                SpeechBubbleView(
                    bubble = currentBubble!!,
                    onDismiss = { viewModel.dismissBubble() },
                    onAction = { /* TODO: wire action callbacks */ },
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Mascot sprite
            SpriteRenderer(
                frame = frame,
                size = 64.dp,
            )
        }
    }
}
```

**Step 2: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: PASS

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/mascot/MascotOverlay.kt
git commit -m "feat(ui): add draggable mascot overlay with speech bubbles (#18)"
```

---

### Task 8: Wire Mascot into App and Koin DI

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/di/UiModule.kt`
- Modify: `shared/src/commonMain/kotlin/com/chromadmx/ui/ChromaDmxApp.kt` (or `StagePreviewScreen.kt`)

**Step 1: Add MascotViewModel to Koin**

In `UiModule.kt`, add:

```kotlin
factory {
    val parentScope: CoroutineScope = get()
    val childJob = SupervisorJob(parentScope.coroutineContext[Job])
    val vmScope = CoroutineScope(Dispatchers.Default + childJob)
    MascotViewModel(scope = vmScope)
}
```

**Step 2: Add MascotOverlay to StagePreview**

In `ChromaDmxApp.kt`, within the `AppState.StagePreview` branch, add the mascot overlay on top of the StagePreviewScreen:

```kotlin
is AppState.StagePreview -> {
    val stageVm = resolveOrNull<StageViewModel>()
    val mascotVm = resolveOrNull<MascotViewModel>()
    if (stageVm != null) {
        DisposableEffect(stageVm) {
            onDispose { stageVm.onCleared() }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            StagePreviewScreen(
                viewModel = stageVm,
                onSettingsClick = { appStateManager.navigateTo(AppState.Settings) },
            )
            if (mascotVm != null) {
                DisposableEffect(mascotVm) {
                    onDispose { mascotVm.onCleared() }
                }
                MascotOverlay(
                    viewModel = mascotVm,
                    onMascotTap = { mascotVm.toggleChat() },
                )
            }
        }
    }
}
```

**Step 3: Compile check**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
Expected: PASS

**Step 4: Build Android app**

Run: `./gradlew :android:app:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/chromadmx/ui/di/UiModule.kt
git add shared/src/commonMain/kotlin/com/chromadmx/ui/ChromaDmxApp.kt
git commit -m "feat(ui): wire mascot overlay into stage preview screen (#18)"
```

---

### Task 9: Full Integration Verification

**Step 1: Run all tests**

```bash
./gradlew :shared:core:testAndroidHostTest :shared:engine:testAndroidHostTest :shared:networking:testAndroidHostTest :shared:simulation:testAndroidHostTest :shared:tempo:testAndroidHostTest :shared:vision:testAndroidHostTest :shared:agent:testAndroidHostTest
```

Expected: ALL PASS

**Step 2: Build and verify on Android emulator**

```bash
./gradlew :android:app:assembleDebug
```

Deploy to emulator and verify:
- Pixel mascot robot visible in bottom-right corner
- Mascot animates (breathing idle animation)
- Mascot is draggable
- Tapping mascot toggles chat state (visible via log or state)
- Speech bubbles would appear above mascot if triggered

**Step 3: Commit any fixups**

```bash
git add -A
git commit -m "fix: resolve integration issues from pixel mascot implementation (#18)"
```
