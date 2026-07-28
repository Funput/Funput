package app.funput.funput.keyboard.popover.rendering

import app.funput.funput.keyboard.layout.KeyBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternatePaletteLayoutTest {
    private val surface = KeyBounds(0f, 0f, 390f, 304f)

    @Test
    fun `large catalog wraps and remains inside the surface`() {
        val layout = resolve(18, KeyBounds(102f, 198f, 138f, 242f))

        assertEquals(18, layout.itemBounds.size)
        assertEquals(9, layout.itemBounds.count { it.top == layout.itemBounds.first().top })
        assertTrue(layout.bounds.left >= 6f)
        assertTrue(layout.bounds.right <= 384f)
        assertTrue(layout.bounds.top >= 4f)
    }

    @Test
    fun `palette uses below placement when top has no room`() {
        val source = KeyBounds(60f, 8f, 100f, 48f)
        val layout = resolve(12, source)

        assertTrue(layout.bounds.top > source.bottom)
    }

    @Test
    fun `source and item points resolve while outside cancels`() {
        val source = KeyBounds(102f, 198f, 138f, 242f)
        val layout = resolve(18, source)
        val second = layout.itemBounds[1]

        assertEquals(0, layout.indexAt(source.centerX, source.centerY, 1f))
        assertEquals(1, layout.indexAt(second.centerX, second.centerY, 1f))
        assertNull(layout.indexAt(389f, 303f, 1f))
    }

    private fun resolve(count: Int, source: KeyBounds) =
        AlternatePaletteLayout.resolve(count, source, surface, density = 1f)
}
