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
        val reopener = reopener().apply { committed("chào", "chào ") }

        assertEquals("chào", reopener.wordToReopen(withheld(), before = null))
    }

    @Test
    fun `text the editor shows wins over a remembered word it contradicts`() {
        val reopener = reopener().apply { committed("ba", "ba ") }

        assertEquals("mo", reopener.wordToReopen(showing("mo"), before = "mot"))
        assertNull("the contradicted memory is gone", reopener.wordToReopen(withheld(), before = null))
    }

    @Test
    fun `a contradicted memory is dropped even when nothing can be reopened`() {
        val reopener = reopener().apply { committed("ba", "ba ") }

        assertNull(reopener.wordToReopen(showing("hai "), before = "hai x"))
        assertNull(reopener.wordToReopen(withheld(), before = null))
    }

    @Test
    fun `a stale read still reopens the remembered word`() {
        val reopener = reopener().apply { committed("chào", "chào ") }

        assertEquals("chào", reopener.wordToReopen(showing("chào "), before = "chào "))
    }

    @Test
    fun `a refused word stays remembered until a later delete makes it adoptable`() {
        val reopener = reopener().apply { committed("dungh", "dungh ") }

        assertNull(reopener.wordToReopen(withheld(), before = null))
        assertEquals("dung", reopener.wordToReopen(withheld(), before = null))
    }

    @Test
    fun `moving the caret away forgets the remembered word`() {
        val reopener = reopener()
        reopener.caretReported(start = 2, end = 2, composingStart = 0, owned = true)
        reopener.committed("ba", "ba ")
        reopener.caretReported(start = 3, end = 3, composingStart = -1, owned = false)

        reopener.caretReported(start = 0, end = 0, composingStart = -1, owned = false)

        assertNull(reopener.wordToReopen(withheld(), before = null))
    }

    @Test
    fun `selecting text forgets the remembered word`() {
        val reopener = reopener().apply { committed("ba", "ba ") }

        reopener.caretReported(start = 0, end = 2, composingStart = -1, owned = false)

        assertNull(reopener.wordToReopen(withheld(), before = null))
    }

    private fun reopener() = PreviousWordReopener(adoptable::contains)

    private fun showing(text: String) = ReopenWordEditor(textBeforeCursor = text).proxy

    private fun withheld() = CommittedEditor(exposesSurroundingText = false).proxy
}
