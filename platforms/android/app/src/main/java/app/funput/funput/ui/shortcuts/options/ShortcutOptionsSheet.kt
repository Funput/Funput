package app.funput.funput.ui.shortcuts.options

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.overlays.FunputSheet
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.shortcuts.ShortcutsScreenModel

/** How shortcuts expand: matching the typed case, and whether they also work in English mode. */
@Composable
internal fun ShortcutOptionsSheet(model: ShortcutsScreenModel, dismiss: () -> Unit) {
    val library = model.library
    FunputSheet(
        onDismiss = { if (!model.isSaving) dismiss() },
        title = stringResource(R.string.shortcuts_options_title),
    ) {
        FunputSection(title = null, footer = stringResource(R.string.shortcuts_reopen_hint)) {
            ToggleRow(
                title = stringResource(R.string.shortcuts_smart_case),
                summary = stringResource(R.string.shortcuts_smart_case_summary),
                checked = library.smartCase,
                onCheckedChange = { value -> model.updateOptions { it.copy(smartCase = value) } },
                icon = FunputIcons.Capitalize,
                enabled = model.canWrite,
            )
            FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
            ToggleRow(
                title = stringResource(R.string.shortcuts_in_english),
                summary = stringResource(R.string.shortcuts_in_english_summary),
                checked = library.inEnglish,
                onCheckedChange = { value -> model.updateOptions { it.copy(inEnglish = value) } },
                icon = FunputIcons.Language,
                enabled = model.canWrite,
            )
        }
        FunputButton(
            text = stringResource(R.string.shortcuts_done),
            onClick = dismiss,
            enabled = !model.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
