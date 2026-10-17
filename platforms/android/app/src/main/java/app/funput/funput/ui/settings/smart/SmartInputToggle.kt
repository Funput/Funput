package app.funput.funput.ui.settings.smart

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.funput.funput.R
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.settings.SettingsScreenState

/**
 * The switches of the smart page, declared once as data. The page, the summary under its hub row
 * and the settings search all read this list, so a new switch shows up in all three at once.
 */
internal enum class SmartInputToggle(
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int?,
    @param:DrawableRes val icon: Int,
    private val read: (SettingsScreenState) -> Boolean,
    private val write: (SettingsScreenState) -> (Boolean) -> Unit,
    /** Extra words the settings search finds this switch by; never shown. */
    val keywords: List<String>,
) {
    SMART_RESTORE(
        R.string.settings_smart_restore_title, null, FunputIcons.Restore,
        { it.smartComposition.smartRestoreEnabled }, { it.onSmartRestoreChanged },
        listOf("english", "tieng anh", "restore"),
    ),
    SPELL_CHECK(
        R.string.settings_spell_check_title, null, FunputIcons.SpellCheck,
        { it.smartComposition.spellCheckEnabled }, { it.onSpellCheckChanged },
        listOf("spell", "chinh ta"),
    ),
    AUTO_CAPITALIZE(
        R.string.settings_auto_capitalize_title, R.string.settings_auto_capitalize_summary, FunputIcons.Capitalize,
        { it.smartComposition.autoCapitalizeEnabled }, { it.onAutoCapitalizeChanged },
        listOf("capitalize", "chu hoa"),
    ),
    TYPO_CORRECTION(
        R.string.settings_typo_correction_title, R.string.settings_typo_correction_summary, FunputIcons.TypoCorrection,
        { it.smartComposition.typoCorrectionEnabled }, { it.onTypoCorrectionChanged },
        listOf("typo", "go nham", "sua loi", "autocorrect"),
    ),
    SUGGESTIONS(
        R.string.settings_personal_suggestions_title, R.string.settings_personal_suggestions_description,
        FunputIcons.Suggestions, { it.personalSuggestionsEnabled }, { it.onPersonalSuggestionsChanged },
        listOf("suggestion", "learned"),
    ),
    SMART_GESTURES(
        R.string.settings_smart_gestures_title, R.string.settings_smart_gestures_summary, FunputIcons.Gestures,
        { it.smartGesturesEnabled }, { it.onSmartGesturesChanged },
        listOf("gesture", "con tro", "cursor", "space"),
    ),
    RETURN_TO_LETTERS(
        R.string.settings_letter_return_title, R.string.settings_letter_return_summary, FunputIcons.ReturnToLetters,
        { it.returnsToLettersEnabled }, { it.onReturnsToLettersChanged },
        listOf("punctuation", "dau cau"),
    ),
    ;

    /** Whether this switch is on in [state]. */
    fun isOn(state: SettingsScreenState): Boolean = read(state)

    /** Sends [enabled] to whatever [state] saves this switch through. */
    fun set(state: SettingsScreenState, enabled: Boolean) = write(state)(enabled)
}
