package app.funput.funput.ime.editing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The words read back from the document after the caret moved: the bar may ask after
 * them, and the store never learns from them.
 */
class BorrowedContextTest {
    private val tracker = AuthoredTokenTracker()

    @Test
    fun `a caret set after a word borrows it, and the word before it`() {
        tracker.reconcileContext("Xin cảm ơn ")

        val update = tracker.consume()
        assertEquals("cảm ơn", update.queryContext)
        assertTrue(update.borrowed)
        assertNull("a document's words are never learned from", update.learnContext)
    }

    @Test
    fun `Funput's own space does not turn the word it typed into a borrowed one`() {
        tracker.input("xin ")
        // The editor reports a selection change after every edit, this one included.
        tracker.reconcileContext("Xin ")

        val update = tracker.consume()
        assertFalse(update.borrowed)
        assertEquals("xin", update.learnContext)
    }

    @Test
    fun `a word typed after a borrowed one is learned on its own, then becomes authored`() {
        tracker.reconcileContext("cảm ")
        tracker.consume()

        tracker.input("ơn ")
        val typed = tracker.consume()
        assertEquals("ơn", typed.completedToken)
        assertEquals("the borrowed word is still asked about", "cảm ơn", typed.queryContext)
        assertFalse("the word before the caret is Funput's own again", typed.borrowed)
        assertEquals("ơn", typed.learnContext)
    }

    @Test
    fun `a caret moved away from the typed word drops it`() {
        tracker.input("xin ")
        tracker.reconcileContext("chào anh")

        assertNull("the caret is inside a word: nothing to follow", tracker.consume().context)
    }

    @Test
    fun `a caret moved after another word swaps the context for that word`() {
        tracker.input("xin ")
        tracker.reconcileContext("hôm ")

        val update = tracker.consume()
        assertEquals("hôm", update.context)
        assertTrue(update.borrowed)
    }

    @Test
    fun `a word in progress keeps the context it was started with`() {
        tracker.input("xin ch")
        tracker.reconcileContext("hôm ")

        val update = tracker.consume()
        assertEquals("ch", update.prefix)
        assertEquals("xin", update.context)
        assertFalse(update.borrowed)
    }
}
