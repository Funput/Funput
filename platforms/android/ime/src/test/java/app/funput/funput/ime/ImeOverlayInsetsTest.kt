package app.funput.funput.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeOverlayInsetsTest {
    @Test
    fun noPadLeavesInsetsAlone() {
        val next = ImeOverlayInsets.adjustment(280, 280, overlayPadTop = 0)

        assertEquals(280, next.contentTopInsets)
        assertEquals(280, next.visibleTopInsets)
        assertFalse(next.touchableFrame)
    }

    @Test
    fun padKeepsTheAppAnchoredToTheKeyboard() {
        val next = ImeOverlayInsets.adjustment(360, 360, overlayPadTop = 80)

        assertEquals(280, next.contentTopInsets)
        assertEquals(280, next.visibleTopInsets)
        assertTrue(next.touchableFrame)
    }
}
