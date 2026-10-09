package app.funput.funput.keyboard.popover.rendering

import app.funput.funput.keyboard.layout.KeyBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionPaletteLayoutTest {
    @Test
    fun `action palette fits a narrow surface at fractional density without a default`() {
        val source = KeyBounds(30f, 450f, 100f, 540f)
        val density = 411f / 160f
        val layout = AlternatePaletteLayout.resolve(
            2, source, KeyBounds(0f, -800f, 220f, 600f), density, defaultIndex = null, preferredColumns = 2,
        )
        assertEquals(1, layout.itemBounds.map { it.top }.distinct().size)
        assertTrue(layout.bounds.left >= 0f && layout.bounds.right <= 220f)
        assertNull(layout.selectionAt(source.centerX, source.centerY, source.centerX, source.centerY, density))
        layout.itemBounds.forEachIndexed { index, bounds ->
            assertEquals(index, layout.indexAt(bounds.centerX, bounds.centerY, density))
        }
    }

    @Test
    fun `palette over its source requires a drag before selecting a function`() {
        val source = KeyBounds(130f, 0f, 170f, 40f)
        val layout = AlternatePaletteLayout.resolve(
            2, source, KeyBounds(0f, 0f, 320f, 70f), 1f, defaultIndex = null, preferredColumns = 2,
        )
        assertTrue(layout.overlapsSource)
        assertNull(layout.selectionAt(150f, 20f, 150f, 20f, 1f))
        val cell = layout.itemBounds[1]
        assertEquals(1, layout.selectionAt(cell.centerX, cell.centerY, 150f, 20f, 1f))
    }
}
