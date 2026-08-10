package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor

/**
 * Which theme this one is built on.
 *
 * This used to live inside a "Khôi phục" button in the title bar, where the first decision of the
 * screen was disguised as an undo. It is the first thing shown now, and restoring is a separate
 * action that means only what it says.
 */
@Composable
internal fun BaseThemeSelector(
    baseThemes: List<KeyboardThemeDescriptor>,
    selectedValue: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CustomThemeSection(title = stringResource(R.string.custom_theme_base_title), modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            baseThemes.forEach { descriptor ->
                FilterChip(
                    selected = descriptor.id.value == selectedValue,
                    onClick = { onSelected(descriptor.id.value) },
                    label = { Text(descriptor.name) },
                )
            }
        }
    }
}
