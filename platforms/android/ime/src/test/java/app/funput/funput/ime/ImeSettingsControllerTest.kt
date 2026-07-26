package app.funput.funput.ime

import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.ime.settings.InputMethodSettings
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.ToneStyleSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ImeSettingsControllerTest {
    @Test
    fun engineIsConfiguredBeforeAnySettingReportsAChange() {
        val engine = RecordingEngine()

        ImeSettingsController(
            engine = engine,
            onInputMethodChanged = {},
            onViewSettingsChanged = {},
            onPersonalSuggestionsChanged = {},
        )

        // On a fresh install every stored setting equals its default, so no flow reports a
        // change and nothing else would ever configure the engine. Without this the engine kept
        // its own default of Telex while the app showed VNI, and Vietnamese input did nothing
        // until the user changed the setting to something else and back.
        assertEquals(
            EngineConfiguration(
                inputMethod = InputMethodSettings.DefaultInputMethod,
                toneStyle = ToneStyleSettings.DefaultToneStyle,
                smartRestore = SmartCompositionPreferences.Default.smartRestoreEnabled,
                eagerRestore = SmartCompositionPreferences.Default.smartRestoreEnabled,
                spellCheck = SmartCompositionPreferences.Default.spellCheckEnabled,
            ),
            engine.configurations.single(),
        )
    }
}

private class RecordingEngine : VietnameseEngine {
    val configurations = mutableListOf<EngineConfiguration>()

    override fun configure(configuration: EngineConfiguration) {
        configurations += configuration
    }

    override fun setEnabled(enabled: Boolean) = Unit
    override fun adopt(word: String): Boolean = false
    override fun process(codePoint: Int): String = ""
    override fun processBoundary(codePoint: Int): String? = null
    override fun backspace(): String = ""
    override fun clear() = Unit
    override fun close() = Unit
}
