package app.funput.funput.ime.nativebridge.configuration

import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetter
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetters
import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Protects the JNI contract and the complete snapshot consumed by the IME controller. */
class CompositionSettingsMappingTest {
    @Test fun `each letter has its documented JNI bit`() {
        listOf(ExtraOnsetLetter.F to 1, ExtraOnsetLetter.J to 2, ExtraOnsetLetter.W to 4, ExtraOnsetLetter.Z to 8)
            .forEach { (letter, bit) ->
                assertEquals(bit, ExtraOnsetLetters.None.withLetter(letter, true).nativeMask)
            }
        assertEquals(0, ExtraOnsetLetters.None.nativeMask)
        assertEquals(15, ExtraOnsetLetters.All.nativeMask)
        assertEquals(9, ExtraOnsetLetters.parse("zf").nativeMask)
    }

    @Test fun `all input methods and tone styles retain the selected onsets and restore options`() {
        val preferences = SmartCompositionPreferences(true, false, true, ExtraOnsetLetters.parse("fw"))
        listOf(KeyboardInputMethod.TELEX, KeyboardInputMethod.VNI, KeyboardInputMethod.TELEX_ADVANCED)
            .forEach { method ->
                listOf(ToneStyle.TRADITIONAL, ToneStyle.MODERN).forEach { tone ->
                    val configuration = preferences.engineConfiguration(method, tone)
                    assertEquals(method, configuration.inputMethod)
                    assertEquals(tone, configuration.toneStyle)
                    assertEquals(preferences.extraOnsets, configuration.extraOnsets)
                    assertFalse(configuration.smartRestore)
                    assertFalse(configuration.eagerRestore)
                    assertEquals(true, configuration.spellCheck)
                    assertFalse(configuration.autoCapitalize)
                }
            }
        val defaults = SmartCompositionPreferences.Default.engineConfiguration(KeyboardInputMethod.VNI, ToneStyle.MODERN)
        assertEquals(ExtraOnsetLetters.None, defaults.extraOnsets)
        assertEquals(true, defaults.smartRestore)
        assertEquals(true, defaults.eagerRestore)
    }
}
