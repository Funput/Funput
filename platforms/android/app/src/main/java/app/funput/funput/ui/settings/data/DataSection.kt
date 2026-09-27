package app.funput.funput.ui.settings.data

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.overlays.FunputDialog
import app.funput.funput.ui.kit.rows.ActionRow
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.theme.FunputTint

/** What can be erased, and the words the confirmation uses for it. */
private enum class DataAction(
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    @param:StringRes val confirm: Int,
) {
    SUGGESTIONS(
        R.string.settings_personal_suggestions_reset_title,
        R.string.settings_personal_suggestions_reset_body,
        R.string.settings_personal_suggestions_reset_confirm,
    ),
    CLIPBOARD(
        R.string.settings_clipboard_clear_dialog_title,
        R.string.settings_clipboard_clear_dialog_body,
        R.string.settings_clipboard_clear,
    ),
}

/**
 * Data Funput keeps on the device and can erase: learned words and clipboard history. Both are
 * destructive and cannot be undone, so each asks first; cancelling keeps everything.
 */
@Composable
internal fun DataSection(onResetPersonalSuggestions: () -> Unit, onClearClipboardHistory: () -> Unit) {
    var pending by rememberSaveable { mutableStateOf<DataAction?>(null) }
    FunputSection(
        title = stringResource(R.string.settings_section_data),
        modifier = Modifier.testTag(DataSettingsSectionTag),
    ) {
        ActionRow(
            title = stringResource(R.string.settings_data_suggestions_title),
            summary = stringResource(R.string.settings_data_suggestions_summary),
            onClick = { pending = DataAction.SUGGESTIONS },
            icon = FunputIcons.Delete,
            tint = FunputTint.RED,
            destructive = true,
        )
        FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
        ActionRow(
            title = stringResource(R.string.settings_data_clipboard_title),
            summary = stringResource(R.string.settings_data_clipboard_summary),
            onClick = { pending = DataAction.CLIPBOARD },
            icon = FunputIcons.Clear,
            tint = FunputTint.RED,
            destructive = true,
        )
    }
    pending?.let { action ->
        FunputDialog(
            title = stringResource(action.title),
            message = stringResource(action.body),
            confirmLabel = stringResource(action.confirm),
            onConfirm = {
                pending = null
                when (action) {
                    DataAction.SUGGESTIONS -> onResetPersonalSuggestions()
                    DataAction.CLIPBOARD -> onClearClipboardHistory()
                }
            },
            onDismiss = { pending = null },
            dismissLabel = stringResource(R.string.settings_clipboard_clear_dialog_cancel),
            destructive = true,
        )
    }
}

/** Test tag of the data section. */
internal const val DataSettingsSectionTag = "settings-section-data"
