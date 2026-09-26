package app.funput.funput.ime.localtext

import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.ui.localtext.LocalTextEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalTextComposerTest {
    private val engine = LocalTextTestEngine()
    private var settings = LocalTextSettings(engineConfiguration(KeyboardInputMethod.TELEX), true)
    private val field = LocalTextComposer(engine) { settings }.apply { reset() }

    @Test fun `composes in the buffer and commits it on a space`() {
        type("as ddo")
        assertEquals("á đo", field.text)
        assertEquals(KeyboardInputMethod.TELEX, field.inputMethod)
    }

    @Test fun `a boundary replacement stands in for the word`() {
        type("te")
        engine.boundaryReplacement = "tex "
        field.apply(LocalTextEdit.Space)
        assertEquals("tex ", field.text)
    }

    @Test fun `a key the engine will not compose keeps the word and the key`() {
        type("aq")
        assertEquals("aq", field.text)
        type("es")
        assertEquals("aqé", field.text)
    }

    @Test fun `backspace steps back inside the composition`() {
        type("as")
        field.apply(LocalTextEdit.DeleteBackward)
        assertEquals("", field.text)
        type("es")
        assertEquals("é", field.text)
    }

    @Test fun `backspace over the space reopens the word for a new tone`() {
        engine.adoptable = setOf("cha")
        type("cha ")
        field.apply(LocalTextEdit.DeleteBackward)
        type("s")
        assertEquals("chá", field.text)
    }

    @Test fun `English keeps keys literal and leaves the engine alone`() {
        settings = settings.copy(composesVietnamese = false)
        field.reset()
        type("as dd")
        field.apply(LocalTextEdit.DeleteBackward)
        assertEquals("as d", field.text)
        assertEquals(0, engine.keyCalls)
        assertEquals(false, engine.enabled)
        assertNull(field.inputMethod)
    }

    @Test fun `refused spaces only end the word`() {
        val clears = engine.clears
        type("  a  ")
        assertEquals("a ", field.text)
        // Two leading spaces and the one after "a " are refused; the first after "a" commits it.
        assertEquals(clears + 3, engine.clears)
    }

    @Test fun `reset picks up the document's current settings`() {
        type("as")
        settings = settings.copy(configuration = engineConfiguration(KeyboardInputMethod.VNI))
        field.reset()
        assertEquals("", field.text)
        assertEquals(KeyboardInputMethod.VNI, engine.configuration?.inputMethod)
        assertEquals(KeyboardInputMethod.VNI, field.inputMethod)
        assertEquals(true, engine.enabled)
    }

    @Test fun `a closed field is inert and closes its engine once`() {
        field.close()
        field.close()
        type("as")
        field.reset()
        assertEquals("", field.text)
        assertEquals(0, engine.keyCalls)
        assertEquals(1, engine.closes)
    }

    private fun type(keys: String) = keys.forEach { key ->
        field.apply(if (key == ' ') LocalTextEdit.Space else LocalTextEdit.Text(key.toString()))
    }
}
