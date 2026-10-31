package app.funput.funput.ime.suggestions

import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.RecordingVietnameseEngine
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class SeedLanguageReportingEngineTest {
    private val inner = RecordingVietnameseEngine()
    private val reports = mutableListOf<SeedLanguage>()
    private val engine = SeedLanguageReportingEngine(inner) { reports += it }

    @Test fun everyCallStillReachesTheComposer() {
        engine.configure(configuration(ToneStyle.MODERN))
        engine.setEnabled(false)
        assertEquals(listOf(ToneStyle.MODERN), inner.configurations.map { it.toneStyle })
        assertEquals(listOf(false), inner.enabledCalls)
    }

    @Test fun nothingIsReportedUntilBothHalvesAreKnown() {
        engine.configure(configuration(ToneStyle.MODERN))
        assertEquals(emptyList<SeedLanguage>(), reports)
        engine.setEnabled(true)
        assertEquals(listOf<SeedLanguage>(SeedLanguage.Vietnamese(ToneStyle.MODERN)), reports)
    }

    @Test fun theLanguageKeyAndFieldsThatDoNotComposeTurnItOff() {
        engine.setEnabled(true)
        engine.configure(configuration(ToneStyle.TRADITIONAL))
        engine.setEnabled(false)
        engine.setEnabled(true)
        assertEquals(
            listOf(
                SeedLanguage.Vietnamese(ToneStyle.TRADITIONAL),
                SeedLanguage.Off,
                SeedLanguage.Vietnamese(ToneStyle.TRADITIONAL),
            ),
            reports,
        )
    }

    @Test fun aToneStyleChangeIsReportedAndARepeatIsNot() {
        engine.setEnabled(true)
        engine.configure(configuration(ToneStyle.MODERN))
        engine.configure(configuration(ToneStyle.MODERN))
        engine.setEnabled(true)
        engine.configure(configuration(ToneStyle.TRADITIONAL))
        assertEquals(
            listOf<SeedLanguage>(
                SeedLanguage.Vietnamese(ToneStyle.MODERN),
                SeedLanguage.Vietnamese(ToneStyle.TRADITIONAL),
            ),
            reports,
        )
    }

    @Test fun offIgnoresTheToneStyle() {
        engine.setEnabled(false)
        engine.configure(configuration(ToneStyle.MODERN))
        engine.configure(configuration(ToneStyle.TRADITIONAL))
        assertEquals(listOf<SeedLanguage>(SeedLanguage.Off), reports)
    }

    @Test fun theWireCodesMatchTheNativeContract() {
        assertEquals(0, SeedLanguage.Off.wireCode)
        assertEquals(1, SeedLanguage.Vietnamese(ToneStyle.MODERN).wireCode)
        assertEquals(1, SeedLanguage.Vietnamese(ToneStyle.MODERN).toneStyleValue)
        assertEquals(0, SeedLanguage.Vietnamese(ToneStyle.TRADITIONAL).toneStyleValue)
    }

    private fun configuration(toneStyle: ToneStyle) = EngineConfiguration(
        inputMethod = KeyboardInputMethod.TELEX,
        toneStyle = toneStyle,
        smartRestore = true,
        eagerRestore = true,
        spellCheck = true,
    )
}
