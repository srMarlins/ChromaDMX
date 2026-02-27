package com.chromadmx.ui.mascot

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.state.MascotEvent
import com.chromadmx.ui.viewmodel.MascotViewModelV2
import kotlin.math.roundToInt
import com.chromadmx.ui.state.SpeechBubble as StateSpeechBubble

/**
 * Full-screen overlay that positions the mascot sprite and speech bubble.
 *
 * The mascot is draggable and tappable (opens chat panel).
 * Speech bubbles appear above the mascot. Action buttons route through
 * [MascotViewModelV2.onEvent] using the bubble's [SpeechBubble.actionId].
 *
 * Uses [AnimationController] from the ViewModel for frame-accurate
 * sprite rendering and converts [StateSpeechBubble] to the mascot-layer
 * [SpeechBubble] for [SpeechBubbleView].
 */
@Composable
fun MascotOverlay(
    viewModel: MascotViewModelV2,
    onMascotTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mascotState by viewModel.animationController.currentState.collectAsState()
    val frameIndex by viewModel.animationController.currentFrameIndex.collectAsState()
    val uiState by viewModel.state.collectAsState()

    // Draggable offset (starts at bottom-right)
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val animation = remember(mascotState) { MascotSprites.animationFor(mascotState) }
    val frame = animation.frameAt(frameIndex)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .clickable { onMascotTap() }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Speech bubble (above mascot)
            val bubble = uiState.currentBubble
            if (bubble != null) {
                val mascotBubble = remember(bubble) { bubble.toMascotBubble() }
                SpeechBubbleView(
                    bubble = mascotBubble,
                    onDismiss = { viewModel.onEvent(MascotEvent.DismissBubble) },
                    onAction = { actionId ->
                        viewModel.onEvent(MascotEvent.OnBubbleAction(actionId))
                    },
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

/**
 * Convert UDF [StateSpeechBubble] to mascot-layer [SpeechBubble].
 * Both types share the same [BubbleType] enum so the conversion is 1:1.
 */
private fun StateSpeechBubble.toMascotBubble(): SpeechBubble = SpeechBubble(
    text = text,
    type = type,
    actionLabel = actionLabel,
    actionId = actionId,
    autoDismissMs = autoDismissMs,
)
