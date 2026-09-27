package app.funput.funput.ui.settings.clipboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.settings.label

/** Clipboard history: whether Funput keeps it, and how long unpinned items last. One green group. */
@Composable
internal fun ClipboardSection(
    enabled: Boolean,
    expiry: ClipboardExpiry,
    onEnabledChanged: (Boolean) -> Unit,
    onOpenExpiry: () -> Unit,
) {
    FunputSection(
        title = stringResource(R.string.settings_clipboard_section),
        modifier = Modifier.testTag(ClipboardSettingsSectionTag),
    ) {
        ToggleRow(
            title = stringResource(R.string.settings_clipboard_enabled_title),
            summary = stringResource(R.string.settings_clipboard_enabled_summary),
            checked = enabled,
            onCheckedChange = onEnabledChanged,
            icon = FunputIcons.Clipboard,
            tint = FunputTint.GREEN,
        )
        FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
        LinkRow(
            title = stringResource(R.string.settings_clipboard_expiry_title),
            summary = stringResource(R.string.settings_clipboard_expiry_summary),
            value = expiry.label(),
            onClick = onOpenExpiry,
            icon = FunputIcons.ClipboardExpiry,
            tint = FunputTint.GREEN,
        )
    }
}

/** Test tag of the clipboard section. */
internal const val ClipboardSettingsSectionTag = "settings-section-clipboard"
