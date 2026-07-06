package app.funput.funput.keyboard.ui

import androidx.core.graphics.Insets
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardSafeAreaControllerTest {
    @Test
    fun `navigation bar inset reserves its occupied edges`() {
        val expected = Insets.of(4, 0, 6, 24)

        assertEquals(expected, mergeKeyboardSafeAreas(expected, Insets.NONE))
    }

    @Test
    fun `IME caption bar contributes to bottom safe area`() {
        val caption = Insets.of(0, 0, 0, 32)

        assertEquals(caption, mergeKeyboardSafeAreas(Insets.NONE, caption))
    }

    @Test
    fun `overlapping bar types use the largest edge instead of summing`() {
        val navigation = Insets.of(0, 4, 8, 20)
        val caption = Insets.of(6, 0, 0, 28)

        assertEquals(Insets.of(6, 4, 8, 28), mergeKeyboardSafeAreas(navigation, caption))
    }
}
