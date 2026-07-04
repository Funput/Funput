package app.funput.funput.keyboard.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiGridColumnsTest {
    @Test
    fun `uses touch-friendly columns across device widths`() {
        assertEquals(9, EmojiGridColumns.forWidth(320f))
        assertEquals(10, EmojiGridColumns.forWidth(360f))
        assertEquals(12, EmojiGridColumns.forWidth(600f))
        assertEquals(14, EmojiGridColumns.forWidth(840f))
    }
}
