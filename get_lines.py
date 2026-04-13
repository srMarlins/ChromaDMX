with open('shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/SettingsScreen.kt', 'r') as f:
    lines = f.readlines()
for i, line in enumerate(lines[732:748]):
    print(f"{733+i}: {line}", end='')
