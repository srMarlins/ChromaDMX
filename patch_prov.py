with open('shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/ProvisioningScreen.kt', 'r') as f:
    content = f.read()

# Add imports
if 'import androidx.compose.foundation.text.KeyboardOptions' not in content:
    content = content.replace('import androidx.compose.ui.text.font.FontWeight\n', 'import androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.foundation.text.KeyboardOptions\nimport androidx.compose.ui.text.input.KeyboardType\n')

# Patch OutlinedTextField for wifiPassword
old_block = """                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text("Wi-Fi Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )"""

new_block = """                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text("Wi-Fi Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                        singleLine = true
                    )"""

content = content.replace(old_block, new_block)

with open('shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/ProvisioningScreen.kt', 'w') as f:
    f.write(content)
