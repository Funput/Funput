package app.funput.funput.keyboard.surface

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardSurfaceOverlayPadTest {
    @Test
    fun syncRoundsOverflowUpAndIgnoresRepeats() {
        var layouts = 0
        var reported = 0
        val pad = KeyboardSurfaceOverlayPad(requestLayout = { layouts++ }, onPixelsChanged = { reported = it })

        pad.sync(40.2f)
        pad.sync(40.2f)

        assertEquals(41, pad.pixels)
        assertEquals(41, reported)
        assertEquals(1, layouts)
        assertEquals(259, pad.keyboardHeight(300))
    }

    @Test
    fun aClearedPaletteRemovesThePad() {
        val pad = KeyboardSurfaceOverlayPad(requestLayout = {}, onPixelsChanged = {})

        pad.sync(80f)
        pad.sync(0f)

        assertEquals(0, pad.pixels)
        assertEquals(300, pad.keyboardHeight(300))
    }
}
