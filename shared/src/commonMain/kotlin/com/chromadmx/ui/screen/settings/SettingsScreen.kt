package com.chromadmx.ui.screen.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.chromadmx.agent.config.AgentConfig
import com.chromadmx.core.model.FixtureProfile
import com.chromadmx.simulation.fixtures.RigPreset
import com.chromadmx.simulation.fixtures.SimulatedFixtureRig
import com.chromadmx.ui.components.PixelCard
import com.chromadmx.ui.components.VirtualNodeBadge
import com.chromadmx.ui.theme.NeonCyan
import com.chromadmx.ui.theme.NeonGreen
import com.chromadmx.ui.util.presetDisplayName
import com.chromadmx.ui.viewmodel.AgentStatus
import com.chromadmx.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pollInterval by viewModel.pollInterval.collectAsState()
    val protocol by viewModel.protocol.collectAsState()
    val manualIp by viewModel.manualIp.collectAsState()
    val manualUniverse by viewModel.manualUniverse.collectAsState()
    val manualStartAddress by viewModel.manualStartAddress.collectAsState()
    val fixtureProfiles by viewModel.fixtureProfiles.collectAsState()
    val simulationEnabled by viewModel.simulationEnabled.collectAsState()
    val selectedRigPreset by viewModel.selectedRigPreset.collectAsState()
    val agentConfig by viewModel.agentConfig.collectAsState()
    val agentStatus by viewModel.agentStatus.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Settings")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Network Section
                item {
                    NetworkSettingsSection(
                        pollInterval = pollInterval,
                        protocol = protocol,
                        manualIp = manualIp,
                        manualUniverse = manualUniverse,
                        manualStartAddress = manualStartAddress,
                        onPollIntervalChange = { viewModel.setPollInterval(it) },
                        onProtocolChange = { viewModel.setProtocol(it) },
                        onManualIpChange = { viewModel.setManualIp(it) },
                        onManualUniverseChange = { viewModel.setManualUniverse(it) },
                        onManualStartAddressChange = { viewModel.setManualStartAddress(it) },
                        onForceRescan = { viewModel.forceRescan() }
                    )
                }

                // Fixture Profiles Section
                item {
                    FixtureProfilesSection(
                        profiles = fixtureProfiles,
                        onDeleteProfile = { viewModel.deleteFixtureProfile(it) },
                        onAddProfile = { /* Open editor */ }
                    )
                }

                // Simulation Section
                item {
                    SimulationSettingsSection(
                        enabled = simulationEnabled,
                        selectedPreset = selectedRigPreset,
                        onToggleEnabled = { viewModel.toggleSimulation(it) },
                        onSelectPreset = { viewModel.setRigPreset(it) },
                        onReset = { viewModel.resetSimulation() }
                    )
                }

                // Agent Section
                item {
                    AgentSettingsSection(
                        config = agentConfig,
                        status = agentStatus,
                        onConfigChange = { viewModel.updateAgentConfig(it) },
                        onTestConnection = { viewModel.testAgentConnection() }
                    )
                }

                // App Section
                item {
                    AppSettingsSection(
                        onResetOnboarding = { viewModel.resetOnboarding() },
                        onExportData = { viewModel.exportAppData() },
                        onImportData = { viewModel.importAppData() }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun NetworkSettingsSection(
    pollInterval: Long,
    protocol: String,
    manualIp: String,
    manualUniverse: String,
    manualStartAddress: String,
    onPollIntervalChange: (Long) -> Unit,
    onProtocolChange: (String) -> Unit,
    onManualIpChange: (String) -> Unit,
    onManualUniverseChange: (String) -> Unit,
    onManualStartAddressChange: (String) -> Unit,
    onForceRescan: () -> Unit
) {
    PixelCard(title = { SectionTitle("NETWORK (ADVANCED)") }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Protocol selection
            Column {
                Text("Protocol", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = protocol == "Art-Net",
                        onClick = { onProtocolChange("Art-Net") },
                        label = { Text("Art-Net") }
                    )
                    FilterChip(
                        selected = protocol == "sACN",
                        onClick = { onProtocolChange("sACN") },
                        label = { Text("sACN") }
                    )
                }
            }

            // Manual Configuration
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Manual Node Configuration", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = manualIp,
                    onValueChange = onManualIpChange,
                    label = { Text("Node IP Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = manualUniverse,
                        onValueChange = onManualUniverseChange,
                        label = { Text("Universe") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = manualStartAddress,
                        onValueChange = onManualStartAddressChange,
                        label = { Text("Start Addr") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Discovery interval
            Column {
                Text("Discovery Interval: ${pollInterval}ms", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = pollInterval.toFloat(),
                    onValueChange = { onPollIntervalChange(it.toLong()) },
                    valueRange = 500f..10000f,
                    steps = 19
                )
            }

            Button(
                onClick = onForceRescan,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Text("FORCE RE-SCAN")
            }
        }
    }
}

@Composable
fun FixtureProfilesSection(
    profiles: List<FixtureProfile>,
    onDeleteProfile: (String) -> Unit,
    onAddProfile: () -> Unit
) {
    PixelCard(title = { SectionTitle("FIXTURE PROFILES") }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (profiles.isEmpty()) {
                Text(
                    "No custom profiles loaded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                profiles.forEach { profile ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(profile.name, style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { onDeleteProfile(profile.profileId) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAddProfile,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("ADD CUSTOM")
                }
                OutlinedButton(
                    onClick = { /* Import */ },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("IMPORT JSON")
                }
            }
        }
    }
}

@Composable
fun SimulationSettingsSection(
    enabled: Boolean,
    selectedPreset: RigPreset,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (RigPreset) -> Unit,
    onReset: () -> Unit
) {
    PixelCard(title = { SectionTitle("SIMULATION") }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Enable/disable toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Simulation Mode", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = enabled, onCheckedChange = onToggleEnabled)
            }

            // Current status summary
            if (enabled) {
                val rig = remember(selectedPreset) {
                    SimulatedFixtureRig(selectedPreset)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonGreen,
                        )
                        Text(
                            text = "${selectedPreset.presetDisplayName()} -- ${rig.fixtureCount} fixtures",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    VirtualNodeBadge()
                }
            }

            // Inline rig preset selector (cards, not just radio buttons)
            Column {
                Text("Rig Preset", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                RigPreset.entries.forEach { preset ->
                    val isSelected = selectedPreset == preset
                    val rig = remember(preset) {
                        SimulatedFixtureRig(preset)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) NeonCyan.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectPreset(preset) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                preset.presetDisplayName(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                            Text(
                                "${rig.fixtureCount} fixtures, ${rig.universeCount} universe${if (rig.universeCount != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Reset button
            Button(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = MaterialTheme.shapes.small
            ) {
                Text("RESET SIMULATION")
            }
        }
    }
}

@Composable
fun AgentSettingsSection(
    config: AgentConfig,
    status: AgentStatus,
    onConfigChange: (AgentConfig) -> Unit,
    onTestConnection: () -> Unit
) {
    PixelCard(title = { SectionTitle("AGENT / AI") }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = config.apiKey,
                onValueChange = { onConfigChange(config.copy(apiKey = it)) },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Model: ${config.modelId}")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SettingsViewModel.AVAILABLE_MODELS.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                onConfigChange(config.copy(modelId = model))
                                expanded = false
                            }
                        )
                    }
                }
            }

            Column {
                Text("Max Iterations: ${config.maxIterations}", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = config.maxIterations.toFloat(),
                    onValueChange = { onConfigChange(config.copy(maxIterations = it.toInt())) },
                    valueRange = 1f..100f
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onTestConnection,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("TEST CONNECTION")
                }

                val statusText = when (status) {
                    is AgentStatus.Idle -> null
                    is AgentStatus.Testing -> "Testing..."
                    is AgentStatus.Success -> "Connection Successful!"
                    is AgentStatus.Error -> "Failed: ${status.message}"
                }
                val statusColor = when (status) {
                    is AgentStatus.Success -> MaterialTheme.colorScheme.primary
                    is AgentStatus.Error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                if (statusText != null) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AppSettingsSection(
    onResetOnboarding: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit
) {
    PixelCard(title = { SectionTitle("APP") }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onResetOnboarding,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Text("RESET ONBOARDING")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExportData,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("EXPORT DATA")
                }
                Button(
                    onClick = onImportData,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("IMPORT DATA")
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}
