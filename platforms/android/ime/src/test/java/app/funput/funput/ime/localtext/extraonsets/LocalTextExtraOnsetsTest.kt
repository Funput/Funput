package app.funput.funput.ime.localtext.extraonsets

import app.funput.funput.ime.localtext.LocalTextComposer
import app.funput.funput.ime.localtext.LocalTextSettings
import app.funput.funput.ime.localtext.LocalTextTestEngine
import app.funput.funput.ime.localtext.engineConfiguration
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetters
import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Test

/** Search fields copy current document options when they reopen, including a cleared selection. */
class LocalTextExtraOnsetsTest {
    @Test fun `reset copies each new selection and input method`() {
        val engine = LocalTextTestEngine()
        var configuration = engineConfiguration(KeyboardInputMethod.TELEX)
        LocalTextComposer(engine) { LocalTextSettings(configuration, true) }.use { field ->
            listOf("z", "f", "w", "j", "zfwj", "").forEach { letters ->
                configuration = configuration.copy(extraOnsets = ExtraOnsetLetters.parse(letters))
                field.reset()
                assertEquals(configuration, engine.configuration)
            }
            configuration = configuration.copy(inputMethod = KeyboardInputMethod.VNI)
            field.reset()
            assertEquals(configuration, engine.configuration)
            assertEquals(KeyboardInputMethod.VNI, field.inputMethod)
        }
    }
}
