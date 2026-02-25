package com.chromadmx.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.ui.viewmodel.MascotAnimation
import com.chromadmx.ui.viewmodel.MascotViewModel
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.PixelFontFamily

/**
 * Mascot component with speech bubble for alerts.
 */
@Composable
fun MascotView(
    viewModel: MascotViewModel,
    modifier: Modifier = Modifier
) {
    val animation by viewModel.animation.collectAsState()
    val alert by viewModel.currentAlert.collectAsState()

    Row(
        modifier = modifier
            .padding(16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mascot Sprite Placeholder
        MascotSprite(animation)

        // Speech Bubble
        AnimatedVisibility(
            visible = alert != null,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            alert?.let { alertData ->
                SpeechBubble(
                    message = alertData.message,
                    actionLabel = alertData.actionLabel,
                    onAction = {
                        alertData.onAction?.invoke()
                        viewModel.dismissAlert()
                    },
                    onDismiss = { viewModel.dismissAlert() }
                )
            }
        }
    }
}

@Composable
private fun MascotSprite(animation: MascotAnimation) {
    val color = when (animation) {
        MascotAnimation.IDLE -> NeonCyan
        MascotAnimation.THINKING -> Color.Yellow
        MascotAnimation.HAPPY -> Color.Green
        MascotAnimation.ALERT -> Color.Red
        MascotAnimation.CONFUSED -> Color.Magenta
        MascotAnimation.DANCING -> Color.Cyan
    }

    PixelCard(
        modifier = Modifier.size(48.dp),
        backgroundColor = color.copy(alpha = 0.8f),
        borderColor = Color.White
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = when(animation) {
                    MascotAnimation.HAPPY -> "^_^"
                    MascotAnimation.ALERT -> "O_O"
                    MascotAnimation.THINKING -> "?.?"
                    MascotAnimation.CONFUSED -> "o_O"
                    else -> "u_u"
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color.Black
            )
        }
    }
}

@Composable
private fun SpeechBubble(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    onDismiss: () -> Unit
) {
    PixelCard(
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        borderColor = NeonCyan,
        modifier = Modifier.widthIn(max = 200.dp).clickable { onDismiss() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (actionLabel != null) {
                PixelButton(
                    onClick = onAction,
                    backgroundColor = NeonCyan,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color.Black
                    )
                }
            }
        }
    }
}
