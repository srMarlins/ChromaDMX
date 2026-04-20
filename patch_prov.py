import sys

file_path = "shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/ProvisioningScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

import_search = """import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation"""
import_replace = """import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation"""

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

content = content.replace(import_search, import_replace)
if search in content:
    with open(file_path, "w") as f:
        f.write(content.replace(search, replace))
    print("Provisioning patched successfully.")
else:
    print("Search block not found in Provisioning.")
