package app.funput.funput.ui.settings.quick

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.funput.funput.R
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.settings.SettingsScreenState

/**
 * The tiles of the hub's quick settings, in grid order: the switches people flip by taste rather
 * than set once. [title] is the short name on the tile, [accessibilityTitle] the setting's full
 * name, which TalkBack reads and the settings pages use.
 *
 * iOS has a key preview tile here; Android has no such setting, so smart gestures take its place.
 */
internal enum class QuickSetting(
    @param:StringRes val title: Int,
    @param:StringRes val accessibilityTitle: Int,
    @param:DrawableRes val icon: Int,
    private val read: (SettingsScreenState) -> Boolean,
    private val write: (SettingsScreenState) -> (Boolean) -> Unit,
) {
    HAPTICS(
        R.string.quick_haptics, R.string.settings_haptics_title, FunputIcons.Haptics,
        { it.hapticsEnabled }, { it.onHapticsChanged },
    ),
    SOUNDS(
        R.string.quick_sounds, R.string.settings_sounds_title, FunputIcons.Sound,
        { it.soundsEnabled }, { it.onSoundsChanged },
    ),
    AUTO_CAPITALIZE(
        R.string.quick_capitalize, R.string.settings_auto_capitalize_title, FunputIcons.Capitalize,
        { it.smartComposition.autoCapitalizeEnabled }, { it.onAutoCapitalizeChanged },
    ),
    SUGGESTIONS(
        R.string.quick_suggestions, R.string.settings_personal_suggestions_title, FunputIcons.Suggestions,
        { it.personalSuggestionsEnabled }, { it.onPersonalSuggestionsChanged },
    ),
    NUMBER_ROW(
        R.string.quick_number_row, R.string.settings_number_row_title, FunputIcons.NumberRow,
        { it.showsNumberRow }, { it.onShowsNumberRowChanged },
    ),
    SMART_GESTURES(
        R.string.quick_gestures, R.string.settings_smart_gestures_title, FunputIcons.Gestures,
        { it.smartGesturesEnabled }, { it.onSmartGesturesChanged },
    ),
    ;

    /**
     * Whether this tile cannot be changed in [state]. VNI types tones with the digits, so for it
     * the number row is always on.
     */
    fun isLocked(state: SettingsScreenState): Boolean = this == NUMBER_ROW && !state.inputMethod.isTelexFamily

    /** Whether the tile shows as on: a locked number row always is. */
    fun isOn(state: SettingsScreenState): Boolean = isLocked(state) || read(state)

    /** The tile's state line: "Bật", "Tắt", or "Luôn bật" when locked. */
    @StringRes
    fun stateLabel(state: SettingsScreenState): Int = when {
        isLocked(state) -> R.string.quick_locked
        isOn(state) -> R.string.quick_on
        else -> R.string.quick_off
    }

    /** Flips the setting through [state]; a locked tile ignores the tap. */
    fun toggle(state: SettingsScreenState) {
        if (!isLocked(state)) write(state)(!read(state))
    }
}
