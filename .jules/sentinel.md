## 2026-03-01 - [High] Secure Masking of Secrets in Jetpack Compose UI
**Vulnerability:** OS dictionary caching and autocomplete were enabled on sensitive text fields (like API keys and Wi-Fi passwords), meaning secrets could be cached by the OS keyboard and suggested to the user later.
**Learning:** For masking secrets like API keys in Compose UI state, custom text fields must support Compose's `VisualTransformation` parameter AND set `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` to prevent OS dictionary caching and autocomplete.
**Prevention:** When building custom TextFields (e.g., `PixelTextField`), always set `KeyboardOptions` to use `KeyboardType.Password` and `autoCorrectEnabled = false` alongside `PasswordVisualTransformation()`.
