package app.funput.funput.ime.editing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The two words a prefix is typed after, which is what the engine's trigram reads:
 * kept while the words were typed in a row, dropped at every boundary that drops one.
 *
 * Read alongside the iOS `PersonalSuggestionTwoWordContextTests`: the rules are the same.
 */
class AuthoredTokenTwoWordContextTest {
    @Test
    fun `two words typed in a row are both the context, oldest first`() {
        val tracker = AuthoredTokenTracker()
        tracker.input("cảm ơn ")
        val update = tracker.consume()
        assertEquals("ơn", update.context)
        assertEquals("cảm", update.older)
        assertEquals("cảm ơn", update.queryContext)
    }

    @Test
    fun `only the last two words are kept`() {
        val tracker = AuthoredTokenTracker()
        tracker.input("xin cảm ơn ")
        assertEquals("cảm ơn", tracker.consume().queryContext)
    }

    @Test
    fun `one word alone is a one word context`() {
        val tracker = AuthoredTokenTracker()
        tracker.input("ơn ")
        val update = tracker.consume()
        assertNull(update.older)
        assertEquals("ơn", update.queryContext)
    }

    @Test
    fun `a full stop between the words keeps only the second`() {
        val tracker = AuthoredTokenTracker()
        tracker.input("cảm. ơn ")
        val update = tracker.consume()
        assertNull("the words either side of the stop were never adjacent", update.older)
        assertEquals("ơn", update.queryContext)
    }

    @Test
    fun `a composed word extends the run as a typed one does`() {
        val tracker = AuthoredTokenTracker()
        tracker.update("", "cảm", completedOnSpace = true)
        tracker.update("", "ơn", completedOnSpace = true)
        assertEquals("cảm ơn", tracker.consume().queryContext)

        tracker.update("", "bạn", completedOnSpace = false)
        assertNull(tracker.consume().queryContext)
    }

    @Test
    fun `an accepted candidate follows the word before it`() {
        val tracker = AuthoredTokenTracker()
        tracker.input("cảm ")
        tracker.accepted("ơn")
        assertEquals("cảm ơn", tracker.consume().queryContext)
    }

    @Test
    fun `backspacing past a boundary and reset drop both words`() {
        val tracker = AuthoredTokenTracker()
        tracker.input("cảm ơn ")
        tracker.backspace()
        val crossed = tracker.consume()
        assertNull(crossed.context)
        assertNull(crossed.older)

        tracker.input("cảm ơn ")
        tracker.reset()
        assertNull(tracker.consume().queryContext)

        // A fresh word after the reset does not reach back past it.
        tracker.input("bạn ")
        assertEquals("bạn", tracker.consume().queryContext)
    }
}
