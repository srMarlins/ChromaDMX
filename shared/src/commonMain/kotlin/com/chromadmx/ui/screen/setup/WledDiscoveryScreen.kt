package com.chromadmx.ui.screen.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.ui.components.PixelButton
import com.chromadmx.ui.components.PixelButtonVariant
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.PixelLoadingSpinner
import com.chromadmx.ui.components.SpinnerSize
import com.chromadmx.ui.theme.PixelDesign
import com.chromadmx.ui.theme.PixelFontFamily
import com.chromadmx.wled.WledDevice

/**
 * WLED discovery screen for the "My Room" onboarding path.
 *
 * Shows auto-discovered WLED devices on the local network with tap-to-adopt.
 * Users can also manually add a device or continue once satisfied.
 */
@Composable
fun WledDiscoveryScreen(
    devices: List<WledDevice>,
    isScanning: Boolean,
    onAdoptDevice: (WledDevice) -> Unit,
    onManualAdd: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = PixelDesign.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(PixelDesign.spacing.large))

        // Heading
        Text(
            text = "Looking for lights...",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = PixelFontFamily,
                letterSpacing = 2.sp,
            ),
            color = PixelDesign.colors.info,
        )

        Spacer(modifier = Modifier.height(PixelDesign.spacing.small))

        // Scanning subtitle + spinner
        if (isScanning) {
            Text(
                text = "Scanning your network",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PixelFontFamily),
                color = PixelDesign.colors.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

            PixelLoadingSpinner(
                spinnerSize = SpinnerSize.Medium,
                color = PixelDesign.colors.info,
            )
        }

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        // Discovered devices list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(devices, key = { it.ipAddress }) { device ->
                WledDeviceCard(
                    device = device,
                    onAdopt = { onAdoptDevice(device) },
                )
            }
        }

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))

        // Bottom row: manual add + continue
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PixelButton(
                onClick = onManualAdd,
                variant = PixelButtonVariant.Surface,
                modifier = Modifier.weight(1f),
            ) {
                Text("ADD MANUALLY")
            }
            PixelButton(
                onClick = onContinue,
                variant = PixelButtonVariant.Primary,
                modifier = Modifier.weight(1f),
            ) {
                Text("CONTINUE")
            }
        }

        Spacer(modifier = Modifier.height(PixelDesign.spacing.medium))
    }
}

// ── Device Card ──────────────────────────────────────────────────────

@Composable
private fun WledDeviceCard(
    device: WledDevice,
    onAdopt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PixelCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = PixelDesign.colors.info.copy(alpha = 0.4f),
        glowing = device.isOnline,
        onClick = onAdopt,
    ) {
        Column {
            Text(
                text = device.name.ifBlank { device.ipAddress },
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = PixelFontFamily),
                color = PixelDesign.colors.info,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(PixelDesign.spacing.medium),
            ) {
                Text(
                    text = device.ipAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelDesign.colors.onSurfaceVariant,
                )
                if (device.totalLeds > 0) {
                    Text(
                        text = "${device.totalLeds} LEDs",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = PixelFontFamily),
                        color = PixelDesign.colors.secondary.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}
