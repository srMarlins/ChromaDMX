import sys

filepath = 'shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt'
with open(filepath, 'r') as f:
    content = f.read()

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

if search in content:
    content = content.replace(search, replace)
    with open(filepath, 'w') as f:
        f.write(content)
    print("Patched SettingsScreen.kt successfully.")
else:
    print("Could not find search string in SettingsScreen.kt.")
