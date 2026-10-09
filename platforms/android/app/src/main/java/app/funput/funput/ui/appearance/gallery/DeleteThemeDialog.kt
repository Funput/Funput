package app.funput.funput.ui.appearance.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.kit.overlays.FunputDialog
import app.funput.funput.ui.theme.localizedName

/** Asks before deleting [theme]; nothing is removed until the user confirms. */
@Composable
internal fun DeleteThemeDialog(
    theme: KeyboardThemeDescriptor,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    FunputDialog(
        title = stringResource(R.string.theme_gallery_delete_dialog_title),
        message = stringResource(R.string.theme_gallery_delete_dialog_body, theme.localizedName()),
        confirmLabel = stringResource(R.string.theme_gallery_delete_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        dismissLabel = stringResource(R.string.theme_gallery_delete_cancel),
        destructive = true,
    )
}
