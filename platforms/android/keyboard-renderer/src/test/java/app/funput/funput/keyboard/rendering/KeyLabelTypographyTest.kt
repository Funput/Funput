package app.funput.funput.keyboard.rendering

import android.graphics.Typeface
import app.funput.funput.theme.KeyboardLabelWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyLabelTypographyTest {
    @Test
    fun `the default weights draw exactly the fonts the keyboard always used`() {
        // Letters were plain sans-serif and modifiers sans-serif-medium before weights existed.
        val letters = KeyLabelTypography.fontFor(KeyboardLabelWeight.Default)
        val modifiers = KeyLabelTypography.fontFor(KeyboardLabelWeight.Default.heavier())

        assertEquals(LabelFont("sans-serif", Typeface.NORMAL), letters)
        assertEquals(LabelFont("sans-serif-medium", Typeface.NORMAL), modifiers)
    }

    @Test
    fun `every weight maps to a distinct font`() {
        val fonts = KeyboardLabelWeight.entries.map(KeyLabelTypography::fontFor)

        assertEquals(fonts.size, fonts.toSet().size)
        assertEquals(LabelFont("sans-serif-light", Typeface.NORMAL), fonts.first())
        assertEquals(LabelFont("sans-serif", Typeface.BOLD), fonts.last())
    }

    @Test
    fun `text that fits is left alone and text that does not shrinks to the width`() {
        assertEquals(1f, KeyLabelTypography.fitFactor(textWidth = 40f, maxWidth = 50f))
        assertEquals(0.5f, KeyLabelTypography.fitFactor(textWidth = 100f, maxWidth = 50f))
        // A key too narrow to measure against keeps its size rather than collapsing to nothing.
        assertEquals(1f, KeyLabelTypography.fitFactor(textWidth = 100f, maxWidth = 0f))
    }
}
