package com.chromadmx.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.ChromaAnimations
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.ui.theme.PixelShape
import kotlin.math.roundToInt

@Composable
fun PixelSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    accentColor: Color = PixelDesign.colors.primary,
    showValueLabel: Boolean = false,
    valueLabelFormatter: (Float) -> String = { "${(it * 100).toInt()}%" },
) {
    var width by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    // Use ChromaAnimations.dragReturn spring for snap animations
    val dragSpring = ChromaAnimations.dragReturn
    val thumbSize = 20.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val pixelSize = PixelDesign.spacing.pixelSize

    val progress = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    // Animate thumb position with dragReturn spring
    val thumbOffsetTarget = if (width > 0) {
        (progress * (width - thumbSizePx)).coerceAtLeast(0f)
    } else {
        0f
    }
    val animatedThumbOffset by animateDpAsState(
        targetValue = with(density) { thumbOffsetTarget.toInt().dp },
        animationSpec = spring(
            dampingRatio = dragSpring.dampingRatio,
            stiffness = dragSpring.stiffness,
        ),
    )

    Column(modifier = modifier) {
        // Optional value label above the slider
        if (showValueLabel) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                val labelOffsetPx = with(density) { animatedThumbOffset.toPx() }
                Text(
                    text = valueLabelFormatter(value),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PixelFontFamily,
                        color = PixelDesign.colors.onSurface,
                    ),
                    modifier = Modifier.offset {
                        IntOffset(
                            x = labelOffsetPx.roundToInt(),
                            y = 0
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .height(32.dp)
                .fillMaxWidth()
                .onSizeChanged { width = it.width }
                .semantics(mergeDescendants = true) {
                    if (!enabled) disabled()
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = value,
                        range = valueRange
                    )
                    setProgress { targetValue ->
                        if (enabled && targetValue in valueRange) {
                            onValueChange(targetValue)
                            true
                        } else {
                            false
                        }
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        if (width <= 0) return@detectTapGestures
                        val newValue = (offset.x / width).coerceIn(0f, 1f)
                        onValueChange(
                            valueRange.start + newValue * (valueRange.endInclusive - valueRange.start)
                        )
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        if (width <= 0) return@detectDragGestures
                        val newValue = (change.position.x / width).coerceIn(0f, 1f)
                        onValueChange(
                            valueRange.start + newValue * (valueRange.endInclusive - valueRange.start)
                        )
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pixelSize * 2)
                    .clip(PixelShape(4.dp))
                    .background(PixelDesign.colors.surfaceVariant, PixelShape(4.dp))
                    .pixelBorder(chamfer = 4.dp)
            )

            // Active Track
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(pixelSize * 2)
                    .clip(PixelShape(4.dp))
                    .background(accentColor, PixelShape(4.dp))
            )

            // Thumb (Square blocky, 20dp)
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatedThumbOffset.roundToPx(), 0) }
                    .size(thumbSize)
                    .clip(PixelShape(4.dp))
                    .pixelBorder(chamfer = 4.dp)
                    .background(accentColor, PixelShape(4.dp))
            )
        }
    }
}
