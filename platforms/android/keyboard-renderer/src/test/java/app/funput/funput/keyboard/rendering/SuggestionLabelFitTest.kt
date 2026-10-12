package app.funput.funput.keyboard.rendering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionLabelFitTest {
    @Test
    fun `a word that fits is drawn as it is`() {
        assertEquals(SuggestionLabelFit(1f, 1f, ellipsize = false), SuggestionLabelFit.fit(80f, 100f))
    }

    @Test
    fun `a slightly long word only gets smaller`() {
        val fit = SuggestionLabelFit.fit(textWidth = 110f, availableWidth = 100f)

        assertEquals(100f / 110f, fit.sizeFactor, Tolerance)
        assertEquals(1f, fit.scaleX)
        assertFalse(fit.ellipsize)
    }

    @Test
    fun `a longer word is narrowed after the size floor and fits exactly`() {
        // "Automatically" in the issue: too wide for the size floor alone.
        val fit = SuggestionLabelFit.fit(textWidth = 150f, availableWidth = 100f)

        assertEquals(SuggestionLabelFit.MinSizeFactor, fit.sizeFactor)
        assertEquals(100f, 150f * fit.sizeFactor * fit.scaleX, Tolerance)
        assertTrue(fit.scaleX > SuggestionLabelFit.MinScaleX)
        assertFalse(fit.ellipsize)
    }

    @Test
    fun `a word too long for both is shortened at both floors`() {
        val fit = SuggestionLabelFit.fit(textWidth = 400f, availableWidth = 100f)

        assertEquals(SuggestionLabelFit.MinSizeFactor, fit.sizeFactor)
        assertEquals(SuggestionLabelFit.MinScaleX, fit.scaleX)
        assertTrue(fit.ellipsize)
    }

    @Test
    fun `a slot with no room leaves the text alone rather than dividing by zero`() {
        assertEquals(SuggestionLabelFit(1f, 1f, ellipsize = false), SuggestionLabelFit.fit(50f, 0f))
    }

    private companion object {
        const val Tolerance = 0.0001f
    }
}
