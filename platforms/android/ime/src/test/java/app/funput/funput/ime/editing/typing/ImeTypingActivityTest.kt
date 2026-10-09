package app.funput.funput.ime.editing.typing

import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.RecordingConnection
import app.funput.funput.ime.editing.ScriptedEngine
import app.funput.funput.ime.editing.testSession
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Test

class ImeTypingActivityTest {
    @Test
    fun `typing hides placement even without suggestions`() {
        listOf(KeyAction.Input("a", "a"), KeyAction.Space, KeyAction.Enter).forEach { action ->
            val handler = handler()
            val changes = mutableListOf<Boolean>()
            handler.typingSession.observe(changes::add)
            handler.onKeyAction(action)
            handler.finish()
            handler.start(allowComposition = false, allowSuggestions = false)
            assertEquals(listOf(false, true), changes)
        }
    }

    @Test
    fun `panel navigation and shift do not count as typing`() {
        val handler = handler()
        val changes = mutableListOf<Boolean>()
        handler.typingSession.observe(changes::add)
        listOf(KeyAction.Shift(ShiftState.ON), KeyAction.Symbols, KeyAction.Letters).forEach(handler::onKeyAction)
        assertEquals(listOf(false), changes)
    }

    @Test
    fun `inserting emoji or clipboard text counts as input`() {
        listOf<(ImeKeyActionHandler) -> Unit>(
            { it.onEmojiSelected("😀") }, { it.onClipboardSelected("text") },
        ).forEach { insert ->
            val handler = handler()
            val changes = mutableListOf<Boolean>()
            handler.typingSession.observe(changes::add)
            insert(handler)
            assertEquals(listOf(false, true), changes)
        }
    }

    private fun handler(): ImeKeyActionHandler {
        val connection = RecordingConnection()
        return ImeKeyActionHandler(
            composition = testSession(ScriptedEngine(ArrayDeque())),
            editor = InputConnectionEditor(),
            connection = { connection.proxy },
            enterCommand = { ImeEditCommand.CommitText("\n") },
        ).apply {
            start(allowComposition = false, allowSuggestions = false, allowShortcuts = false)
            typingSession.begin()
        }
    }
}
