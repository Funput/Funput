package app.funput.funput.ime.editing.backspace

import app.funput.funput.ime.editing.CompositionRenderMode
import app.funput.funput.keyboard.model.KeyAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Issue #638: after the user selected a word, or tapped the caret elsewhere, Backspace reopened
 * the last word typed instead: it deleted that many characters before the new caret and wrote the
 * word there (`Dữ liệu thị [trường] bình thường ` became `Dữ liệthường bình thường `).
 */
class MovedCaretBackspaceTest {
    private val words = setOf("du", "lieu", "thi", "truong", "binh", "thuong", "mo", "mot", "hai", "ba")

    @Test
    fun `backspace on a selection deletes only the selection`() {
        for (mode in RenderModes) {
            for (hostReportsSelection in listOf(false, true)) {
                val typist = typist(mode).apply { type("du lieu thi truong binh thuong ") }
                typist.document.select(12, 18)
                if (hostReportsSelection) typist.document.echoTo(typist.handler)

                typist.press(KeyAction.Backspace)

                assertEquals("$mode / $hostReportsSelection", "du lieu thi  binh thuong ", typist.document.text)
            }
        }
    }

    @Test
    fun `deleting a selection resets the word trackers it bypasses`() {
        val typist = typist(CompositionRenderMode.COMPOSING, allowComposition = false)
        typist.type("hello world")
        typist.document.select(6, 11)

        typist.press(KeyAction.Backspace)

        assertEquals("hello ", typist.document.text)
        assertEquals("", typist.handler.takeSuggestionUpdate().prefix)
    }

    @Test
    fun `backspace after the caret moved does not reopen the last word typed`() {
        for (mode in RenderModes) {
            val typist = typist(mode, exposesText = false).apply { type("mot hai ba ") }
            typist.document.tap(3)
            typist.document.echoTo(typist.handler)

            typist.press(KeyAction.Backspace)

            assertEquals("$mode", "mo hai ba ", typist.document.text)
            assertFalse("$mode", typist.session.isComposing)
        }
    }

    @Test
    fun `text the editor shows overrides the remembered word`() {
        val typist = typist(CompositionRenderMode.COMPOSING).apply { type("mot hai ba ", echo = false) }
        typist.document.tap(3)

        typist.press(KeyAction.Backspace)

        assertEquals("mo hai ba ", typist.document.text)
        assertEquals("mo", typist.session.composingText)
    }

    @Test
    fun `backspace right after typing still reopens the word in a host that withholds text`() {
        for (mode in RenderModes) {
            val typist = typist(mode, exposesText = false).apply { type("mot hai ba ") }

            typist.press(KeyAction.Backspace)
            typist.document.echoTo(typist.handler)

            assertEquals("$mode", "mot hai ba", typist.document.text)
            assertEquals("$mode", "ba", typist.session.composingText)
        }
    }

    private fun typist(mode: CompositionRenderMode, exposesText: Boolean = true, allowComposition: Boolean = true) =
        CaretTypist(CaretDocument(exposesText), words, mode, allowComposition)

    private companion object {
        val RenderModes = listOf(CompositionRenderMode.COMPOSING, CompositionRenderMode.COMMITTED)
    }
}
