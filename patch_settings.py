import sys

filepath = "shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt"

with open(filepath, "r") as f:
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false
                ),
                modifier = Modifier.fillMaxWidth(),"""

content = content.replace(search, replace)

if "import androidx.compose.ui.text.input.KeyboardType" not in content:
    content = content.replace("import androidx.compose.foundation.text.KeyboardOptions\n", "import androidx.compose.foundation.text.KeyboardOptions\nimport androidx.compose.ui.text.input.KeyboardType\n")

with open(filepath, "w") as f:
    f.write(content)
print("SettingsScreen.kt patched.")
