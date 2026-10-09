package app.funput.funput.ui.theme.custom.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * Save and cancel, always reachable. A save button at the end of a scrolling page was invisible
 * while working on the page that needed it most.
 */
@Composable
internal fun ThemeStudioActionBar(canSave: Boolean, onSave: () -> Unit, onCancel: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(FunputUi.spacing.medium),
        modifier = Modifier.padding(horizontal = FunputUi.spacing.pageMargin, vertical = FunputUi.spacing.medium),
    ) {
        FunputButton(
            text = stringResource(R.string.custom_theme_cancel),
            onClick = onCancel,
            style = FunputButtonStyle.SECONDARY,
            modifier = Modifier.weight(1f),
        )
        FunputButton(
            text = stringResource(R.string.custom_theme_save),
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.weight(2f),
        )
    }
}
