## 2026-03-07 - Prevent Password Autocomplete
**Vulnerability:** OS Dictionary caching and autocomplete can leak masked secrets in Jetpack Compose custom TextFields.
**Learning:** Setting KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false) disables OS dictionary caching.
**Prevention:** Use PasswordVisualTransformation alongside correct KeyboardOptions in custom password fields.
