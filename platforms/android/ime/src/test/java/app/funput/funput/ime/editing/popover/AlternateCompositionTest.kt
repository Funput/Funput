package app.funput.funput.ime.editing.popover

import app.funput.funput.ime.editing.RecordingConnection
import app.funput.funput.ime.editing.ScriptedEngine
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.testSession
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class AlternateCompositionTest {
    @Test
    fun `precomposed alternate remains in Vietnamese composition`() {
        val connection = RecordingConnection()
        val session = testSession(ScriptedEngine(ArrayDeque(listOf("t", "tế", "tết"))))

        listOf("t", "ế", "t").forEach { session.input(connection.proxy, it) }

        assertEquals("tết", session.composingText)
        assertEquals(listOf("t", "tế", "tết"), connection.composingTexts)
        assertEquals(emptyList<String>(), connection.committedTexts)
    }

    @Test
    fun `English mode commits precomposed alternate directly`() {
        val connection = RecordingConnection()
        val handler = ImeKeyActionHandler(
            composition = testSession(ScriptedEngine(ArrayDeque())),
            editor = InputConnectionEditor(),
            connection = { connection.proxy },
            enterCommand = { ImeEditCommand.CommitText("\n") },
        )
        handler.start()
        handler.onKeyAction(KeyAction.ToggleLanguage(KeyboardLanguage.ENGLISH))
        handler.onKeyAction(KeyAction.Input("character-u", "ư"))

        assertEquals(listOf("ư"), connection.committedTexts)
    }
}
