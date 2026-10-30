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
    fun `the echo of our own edit is ours`() {
        val anchor = settledAt(10)
        anchor.edited(replaced = 2, written = 3)

        assertTrue(anchor.accepts(11, 11))
        assertFalse("settled at 11 now", anchor.accepts(10, 10))
    }

    @Test
    fun `intermediate echoes inside the write stay ours`() {
        val anchor = settledAt(10)
        anchor.edited(replaced = 2, written = 3)

        assertTrue("before the write", anchor.accepts(10, 10))
        assertTrue("between the delete and the commit", anchor.accepts(8, 8))
        assertTrue("the write itself", anchor.accepts(11, 11))
    }

    @Test
    fun `a commit is measured from where the composing region starts`() {
        val anchor = TailCaretAnchor()
        // `b` composing at 8..9; `a` and the space land before the echo for `a` arrives.
        anchor.settle(end = 9, composingStart = 8)
        anchor.committed(composed = 2, written = 3)

        assertTrue("the late echo for `ba`", anchor.accepts(10, 10))
        assertTrue("the commit of `ba `", anchor.accepts(11, 11))
        assertFalse(anchor.accepts(10, 10))
    }

    @Test
    fun `a commit without a reported region falls back to the confirmed caret`() {
        val anchor = settledAt(10)
        anchor.committed(composed = 2, written = 3)

        assertTrue(anchor.accepts(11, 11))
    }

    @Test
    fun `chained writes widen the window until the last one lands`() {
        val anchor = settledAt(10)
        anchor.edited(replaced = 2, written = 3)
        anchor.edited(replaced = 1, written = 0)

        assertTrue(anchor.accepts(11, 11))
        assertTrue(anchor.accepts(10, 10))
        assertFalse("settled at 10 now", anchor.accepts(11, 11))
    }

    @Test
    fun `a tap outside the window is the user moving the caret`() {
        val anchor = settledAt(10)
        anchor.edited(replaced = 2, written = 3)

        assertFalse(anchor.accepts(3, 3))
        assertTrue("the tapped caret is the new reference", anchor.accepts(3, 3))
    }

    @Test
    fun `a selection is never ours`() {
        assertFalse(settledAt(10).accepts(4, 10))
    }

    @Test
    fun `a settled caret rejects any other position`() {
        assertFalse(settledAt(10).accepts(9, 9))
    }

    @Test
    fun `writes with no known caret leave it unknown`() {
        val anchor = TailCaretAnchor()
        anchor.edited(replaced = 2, written = 3)

        assertTrue(anchor.accepts(42, 42))
    }

    private fun settledAt(caret: Int) = TailCaretAnchor().apply { settle(caret, composingStart = -1) }
}
