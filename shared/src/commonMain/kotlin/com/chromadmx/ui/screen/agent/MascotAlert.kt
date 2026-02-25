package com.chromadmx.ui.screen.agent

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromadmx.ui.viewmodel.NetworkAlert
import com.chromadmx.ui.viewmodel.NetworkAlertType

/**
 * Global mascot alert component.
 * Displays a speech bubble with an optional action button
 * and a pixel-styled mascot character.
 */
@Composable
fun MascotAlert(
    alert: NetworkAlert?,
    onDiagnose: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        AnimatedVisibility(
            visible = alert != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            if (alert != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Speech Bubble
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 4.dp
                        ),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(bottom = 32.dp, end = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = alert.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            if (alert.type == NetworkAlertType.DISCONNECTED && alert.nodeId != null) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = onDismiss) {
                                        Text("Ignore", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = { onDiagnose(alert.nodeId) },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                    ) {
                                        Text("Diagnose", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            } else {
                                TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    // Mascot Character
                    MascotCharacter(
                        isHappy = alert.type != NetworkAlertType.DISCONNECTED,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MascotCharacter(
    isHappy: Boolean,
    modifier: Modifier = Modifier
) {
    // Pixel-styled mascot placeholder
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isHappy) " ^ ω ^ " else " > ﹏ < ",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "CHROMA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        }
    }
}
