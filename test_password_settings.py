with open('shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

print("KeyboardType.Password" in content)
