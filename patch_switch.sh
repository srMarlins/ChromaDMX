sed -i 's/import androidx.compose.foundation.clickable/import androidx.compose.foundation.selection.toggleable/' shared/src/commonMain/kotlin/com/chromadmx/ui/components/PixelSwitch.kt
sed -i 's/\.clickable(/.toggleable(\n            value = checked,/' shared/src/commonMain/kotlin/com/chromadmx/ui/components/PixelSwitch.kt
sed -i 's/onClick = { onCheckedChange?.invoke(!checked) }/onValueChange = { onCheckedChange?.invoke(it) }/' shared/src/commonMain/kotlin/com/chromadmx/ui/components/PixelSwitch.kt
