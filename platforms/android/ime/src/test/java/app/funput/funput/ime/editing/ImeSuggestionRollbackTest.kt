package app.funput.funput.ime.editing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeSuggestionRollbackTest {
    @Test
    fun `failed direct suggestion restores prefix and can be retried`() {
        val editor = CommittedEditor("hel")
        val session = ImeSuggestionSession(testSession(ScriptedEngine(ArrayDeque())), { editor.proxy })
        session.inputDirect("hel")
        editor.remainingCommitFailures = 1

        assertFalse(session.accept("hello", "hel", usesComposition = false))
        assertEquals("hel", editor.text)
        val failedUpdate = session.takeUpdate()
        assertEquals("hel", failedUpdate.prefix)
        assertEquals(null, failedUpdate.completedToken)

        assertTrue(session.accept("hello", "hel", usesComposition = false))
        assertEquals("hello ", editor.text)
        val acceptedUpdate = session.takeUpdate()
        assertEquals("", acceptedUpdate.prefix)
        assertEquals("hello", acceptedUpdate.completedToken)
    }
}
