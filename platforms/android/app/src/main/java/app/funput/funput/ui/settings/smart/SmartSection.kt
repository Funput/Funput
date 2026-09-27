package app.funput.funput.ui.settings.smart

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.kit.theme.FunputTint

/** One switch of the smart group: its text, icon, current value and what flipping it does. */
private class SmartToggle(
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int?,
    @param:DrawableRes val icon: Int,
    val checked: Boolean,
    val onChange: (Boolean) -> Unit,
)

/**
 * What Funput does on its own while you type: restore English words, check spelling, capitalise,
 * suggest, understand gestures, return to the letters. Six switches, one purple group.
 */
@Composable
internal fun SmartSection(
    preferences: SmartCompositionPreferences,
    personalSuggestionsEnabled: Boolean,
    smartGesturesEnabled: Boolean,
    returnsToLettersEnabled: Boolean,
    onSmartRestoreChanged: (Boolean) -> Unit,
    onSpellCheckChanged: (Boolean) -> Unit,
    onAutoCapitalizeChanged: (Boolean) -> Unit,
    onPersonalSuggestionsChanged: (Boolean) -> Unit,
    onSmartGesturesChanged: (Boolean) -> Unit,
    onReturnsToLettersChanged: (Boolean) -> Unit,
) {
    val toggles = listOf(
        SmartToggle(
            R.string.settings_smart_restore_title, null, FunputIcons.Restore,
            preferences.smartRestoreEnabled, onSmartRestoreChanged,
        ),
        SmartToggle(
            R.string.settings_spell_check_title, null, FunputIcons.SpellCheck,
            preferences.spellCheckEnabled, onSpellCheckChanged,
        ),
        SmartToggle(
            R.string.settings_auto_capitalize_title, R.string.settings_auto_capitalize_summary, FunputIcons.Capitalize,
            preferences.autoCapitalizeEnabled, onAutoCapitalizeChanged,
        ),
        SmartToggle(
            R.string.settings_personal_suggestions_title, R.string.settings_personal_suggestions_description,
            FunputIcons.Suggestions, personalSuggestionsEnabled, onPersonalSuggestionsChanged,
        ),
        SmartToggle(
            R.string.settings_smart_gestures_title, R.string.settings_smart_gestures_summary, FunputIcons.Gestures,
            smartGesturesEnabled, onSmartGesturesChanged,
        ),
        SmartToggle(
            R.string.settings_letter_return_title, R.string.settings_letter_return_summary,
            FunputIcons.ReturnToLetters, returnsToLettersEnabled, onReturnsToLettersChanged,
        ),
    )
    FunputSection(
        title = stringResource(R.string.settings_section_smart),
        modifier = Modifier.testTag(SmartSettingsSectionTag),
    ) {
        toggles.forEachIndexed { index, toggle ->
            if (index > 0) FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
            ToggleRow(
                title = stringResource(toggle.title),
                summary = toggle.summary?.let { stringResource(it) },
                checked = toggle.checked,
                onCheckedChange = toggle.onChange,
                icon = toggle.icon,
                tint = FunputTint.PURPLE,
            )
        }
    }
}

/** Test tag of the smart input section. */
internal const val SmartSettingsSectionTag = "settings-section-smart"
