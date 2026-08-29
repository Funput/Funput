package app.funput.funput.ime.editing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthoredTokenTrackerTest {
    @Test
    fun `tracks final composed output and completed token`() {
        val tracker = AuthoredTokenTracker()

        tracker.update("tiếng", null, completedOnSpace = true)
        assertEquals("tiếng", tracker.consume().prefix)
        tracker.update("", "tiếng", completedOnSpace = true)

        val update = tracker.consume()
        assertEquals("", update.prefix)
        assertEquals("tiếng", update.completedToken)
        assertNull(tracker.consume().completedToken)
    }

    @Test
    fun `supports decomposed marks and rejects boundaries`() {
        val tracker = AuthoredTokenTracker()

        tracker.update("tie\u0302\u0301ng", null, completedOnSpace = true)
        assertEquals("tie\u0302\u0301ng", tracker.consume().prefix)
        tracker.update("xin chào", "a!", completedOnSpace = true)

        assertEquals(AuthoredSuggestionUpdate.Empty, tracker.consume())
    }

    @Test
    fun `bounds token by Unicode scalar count`() {
        val tracker = AuthoredTokenTracker()
        val valid = "a".repeat(32)

        tracker.update(valid, valid, completedOnSpace = true)
        assertEquals(valid, tracker.consume().prefix)
        tracker.update("a".repeat(33), "a".repeat(33), completedOnSpace = true)

        assertEquals(AuthoredSuggestionUpdate.Empty, tracker.consume())
    }

    @Test
    fun `accepted candidate is learned exactly once`() {
        val tracker = AuthoredTokenTracker()

        tracker.accepted("Việt")

        assertEquals("Việt", tracker.consume().completedToken)
        assertNull(tracker.consume().completedToken)
    }

    @Test
    fun `direct input tracks letters digits boundaries and backspace`() {
        val tracker = AuthoredTokenTracker()

        tracker.input("ab12")
        tracker.backspace()
        assertEquals("ab1", tracker.consume().prefix)
        tracker.input(" ")

        assertEquals(AuthoredSuggestionUpdate("", "ab1", context = "ab1"), tracker.consume())
    }

    @Test
    fun `oversized direct token stays suppressed through its boundary`() {
        val tracker = AuthoredTokenTracker()

        tracker.input("a".repeat(33))
        tracker.backspace()
        tracker.input(" ")

        assertEquals(AuthoredSuggestionUpdate.Empty, tracker.consume())
    }

    @Test
    fun `a space keeps the finished word as context and a full stop clears it`() {
        val tracker = AuthoredTokenTracker()
        tracker.update("", "xin", completedOnSpace = true)
        assertEquals("xin", tracker.consume().context)

        tracker.update("", "xin", completedOnSpace = false)
        val ended = tracker.consume()
        assertEquals("the word is still learned", "xin", ended.completedToken)
        assertNull("but nothing follows it", ended.context)
    }

    @Test
    fun `direct input decides the context from its own separator`() {
        val tracker = AuthoredTokenTracker()
        tracker.input("xin ")
        assertEquals("xin", tracker.consume().context)

        tracker.input("xin.")
        assertNull(tracker.consume().context)
    }

    @Test
    fun `the context survives while the next word is typed`() {
        val tracker = AuthoredTokenTracker()
        tracker.input("xin ")
        tracker.consume()
        tracker.input("ch")
        val update = tracker.consume()
        assertEquals("ch", update.prefix)
        assertEquals("xin", update.context)
    }

    @Test
    fun `accepting a candidate makes it the context`() {
        val tracker = AuthoredTokenTracker()
        tracker.accepted("bạn")
        assertEquals("bạn", tracker.consume().context)
    }

    @Test
    fun `reset and backspacing past the word abandon the context`() {
        val tracker = AuthoredTokenTracker()
        tracker.input("xin ")
        tracker.consume()
        tracker.backspace()
        assertNull("the caret crossed back over the boundary", tracker.consume().context)

        tracker.input("xin ")
        tracker.consume()
        tracker.reset()
        assertNull(tracker.consume().context)
    }
}
