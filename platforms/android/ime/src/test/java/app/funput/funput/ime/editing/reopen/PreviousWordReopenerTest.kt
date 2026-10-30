package app.funput.funput.ime.editing.reopen

import app.funput.funput.ime.editing.CommittedEditor
import app.funput.funput.ime.editing.ReopenWordEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviousWordReopenerTest {
    private val adoptable = setOf("chào", "dung", "ba", "mo")

    @Test
    fun `a host that withholds text reopens the remembered word`() {
        val reopener = reopener().apply { recorded("chào", "chào ") }

        assertEquals("chào", reopener.wordToReopen(CommittedEditor(exposesSurroundingText = false).proxy))
    }

    @Test
    fun `text the editor shows wins over a remembered word it contradicts`() {
        val reopener = reopener().apply { recorded("ba", "ba ") }

        assertEquals("mo", reopener.wordToReopen(ReopenWordEditor(textBeforeCursor = "mo").proxy))
        assertNull("the contradicted memory is gone", reopener.wordToReopen(withheld()))
    }

    @Test
    fun `a contradicted memory is dropped even when nothing can be reopened`() {
        val reopener = reopener().apply { recorded("ba", "ba ") }

        assertNull(reopener.wordToReopen(ReopenWordEditor(textBeforeCursor = "hai ").proxy))
        assertNull(reopener.wordToReopen(withheld()))
    }

    @Test
    fun `a refused word stays remembered until a later delete makes it adoptable`() {
        val reopener = reopener().apply { recorded("dungh", "dungh ") }

        assertNull(reopener.wordToReopen(withheld()))
        assertEquals("dung", reopener.wordToReopen(withheld()))
    }

    @Test
    fun `moving the caret away forgets the remembered word`() {
        val reopener = reopener()
        reopener.caretReported(start = 2, end = 2, owned = true)
        reopener.recorded("ba", "ba ")
        reopener.caretReported(start = 3, end = 3, owned = false)

        reopener.caretReported(start = 0, end = 0, owned = false)

        assertNull(reopener.wordToReopen(withheld()))
    }

    @Test
    fun `selecting text forgets the remembered word`() {
        val reopener = reopener().apply { recorded("ba", "ba ") }

        reopener.caretReported(start = 0, end = 2, owned = false)

        assertNull(reopener.wordToReopen(withheld()))
    }

    private fun reopener() = PreviousWordReopener(adoptable::contains)

    private fun withheld() = CommittedEditor(exposesSurroundingText = false).proxy
}
