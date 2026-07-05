package app.funput.funput.keyboard.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiGridColumnsTest {
    @Test
    fun `derives denser columns as width grows`() {
        assertEquals(9, EmojiGridColumns.forWidth(320f))
        assertEquals(11, EmojiGridColumns.forWidth(360f))
        assertEquals(12, EmojiGridColumns.forWidth(411f))
    }

    @Test
    fun `clamps to keep emojis usable on extreme widths`() {
        assertEquals(9, EmojiGridColumns.forWidth(200f))
        assertEquals(15, EmojiGridColumns.forWidth(600f))
        assertEquals(15, EmojiGridColumns.forWidth(840f))
    }
}
