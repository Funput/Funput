package app.funput.funput.ui.settings.summary

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import app.funput.funput.R
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetter
import app.funput.funput.keyboard.KeyboardHapticStrength
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.pages.SettingsPage
import app.funput.funput.ui.settings.smart.SmartInputToggle
import kotlin.math.roundToInt

/**
 * The sentence under each hub row: what that page is set to, in words. Plain sentences only, so
 * the row reads like the rest of the app ("Đặt dấu kiểu mới, cho phép z, f, w và j đầu từ").
 *
 * Built from [Resources] rather than inside composition, so tests can check every sentence
 * without drawing the hub.
 */
internal class SettingsSummaries(private val resources: Resources) {
    fun of(page: SettingsPage, state: SettingsScreenState): String? = when (page) {
        SettingsPage.TYPING -> typing(state)
        SettingsPage.LAYOUT -> layout(state)
        SettingsPage.SMART_INPUT -> resources.countSentence(
            on = SmartInputToggle.entries.count { toggle -> toggle.isOn(state) },
            total = SmartInputToggle.entries.size,
        )
        SettingsPage.ENTER_KEY -> state.enterToSendApps?.let(::enterKey)
        SettingsPage.FEEDBACK -> resources.enabledSentence(
            listOfNotNull(
                state.activeHapticStrength?.let(::haptics),
                resources.getString(R.string.settings_summary_sounds).takeIf { state.soundsEnabled },
                resources.getString(R.string.settings_summary_key_previews).takeIf { state.keyPreviewsEnabled },
            ),
        )
        SettingsPage.CLIPBOARD -> clipboard(state)
        SettingsPage.SPEECH -> resources.getString(R.string.speech_settings_summary)
        SettingsPage.HARDWARE_KEYBOARD -> enabled(
            R.string.settings_summary_hardware_show to state.hardwareKeyboard.preferences.showsSoftKeyboard,
            R.string.settings_summary_hardware_hotkey to state.hardwareKeyboard.preferences.toggleHotkeyEnabled,
        )
        SettingsPage.DATA -> null
    }

    /** "Đang bật …" naming each item whose flag is on, or "Đang tắt tất cả". */
    private fun enabled(vararg items: Pair<Int, Boolean>): String =
        resources.enabledSentence(items.filter { (_, on) -> on }.map { (name, _) -> resources.getString(name) })

    /** Which apps Enter sends in, or that it always breaks the line. */
    private fun enterKey(apps: List<String>): String =
        if (apps.isEmpty()) resources.getString(R.string.settings_summary_enter_key_off)
        else resources.getString(R.string.settings_summary_enter_key_on, resources.joinAsSentence(apps))

    /** "rung nhẹ", "rung trung bình" or "rung mạnh". */
    private fun haptics(strength: KeyboardHapticStrength): String {
        val word = when (strength) {
            KeyboardHapticStrength.LIGHT -> R.string.settings_summary_strength_light
            KeyboardHapticStrength.MEDIUM -> R.string.settings_summary_strength_medium
            KeyboardHapticStrength.STRONG -> R.string.settings_summary_strength_strong
        }
        return resources.getString(R.string.settings_summary_haptics_strength, resources.getString(word))
    }

    private fun typing(state: SettingsScreenState): String {
        val tone = resources.getString(
            when (state.toneStyle) {
                ToneStyle.MODERN -> R.string.settings_summary_tone_modern
                ToneStyle.TRADITIONAL -> R.string.settings_summary_tone_traditional
            },
        )
        val onsets = state.smartComposition.extraOnsets
        if (!onsets.isEnabled) return tone
        val letters = ExtraOnsetLetter.Ordered.filter { letter -> letter in onsets }.map { it.spelling.toString() }
        val joined = resources.joinAsSentence(letters)
        return resources.getString(R.string.settings_summary_typing_extra_onsets, tone, joined)
    }

    private fun layout(state: SettingsScreenState): String {
        val mode = resources.getString(
            when (state.placement.activeMode) {
                KeyboardPlacementMode.STANDARD -> R.string.settings_summary_mode_standard
                KeyboardPlacementMode.ELEVATED -> R.string.settings_summary_mode_elevated
                KeyboardPlacementMode.ONE_HANDED -> R.string.settings_summary_mode_one_handed
            },
        )
        val percent = (state.keySizeProfile.heightScale * 100).roundToInt()
        return resources.getString(R.string.settings_summary_layout, mode, percent)
    }

    private fun clipboard(state: SettingsScreenState): String {
        if (!state.clipboardPreferences.enabled) return resources.getString(R.string.settings_summary_clipboard_off)
        val expiry = resources.getString(
            when (state.clipboardPreferences.expiry) {
                ClipboardExpiry.HOUR -> R.string.settings_clipboard_expiry_hour
                ClipboardExpiry.DAY -> R.string.settings_clipboard_expiry_day
                ClipboardExpiry.WEEK -> R.string.settings_clipboard_expiry_week
            },
        )
        return resources.getString(R.string.settings_summary_clipboard_on, expiry)
    }
}

/** [SettingsSummaries] for the current configuration's resources. */
@Composable
internal fun rememberSettingsSummaries(): SettingsSummaries {
    val resources = LocalResources.current
    return remember(resources) { SettingsSummaries(resources) }
}
