1. Update `ProvisioningScreen.kt` to include `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` on the `OutlinedTextField` for `wifiPassword`. Also add necessary imports (`androidx.compose.foundation.text.KeyboardOptions`, `androidx.compose.ui.text.input.KeyboardType`).
2. Update `SettingsScreen.kt` to include `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` on the `PixelTextField` for `apiKey`.
3. Add a journal entry in `.jules/sentinel.md` documenting the requirement to add `KeyboardOptions` to password fields to prevent OS dictionary caching and autocomplete.
4. Run tests and lint checks.
5. Create a dummy screenshot and run `frontend_verification_instructions` and `frontend_verification_complete`.
6. Follow pre commit instructions.
