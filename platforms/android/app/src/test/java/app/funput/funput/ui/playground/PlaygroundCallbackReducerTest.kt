package app.funput.funput.ui.playground

import app.funput.funput.keyboard.model.SuggestionSelection
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaygroundCallbackReducerTest {
    @Test
    fun `emoji inserts the complete sequence at cursor`() {
        val emoji = "👨‍👩‍👧‍👦"
        val buffer = PlaygroundTextBuffer.from("AB", cursor = 1)

        val result = PlaygroundCallbackReducer.emojiSelected(buffer, emoji)

        assertEquals("A${emoji}B", result.text)
        assertEquals(1 + emoji.length, result.cursor)
    }

    @Test
    fun `suggestion replaces the whole token containing cursor`() {
        val text = "xin chao ban"
        val buffer = PlaygroundTextBuffer.from(text, cursor = "xin ch".length)

        val result = PlaygroundCallbackReducer.suggestionSelected(buffer, suggestion("chào"))

        assertEquals("xin chào ban", result.text)
        assertEquals("xin chào".length, result.cursor)
    }

    @Test
    fun `suggestion recognizes combining marks as part of token`() {
        val buffer = PlaygroundTextBuffer.from("go a\u0301n", cursor = "go a\u0301".length)

        val result = PlaygroundCallbackReducer.suggestionSelected(buffer, suggestion("ăn"))

        assertEquals("go ăn", result.text)
    }

    @Test
    fun `suggestion inserts at cursor when no token is active`() {
        val buffer = PlaygroundTextBuffer.from("xin  ban", cursor = 4)

        val result = PlaygroundCallbackReducer.suggestionSelected(buffer, suggestion("chào"))

        assertEquals("xin chào ban", result.text)
        assertEquals("xin chào".length, result.cursor)
    }

    @Test
    fun `suggestion keeps punctuation outside replacement`() {
        val buffer = PlaygroundTextBuffer.from("hello, bạn", cursor = "hello".length)

        val result = PlaygroundCallbackReducer.suggestionSelected(buffer, suggestion("chào"))

        assertEquals("chào, bạn", result.text)
    }

    private fun suggestion(text: String) = SuggestionSelection(index = 0, text = text)
}
