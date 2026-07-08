package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId

@Composable
internal fun BaseThemeSelector(
    themes: List<KeyboardThemeDescriptor>,
    selectedThemeId: KeyboardThemeId,
    onSelected: (KeyboardThemeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    CustomThemeSection(title = stringResource(R.string.custom_theme_base_title), modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            themes.forEach { theme ->
                SelectablePill(
                    label = theme.localizedName(),
                    selected = theme.id == selectedThemeId,
                    onClick = { onSelected(theme.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SelectablePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CardShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        ),
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun KeyboardThemeDescriptor.localizedName(): String = when (id) {
    KeyboardThemeId.Light -> stringResource(R.string.settings_keyboard_theme_light)
    KeyboardThemeId.Dark -> stringResource(R.string.settings_keyboard_theme_dark)
    else -> name
}
