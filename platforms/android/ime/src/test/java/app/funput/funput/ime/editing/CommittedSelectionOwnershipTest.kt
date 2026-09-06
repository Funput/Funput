package app.funput.funput.ime.editing

import app.funput.funput.keyboard.model.KeyAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Who owns the caret in committed mode, where there is no composing region to compare
 * against and the answer has to come from the position and the surrounding text.
 */
class CommittedSelectionOwnershipTest {
    @Test
    fun `selection update from committed replacement keeps engine buffer`() {
        val editor = CommittedEditor()
        val session = testSession(ScriptedEngine(ArrayDeque(listOf("chào"))))
        val handler = committedHandler(session, editor)

        handler.onKeyAction(KeyAction.Input(keyId = "character-a", text = "a"))
        handler.onSelectionChanged(newStart = 4, newEnd = 4, composingEnd = -1)

        assertTrue(session.isComposing)
        assertEquals("chào", editor.text)
    }

    @Test
    fun `caret reported from inside our batch edit keeps the buffer`() {
        val editor = CommittedEditor()
        val session = testSession(ScriptedEngine(ArrayDeque(listOf("u", "uo", "uơ", "ươn"))))
        val handler = committedHandler(session, editor)

        handler.onKeyAction(KeyAction.Input(keyId = "character-u", text = "u"))
        handler.onSelectionChanged(newStart = 1, newEnd = 1, composingEnd = -1)
        handler.onKeyAction(KeyAction.Input(keyId = "character-o", text = "o"))
        handler.onSelectionChanged(newStart = 2, newEnd = 2, composingEnd = -1)
        // Replacing "uo" with "uơ" deletes before it commits; a host that ignores the
        // batch reports the empty document in between before reporting the result.
        handler.onKeyAction(KeyAction.Input(keyId = "character-w", text = "w"))
        editor.textBeforeCursor = ""
        handler.onSelectionChanged(newStart = 0, newEnd = 0, composingEnd = -1)
        editor.textBeforeCursor = null
        handler.onSelectionChanged(newStart = 2, newEnd = 2, composingEnd = -1)
        handler.onKeyAction(KeyAction.Input(keyId = "character-n", text = "n"))

        assertEquals("ươn", editor.text)
        assertEquals("ươn", session.composingText)
    }

    @Test
    fun `caret move after the batch settles releases the buffer`() {
        val editor = CommittedEditor()
        val session = testSession(ScriptedEngine(ArrayDeque(listOf("u", "uo", "uơ"))))
        val handler = committedHandler(session, editor)

        handler.onKeyAction(KeyAction.Input(keyId = "character-u", text = "u"))
        handler.onSelectionChanged(newStart = 1, newEnd = 1, composingEnd = -1)
        handler.onKeyAction(KeyAction.Input(keyId = "character-o", text = "o"))
        handler.onSelectionChanged(newStart = 2, newEnd = 2, composingEnd = -1)
        // The replacement settles first, which is what retires the caret we expect from
        // inside the batch. The tap that follows is the user's, and has to be obeyed.
        handler.onKeyAction(KeyAction.Input(keyId = "character-w", text = "w"))
        handler.onSelectionChanged(newStart = 2, newEnd = 2, composingEnd = -1)
        editor.textBeforeCursor = ""
        handler.onSelectionChanged(newStart = 0, newEnd = 0, composingEnd = -1)

        assertEquals("", session.composingText)
    }
}

private fun committedHandler(
    session: AndroidCompositionSession,
    editor: CommittedEditor,
): ImeKeyActionHandler = ImeKeyActionHandler(
    composition = session,
    editor = InputConnectionEditor(),
    connection = { editor.proxy },
    enterCommand = { ImeEditCommand.CommitText("\n") },
).apply { start(renderMode = CompositionRenderMode.COMMITTED) }
