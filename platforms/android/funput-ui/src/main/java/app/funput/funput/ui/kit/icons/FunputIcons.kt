package app.funput.funput.ui.kit.icons

import androidx.annotation.DrawableRes
import app.funput.funput.ui.kit.R

/**
 * Every icon FunputUI and the app use, named by what it means rather than by what it draws.
 *
 * The drawings are Phosphor (MIT), imported by `scripts/import-phosphor-icons.py` from the list
 * in `funput-ui/icons.txt`. Choosing a different drawing for a meaning is a change here only.
 */
object FunputIcons {
    /** Settings tab, unselected. */
    @DrawableRes val Settings: Int = R.drawable.ph_gear_six

    /** Settings tab, selected. */
    @DrawableRes val SettingsSelected: Int = R.drawable.ph_gear_six_fill

    /** Appearance tab, unselected. */
    @DrawableRes val Appearance: Int = R.drawable.ph_palette

    /** Appearance tab, selected. */
    @DrawableRes val AppearanceSelected: Int = R.drawable.ph_palette_fill

    /** About tab, unselected. */
    @DrawableRes val About: Int = R.drawable.ph_info

    /** About tab, selected. */
    @DrawableRes val AboutSelected: Int = R.drawable.ph_info_fill

    /** Going back one screen. */
    @DrawableRes val Back: Int = R.drawable.ph_caret_left

    /** A row that opens something. */
    @DrawableRes val Forward: Int = R.drawable.ph_caret_right

    /** The chosen option. */
    @DrawableRes val Check: Int = R.drawable.ph_check_bold

    /** Adding an item. */
    @DrawableRes val Add: Int = R.drawable.ph_plus

    /** The keyboard, input method. */
    @DrawableRes val Keyboard: Int = R.drawable.ph_keyboard

    /** Tone marks and their placement. */
    @DrawableRes val ToneMarks: Int = R.drawable.ph_text_aa

    /** Text shortcuts. */
    @DrawableRes val Shortcuts: Int = R.drawable.ph_lightning

    /** The number row. */
    @DrawableRes val NumberRow: Int = R.drawable.ph_number_square_one

    /** Key size and similar adjustments. */
    @DrawableRes val KeySize: Int = R.drawable.ph_sliders_horizontal

    /** Keyboard placement and height. */
    @DrawableRes val Placement: Int = R.drawable.ph_arrows_vertical

    /** One-handed mode. */
    @DrawableRes val OneHanded: Int = R.drawable.ph_hand_pointing

    /** Restoring the typed word. */
    @DrawableRes val Restore: Int = R.drawable.ph_arrow_counter_clockwise

    /** Spell checking. */
    @DrawableRes val SpellCheck: Int = R.drawable.ph_seal_check

    /** Automatic capitalisation. */
    @DrawableRes val Capitalize: Int = R.drawable.ph_text_t

    /** Word suggestions. */
    @DrawableRes val Suggestions: Int = R.drawable.ph_sparkle

    /** Smart gestures. */
    @DrawableRes val Gestures: Int = R.drawable.ph_hand_tap

    /** Returning to the letters after punctuation. */
    @DrawableRes val ReturnToLetters: Int = R.drawable.ph_arrow_u_up_left

    /** Vibration on key press. */
    @DrawableRes val Haptics: Int = R.drawable.ph_vibrate

    /** Key sounds. */
    @DrawableRes val Sound: Int = R.drawable.ph_speaker_high

    /** Clipboard history. */
    @DrawableRes val Clipboard: Int = R.drawable.ph_clipboard_text

    /** How long clipboard items are kept. */
    @DrawableRes val ClipboardExpiry: Int = R.drawable.ph_clock_countdown

    /** A physical keyboard. */
    @DrawableRes val HardwareKeyboard: Int = R.drawable.ph_desktop

    /** Deleting learned data. */
    @DrawableRes val Delete: Int = R.drawable.ph_trash

    /** Clearing a history. */
    @DrawableRes val Clear: Int = R.drawable.ph_broom
}
