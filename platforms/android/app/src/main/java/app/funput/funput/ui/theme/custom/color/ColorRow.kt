package app.funput.funput.ui.theme.custom.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * One colour as a row: its name (and [summary], such as "follows the accent"), with the colour
 * itself as a swatch at the end. Tapping opens the picker. Used for theme roles and the image
 * overlay alike.
 */
@Composable
internal fun ColorRow(
    label: String,
    color: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    val description = stringResource(R.string.custom_theme_color_edit_description, label)
    FunputRow(
        title = label,
        summary = summary,
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        accessory = { Swatch(color) },
    )
}

@Composable
private fun Swatch(color: Int) {
    val shape = FunputUi.shapes.smallTile
    // A transparent colour would otherwise be an empty space; the border marks where it is.
    Box(
        Modifier
            .size(28.dp)
            .clip(shape)
            .background(Color(color))
            .border(1.dp, FunputUi.colors.separator, shape),
    )
}
