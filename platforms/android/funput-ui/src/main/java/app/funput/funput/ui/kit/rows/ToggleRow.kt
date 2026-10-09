package app.funput.funput.ui.kit.rows

import androidx.annotation.DrawableRes
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import app.funput.funput.ui.kit.controls.FunputToggle
import app.funput.funput.ui.kit.theme.FunputTint

/**
 * A setting that is on or off. The whole row is the switch: tapping anywhere flips it, and
 * TalkBack reads the title, summary and state as one switch rather than a label next to a control.
 */
@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
    tint: FunputTint = FunputTint.ORANGE,
    enabled: Boolean = true,
) {
    FunputRow(
        title = title,
        summary = summary,
        icon = icon,
        tint = tint,
        enabled = enabled,
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        accessory = { FunputToggle(checked = checked, onCheckedChange = null) },
    )
}
