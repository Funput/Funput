package app.funput.funput.ui.settings.search

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.funput.funput.R
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.settings.pages.SettingsPage
import app.funput.funput.ui.settings.smart.SmartInputToggle

/**
 * One setting the search can find: what it is called, the page it lives on, and extra words people
 * may type for it (English names, words without accents). Keywords are never shown.
 */
internal class SettingsSearchEntry(
    @param:StringRes val title: Int,
    @param:DrawableRes val icon: Int,
    val page: SettingsPage,
    val keywords: List<String> = emptyList(),
    @param:StringRes val summary: Int? = null,
)

private fun entry(@StringRes title: Int, @DrawableRes icon: Int, page: SettingsPage, vararg keywords: String) =
    SettingsSearchEntry(title, icon, page, keywords.toList())

/** The settings that open a picker or slider. */
private val pickers = listOf(
    entry(R.string.settings_input_method_title, FunputIcons.Keyboard, SettingsPage.TYPING,
        "telex", "vni", "input method", "telex nang cao"),
    entry(R.string.settings_tone_style_title, FunputIcons.ToneMarks, SettingsPage.TYPING,
        "dau", "tone", "hoa", "oa", "truyen thong", "hien dai"),
    entry(R.string.settings_keyboard_mode_title, FunputIcons.Placement, SettingsPage.LAYOUT,
        "mot tay", "tren cao", "one handed", "che do"),
    entry(R.string.settings_key_size_title, FunputIcons.KeySize, SettingsPage.LAYOUT, "size", "co phim", "height"),
    entry(R.string.settings_clipboard_expiry_title, FunputIcons.ClipboardExpiry, SettingsPage.CLIPBOARD,
        "het han", "expiry", "clipboard"),
)

/** Switches and actions listed by hand, with the words people use for them. */
private val rows = listOf(
    entry(R.string.extra_onsets_section, FunputIcons.ToneMarks, SettingsPage.TYPING,
        "z f w j", "teencode", "zo", "fai"),
    entry(R.string.settings_number_row_title, FunputIcons.NumberRow, SettingsPage.LAYOUT, "number row", "123", "so"),
    entry(R.string.settings_haptics_title, FunputIcons.Haptics, SettingsPage.FEEDBACK,
        "haptic", "vibration", "cuong do", "strength", "nhe", "manh"),
    entry(R.string.settings_sounds_title, FunputIcons.Sound, SettingsPage.FEEDBACK, "sound", "click", "tieng"),
    entry(R.string.settings_key_previews_title, FunputIcons.KeyPreview, SettingsPage.FEEDBACK,
        "preview", "phong to", "popup"),
    entry(R.string.settings_clipboard_enabled_title, FunputIcons.Clipboard, SettingsPage.CLIPBOARD,
        "clipboard", "copy", "sao chep", "dan", "paste"),
    entry(R.string.speech_settings_title, FunputIcons.Microphone, SettingsPage.SPEECH,
        "voice", "micro", "speech", "noi"),
    entry(R.string.settings_hardware_keyboard_show_title, FunputIcons.HardwareKeyboard, SettingsPage.HARDWARE_KEYBOARD,
        "hardware", "physical", "vat ly"),
    entry(R.string.settings_hardware_keyboard_hotkey_title, FunputIcons.Hotkey, SettingsPage.HARDWARE_KEYBOARD,
        "hotkey", "phim tat", "shortcut"),
    entry(R.string.settings_hardware_keyboard_system_title, FunputIcons.OpenExternal, SettingsPage.HARDWARE_KEYBOARD,
        "hardware", "physical", "android"),
    entry(R.string.settings_data_suggestions_title, FunputIcons.Delete, SettingsPage.DATA,
        "xoa", "goi y", "learned", "reset"),
    entry(R.string.settings_data_clipboard_title, FunputIcons.Clear, SettingsPage.DATA, "xoa", "clipboard", "ghim"),
)

/** Every searchable setting: the pickers, each smart switch, then the rest. */
internal val SettingsSearchEntries: List<SettingsSearchEntry> = pickers +
    SmartInputToggle.entries.map { toggle ->
        SettingsSearchEntry(toggle.title, toggle.icon, SettingsPage.SMART_INPUT, toggle.keywords, toggle.summary)
    } +
    rows
