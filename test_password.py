with open('shared/src/commonMain/kotlin/com/chromadmx/ui/screen/settings/ProvisioningScreen.kt', 'r') as f:
    content = f.read()

print("KeyboardType" in content)
print("KeyboardOptions" in content)
