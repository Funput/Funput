package app.funput.funput.ime.editing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthoredTokenTrackerTest {
    @Test
    fun `tracks final composed output and completed token`() {
        val tracker = AuthoredTokenTracker()

        tracker.update("tiếng", null)
        assertEquals("tiếng", tracker.consume().prefix)
        tracker.update("", "tiếng")

        val update = tracker.consume()
        assertEquals("", update.prefix)
        assertEquals("tiếng", update.completedToken)
        assertNull(tracker.consume().completedToken)
    }

    @Test
    fun `supports decomposed marks and rejects boundaries`() {
        val tracker = AuthoredTokenTracker()

        tracker.update("tie\u0302\u0301ng", null)
        assertEquals("tie\u0302\u0301ng", tracker.consume().prefix)
        tracker.update("xin chào", "a!")

        assertEquals(AuthoredSuggestionUpdate.Empty, tracker.consume())
    }

    @Test
    fun `bounds token by Unicode scalar count`() {
        val tracker = AuthoredTokenTracker()
        val valid = "a".repeat(32)

        tracker.update(valid, valid)
        assertEquals(valid, tracker.consume().prefix)
        tracker.update("a".repeat(33), "a".repeat(33))

        assertEquals(AuthoredSuggestionUpdate.Empty, tracker.consume())
    }

    @Test
    fun `accepted candidate is learned exactly once`() {
        val tracker = AuthoredTokenTracker()

        tracker.accepted("Việt")

        assertEquals("Việt", tracker.consume().completedToken)
        assertNull(tracker.consume().completedToken)
    }
}
