package app.funput.funput.ime.editing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSuggestionCompositionSessionTest {
    @Test
    fun `candidate replaces composing span and adds one space`() {
        val connection = RecordingConnection()
        val session = testSession(ScriptedEngine(ArrayDeque(listOf("vi"))))
        session.input(connection.proxy, "i")

        assertTrue(session.acceptSuggestion(connection.proxy, "vi", "việt"))

        assertEquals(listOf("việt "), connection.committedTexts)
        assertFalse(session.isComposing)
    }

    @Test
    fun `authored boundary exposes completed final token once`() {
        val connection = RecordingConnection()
        val session = testSession(ScriptedEngine(ArrayDeque(listOf("tiếng"))))
        session.input(connection.proxy, "g")

        session.input(connection.proxy, " ")

        assertEquals("tiếng", session.takeCompletedToken())
        assertEquals(null, session.takeCompletedToken())
    }
}
