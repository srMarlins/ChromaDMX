package com.chromadmx.ui.screen.perform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.DmxPrimary
import com.chromadmx.ui.theme.DmxSurfaceVariant

/**
 * Master dimmer slider (0-100%), rendered vertically with pixel-art styling.
 *
 * Features:
 * - Vertical orientation for edge-of-screen placement
 * - Real-time engine control
 * - Double-tap to reset to 100%
 * - Pixel-art tick marks and blocky thumb
 */
@Composable
fun MasterDimmerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(40.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onValueChange(1.0f) },
                    onTap = { offset ->
                        val newValue = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    val newValue = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onValueChange(newValue)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val sliderHeight = h * value
            val sliderTop = h - sliderHeight

            // Track background - blocky pixel style
            drawRect(
                color = DmxSurfaceVariant.copy(alpha = 0.5f),
                topLeft = Offset(w * 0.4f, 0f),
                size = Size(w * 0.2f, h)
            )

            // Tick marks
            val tickCount = 10
            for (i in 0..tickCount) {
                val y = h * i / tickCount
                val tickWidth = if (i % 5 == 0) w * 0.5f else w * 0.3f
                drawRect(
                    color = Color.White.copy(alpha = 0.3f),
                    topLeft = Offset((w - tickWidth) / 2f, y - 1f),
                    size = Size(tickWidth, 2f)
                )
            }

            // Active level
            drawRect(
                color = DmxPrimary,
                topLeft = Offset(w * 0.4f, sliderTop),
                size = Size(w * 0.2f, sliderHeight)
            )

            // Thumb - chunky pixel block
            val thumbHeight = 12.dp.toPx()
            drawRect(
                color = Color.White,
                topLeft = Offset(w * 0.15f, (sliderTop - thumbHeight / 2f).coerceIn(0f, h - thumbHeight)),
                size = Size(w * 0.7f, thumbHeight)
            )

            // Pixel glow border for thumb
            if (value > 0) {
                 drawRect(
                    color = DmxPrimary.copy(alpha = 0.4f),
                    topLeft = Offset(w * 0.05f, (sliderTop - thumbHeight / 2f - 2.dp.toPx()).coerceIn(-2.dp.toPx(), h - thumbHeight + 2.dp.toPx())),
                    size = Size(w * 0.9f, thumbHeight + 4.dp.toPx())
                )
            }
        }
    }
}
