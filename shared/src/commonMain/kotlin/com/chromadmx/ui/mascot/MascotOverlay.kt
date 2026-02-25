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
import com.chromadmx.ui.viewmodel.MascotViewModel
import kotlin.math.roundToInt

/**
 * Full-screen overlay that positions the mascot sprite and speech bubble.
 *
 * The mascot is draggable and tappable (opens chat panel).
 * Speech bubbles appear above the mascot. Action buttons route through
 * [MascotViewModel.onBubbleAction] using the bubble's [SpeechBubble.actionId].
 */
@Composable
fun MascotOverlay(
    viewModel: MascotViewModel,
    onMascotTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mascotState by viewModel.mascotState.collectAsState()
    val frameIndex by viewModel.currentFrameIndex.collectAsState()
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
            if (currentBubble != null) {
                SpeechBubbleView(
                    bubble = currentBubble!!,
                    onDismiss = { viewModel.dismissBubble() },
                    onAction = { actionId -> viewModel.onBubbleAction(actionId) },
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
