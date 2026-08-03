package app.funput.funput.ime.editing

import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidCompositionSessionTest {
    @Test
    fun `advanced Telex bracket is routed through composition`() {
        val connection = RecordingConnection()
        val engine = ScriptedEngine(
            processed = ArrayDeque(listOf("tư")),
            inputMethod = KeyboardInputMethod.TELEX_ADVANCED,
        )
        val session = testSession(engine)

        session.input(connection.proxy, "[")

        assertEquals(listOf("tư"), connection.composingTexts)
        assertEquals("tư", session.composingText)
    }

    @Test
    fun `Telex bracket stays a punctuation boundary`() {
        val connection = RecordingConnection()
        val engine = ScriptedEngine(processed = ArrayDeque(listOf("t")))
        val session = testSession(engine)
        session.input(connection.proxy, "t")

        session.input(connection.proxy, "[")

        assertEquals(1, connection.finishCount)
        assertEquals(listOf("["), connection.committedTexts)
        assertFalse(session.isComposing)
    }

    @Test
    fun `engine buffer is rendered as composing text after every key`() {
        val engine = ScriptedEngine(processed = ArrayDeque(listOf("a", "an", "án", "ánh")))
        val connection = RecordingConnection()
        val session = testSession(engine)

        "an1h".forEach { character -> session.input(connection.proxy, character.toString()) }

        assertEquals(listOf("a", "an", "án", "ánh"), connection.composingTexts)
        assertEquals("ánh", session.composingText)
    }

    @Test
    fun `ordinary boundary finishes composition and commits itself`() {
        val connection = RecordingConnection()
        val session = testSession(ScriptedEngine(ArrayDeque(listOf("á"))))
        session.input(connection.proxy, "a")

        session.input(connection.proxy, " ")

        assertEquals(1, connection.finishCount)
        assertEquals(listOf(" "), connection.committedTexts)
        assertFalse(session.isComposing)
    }

    @Test
    fun `boundary replacement commits engine output over composing span`() {
        val connection = RecordingConnection()
        val engine = ScriptedEngine(ArrayDeque(listOf("cảd")), boundaryOutput = "card ")
        val session = testSession(engine)
        session.input(connection.proxy, "d")

        session.input(connection.proxy, " ")

        assertEquals(listOf("card "), connection.committedTexts)
        assertEquals(0, connection.finishCount)
    }

    @Test
    fun `backspace updates active composing text`() {
        val connection = RecordingConnection()
        val engine = ScriptedEngine(ArrayDeque(listOf("an")), backspaceOutput = "a")
        val session = testSession(engine)
        session.input(connection.proxy, "n")

        assertTrue(session.backspace(connection.proxy))

        assertEquals(listOf("an", "a"), connection.composingTexts)
        assertEquals("a", session.composingText)
    }

    @Test
    fun `enabled state is forwarded to the native engine adapter`() {
        val engine = ScriptedEngine(ArrayDeque())
        val session = testSession(engine)

        session.setEnabled(false)

        assertEquals(false, engine.enabled)
    }
}
