package com.chromadmx.ui.screen.network

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelButtonVariant
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.PixelDivider
import com.chromadmx.ui.state.NodeDiagnostics
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily

/**
 * Full-screen overlay displaying detailed network diagnostics for a single DMX node.
 *
 * Health color coding for latency:
 * - Green: <50ms (healthy)
 * - Yellow: 50-199ms (degraded)
 * - Red: >=200ms (critical)
 *
 * Tap the scrim or the DISMISS button to close.
 */
@Composable
fun NodeDiagnosticsOverlay(
    diagnostics: NodeDiagnostics,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latencyColor = when {
        diagnostics.latencyMs < 50 -> PixelDesign.colors.success
        diagnostics.latencyMs < 200 -> PixelDesign.colors.warning
        else -> PixelDesign.colors.error
    }

    val connectionColor = if (diagnostics.isAlive) PixelDesign.colors.success else PixelDesign.colors.error
    val connectionText = if (diagnostics.isAlive) "ONLINE" else "OFFLINE"

    val uptimeStr = formatUptime(diagnostics.uptimeMs)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() },
    ) {
        PixelCard(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) { }, // block clicks through to scrim
            borderColor = latencyColor,
            backgroundColor = PixelDesign.colors.surface,
        ) {
            Column {
                // ── Header ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "NODE DIAGNOSTICS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = PixelFontFamily,
                            fontSize = 12.sp,
                        ),
                        color = PixelDesign.colors.primary,
                    )
                    // Connection status pill
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(connectionColor),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = connectionText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = PixelFontFamily,
                                fontSize = 8.sp,
                            ),
                            color = connectionColor,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Node name and IP
                Text(
                    text = diagnostics.nodeName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 14.sp,
                    ),
                    color = PixelDesign.colors.onSurface,
                )
                Text(
                    text = diagnostics.ipAddress,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                    ),
                    color = PixelDesign.colors.onSurfaceVariant,
                )

                Spacer(Modifier.height(8.dp))
                PixelDivider()
                Spacer(Modifier.height(8.dp))

                // ── Diagnostics grid ──────────────────────────────────
                DiagRow(
                    label = "PING LATENCY",
                    value = "${diagnostics.latencyMs}ms",
                    valueColor = latencyColor,
                )
                DiagRow(
                    label = "CONNECTION",
                    value = connectionText,
                    valueColor = connectionColor,
                )
                DiagRow(
                    label = "FIRMWARE",
                    value = diagnostics.firmwareVersion,
                )
                DiagRow(
                    label = "MAC",
                    value = diagnostics.macAddress.ifEmpty { "N/A" },
                )
                DiagRow(
                    label = "PORTS",
                    value = diagnostics.numPorts.toString(),
                )
                DiagRow(
                    label = "UNIVERSES",
                    value = if (diagnostics.universes.isEmpty()) "None" else diagnostics.universes.joinToString(", "),
                )
                DiagRow(
                    label = "UPTIME",
                    value = uptimeStr,
                )
                DiagRow(
                    label = "FRAME COUNT",
                    value = diagnostics.frameCount.toString(),
                )

                // Last error (only shown if present)
                if (diagnostics.lastError != null) {
                    Spacer(Modifier.height(8.dp))
                    PixelDivider(color = PixelDesign.colors.error.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))
                    DiagRow(
                        label = "LAST ERROR",
                        value = diagnostics.lastError,
                        valueColor = PixelDesign.colors.error,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Dismiss button ────────────────────────────────────
                PixelButton(
                    onClick = onDismiss,
                    variant = PixelButtonVariant.Surface,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = "DISMISS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PixelFontFamily,
                            fontSize = 9.sp,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * A single row in the diagnostics grid: label on the left, value on the right.
 */
@Composable
private fun DiagRow(
    label: String,
    value: String,
    valueColor: Color = PixelDesign.colors.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 8.sp,
            ),
            color = PixelDesign.colors.tertiary.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = PixelFontFamily,
                fontSize = 10.sp,
            ),
            color = valueColor,
        )
    }
}

/** Format a duration in milliseconds to a human-readable uptime string. */
private fun formatUptime(ms: Long): String {
    if (ms <= 0) return "N/A"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
