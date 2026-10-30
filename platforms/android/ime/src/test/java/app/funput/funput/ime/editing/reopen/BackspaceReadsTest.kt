package app.funput.funput.ime.editing.reopen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackspaceReadsTest {
    @Test
    fun `a fresh read confirms only the whole word before the caret`() {
        val reads = BackspaceReads(before = "xin chào ", after = "xin chào")

        assertTrue(reads.confirms("chào", erased = " "))
        assertFalse("a suffix of the word is not the word", reads.confirms("ào", erased = " "))
        assertEquals("chào", reads.editorWord)
    }

    @Test
    fun `a fresh read contradicts a word the caret has left`() {
        val reads = BackspaceReads(before = "Một", after = "Mộ")

        assertFalse(reads.confirms("ba", erased = " "))
        assertEquals("Mộ", reads.editorWord)
    }

    @Test
    fun `a stale read is judged without the erased characters`() {
        val reads = BackspaceReads(before = "xin chào ", after = "xin chào ")

        assertTrue(reads.confirms("chào", erased = " "))
        assertNull("the stale word is not what sits before the caret", reads.editorWord)
    }

    @Test
    fun `a stale read that does not end with the erased text confirms nothing`() {
        val reads = BackspaceReads(before = "xin chào", after = "xin chào")

        assertFalse(reads.confirms("chào", erased = " "))
    }

    @Test
    fun `a host that withholds text cannot deny a word`() {
        val reads = BackspaceReads(before = null, after = null)

        assertTrue(reads.confirms("chào", erased = " "))
        assertNull(reads.editorWord)
    }

    @Test
    fun `text that vanished after the delete denies the word`() {
        val reads = BackspaceReads(before = "a", after = "")

        assertFalse(reads.confirms("a", erased = "a"))
    }

    @Test
    fun `an unknown pre-delete read still trusts a fresh read`() {
        val reads = BackspaceReads(before = null, after = "chào")

        assertTrue(reads.confirms("chào", erased = " "))
    }
}
