import re

with open("shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/ProvisioningScreen.kt", "r") as f:
    content = f.read()

# Add imports if they don't exist
if "import androidx.compose.foundation.text.KeyboardOptions" not in content:
    content = content.replace("import androidx.compose.ui.Alignment", "import androidx.compose.foundation.text.KeyboardOptions\nimport androidx.compose.ui.text.input.KeyboardType\nimport androidx.compose.ui.Alignment")

# Update Wi-Fi Password text field
search = """                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text("Wi-Fi Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )"""

replace = """                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text("Wi-Fi Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                        singleLine = true
                    )"""

content = content.replace(search, replace)

with open("shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/ProvisioningScreen.kt", "w") as f:
    f.write(content)
