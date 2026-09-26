package app.funput.funput.keyboard.popover.rendering

import app.funput.funput.keyboard.layout.KeyBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternatePaletteLayoutTest {
    private val surface = KeyBounds(0f, -400f, 390f, 304f)

    @Test
    fun `large catalog wraps and remains inside the surface`() {
        val layout = resolve(18, KeyBounds(102f, 198f, 138f, 242f))

        assertEquals(18, layout.itemBounds.size)
        assertTrue(layout.bounds.left >= 6f)
        assertTrue(layout.bounds.right <= 384f)
        assertTrue(layout.bounds.top >= 4f)
    }

    @Test
    fun `the palette sits fully above the key including a top-row hold`() {
        listOf(62f, 111f, 160f, 209f).forEach { top ->
            val source = KeyBounds(156f, top, 192f, top + 40f)
            val layout = resolve(13, source)

            assertTrue("$top", layout.bounds.bottom <= source.top)
            assertTrue("$top", !layout.overlapsSource)
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
    fun `Vietnamese diacritic keys keep the palette above a top-row hold`() {
        listOf(6, 12, 18).forEach { count ->
            val source = KeyBounds(102f, 62f, 138f, 102f)
            val layout = resolve(count, source)

            assertTrue("$count", layout.bounds.bottom <= source.top)
            assertTrue("$count", !layout.overlapsSource)
        }
    }

    @Test
    fun `short sets stay on one row`() {
        val layout = resolve(2, KeyBounds(156f, 160f, 192f, 200f))

        assertEquals(1, layout.itemBounds.map { it.top }.distinct().size)
        assertEquals(2, layout.itemBounds.size)
    }

    @Test
    fun `period palette uses eight columns and two rows when space allows`() {
        val source = KeyBounds(270f, 200f, 300f, 240f)
        val layout = AlternatePaletteLayout.resolve(
            15, source, KeyBounds(0f, -400f, 320f, 304f), density = 1f,
            defaultIndex = 0, preferredColumns = 8,
        )

        assertEquals(1, layout.itemBounds.take(8).map { it.top }.distinct().size)
        assertEquals(2, layout.itemBounds.map { it.top }.distinct().size)
        assertEquals(8, layout.itemBounds.map { it.left }.distinct().size)
        assertTrue(layout.bounds.left >= 6f && layout.bounds.right <= 314f)
        assertTrue(layout.bounds.bottom <= source.top)
    }

    @Test
    fun `period palette reduces columns on narrow surfaces`() {
        val source = KeyBounds(150f, 200f, 180f, 240f)
        val layout = AlternatePaletteLayout.resolve(
            15, source, KeyBounds(0f, -400f, 190f, 304f), density = 1f,
            defaultIndex = 0, preferredColumns = 8,
        )

        assertTrue(layout.itemBounds.map { it.left }.distinct().size < 8)
        assertTrue(layout.bounds.left >= 6f && layout.bounds.right <= 184f)
        assertEquals(0, layout.indexAt(source.centerX, source.centerY, 1f))
    }

    @Test
    fun `a top-row Vietnamese catalog uses screen space above the keyboard`() {
        val source = KeyBounds(300f, 62f, 336f, 102f)
        val layout = resolve(19, source)

        assertTrue(layout.bounds.bottom <= source.top)
        assertTrue(!layout.overlapsSource)
        assertTrue(layout.bounds.top < 0f)
        assertEquals(0, layout.selectionAt(source.centerX, source.centerY, source.centerX, source.centerY, 1f))
    }

    @Test
    fun `a palette overlapping its key keeps its preferred default until the finger travels`() {
        val source = KeyBounds(290f, 62f, 326f, 102f)
        val covering = KeyBounds(280f, 50f, 356f, 110f)
        val layout = AlternatePaletteLayout(
            bounds = covering,
            itemBounds = listOf(
                KeyBounds(282f, 52f, 316f, 90f),
                KeyBounds(318f, 52f, 354f, 90f),
            ),
            sourceBounds = source,
            defaultIndex = 1,
            overlapsSource = true,
        )
        val startX = source.centerX
        val startY = source.centerY

        assertEquals(1, layout.selectionAt(startX, startY, startX, startY, 1f))
        assertEquals(1, layout.selectionAt(startX + 6f, startY, startX, startY, 1f))
        val covered = layout.indexAt(startX, startY, 1f)
        assertNotEquals(null, covered)
        assertNotEquals(1, covered)
        assertEquals(covered, layout.selectionAt(startX, startY, startX, startY + 60f, 1f))
    }

    @Test
    fun `source and item points resolve while outside cancels`() {
        val source = KeyBounds(102f, 198f, 138f, 242f)
        val layout = resolve(18, source, defaultIndex = 1)
        val third = layout.itemBounds[2]

        assertEquals(1, layout.indexAt(source.centerX, source.centerY, 1f))
        assertEquals(2, layout.indexAt(third.centerX, third.centerY, 1f))
        assertNull(layout.indexAt(389f, 303f, 1f))
    }

    private fun resolve(count: Int, source: KeyBounds, defaultIndex: Int = 0) =
        AlternatePaletteLayout.resolve(
            count, source, surface, density = 1f, defaultIndex = defaultIndex,
        )
}
