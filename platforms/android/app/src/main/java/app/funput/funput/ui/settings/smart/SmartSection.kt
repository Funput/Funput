package app.funput.funput.ui.settings.smart

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.sectionTitle

/**
 * What Funput does on its own while you type: restore English words, check spelling, capitalise,
 * suggest, understand gestures, return to the letters. One switch per [SmartInputToggle].
 */
@Composable
internal fun SmartSection(state: SettingsScreenState) {
    FunputSection(
        title = sectionTitle(R.string.settings_section_smart),
        modifier = Modifier.testTag(SmartSettingsSectionTag),
    ) {
        SmartInputToggle.entries.forEachIndexed { index, toggle ->
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
}

/** Test tag of the smart input section. */
internal const val SmartSettingsSectionTag = "settings-section-smart"
