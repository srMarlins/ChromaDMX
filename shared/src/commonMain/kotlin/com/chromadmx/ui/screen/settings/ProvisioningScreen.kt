package com.chromadmx.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromadmx.networking.ble.BleNode
import com.chromadmx.networking.ble.NodeConfig
import com.chromadmx.networking.ble.ProvisioningState
import com.chromadmx.networking.ble.SignalQuality
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.PixelProgressBar
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.NeonGreen
import com.chromadmx.ui.viewmodel.ProvisioningViewModel

/**
 * BLE Provisioning screen for configuring ESP32 DMX nodes.
 *
 * Provides a complete workflow: scan for nodes, select one, configure it
 * with Wi-Fi and Art-Net settings, and provision it over BLE.
 *
 * When BLE is unavailable (platform stub), displays a graceful fallback message.
 *
 * @param viewModel The provisioning ViewModel
 * @param onClose Callback to navigate back
 * @param modifier Optional modifier
 */
@Composable
fun ProvisioningScreen(
    viewModel: ProvisioningViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val discoveredNodes by viewModel.discoveredNodes.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val provisioningState by viewModel.provisioningState.collectAsState()
    val selectedNode by viewModel.selectedNode.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            ProvisioningHeader(
                onClose = onClose,
                selectedNode = selectedNode,
                onBack = { viewModel.clearSelection() }
            )

            if (!viewModel.isBleAvailable) {
                BleUnavailableMessage()
            } else {
                when {
                    provisioningState == ProvisioningState.SUCCESS -> {
                        ProvisioningSuccessContent(
                            onDone = {
                                viewModel.resetState()
                                viewModel.clearSelection()
                            }
                        )
                    }
                    provisioningState == ProvisioningState.ERROR -> {
                        ProvisioningErrorContent(
                            errorMessage = errorMessage,
                            onRetry = { viewModel.resetState() },
                            onDone = {
                                viewModel.resetState()
                                viewModel.clearSelection()
                            }
                        )
                    }
                    provisioningState != ProvisioningState.IDLE &&
                        provisioningState != ProvisioningState.SCANNING -> {
                        ProvisioningProgressContent(state = provisioningState)
                    }
                    selectedNode != null -> {
                        ConfigFormContent(
                            node = selectedNode!!,
                            errorMessage = errorMessage,
                            onProvision = { config -> viewModel.provision(config) }
                        )
                    }
                    else -> {
                        ScanContent(
                            nodes = discoveredNodes,
                            isScanning = isScanning,
                            onStartScan = { viewModel.startScan() },
                            onStopScan = { viewModel.stopScan() },
                            onSelectNode = { viewModel.selectNode(it) },
                            errorMessage = errorMessage,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProvisioningHeader(
    onClose: () -> Unit,
    selectedNode: BleNode?,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectedNode != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to scan")
                }
            }
            Text(
                text = if (selectedNode != null) "PROVISION NODE" else "BLE PROVISIONING",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

// -- BLE Unavailable --

@Composable
private fun BleUnavailableMessage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BLE Not Available",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Bluetooth Low Energy is not available on this device or platform. " +
                "ESP32 node provisioning requires BLE hardware and platform support.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "You can still add nodes manually via the Network settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// -- Scan Content --

@Composable
private fun ScanContent(
    nodes: List<BleNode>,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onSelectNode: (BleNode) -> Unit,
    errorMessage: String?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Scan control
        item {
            PixelCard(title = { SectionTitle("SCAN FOR NODES") }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Scan for ESP32 DMX nodes advertising the ChromaDMX BLE service.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isScanning) {
                            Button(
                                onClick = onStopScan,
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("STOP SCAN")
                            }
                        } else {
                            Button(
                                onClick = onStartScan,
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("START SCAN")
                            }
                        }
                    }

                    if (isScanning) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scanning...",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
        }

        // Discovered nodes
        if (nodes.isNotEmpty()) {
            item {
                SectionTitle("DISCOVERED NODES (${nodes.size})")
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(nodes, key = { it.deviceId }) { node ->
                BleNodeCard(node = node, onClick = { onSelectNode(node) })
            }
        } else if (!isScanning) {
            item {
                Text(
                    text = "No nodes found. Make sure your ESP32 nodes are powered on and in BLE advertising mode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun BleNodeCard(
    node: BleNode,
    onClick: () -> Unit
) {
    PixelCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        glowColor = if (node.isProvisioned) NeonGreen else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = node.deviceId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (node.isProvisioned) {
                    Text(
                        text = "Already provisioned",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreen
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${node.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = signalColor(node.signalQuality)
                )
                Text(
                    text = node.signalQuality.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = signalColor(node.signalQuality)
                )
            }
        }
    }
}

@Composable
private fun signalColor(quality: SignalQuality): Color = when (quality) {
    SignalQuality.EXCELLENT -> NeonGreen
    SignalQuality.GOOD -> NeonCyan
    SignalQuality.FAIR -> Color(0xFFFFC107) // Amber
    SignalQuality.WEAK -> MaterialTheme.colorScheme.error
}

// -- Config Form --

@Composable
private fun ConfigFormContent(
    node: BleNode,
    errorMessage: String?,
    onProvision: (NodeConfig) -> Unit
) {
    var nodeName by remember { mutableStateOf(node.name ?: "") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var universe by remember { mutableStateOf("0") }
    var dmxStartAddress by remember { mutableStateOf("1") }

    // Input validation
    val universeInt = universe.toIntOrNull()
    val universeError = when {
        universe.isBlank() -> null // allow empty while typing
        universeInt == null -> "Must be a number"
        universeInt !in 0..32767 -> "Must be 0-32767"
        else -> null
    }

    val dmxAddrInt = dmxStartAddress.toIntOrNull()
    val dmxAddrError = when {
        dmxStartAddress.isBlank() -> null
        dmxAddrInt == null -> "Must be a number"
        dmxAddrInt !in 1..512 -> "Must be 1-512"
        else -> null
    }

    val hasValidationErrors = universeError != null || dmxAddrError != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Node info
        item {
            PixelCard(title = { SectionTitle("SELECTED NODE") }) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = node.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${node.deviceId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Signal: ${node.rssi} dBm (${node.signalQuality.name.lowercase()})",
                        style = MaterialTheme.typography.bodySmall,
                        color = signalColor(node.signalQuality)
                    )
                }
            }
        }

        // Config form
        item {
            PixelCard(title = { SectionTitle("NODE CONFIGURATION") }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nodeName,
                        onValueChange = { nodeName = it },
                        label = { Text("Node Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = wifiSsid,
                        onValueChange = { wifiSsid = it },
                        label = { Text("Wi-Fi SSID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text("Wi-Fi Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = universe,
                                onValueChange = { universe = it },
                                label = { Text("Universe") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = universeError != null,
                                supportingText = universeError?.let {
                                    { Text(it, color = MaterialTheme.colorScheme.error) }
                                }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = dmxStartAddress,
                                onValueChange = { dmxStartAddress = it },
                                label = { Text("Start Addr") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = dmxAddrError != null,
                                supportingText = dmxAddrError?.let {
                                    { Text(it, color = MaterialTheme.colorScheme.error) }
                                }
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            val config = NodeConfig(
                                name = nodeName,
                                wifiSsid = wifiSsid,
                                wifiPassword = wifiPassword,
                                universe = universeInt ?: 0,
                                dmxStartAddress = dmxAddrInt ?: 1
                            )
                            onProvision(config)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        enabled = !hasValidationErrors
                    ) {
                        Text("PROVISION NODE")
                    }
                }
            }
        }
    }
}

// -- Progress --

@Composable
private fun ProvisioningProgressContent(state: ProvisioningState) {
    val progress = when (state) {
        ProvisioningState.CONNECTING -> 0.2f
        ProvisioningState.READING_CONFIG -> 0.4f
        ProvisioningState.WRITING_CONFIG -> 0.6f
        ProvisioningState.VERIFYING -> 0.8f
        ProvisioningState.SUCCESS -> 1.0f
        else -> 0f
    }

    val statusText = when (state) {
        ProvisioningState.CONNECTING -> "Connecting to node..."
        ProvisioningState.READING_CONFIG -> "Reading current configuration..."
        ProvisioningState.WRITING_CONFIG -> "Writing new configuration..."
        ProvisioningState.VERIFYING -> "Verifying configuration..."
        ProvisioningState.SUCCESS -> "Provisioning complete!"
        else -> "Preparing..."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        PixelProgressBar(
            progress = progress,
            modifier = Modifier.fillMaxWidth(0.8f),
            progressColor = NeonCyan
        )

        Spacer(modifier = Modifier.height(16.dp))

        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = NeonCyan,
            strokeWidth = 3.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -- Success --

@Composable
private fun ProvisioningSuccessContent(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(NeonGreen.copy(alpha = 0.15f), MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Success",
                tint = NeonGreen,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Provisioning Complete!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = NeonGreen
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "The node will reboot and connect to your Wi-Fi network. " +
                "It should appear as an Art-Net node within a few seconds.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            shape = MaterialTheme.shapes.small
        ) {
            Text("DONE")
        }
    }
}

// -- Error --

@Composable
private fun ProvisioningErrorContent(
    errorMessage: String?,
    onRetry: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Error icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Provisioning Failed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onDone,
                shape = MaterialTheme.shapes.small
            ) {
                Text("CANCEL")
            }
            Button(
                onClick = onRetry,
                shape = MaterialTheme.shapes.small
            ) {
                Text("RETRY")
            }
        }
    }
}
