import sys

file_path = "shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt"
with open(file_path, "r") as f:
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
    with open(file_path, "w") as f:
        f.write(content.replace(search, replace))
    print("Settings patched successfully.")
else:
    print("Search block not found in Settings.")
