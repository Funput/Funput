package app.funput.funput.ime.editing.backspace

import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.ime.editing.testSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Retone-after-Backspace through [ImeKeyActionHandler].
 *
 * Real-device failure: DeleteBackward and reopenPreviousWord were two top-level
 * edits. EditText posted onUpdateSelection for the delete with candidatesEnd=-1
 * *after* reopen had set isComposing, and [ImeKeyActionHandler.onSelectionChanged]
 * finished the restored composition. Unit tests never called onSelectionChanged,
 * so they stayed green.
 */
class RetoneAfterBackspaceTest {
    @Test
    fun `backspace delete and reopen share one outer batch edit`() {
        val editor = MutableEditor(text = "chào ")
        val engine = RetoneAdoptingEngine(adoptable = setOf("chào"))
        val session = testSession(engine)
        val handler = retoneHandler(session, editor)

        handler.start(allowComposition = true)
        handler.onKeyAction(KeyAction.Backspace)

        assertEquals("chào", session.composingText)
        assertEquals("chào", editor.text)
        assertTrue(
            "DeleteBackward and reopen must nest under one outer beginBatchEdit " +
                "so EditText posts a single onUpdateSelection with the composing " +
                "region already set (candidatesEnd == caret).",
            editor.deleteOccurredInsideOuterBatch,
        )
        assertEquals(0, editor.batchDepth)
    }

    @Test
    fun `selection update matching the composing end keeps the reopened word`() {
        val editor = MutableEditor(text = "chào ")
        val session = testSession(RetoneAdoptingEngine(adoptable = setOf("chào")))
        val handler = retoneHandler(session, editor)

        handler.start(allowComposition = true)
        handler.onKeyAction(KeyAction.Backspace)
        // Final state after one batched edit: caret and composing end coincide.
        handler.onSelectionChanged(newStart = 4, newEnd = 4, composingEnd = 4)

        assertTrue(session.isComposing)
        assertEquals("chào", session.composingText)
    }

    @Test
    fun `stale selection update with cleared composing region finishes the word`() {
        val editor = MutableEditor(text = "chào ")
        val session = testSession(RetoneAdoptingEngine(adoptable = setOf("chào")))
        val handler = retoneHandler(session, editor)

        handler.start(allowComposition = true)
        handler.onKeyAction(KeyAction.Backspace)
        // What arrives when DeleteBackward is its own top-level edit: the delete's
        // onUpdateSelection is delivered after reopen has set isComposing.
        handler.onSelectionChanged(newStart = 4, newEnd = 4, composingEnd = -1)

        assertFalse(session.isComposing)
        assertEquals(1, editor.finishCount)
    }
}
