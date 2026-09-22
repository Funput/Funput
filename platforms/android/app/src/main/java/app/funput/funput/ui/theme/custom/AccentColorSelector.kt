package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.theme.custom.color.ColorPickerDialog
import app.funput.funput.ui.theme.custom.color.ColorSwatchRow

/**
 * The accent for a new theme, chosen freely rather than from a fixed set of swatches.
 *
 * Confirming hands the color to [onSelected], which re-dyes the theme. Dismissing the dialog
 * leaves the current accent in place.
 */
@Composable
internal fun AccentColorSelector(
    selectedColor: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf(false) }
    CustomThemeSection(title = stringResource(R.string.custom_theme_accent_title), modifier = modifier) {
        ColorSwatchRow(
            label = stringResource(R.string.custom_theme_accent_pick),
            color = selectedColor,
            onClick = { picking = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AccentColorSelectorTag),
        )
    }
    if (picking) {
        ColorPickerDialog(
            title = stringResource(R.string.custom_theme_accent_title),
            initialColor = selectedColor,
            onDismiss = { picking = false },
            onConfirm = { color ->
                onSelected(color)
                picking = false
            },
        )
    }
}

internal const val AccentColorSelectorTag = "custom-theme-accent"
