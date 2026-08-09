package app.funput.funput.ui.theme.custom.color

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.theme.KeyboardTheme

/**
 * Two columns rather than eighteen full-width rows.
 *
 * A full-width row per color meant scanning a uniform list to find one entry; paired up, a whole
 * group fits on screen at once and the swatch carries as much of the identification as the label.
 */
@Composable
internal fun ColorRoleGrid(
    roles: List<ThemeColorRole>,
    theme: KeyboardTheme,
    onSelect: (ThemeColorRole) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        roles.chunked(2).forEach { pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                pair.forEach { role ->
                    ColorSwatchRow(
                        label = stringResource(role.labelRes),
                        color = role.read(theme),
                        onClick = { onSelect(role) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keeps a lone trailing entry the same width as the others.
                if (pair.size == 1) Column(modifier = Modifier.weight(1f)) {}
            }
        }
    }
}

@Composable
internal fun ThemeColorTabDialogHost(
    editing: ThemeColorRole?,
    theme: KeyboardTheme,
    onColorChange: (ThemeColorRole, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    editing ?: return
    ColorPickerDialog(
        title = stringResource(editing.labelRes),
        initialColor = editing.read(theme),
        onDismiss = onDismiss,
        onConfirm = { color ->
            onColorChange(editing, color)
            onDismiss()
        },
    )
}
