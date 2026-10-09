package app.funput.build.tokens.validation

import app.funput.build.tokens.model.RgbaColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ContrastMathTest {
    private val black = RgbaColor(0, 0, 0)
    private val white = RgbaColor(255, 255, 255)

    @Test
    fun `black on white is the WCAG maximum`() {
        assertEquals(21.0, ContrastMath.ratio(black, white), 0.001)
        assertEquals(21.0, ContrastMath.ratio(white, black), 0.001)
    }

    @Test
    fun `a colour on itself has no contrast`() {
        assertEquals(1.0, ContrastMath.ratio(RgbaColor(0xD4, 0x6B, 0x08), RgbaColor(0xD4, 0x6B, 0x08)), 0.001)
    }

    @Test
    fun `matches a published reference pair`() {
        // #767676 on white is the classic "just passes AA" grey: 4.54:1.
        assertEquals(4.54, ContrastMath.ratio(RgbaColor(0x76, 0x76, 0x76), white), 0.01)
    }

    @Test
    fun `translucent colours are measured after compositing`() {
        val halfBlack = RgbaColor(0, 0, 0, 128)

        assertEquals(RgbaColor(127, 127, 127), ContrastMath.composite(halfBlack, white))
        assertEquals(
            ContrastMath.ratio(RgbaColor(127, 127, 127), white),
            ContrastMath.ratio(halfBlack, white),
            0.0001,
        )
    }

    @Test
    fun `a translucent background is refused`() {
        assertThrows(IllegalArgumentException::class.java) {
            ContrastMath.ratio(black, RgbaColor(255, 255, 255, 10))
        }
    }
}
