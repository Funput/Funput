package app.funput.funput.ime.editing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Re-opening a committed word after Backspace, so the next keystroke retones it.
 * The document must be left untouched whenever the engine refuses the word.
 */
class ReopenPreviousWordTest {
    @Test
    fun `reopens the word before the caret`() {
        val editor = ReopenWordEditor(textBeforeCursor = "chào")
        val engine = ReopenWordEngine(adoptable = setOf("chào"))
        val session = testSession(engine)

        assertTrue(session.reopenPreviousWord(editor.proxy))

        assertEquals("chào", engine.adopted)
        assertEquals(listOf(4), editor.deletedBefore)
        assertEquals(listOf("chào"), editor.composingTexts)
        assertEquals("chào", session.composingText)
        assertEquals(1, editor.batchDepthPeak) // one atomic edit, not two
    }

    @Test
    fun `takes only the last word, not the text before it`() {
        val editor = ReopenWordEditor(textBeforeCursor = "xin chào")
        val engine = ReopenWordEngine(adoptable = setOf("chào"))

        assertTrue(testSession(engine).reopenPreviousWord(editor.proxy))
        assertEquals(listOf(4), editor.deletedBefore)
    }

    @Test
    fun `leaves the document alone when the engine refuses the word`() {
        val editor = ReopenWordEditor(textBeforeCursor = "hello")
        val engine = ReopenWordEngine(adoptable = emptySet())
        val session = testSession(engine)

        assertFalse(session.reopenPreviousWord(editor.proxy))

        assertTrue(editor.deletedBefore.isEmpty())
        assertTrue(editor.composingTexts.isEmpty())
        assertEquals("", session.composingText)
    }

    @Test
    fun `skips when the caret sits on a boundary`() {
        val editor = ReopenWordEditor(textBeforeCursor = "chào ")
        val engine = ReopenWordEngine(adoptable = setOf("chào"))

        assertFalse(testSession(engine).reopenPreviousWord(editor.proxy))
        assertNull(engine.adopted)
    }

    @Test
    fun `skips while a selection is active`() {
        val editor = ReopenWordEditor(textBeforeCursor = "chào", selectedText = "chào")
        val engine = ReopenWordEngine(adoptable = setOf("chào"))

        assertFalse(testSession(engine).reopenPreviousWord(editor.proxy))
        assertNull(engine.adopted)
    }

    @Test
    fun `skips while a composition is already live`() {
        val editor = ReopenWordEditor(textBeforeCursor = "chào")
        val engine = ReopenWordEngine(adoptable = setOf("chào"))
        val session = testSession(engine)
        session.input(editor.proxy, "a") // starts composing
        editor.composingTexts.clear()

        assertFalse(session.reopenPreviousWord(editor.proxy))
        assertNull(engine.adopted)
        assertTrue(editor.composingTexts.isEmpty())
    }

    @Test
    fun `skips when there is no connection`() {
        assertFalse(testSession(ReopenWordEngine(setOf("chào"))).reopenPreviousWord(null))
    }

    @Test
    fun `restores the word when setComposingText fails after deletion`() {
        val editor = ReopenWordEditor(textBeforeCursor = "chào", setComposingTextFails = true)
        val engine = ReopenWordEngine(adoptable = setOf("chào"))
        val session = testSession(engine)

        assertFalse(session.reopenPreviousWord(editor.proxy))

        assertEquals("chào", engine.adopted)
        assertEquals(listOf(4), editor.deletedBefore)
        assertEquals(listOf("chào"), editor.committedTexts)
        assertEquals("chào", editor.text)
        assertEquals("", engine.buffer)
        assertTrue(editor.composingTexts.isEmpty()) // setComposingText was never successful
        assertEquals("", session.composingText)
    }
}
