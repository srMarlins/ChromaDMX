package com.chromadmx.ui.mascot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.theme.PixelDesign

/**
 * Pixel-styled speech bubble composable.
 */
@Composable
fun SpeechBubbleView(
    bubble: SpeechBubble,
    onAction: ((String?) -> Unit)? = null,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = PixelDesign.colors
    val borderColor = when (bubble.type) {
        BubbleType.INFO -> colors.secondary
        BubbleType.ACTION -> colors.primary
        BubbleType.ALERT -> colors.error
    }

    Column(
        modifier = modifier
            .widthIn(max = 200.dp)
            .border(2.dp, borderColor)
            .background(colors.surface)
            .padding(8.dp)
            .clickable { onDismiss() },
    ) {
        Text(
            text = bubble.text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurface,
        )

        if (bubble.actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bubble.actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = borderColor,
                modifier = Modifier.clickable { onAction(bubble.actionId) },
            )
        }
    }
}
