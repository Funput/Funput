package app.funput.funput.ime.editing.composition

import app.funput.funput.ime.editing.CommittedEditor
import app.funput.funput.ime.editing.CompositionRenderMode
import app.funput.funput.ime.editing.ScriptedEngine
import app.funput.funput.ime.editing.testSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rewriting a word already committed before the caret, as undo and a bar repair do. */
class ReplaceCommittedTest {
    private fun replaces(mode: CompositionRenderMode) {
        val editor = CommittedEditor(text = "đi đường ")
        val session = testSession(ScriptedEngine(ArrayDeque())).apply { setRenderMode(mode) }

        assertTrue(session.replaceCommitted(editor.proxy, "đường ", "đườnh "))

        assertEquals("đi đườnh ", editor.text)
        assertEquals(0, editor.setComposingCount)
    }

    @Test
    fun composingHostsDeleteThenCommitInOneBatch() = replaces(CompositionRenderMode.COMPOSING)

    @Test
    fun committedHostsReplaceThroughTheBufferWriter() = replaces(CompositionRenderMode.COMMITTED)

    @Test
    fun aFailedWriteReportsItself() {
        val editor = CommittedEditor(text = "đường ").apply { remainingCommitFailures = 1 }
        val session = testSession(ScriptedEngine(ArrayDeque()))

        assertEquals(false, session.replaceCommitted(editor.proxy, "đường ", "đườnh "))
    }
}
