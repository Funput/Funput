package app.funput.funput.ui.appearance.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.kit.cards.FunputCard
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.overlays.FunputSheet
import app.funput.funput.ui.kit.rows.ActionRow
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.theme.localizedName

/** What can be done to a theme the user made, titled with its name: edit it, or delete it. */
@Composable
internal fun ThemeActionsSheet(
    descriptor: KeyboardThemeDescriptor,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    FunputSheet(onDismiss = onDismiss, title = descriptor.localizedName()) {
        FunputCard {
            ActionRow(
                title = stringResource(R.string.theme_gallery_edit),
                onClick = onEdit,
                icon = FunputIcons.Edit,
            )
            FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
            ActionRow(
                title = stringResource(R.string.theme_gallery_delete),
                onClick = onDelete,
                icon = FunputIcons.Delete,
                destructive = true,
            )
        }
    }
}
