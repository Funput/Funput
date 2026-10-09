package app.funput.funput.keyboard.popover.rendering

import app.funput.funput.keyboard.layout.KeyBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternatePaletteFractionalDensityTest {
    @Test
    fun `full width period palette remains selectable at fractional densities`() {
        listOf(720f to 411, 750f to 393, 360f to 213).forEach { (width, dpi) ->
            val density = dpi / 160f
            val source = KeyBounds(width - 120f, 500f, width - 50f, 600f)
            val surface = KeyBounds(0f, -800f, width, 650f)
            val layout = AlternatePaletteLayout.resolve(
                count = 15,
                source = source,
                surface = surface,
                density = density,
                defaultIndex = 0,
                preferredColumns = 8,
            )

            assertEquals(15, layout.itemBounds.size)
            assertEquals(2, layout.itemBounds.map { it.top }.distinct().size)
            assertEquals(8, layout.itemBounds.map { it.left }.distinct().size)
            assertEquals(6f * density, layout.bounds.left, 0.001f)
            assertEquals(width - 6f * density, layout.bounds.right, 0.001f)
            assertTrue(layout.bounds.bottom < source.top)
            assertEquals(0, layout.indexAt(source.centerX, source.centerY, density))
            layout.itemBounds.forEachIndexed { index, item ->
                assertTrue(item.width > 0f && item.height > 0f)
                assertEquals(index, layout.indexAt(item.centerX, item.centerY, density))
            }
        }
    }
}
