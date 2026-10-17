package app.funput.funput.ui.settings.pages

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.funput.funput.R
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.navigation.AppDestination
import app.funput.funput.ui.navigation.TopLevelDestination

/**
 * Every page the keyboard settings open onto, the Android side of iOS's `SettingsDestination`.
 *
 * The keyboard tab lists [hubRows]; [DATA] is reached from "Tôi", next to where an account will
 * live. Adding a page is an entry here, its destination in [AppDestination] and its items in
 * `settingsPageItems`: the hub row, its title and the navigation follow from it.
 *
 * [destinations] lists the screen for each tab the page can be opened from, its home tab first.
 */
internal enum class SettingsPage(
    @param:StringRes val title: Int,
    @param:DrawableRes val icon: Int,
    val tint: FunputTint,
    val destinations: List<AppDestination>,
) {
    TYPING(
        R.string.settings_section_typing, FunputIcons.ToneMarks, FunputTint.ORANGE,
        listOf(AppDestination.TYPING),
    ),
    LAYOUT(
        R.string.settings_section_layout, FunputIcons.Keyboard, FunputTint.BLUE,
        listOf(AppDestination.LAYOUT),
    ),
    SMART_INPUT(
        R.string.settings_section_smart, FunputIcons.Suggestions, FunputTint.PURPLE,
        listOf(AppDestination.SMART_INPUT),
    ),
    FEEDBACK(
        R.string.settings_section_feedback, FunputIcons.Haptics, FunputTint.PINK,
        listOf(AppDestination.FEEDBACK),
    ),
    CLIPBOARD(
        R.string.settings_clipboard_section, FunputIcons.Clipboard, FunputTint.GREEN,
        listOf(AppDestination.CLIPBOARD),
    ),
    SPEECH(
        R.string.speech_settings_title, FunputIcons.Microphone, FunputTint.ORANGE,
        listOf(AppDestination.SPEECH),
    ),
    HARDWARE_KEYBOARD(
        R.string.settings_section_hardware_keyboard, FunputIcons.HardwareKeyboard, FunputTint.GRAY,
        listOf(AppDestination.HARDWARE_KEYBOARD),
    ),
    DATA(
        R.string.settings_section_data, FunputIcons.Data, FunputTint.RED,
        listOf(AppDestination.DATA),
    ),
    ;

    /** The screen showing this page inside [tab], or its home screen when [tab] has none. */
    fun destinationIn(tab: TopLevelDestination): AppDestination =
        destinations.firstOrNull { destination -> destination.tab == tab } ?: destinations.first()

    companion object {
        /** The rows of the keyboard hub, in order. Voice input only where the build ships it. */
        fun hubRows(speechAvailable: Boolean): List<SettingsPage> =
            entries.filter { page -> page != DATA && (page != SPEECH || speechAvailable) }

        /** The page [destination] shows, or null when it is not a settings page. */
        fun of(destination: AppDestination): SettingsPage? =
            entries.firstOrNull { page -> destination in page.destinations }
    }
}
