package com.chromadmx.ui.screen.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chromadmx.core.model.DmxNode
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.PixelProgressBar
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.NeonGreen
import com.chromadmx.ui.theme.NeonMagenta
import com.chromadmx.ui.theme.NeonYellow
import com.chromadmx.ui.theme.PixelFontFamily

@Composable
fun NetworkDiscoveryScreen(
    isScanning: Boolean,
    discoveredNodes: List<DmxNode>,
    onRetry: () -> Unit,
    onSimulation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedVisibility(
            visible = isScanning,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ScanDotsAnimation()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Scanning for lights...",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PixelFontFamily),
                    color = NeonCyan,
                )
                Spacer(modifier = Modifier.height(16.dp))
                PixelProgressBar(
                    progress = 0f,
                    indeterminate = true,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    progressColor = NeonCyan,
                )
            }
        }

        AnimatedVisibility(
            visible = !isScanning && discoveredNodes.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${discoveredNodes.size} node${if (discoveredNodes.size != 1) "s" else ""} found!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PixelFontFamily),
                    color = NeonGreen,
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(discoveredNodes) { node ->
                        DiscoveredNodeItem(node)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Connecting...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(
            visible = !isScanning && discoveredNodes.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No lights found",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PixelFontFamily),
                    color = NeonYellow,
                )
                Spacer(modifier = Modifier.height(16.dp))

                PixelCard(
                    borderColor = NeonMagenta.copy(alpha = 0.5f),
                    // glowColor removed
                ) {
                    Text(
                        text = "No Art-Net nodes detected on the network. Want to try a virtual stage instead?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PixelButton(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Text("TRY AGAIN")
                    }
                    PixelButton(
                        onClick = onSimulation,
                        modifier = Modifier.weight(1f),
                        backgroundColor = NeonMagenta,
                        contentColor = Color.Black,
                    ) {
                        Text("VIRTUAL STAGE")
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveredNodeItem(node: DmxNode) {
    PixelCard(
        borderColor = NeonGreen.copy(alpha = 0.4f),
    ) {
        Column {
            Text(
                text = node.shortName.ifBlank { node.ipAddress },
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = PixelFontFamily),
                color = NeonCyan,
            )
            Text(
                text = "${node.ipAddress} - ${node.numPorts} port${if (node.numPorts != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScanDotsAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan-dots")
    val dotAlpha = infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan-dot-alpha",
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha = when (index) {
                0 -> dotAlpha.value
                1 -> dotAlpha.value * 0.7f
                else -> dotAlpha.value * 0.4f
            }
            Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(
                    color = NeonCyan.copy(alpha = alpha),
                    radius = size.minDimension / 2f,
                )
            }
            if (index < 2) Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
