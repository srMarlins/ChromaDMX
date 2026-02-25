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

    // Idle frame 2 -- slight "breathing" shift (1 pixel body expand)
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

    // Thinking frame 1 -- dots above head
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

    // Thinking frame 2 -- dots rotate
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

    // Happy frame -- jump up (shifted 1px)
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

    // Alert frame -- exclamation, red antenna
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

    // Confused frame -- tilted head (antenna leans right, head shifted right at top)
    private val CONFUSED_FRAME = spriteFrame(
        "____TTTTTTTT____",
        "_____TTTOGTTT___",
        "_____TTTOBTTT___",
        "____TOOOOOOOT___",
        "___TOBVVBBVBOT__",
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

    // Dancing frames -- side-to-side movement
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
