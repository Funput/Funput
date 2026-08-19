package app.funput.funput.keyboard.popover.rendering

import app.funput.funput.keyboard.layout.KeyBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternatePaletteLayoutTest {
    private val surface = KeyBounds(0f, 0f, 390f, 304f)

    @Test
    fun `large catalog wraps and remains inside the surface`() {
        val layout = resolve(18, KeyBounds(102f, 198f, 138f, 242f))

        assertEquals(18, layout.itemBounds.size)
        assertTrue(layout.bounds.left >= 6f)
        assertTrue(layout.bounds.right <= 384f)
        assertTrue(layout.bounds.top >= 4f)
    }

    @Test
    fun `the palette rises from the key instead of dropping below it`() {
        listOf(62f, 111f, 160f, 209f).forEach { top ->
            val source = KeyBounds(156f, top, 192f, top + 40f)
            val layout = resolve(13, source)

            assertTrue("$top", layout.bounds.top < source.top)
            assertTrue("$top", layout.bounds.left >= 6f && layout.bounds.right <= 384f)
        }
    }

    @Test
    fun `the palette wraps into at most three rows instead of running wide`() {
        listOf(62f, 209f).forEach { top ->
            val source = KeyBounds(156f, top, 192f, top + 40f)
            listOf(13, 18, 19).forEach { count ->
                val layout = resolve(count, source)
                val rows = layout.itemBounds.map { it.top }.distinct().size

                assertTrue("$count", rows in 2..3)
                assertTrue("$count", layout.bounds.width <= surface.width * 0.8f)
            }
        }
    }

    @Test
    fun `short sets stay on one row`() {
        val layout = resolve(2, KeyBounds(156f, 160f, 192f, 200f))

        assertEquals(1, layout.itemBounds.map { it.top }.distinct().size)
        assertEquals(2, layout.itemBounds.size)
    }

    @Test
    fun `a palette clamped over its key keeps the default until the finger travels`() {
        // Nineteen alternates cannot fit above a top-row key, so the palette covers it.
        val source = KeyBounds(300f, 62f, 336f, 102f)
        val layout = resolve(19, source)
        val startX = source.centerX
        val startY = source.centerY

        assertTrue(layout.overlapsSource)
        assertEquals(0, layout.selectionAt(startX, startY, startX, startY, 1f))
        assertEquals(0, layout.selectionAt(startX + 6f, startY, startX, startY, 1f))
        // The cell sitting over the key stays reachable once the finger has moved.
        val covering = layout.indexAt(startX, startY, 1f)
        assertNotEquals(null, covering)
        assertNotEquals(0, covering)
        assertEquals(covering, layout.selectionAt(startX, startY, startX, startY + 60f, 1f))
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
