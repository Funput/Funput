package app.funput.funput.ime.editing.backspace

import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.CompositionRenderMode
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.testSession
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
            val (document, handler) = typed("du lieu thi truong binh thuong ", mode)
            document.select(12, 18)

            handler.onKeyAction(KeyAction.Backspace)

            assertEquals("$mode", "du lieu thi  binh thuong ", document.text)
        }
    }

    @Test
    fun `backspace after the caret moved does not reopen the last word typed`() {
        for (mode in RenderModes) {
            val (document, handler, session) = typed("mot hai ba ", mode, exposesText = false)
            document.tap(3)
            document.echoTo(handler)

            handler.onKeyAction(KeyAction.Backspace)

            assertEquals("$mode", "mo hai ba ", document.text)
            assertFalse("$mode", session.isComposing)
        }
    }

    @Test
    fun `text the editor shows overrides the remembered word`() {
        val (document, handler, session) = typed("mot hai ba ", CompositionRenderMode.COMPOSING, echo = false)
        document.tap(3)

        handler.onKeyAction(KeyAction.Backspace)

        assertEquals("mo hai ba ", document.text)
        assertEquals("mo", session.composingText)
    }

    @Test
    fun `backspace right after typing still reopens the word in a host that withholds text`() {
        for (mode in RenderModes) {
            val (document, handler, session) = typed("mot hai ba ", mode, exposesText = false)

            handler.onKeyAction(KeyAction.Backspace)
            document.echoTo(handler)

            assertEquals("$mode", "mot hai ba", document.text)
            assertEquals("$mode", "ba", session.composingText)
        }
    }

    private fun typed(
        text: String,
        mode: CompositionRenderMode,
        exposesText: Boolean = true,
        echo: Boolean = true,
    ): Typed {
        val document = CaretDocument(exposesText)
        val session = testSession(RetoneAdoptingEngine(words))
        val handler = ImeKeyActionHandler(
            composition = session,
            editor = InputConnectionEditor(),
            connection = { document.proxy },
            enterCommand = { ImeEditCommand.CommitText("\n") },
        )
        handler.start(renderMode = mode)
        text.forEach { char ->
            handler.onKeyAction(
                if (char == ' ') KeyAction.Space else KeyAction.Input(keyId = "character-$char", text = "$char"),
            )
            if (echo) document.echoTo(handler)
        }
        return Typed(document, handler, session)
    }

    private data class Typed(
        val document: CaretDocument,
        val handler: ImeKeyActionHandler,
        val session: AndroidCompositionSession,
    )

    private companion object {
        val RenderModes = listOf(CompositionRenderMode.COMPOSING, CompositionRenderMode.COMMITTED)
    }
}
