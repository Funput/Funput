package app.funput.funput.ui.playground

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PlaygroundKeyActionReducerTest {
    @Test
    fun `input inserts renderer output at cursor`() {
        val buffer = PlaygroundTextBuffer.from("ac", cursor = 1)

        val result = PlaygroundKeyActionReducer.reduce(buffer, KeyAction.Input("character-b", "B"))

        assertEquals("aBc", result.text)
        assertEquals(2, result.cursor)
    }

    @Test
    fun `space and enter insert their text equivalents`() {
        val withSpace = PlaygroundKeyActionReducer.reduce(PlaygroundTextBuffer.from("hello"), KeyAction.Space)
        val result = PlaygroundKeyActionReducer.reduce(withSpace, KeyAction.Enter)

        assertEquals("hello \n", result.text)
        assertEquals(result.text.length, result.cursor)
    }

    @Test
    fun `backspace removes the grapheme before cursor`() {
        val buffer = PlaygroundTextBuffer.from("A👨‍👩‍👧‍👦")

        val result = PlaygroundKeyActionReducer.reduce(buffer, KeyAction.Backspace)

        assertEquals("A", result.text)
        assertEquals(1, result.cursor)
    }

    @Test
    fun `state and navigation commands do not edit text`() {
        val buffer = PlaygroundTextBuffer.from("unchanged")
        val commands = listOf(
            KeyAction.Shift(ShiftState.CAPS_LOCK),
            KeyAction.ToggleLanguage(KeyboardLanguage.ENGLISH),
            KeyAction.Symbols,
            KeyAction.MoreSymbols,
            KeyAction.Letters,
        )

        commands.forEach { action ->
            assertSame(buffer, PlaygroundKeyActionReducer.reduce(buffer, action))
        }
    }
}
