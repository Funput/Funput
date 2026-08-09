package app.funput.funput.ui.theme.custom.color

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextButton
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme

@Composable
internal fun ThemeColorTab(
    theme: KeyboardTheme,
    onColorChange: (ThemeColorRole, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<ThemeColorRole?>(null) }

    var showsDerived by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = modifier) {
        ThemeColorGroup.entries.forEach { group ->
            val roles = ThemeColorRole.entries.filter { role -> role.group == group }
            // Roles that follow another are only shown once asked for. They are not choices until
            // somebody sets them apart, and listing them as choices is what made six look like
            // twenty.
            val direct = roles.filter { role -> !ThemeColorLinks.isAutomatic(role, theme) }
            val automatic = roles - direct.toSet()
            if (group == ThemeColorGroup.Advanced && !showsDerived) return@forEach
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(group.titleRes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                ColorRoleGrid(roles = direct, theme = theme, onSelect = { role -> editing = role })
                automatic.forEach { role -> AutomaticColorRow(role, theme) { editing = role } }
            }
        }
        TextButton(onClick = { showsDerived = !showsDerived }) {
            Text(
                stringResource(
                    if (showsDerived) {
                        R.string.custom_theme_color_hide_advanced
                    } else {
                        R.string.custom_theme_color_show_advanced
                    },
                ),
            )
        }
        ThemeContrastWarnings(theme)
    }

    ThemeColorTabDialogHost(
        editing = editing,
        theme = theme,
        onColorChange = onColorChange,
        onDismiss = { editing = null },
    )
}

/**
 * Two columns rather than eighteen full-width rows.
 *
 * A full-width row per color meant scanning a uniform list to find one entry; paired up, a whole
 * group fits on screen at once and the swatch carries as much of the identification as the label.
 */
@Composable
private fun ColorRoleGrid(
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
private fun ThemeColorTabDialogHost(
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
