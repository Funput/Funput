package app.funput.funput.ime.editing

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyActionEditCommandMapperTest {
    @Test
    fun `text producing actions map to commit commands`() {
        val cases = mapOf(
            KeyAction.Input("letter-a", "a") to ImeEditCommand.CommitText("a"),
            KeyAction.Space to ImeEditCommand.CommitText(" "),
            KeyAction.Enter to ImeEditCommand.CommitText("\n"),
        )

        cases.forEach { (action, expected) ->
            assertEquals(expected, action.toImeEditCommand())
        }
    }

    @Test
    fun `backspace maps to delete command`() {
        assertEquals(ImeEditCommand.DeleteBackward, KeyAction.Backspace.toImeEditCommand())
    }

    @Test
    fun `state and panel actions do not edit text`() {
        val actions = listOf(
            KeyAction.Shift(ShiftState.CAPS_LOCK),
            KeyAction.ToggleLanguage(KeyboardLanguage.ENGLISH),
            KeyAction.Symbols,
            KeyAction.MoreSymbols,
            KeyAction.Letters,
        )

        actions.forEach { action -> assertNull(action.toImeEditCommand()) }
    }
}
