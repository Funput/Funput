package app.funput.funput.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardLabelWeightTest {
    @Test
    fun `modifier labels sit one step heavier and stop at bold`() {
        assertEquals(KeyboardLabelWeight.REGULAR, KeyboardLabelWeight.LIGHT.heavier())
        assertEquals(KeyboardLabelWeight.MEDIUM, KeyboardLabelWeight.REGULAR.heavier())
        assertEquals(KeyboardLabelWeight.BOLD, KeyboardLabelWeight.MEDIUM.heavier())
        assertEquals(KeyboardLabelWeight.BOLD, KeyboardLabelWeight.BOLD.heavier())
    }

    @Test
    fun `stored names parse back and anything else is regular`() {
        KeyboardLabelWeight.entries.forEach { weight ->
            assertEquals(weight, KeyboardLabelWeight.parseOrDefault(weight.name))
        }
        assertEquals(KeyboardLabelWeight.REGULAR, KeyboardLabelWeight.parseOrDefault(null))
        assertEquals(KeyboardLabelWeight.REGULAR, KeyboardLabelWeight.parseOrDefault("light"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a theme written in code cannot use a label size outside the range`() {
        KeyboardThemes.Ink.copy(labelScale = 2f)
    }
}
