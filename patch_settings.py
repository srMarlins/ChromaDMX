import re

with open("shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt", "r") as f:
    content = f.read()

# Update API Key text field to add keyboardOptions
search = """                visualTransformation = if (showKey || state.agentConfig.apiKey.isEmpty()) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                },
                modifier = Modifier.fillMaxWidth(),"""

replace = """                visualTransformation = if (showKey || state.agentConfig.apiKey.isEmpty()) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),"""

content = content.replace(search, replace)

with open("shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt", "w") as f:
    f.write(content)
