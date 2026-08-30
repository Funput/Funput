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
    fun `direct input tracks letters boundaries and backspace`() {
        val tracker = AuthoredTokenTracker()

        tracker.input("abcd")
        tracker.backspace()
        assertEquals("abc", tracker.consume().prefix)
        tracker.input(" ")

        assertEquals(AuthoredSuggestionUpdate("", "abc", context = "abc"), tracker.consume())
    }

    @Test
    fun `oversized direct token stays suppressed through its boundary`() {
        val tracker = AuthoredTokenTracker()

        tracker.input("a".repeat(33))
        tracker.backspace()
        tracker.input(" ")

        assertEquals(AuthoredSuggestionUpdate.Empty, tracker.consume())
    }

}
