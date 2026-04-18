import sys

filepath = "shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/ProvisioningScreen.kt"

with open(filepath, "r") as f:
    content = f.read()

# Add KeyboardOptions import if missing
if "import androidx.compose.foundation.text.KeyboardOptions" not in content:
    content = content.replace("import androidx.compose.foundation.layout.width\n", "import androidx.compose.foundation.layout.width\nimport androidx.compose.foundation.text.KeyboardOptions\n")

if "import androidx.compose.ui.text.input.KeyboardType" not in content:
    content = content.replace("import androidx.compose.ui.text.input.PasswordVisualTransformation\n", "import androidx.compose.ui.text.input.PasswordVisualTransformation\nimport androidx.compose.ui.text.input.KeyboardType\n")

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

with open(filepath, "w") as f:
    f.write(content)
print("ProvisioningScreen.kt patched.")
