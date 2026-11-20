package app.funput.funput.ime.editing.backspace.rawkey

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.backspace.RetoneAdoptingEngine
import app.funput.funput.ime.editing.testSession
import app.funput.funput.keyboard.model.KeyAction
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #755: Backspace did nothing in Termux. A terminal declares no input class and keeps no
 * text, so the code-point delete Funput sent landed on an empty buffer and reported success.
 */
class RawKeyEditorBackspaceTest {
    private val terminal = TerminalEditor()
    private val session = testSession(RetoneAdoptingEngine(adoptable = setOf("xin")))

    @Test
    fun `backspace on committed text presses the delete key`() {
        val handler = handler(rawKeyEditor = true).apply { start(allowComposition = false) }

        handler.type("ls")
        handler.onKeyAction(KeyAction.Backspace)

        assertEquals("l", terminal.screen)
        assertEquals(1, terminal.keyDeletes)
    }

    @Test
    fun `backspace after a committed word deletes one character and reopens nothing`() {
        val handler = handler(rawKeyEditor = true).apply { start(allowComposition = true) }

        handler.type("xin ")
        handler.onKeyAction(KeyAction.Backspace)

        assertEquals("xin", terminal.screen)
        assertEquals(1, terminal.keyDeletes)
        assertEquals(0, terminal.textDeletes)
        assertFalse(session.isComposing)
    }

    @Test
    fun `backspace inside a live composition still edits the composition`() {
        val handler = handler(rawKeyEditor = true).apply { start(allowComposition = true) }

        handler.type("xi")
        handler.onKeyAction(KeyAction.Backspace)

        assertEquals("x", session.composingText)
        assertEquals(0, terminal.keyDeletes)
    }

    @Test
    fun `an editor with an input class keeps the text edit and the reopened word`() {
        val handler = handler(rawKeyEditor = false).apply { start(allowComposition = true) }

        handler.type("xin ")
        handler.onKeyAction(KeyAction.Backspace)

        assertEquals(0, terminal.keyDeletes)
        assertTrue(session.isComposing)
        assertEquals("xin", session.composingText)
    }

    private fun handler(rawKeyEditor: Boolean) = ImeKeyActionHandler(
        composition = session,
        editor = InputConnectionEditor(),
        connection = { terminal.proxy },
        enterCommand = { ImeEditCommand.CommitText("\n") },
        rawKeyEditor = { rawKeyEditor },
    )

    private fun ImeKeyActionHandler.type(text: String) = text.forEach { char ->
        onKeyAction(if (char == ' ') KeyAction.Space else KeyAction.Input("key-$char", "$char"))
    }
}

/**
 * Termux's connection: text reaches the screen only once committed, nothing can be read back,
 * and a delete happens on a key press. The code-point delete is not overridden there, so it
 * succeeds without deleting.
 */
private class TerminalEditor {
    var screen = ""
        private set
    var keyDeletes = 0
        private set
    var textDeletes = 0
        private set
    private var composing = ""
    private var keyEvents = 0

    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "beginBatchEdit", "endBatchEdit", "deleteSurroundingTextInCodePoints" -> true
            "setComposingText" -> true.also { composing = arguments?.first().toString() }
            "finishComposingText" -> true.also { flush(composing) }
            "commitText" -> true.also { flush(arguments?.first().toString()) }
            "deleteSurroundingText" -> true.also {
                textDeletes += 1
                screen = screen.dropLast(arguments?.first() as Int)
            }
            // Android stubs block KeyEvent getters; DOWN and UP together are one delete.
            "sendKeyEvent" -> true.also {
                keyEvents += 1
                if (keyEvents % 2 == 0) {
                    keyDeletes += 1
                    screen = screen.dropLast(1)
                }
            }
            "getTextBeforeCursor" -> ""
            "getSelectedText" -> null
            "toString" -> "TerminalEditor"
            else -> false
        }
    } as InputConnection

    private fun flush(text: String) {
        screen += text
        composing = ""
    }
}
