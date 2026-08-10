package app.funput.funput.keyboard.ui.kaomoji.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KaomojiCellMetricsTest {
    @Test
    fun `cells grow with text and keep the minimum width`() {
        val narrow = KaomojiCellMetrics.widthFor(12f, 320f)
        val wide = KaomojiCellMetrics.widthFor(120f, 320f)

        assertEquals(44f, narrow)
        assertTrue(wide > narrow)
    }

    @Test
    fun `long text is clamped to the available row`() {
        assertEquals(304f, KaomojiCellMetrics.widthFor(1_000f, 304f))
    }
}
