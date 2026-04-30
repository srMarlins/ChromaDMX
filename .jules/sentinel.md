## 2025-03-03 - Insecure ADB Backup Configuration
**Vulnerability:** The AndroidManifest.xml had `android:allowBackup="true"`.
**Learning:** This is a common default that exposes app data to unauthorized local extraction via `adb backup`.
**Prevention:** Always explicitly set `android:allowBackup="false"` in Android apps unless there is a specific, well-reasoned need for it, and then use `android:fullBackupContent` to restrict what is backed up.
