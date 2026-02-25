package com.chromadmx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.LocalPixelTheme
import kotlin.math.roundToInt

@Composable
fun PixelSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    pixelSize: Dp = LocalPixelTheme.current.pixelSize
) {
    var width by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .height(32.dp)
            .fillMaxWidth()
            .onSizeChanged { width = it.width }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    if (width <= 0) return@detectTapGestures
                    val newValue = (offset.x / width).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + newValue * (valueRange.endInclusive - valueRange.start))
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures { change, _ ->
                    if (width <= 0) return@detectDragGestures
                    val newValue = (change.position.x / width).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + newValue * (valueRange.endInclusive - valueRange.start))
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pixelSize * 2)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pixelBorder(color = Color.DarkGray, pixelSize = pixelSize)
        )

        // Active Track
        val progress = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(pixelSize * 2)
                .background(accentColor)
        )

        // Thumb (Blocky)
        val thumbSize = 16.dp
        val thumbSizePx = with(density) { thumbSize.toPx() }
        val thumbOffset = (progress * (width - thumbSizePx)).coerceAtLeast(0f)

        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.roundToInt(), 0) }
                .size(thumbSize)
                .pixelBorder(color = Color.White, pixelSize = pixelSize)
                .background(accentColor)
        )
    }
}
