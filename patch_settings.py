with open('shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

old_block = """            PixelTextField(
                value = state.agentConfig.apiKey,
                onValueChange = { newValue ->
                    if (!showKey) showKey = true
                    onEvent(SettingsEvent.UpdateAgentConfig(state.agentConfig.copy(apiKey = newValue)))
                },
                label = "API Key",
                placeholder = "Enter API key...",
                visualTransformation = if (showKey || state.agentConfig.apiKey.isEmpty()) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                },
                modifier = Modifier.fillMaxWidth(),
            )"""

new_block = """            PixelTextField(
                value = state.agentConfig.apiKey,
                onValueChange = { newValue ->
                    if (!showKey) showKey = true
                    onEvent(SettingsEvent.UpdateAgentConfig(state.agentConfig.copy(apiKey = newValue)))
                },
                label = "API Key",
                placeholder = "Enter API key...",
                visualTransformation = if (showKey || state.agentConfig.apiKey.isEmpty()) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
            )"""

content = content.replace(old_block, new_block)

with open('shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt', 'w') as f:
    f.write(content)
