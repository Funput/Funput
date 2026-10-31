package app.funput.funput.ui.settings.smart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputListIcons
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.settings.SettingsScreenState

/**
 * What Funput does on its own while you type, one switch per [SmartInputToggle]. The suggestion
 * bar's switches get a card of their own, with the way into the words it has learned; everything
 * that acts while typing shares the other.
 */
@Composable
internal fun SmartSection(state: SettingsScreenState, onOpenDictionary: () -> Unit = {}) {
    Column(
        verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.section),
        modifier = Modifier.testTag(SmartSettingsSectionTag),
    ) {
        FunputSection(
            title = stringResource(R.string.settings_smart_section_suggestions),
            footer = stringResource(R.string.settings_smart_section_suggestions_footer),
        ) {
            toggles(SmartInputToggle.suggestionBar, state)
            FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
            LinkRow(
                title = stringResource(R.string.dictionary_title),
                summary = stringResource(R.string.dictionary_link_summary),
                onClick = onOpenDictionary,
                icon = FunputListIcons.Dictionary,
                tint = FunputTint.PURPLE,
            )
        }
        FunputSection(title = stringResource(R.string.settings_smart_section_typing)) {
            toggles(SmartInputToggle.typing, state)
        }
    }
}

@Composable
private fun ColumnScope.toggles(toggles: List<SmartInputToggle>, state: SettingsScreenState) {
    toggles.forEachIndexed { index, toggle ->
        if (index > 0) FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
        ToggleRow(
            title = stringResource(toggle.title),
            summary = toggle.summary?.let { stringResource(it) },
            checked = toggle.isOn(state),
            onCheckedChange = { enabled -> toggle.set(state, enabled) },
            icon = toggle.icon,
            tint = FunputTint.PURPLE,
        )
    }
}

/** Test tag of the smart input section. */
internal const val SmartSettingsSectionTag = "settings-section-smart"
