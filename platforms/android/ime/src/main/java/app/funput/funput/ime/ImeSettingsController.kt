package app.funput.funput.ime

import android.content.Context
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.ime.settings.InputMethodSettings
import app.funput.funput.ime.settings.KeyboardFeedbackPreferences
import app.funput.funput.ime.settings.KeyboardFeedbackSettings
import app.funput.funput.ime.settings.KeyboardSizingSettings
import app.funput.funput.ime.settings.KeyboardThemeSelection
import app.funput.funput.ime.settings.KeyboardThemeSettings
import app.funput.funput.ime.settings.NumberRowSettings
import app.funput.funput.ime.settings.PersonalSuggestionPreferences
import app.funput.funput.ime.settings.PersonalSuggestionSettings
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.SmartCompositionSettings
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.ime.settings.ToneStyleSettings
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import kotlinx.coroutines.CoroutineScope

/** Keeps persisted IME settings synchronized with native and view state. */
internal class ImeSettingsController(
    private val engine: VietnameseEngine,
    private val onInputMethodChanged: (KeyboardInputMethod) -> Unit,
    private val onViewSettingsChanged: () -> Unit,
    private val onPersonalSuggestionsChanged: (PersonalSuggestionPreferences) -> Unit,
) {
    var inputMethod = InputMethodSettings.DefaultInputMethod
        private set
    var sizingProfile = KeyboardSizingSettings.DefaultProfile
        private set
    var themeSelection = KeyboardThemeSettings.DefaultSelection
        private set
    var feedback = KeyboardFeedbackPreferences.Default
        private set
    var showsNumberRow = NumberRowSettings.DefaultShowsNumberRow
        private set

    // Seeded with the same defaults the settings flows fall back to, so the engine
    // configuration below is always complete — no option has to be invented when one
    // flow emits before the others.
    private var toneStyle = ToneStyleSettings.DefaultToneStyle
    private var smartComposition = SmartCompositionPreferences.Default

    init {
        // Push the defaults before any flow reports anything.
        //
        // Each apply* below ignores a value equal to the one already held, and on a fresh
        // install every stored setting *is* the default — so nothing would ever reach the
        // engine, which would keep composing with its own built-in defaults. Those are not the
        // same defaults: the engine starts on Telex while this app starts on VNI, so the
        // keyboard silently ignored VNI keystrokes until the user changed the setting to
        // something else and back. Configuring once up front keeps the two sides in agreement
        // no matter which defaults either of them picks later.
        applyEngineConfiguration()
    }

    fun observe(context: Context, scope: CoroutineScope) {
        InputMethodSettings(context).inputMethod.collectIn(scope, ::applyInputMethod)
        ToneStyleSettings(context).toneStyle.collectIn(scope, ::applyToneStyle)
        SmartCompositionSettings(context).preferences.collectIn(scope, ::applySmartComposition)
        KeyboardSizingSettings(context).profile.collectIn(scope, ::applySizingProfile)
        KeyboardThemeSettings(context).selection.collectIn(scope, ::applyThemeSelection)
        KeyboardFeedbackSettings(context).preferences.collectIn(scope, ::applyFeedback)
        NumberRowSettings(context).showsNumberRow.collectIn(scope, ::applyShowsNumberRow)
        PersonalSuggestionSettings(context).preferences.collectIn(scope, onPersonalSuggestionsChanged)
    }

    private fun applyInputMethod(value: KeyboardInputMethod) {
        if (value == inputMethod) return
        inputMethod = value
        // Configure first: the session restart below rebuilds composition state and
        // must see the engine already switched to the new grammar.
        applyEngineConfiguration()
        onInputMethodChanged(value)
    }

    private fun applyToneStyle(value: ToneStyle) {
        if (value == toneStyle) return
        toneStyle = value
        applyEngineConfiguration()
    }

    private fun applySmartComposition(value: SmartCompositionPreferences) {
        if (value == smartComposition) return
        smartComposition = value
        applyEngineConfiguration()
    }

    /**
     * The single writer of engine configuration: every settings flow above funnels
     * here, so the engine can never end up with a half-applied set of options.
     */
    private fun applyEngineConfiguration() {
        engine.configure(
            EngineConfiguration(
                inputMethod = inputMethod,
                toneStyle = toneStyle,
                // One "smart restore" switch drives both restore behaviors on Android.
                smartRestore = smartComposition.smartRestoreEnabled,
                eagerRestore = smartComposition.smartRestoreEnabled,
                spellCheck = smartComposition.spellCheckEnabled,
            )
        )
    }

    private fun applySizingProfile(value: KeyboardSizingProfile) {
        if (value == sizingProfile) return
        sizingProfile = value
        onViewSettingsChanged()
    }

    private fun applyThemeSelection(value: KeyboardThemeSelection) {
        if (value == themeSelection) return
        themeSelection = value
        onViewSettingsChanged()
    }

    private fun applyFeedback(value: KeyboardFeedbackPreferences) {
        if (value == feedback) return
        feedback = value
        onViewSettingsChanged()
    }

    private fun applyShowsNumberRow(value: Boolean) {
        if (value == showsNumberRow) return
        showsNumberRow = value
        onViewSettingsChanged()
    }
}
