package app.funput.funput.theme

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DominantColorsTest {

    @Test
    fun `ranks colours by how much of the picture they cover`() {
        val pixels = image(0xFF2050C0.toInt() to 100, 0xFFC03020.toInt() to 40)

        val colours = DominantColors.extract(pixels)

        assertEquals(2, colours.size)
        assertNear(222f, colours[0].hueDegrees())
        assertNear(8f, colours[1].hueDegrees())
    }

    @Test
    fun `greys and near-blacks carry no hue and are skipped`() {
        // The subject of most photographs is surrounded by these; ranking by raw population would
        // hand back the background of the picture rather than its colour.
        val pixels = image(
            0xFF101010.toInt() to 500,
            0xFFF6F6F6.toInt() to 500,
            0xFF808080.toInt() to 500,
            0xFF20A040.toInt() to 20,
        )

        val colours = DominantColors.extract(pixels)

        assertEquals(1, colours.size)
        assertNear(135f, colours[0].hueDegrees())
    }

    @Test
    fun `a picture with no colour at all returns none`() {
        assertEquals(emptyList<Int>(), DominantColors.extract(image(0xFF404040.toInt() to 50)))
    }

    @Test
    fun `near-identical hues collapse into one swatch`() {
        // Two buckets either side of a boundary are one colour to the eye, and a row of five of
        // them is a row of one choice.
        val pixels = image(0xFF2050C0.toInt() to 60, 0xFF2A55C8.toInt() to 50)

        assertEquals(1, DominantColors.extract(pixels).size)
    }

    @Test
    fun `transparent pixels are not part of the picture`() {
        val pixels = image(0x40FF0000 to 900, 0xFF20A040.toInt() to 10)

        val colours = DominantColors.extract(pixels)

        assertEquals(1, colours.size)
        assertNear(135f, colours[0].hueDegrees())
    }

    @Test
    fun `the limit is honoured`() {
        val pixels = image(
            0xFFC02020.toInt() to 10,
            0xFF20C020.toInt() to 9,
            0xFF2020C0.toInt() to 8,
            0xFFC0C020.toInt() to 7,
        )

        assertEquals(2, DominantColors.extract(pixels, limit = 2).size)
    }

    private fun image(vararg runs: Pair<Int, Int>): IntArray =
        runs.flatMap { (colour, count) -> List(count) { colour } }.toIntArray()

    private fun assertNear(expected: Float, actual: Float) {
        val difference = abs(expected - actual)
        assertTrue("expected about $expected, got $actual", minOf(difference, 360f - difference) < 12f)
    }
}
