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
