import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

fun testToggleable(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    interactionSource: MutableInteractionSource,
    enabled: Boolean
): Modifier {
    return Modifier.toggleable(
        value = checked,
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        role = Role.Switch,
        onValueChange = { onCheckedChange?.invoke(it) }
    )
}
