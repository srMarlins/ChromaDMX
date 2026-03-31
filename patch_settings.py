import re

filepath = "shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt"

with open(filepath, 'r') as f:
    content = f.read()

# Add imports
if 'import androidx.compose.ui.text.input.KeyboardType' not in content:
    content = content.replace(
        'import androidx.compose.foundation.text.KeyboardOptions\n',
        'import androidx.compose.foundation.text.KeyboardOptions\nimport androidx.compose.ui.text.input.KeyboardType\n'
    )

# Find the API Key field and update keyboardOptions
search = """            PixelTextField(
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

replace = """            PixelTextField(
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

if search in content:
    content = content.replace(search, replace)
    with open(filepath, 'w') as f:
        f.write(content)
    print("SettingsScreen.kt patched successfully")
else:
    print("Could not find the target block in SettingsScreen.kt")
