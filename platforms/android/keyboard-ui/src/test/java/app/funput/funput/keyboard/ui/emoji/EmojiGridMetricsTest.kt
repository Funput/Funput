package app.funput.funput.keyboard.ui.emoji

import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiGridMetricsTest {
    @Test fun `uses iOS cell width and clamps compact and wide layouts`() {
        assertEquals(7, EmojiGridMetrics.columnsFor(320f))
        assertEquals(7, EmojiGridMetrics.columnsFor(200f))
        assertEquals(8, EmojiGridMetrics.columnsFor(375f))
        assertEquals(10, EmojiGridMetrics.columnsFor(600f))
    }
}
