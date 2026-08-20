package app.funput.funput.ui

import android.content.Context
import app.funput.funput.ime.settings.AppearanceSettings
import app.funput.funput.ime.settings.ClipboardSettings
import app.funput.funput.ime.settings.DynamicColorSettings
import app.funput.funput.ime.settings.InputMethodSettings
import app.funput.funput.ime.settings.KeyboardFeedbackSettings
import app.funput.funput.ime.settings.KeyboardSizingSettings
import app.funput.funput.ime.settings.KeyboardThemeSettings
import app.funput.funput.ime.settings.NumberRowSettings
import app.funput.funput.ime.settings.PersonalSuggestionSettings
import app.funput.funput.ime.settings.SmartCompositionSettings
import app.funput.funput.ime.settings.ToneStyleSettings
import app.funput.funput.ime.settings.gestures.SmartGestureSettings

/** Remembers the DataStore wrappers used by [rememberFunputSettings]. */
internal class FunputSettingsStores(context: Context) {
    val input = InputMethodSettings(context)
    val sizing = KeyboardSizingSettings(context)
    val keyboardTheme = KeyboardThemeSettings(context)
    val toneStyleStore = ToneStyleSettings(context)
    val appearance = AppearanceSettings(context)
    val clipboard = ClipboardSettings(context)
    val dynamicColorStore = DynamicColorSettings(context)
    val feedbackStore = KeyboardFeedbackSettings(context)
    val numberRowStore = NumberRowSettings(context)
    val smartCompositionStore = SmartCompositionSettings(context)
    val smartGestureStore = SmartGestureSettings(context)
    val personalSuggestionStore = PersonalSuggestionSettings(context)
}
