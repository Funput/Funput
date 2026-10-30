package app.funput.funput.ui.settings.enterkey.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.theme.FunputTint

/** How to break a line once Enter sends: holding the key, or Shift+Enter on a hardware keyboard. */
@Composable
internal fun NewLineSection() {
    FunputSection(
        title = stringResource(R.string.settings_enter_key_new_line_title),
        footer = stringResource(R.string.settings_enter_key_new_line_footer),
    ) {
        FunputRow(
            title = stringResource(R.string.settings_enter_key_hold_title),
            summary = stringResource(R.string.settings_enter_key_hold_summary),
            icon = FunputIcons.Gestures,
            tint = FunputTint.BLUE,
        )
        FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
        FunputRow(
            title = stringResource(R.string.settings_enter_key_shift_title),
            summary = stringResource(R.string.settings_enter_key_shift_summary),
            icon = FunputIcons.HardwareKeyboard,
            tint = FunputTint.BLUE,
        )
    }
}
