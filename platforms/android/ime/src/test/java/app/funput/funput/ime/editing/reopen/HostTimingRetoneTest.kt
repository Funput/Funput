package app.funput.funput.ime.editing.reopen

import app.funput.funput.ime.editing.CompositionRenderMode
import app.funput.funput.ime.editing.backspace.CaretDocument
import app.funput.funput.ime.editing.backspace.CaretTypist
import app.funput.funput.keyboard.model.KeyAction
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Retone-after-Backspace on hosts whose answers lag behind our writes. Guarding against a moved
 * caret must not cost the feature here: the text and the caret are still where we left them.
 */
class HostTimingRetoneTest {
    @Test
    fun `a host that reads stale text inside the batch still reopens the word`() {
        val typist = CaretTypist(CaretDocument(staleInsideBatch = true), setOf("chao"), CompositionRenderMode.COMPOSING)
        typist.type("chao ")

        typist.press(KeyAction.Backspace)

        assertEquals("chao", typist.document.text)
        assertEquals("chao", typist.session.composingText)
    }

    @Test
    fun `echoes that arrive after the word was committed do not drop it`() {
        val typist = CaretTypist(CaretDocument(exposesText = false), setOf("ba"), CompositionRenderMode.COMPOSING)
        typist.type("b")
        typist.press(KeyAction.Input(keyId = "character-a", text = "a"))
        val lateEcho = typist.document.report()
        typist.type(" ", echo = false)

        lateEcho.deliverTo(typist.handler)
        typist.document.echoTo(typist.handler)
        typist.press(KeyAction.Backspace)

        assertEquals("ba", typist.document.text)
        assertEquals("ba", typist.session.composingText)
    }
}
