package app.funput.funput.ime.editing.correction.undo

import app.funput.funput.ime.editing.CommittedEditor
import app.funput.funput.ime.editing.CompositionRenderMode
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.backspace.ImeBackspaceHandler
import app.funput.funput.ime.editing.correction.CorrectionFixture
import app.funput.funput.ime.editing.correction.FakeDictionary
import app.funput.funput.ime.nativebridge.correction.CorrectionEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CorrectionUndoTest {
    private val dictionary = FakeDictionary()

    private fun corrected(mode: CompositionRenderMode = CompositionRenderMode.COMMITTED, editor: CommittedEditor) =
        CorrectionFixture(listOf("đườnh"), mode = mode, dictionary = dictionary).apply {
            correction.chosen = 0
            session.input(editor.proxy, "h")
            correction.parkAt("đườnh", "đường")
            correction.undoEdit = CorrectionEdit(6, "đườnh ")
            session.input(editor.proxy, " ")
        }

    private fun backspace(fixture: CorrectionFixture, editor: CommittedEditor) =
        ImeBackspaceHandler(fixture.session, InputConnectionEditor(), { editor.proxy }, { true }, {}, {}).perform()

    private fun undo(mode: CompositionRenderMode) {
        val editor = CommittedEditor()
        val fixture = corrected(mode, editor)
        assertEquals("đường ", editor.text)
        backspace(fixture, editor)
        assertEquals("the boundary comes back too, and nothing else is deleted", "đườnh ", editor.text)
    }

    @Test fun undoComposing() = undo(CompositionRenderMode.COMPOSING)
    @Test fun undoCommitted() = undo(CompositionRenderMode.COMMITTED)

    @Test
    fun keepsTheTypedWord() {
        val editor = CommittedEditor()
        backspace(corrected(editor = editor), editor)
        assertEquals(listOf("đườnh"), dictionary.kept)
    }

    @Test
    fun doesNotReopen() {
        val editor = CommittedEditor()
        val fixture = corrected(editor = editor)
        backspace(fixture, editor)
        assertEquals("", fixture.session.composingText)
    }

    @Test
    fun disarmed() {
        // Another key spent the undo: Backspace deletes a character as always.
        val editor = CommittedEditor()
        val fixture = corrected(editor = editor)
        fixture.correction.undoEdit = null
        backspace(fixture, editor)
        assertEquals("đường", editor.text)
    }

    @Test
    fun documentMovedFallsBack() {
        val editor = CommittedEditor()
        val fixture = corrected(editor = editor)
        editor.textBeforeCursor = "khác "
        backspace(fixture, editor)
        assertEquals("the engine is not asked when the repair is not before the caret",
            false, fixture.correction.calls.contains("undo"))
        assertNull(fixture.state.applied)
    }

    @Test
    fun backspaceDropsAParkedRepair() {
        val editor = CommittedEditor()
        val fixture = CorrectionFixture(listOf("đườnh")).apply {
            correction.ambiguous()
            session.input(editor.proxy, "h")
            correction.parkAt("đườnh", "đường", "dường")
            session.input(editor.proxy, " ")
        }
        backspace(fixture, editor)
        assertNull(fixture.state.parked)
        assertEquals("apply(-1)", fixture.correction.calls.last())
    }
}
