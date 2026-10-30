package app.funput.funput.ime.editing.composition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpectedCaretTest {
    @Test
    fun `a write is measured from the confirmed caret`() {
        val caret = ExpectedCaret().apply { confirm(10) }

        assertEquals("the caret between delete and commit", 8, caret.expect(replaced = 2, written = 3))
        assertEquals(11, caret.target)
        assertEquals(8..11, caret.span)
    }

    @Test
    fun `chained writes start where the previous one ends`() {
        val caret = ExpectedCaret().apply { confirm(10) }
        caret.expect(replaced = 2, written = 3)
        caret.expect(replaced = 1, written = 0)

        assertEquals(10, caret.target)
        assertEquals(8..11, caret.span)
    }

    @Test
    fun `an explicit origin overrides the confirmed caret`() {
        val caret = ExpectedCaret().apply { confirm(9) }
        caret.expect(replaced = 2, written = 3, from = 10)

        assertEquals(11, caret.target)
    }

    @Test
    fun `nothing is expected without a known caret`() {
        val caret = ExpectedCaret()

        assertNull(caret.expect(replaced = 2, written = 3))
        assertNull(caret.target)
        assertNull(caret.span)
    }

    @Test
    fun `confirming lands the writes in flight`() {
        val caret = ExpectedCaret().apply { confirm(10) }
        caret.expect(replaced = 2, written = 3)
        caret.confirm(11)

        assertEquals(11, caret.confirmed)
        assertNull(caret.target)
        assertNull(caret.span)
    }
}
