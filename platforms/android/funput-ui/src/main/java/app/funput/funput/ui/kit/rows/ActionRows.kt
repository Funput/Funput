package app.funput.funput.ui.kit.rows

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.R
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * A row that opens something: a sheet, a sub-screen, a picker. Shows the current [value] (which
 * moves under the title when long) and a chevron.
 */
@Composable
fun LinkRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    value: String? = null,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val colors = FunputUi.colors
    FunputRow(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        modifier = modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        detail = value?.let {
            // No end alignment: beside the title the layout already places it at the end, and
            // under the title it must read from the start like the title does.
            { BasicText(text = it, style = FunputUi.typography.body.copy(color = colors.secondaryLabel)) }
        },
        accessory = { RowGlyph(R.drawable.funput_ic_chevron, colors.tertiaryLabel) },
    )
}

/**
 * One option in a list of mutually exclusive choices, such as a picker sheet. The chosen one
 * carries a check; TalkBack reads each as a radio button with its state.
 */
@Composable
fun ChoiceRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    FunputRow(
        title = title,
        summary = summary,
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        accessory = if (selected) {
            { RowGlyph(R.drawable.funput_ic_check, FunputUi.colors.accent) }
        } else {
            null
        },
    )
}

@Composable
private fun RowGlyph(@DrawableRes icon: Int, tint: Color) {
    Image(
        painter = painterResource(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = Modifier.size(20.dp),
    )
}
