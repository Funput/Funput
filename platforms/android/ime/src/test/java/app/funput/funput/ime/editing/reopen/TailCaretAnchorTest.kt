package app.funput.funput.ime.editing.reopen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TailCaretAnchorTest {
    @Test
    fun `an unknown caret is learned from the first report`() {
        val anchor = TailCaretAnchor()

        assertTrue(anchor.accepts(7, 7))
        assertTrue(anchor.accepts(7, 7))
        assertFalse(anchor.accepts(3, 3))
    }

    @Test
    fun `the echo of our own commit is ours`() {
        val anchor = settledAt(10)
        anchor.expect(replaced = 2, written = 3)

        assertTrue(anchor.accepts(11, 11))
        assertFalse("settled at 11 now", anchor.accepts(10, 10))
    }

    @Test
    fun `intermediate and late echoes inside the write stay ours`() {
        val anchor = settledAt(10)
        anchor.expect(replaced = 2, written = 3)

        assertTrue("finishComposingText, before the commit", anchor.accepts(10, 10))
        assertTrue("between the delete and the commit", anchor.accepts(8, 8))
        assertTrue("the commit itself", anchor.accepts(11, 11))
    }

    @Test
    fun `chained writes widen the window until the last one lands`() {
        val anchor = settledAt(10)
        anchor.expect(replaced = 2, written = 3)
        anchor.expect(replaced = 1, written = 0)

        assertTrue(anchor.accepts(11, 11))
        assertTrue(anchor.accepts(10, 10))
        assertFalse("settled at 10 now", anchor.accepts(11, 11))
    }

    @Test
    fun `a tap outside the window is the user moving the caret`() {
        val anchor = settledAt(10)
        anchor.expect(replaced = 2, written = 3)

        assertFalse(anchor.accepts(3, 3))
        assertTrue("the tapped caret is the new reference", anchor.accepts(3, 3))
    }

    @Test
    fun `a selection is never ours`() {
        val anchor = settledAt(10)

        assertFalse(anchor.accepts(4, 10))
    }

    @Test
    fun `a settled caret rejects any other position`() {
        val anchor = settledAt(10)

        assertFalse(anchor.accepts(9, 9))
    }

    @Test
    fun `writes with no known caret leave it unknown`() {
        val anchor = TailCaretAnchor()
        anchor.expect(replaced = 2, written = 3)

        assertTrue(anchor.accepts(42, 42))
    }

    private fun settledAt(caret: Int) = TailCaretAnchor().apply { settle(caret) }
}
