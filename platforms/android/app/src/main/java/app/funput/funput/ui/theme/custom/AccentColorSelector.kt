package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import androidx.compose.ui.res.stringResource

@Composable
internal fun AccentColorSelector(
    selectedColor: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    CustomThemeSection(title = stringResource(R.string.custom_theme_accent_title), modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            AccentPresets.forEach { preset ->
                AccentColorSwatch(
                    preset = preset,
                    selected = preset.argb == selectedColor,
                    onClick = { onSelected(preset.argb) },
                )
            }
        }
    }
}

@Composable
private fun AccentColorSwatch(
    preset: AccentPreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = preset.color,
        border = BorderStroke(
            width = if (selected) 4.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        ),
        modifier = Modifier
            .size(46.dp)
            .semantics { contentDescription = preset.label }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {}
}
