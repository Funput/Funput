package app.funput.funput.ime

import android.content.Context
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.ime.settings.InputMethodSettings
import app.funput.funput.ime.settings.KeyboardFeedbackPreferences
import app.funput.funput.ime.settings.KeyboardFeedbackSettings
import app.funput.funput.ime.settings.KeyboardSizingSettings
import app.funput.funput.ime.settings.KeyboardThemeSettings
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.SmartCompositionSettings
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.ime.settings.ToneStyleSettings
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.theme.KeyboardThemeId
import kotlinx.coroutines.CoroutineScope

/** Keeps persisted IME settings synchronized with native and view state. */
internal class ImeSettingsController(
    private val engine: VietnameseEngine,
    private val onInputMethodChanged: (KeyboardInputMethod) -> Unit,
    private val onViewSettingsChanged: () -> Unit,
) {
    var inputMethod = InputMethodSettings.DefaultInputMethod
        private set
    var sizingProfile = KeyboardSizingSettings.DefaultProfile
        private set
    var keyboardThemeId = KeyboardThemeSettings.DefaultThemeId
        private set
    var feedback = KeyboardFeedbackPreferences.Default
        private set

    private var toneStyle: ToneStyle? = null
    private var smartComposition: SmartCompositionPreferences? = null

    fun observe(context: Context, scope: CoroutineScope) {
        InputMethodSettings(context).inputMethod.collectIn(scope, ::applyInputMethod)
        ToneStyleSettings(context).toneStyle.collectIn(scope, ::applyToneStyle)
        SmartCompositionSettings(context).preferences.collectIn(scope, ::applySmartComposition)
        KeyboardSizingSettings(context).profile.collectIn(scope, ::applySizingProfile)
        KeyboardThemeSettings(context).themeId.collectIn(scope, ::applyKeyboardTheme)
        KeyboardFeedbackSettings(context).preferences.collectIn(scope, ::applyFeedback)
    }

    private fun applyInputMethod(value: KeyboardInputMethod) {
        if (value == inputMethod) return
        inputMethod = value
        onInputMethodChanged(value)
    }

    private fun applyToneStyle(value: ToneStyle) {
        if (value == toneStyle) return
        toneStyle = value
        engine.setToneStyle(value)
    }

    private fun applySmartComposition(value: SmartCompositionPreferences) {
        if (value == smartComposition) return
        smartComposition = value
        engine.setSpellCheck(value.spellCheckEnabled)
        engine.setSmartRestore(value.smartRestoreEnabled)
        engine.setEagerRestore(value.smartRestoreEnabled)
    }

    private fun applySizingProfile(value: KeyboardSizingProfile) {
        if (value == sizingProfile) return
        sizingProfile = value
        onViewSettingsChanged()
    }

    private fun applyKeyboardTheme(value: KeyboardThemeId) {
        if (value == keyboardThemeId) return
        keyboardThemeId = value
        onViewSettingsChanged()
    }

    private fun applyFeedback(value: KeyboardFeedbackPreferences) {
        if (value == feedback) return
        feedback = value
        onViewSettingsChanged()
    }
}
