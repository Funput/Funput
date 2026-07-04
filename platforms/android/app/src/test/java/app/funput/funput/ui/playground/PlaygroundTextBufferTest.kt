package app.funput.funput.ui.playground

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PlaygroundTextBufferTest {
    @Test
    fun `inserts text at the cursor`() {
        val result = PlaygroundTextBuffer.from("ac", cursor = 1).insert("b")

        assertEquals("abc", result.text)
        assertEquals(2, result.cursor)
    }

    @Test
    fun `supports spaces and multiline input`() {
        val result = PlaygroundTextBuffer.Empty
            .insert("hello")
            .insert(" ")
            .insert("world")
            .insert("\n")

        assertEquals("hello world\n", result.text)
        assertEquals(result.text.length, result.cursor)
    }

    @Test
    fun `backspace removes one grapheme instead of one UTF-16 unit`() {
        val clusters = listOf("😀", "👍🏽", "👨‍👩‍👧‍👦", "🇻🇳", "a\u0301", "1️⃣")

        clusters.forEach { cluster ->
            val result = PlaygroundTextBuffer.from("A$cluster").backspace()
            assertEquals("A", result.text)
            assertEquals(1, result.cursor)
        }
    }

    @Test
    fun `backspace at start is a no-op`() {
        val buffer = PlaygroundTextBuffer.from("hello", cursor = 0)

        assertSame(buffer, buffer.backspace())
    }

    @Test
    fun `cursor movement never splits a grapheme`() {
        val emoji = "👨‍👩‍👧‍👦"
        val start = PlaygroundTextBuffer.from("A${emoji}B", cursor = 1)

        val afterEmoji = start.moveRight()
        assertEquals(1 + emoji.length, afterEmoji.cursor)
        assertEquals(1, afterEmoji.moveLeft().cursor)
    }

    @Test
    fun `arbitrary cursor offsets snap to the preceding grapheme boundary`() {
        val emoji = "👍🏽"
        val result = PlaygroundTextBuffer.from("A${emoji}B").moveCursorTo(2)

        assertEquals(1, result.cursor)
    }

    @Test
    fun `clear restores canonical empty state`() {
        val result = PlaygroundTextBuffer.from("hello").clear()

        assertEquals(PlaygroundTextBuffer.Empty, result)
        assertSame(PlaygroundTextBuffer.Empty, PlaygroundTextBuffer.Empty.clear())
    }
}
