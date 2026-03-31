import re

filepath = "shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/ProvisioningScreen.kt"

with open(filepath, 'r') as f:
    content = f.read()

# Add imports
if 'import androidx.compose.ui.text.input.KeyboardType' not in content:
    content = content.replace(
        'import androidx.compose.ui.text.input.PasswordVisualTransformation\n',
        'import androidx.compose.foundation.text.KeyboardOptions\nimport androidx.compose.ui.text.input.KeyboardType\nimport androidx.compose.ui.text.input.PasswordVisualTransformation\n'
    )

# Find the Wi-Fi Password field and update
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

if search in content:
    content = content.replace(search, replace)
    with open(filepath, 'w') as f:
        f.write(content)
    print("ProvisioningScreen.kt patched successfully")
else:
    print("Could not find the target block in ProvisioningScreen.kt")
