package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolbarClipboardYieldTest {
    private val spec = KeyboardGeometrySpec(
        horizontalPadding = 21f,
        verticalPadding = 24f,
        horizontalGap = 0f,
        verticalGap = 0f,
        horizontalGapRatio = 0.11f,
        verticalGapRatio = 0.16f,
        keyAspectRatio = 0.75f,
        suggestionBarHeight = 126f,
        suggestionBarGap = 18f,
    )

    @Test
    fun clipboardDoesNotMoveEmojiAndYieldsWithSuggestions() {
        val hidden = requireNotNull(resolve(showClipboard = false).suggestionBar)
        val shown = requireNotNull(resolve(showClipboard = true).suggestionBar)
        assertNull(hidden.clipboardKey)
        assertNotNull(shown.clipboardKey)
        assertEquals(hidden.emojiKey.bounds, shown.emojiKey.bounds)
        assertTrue(hidden.suggestionsBounds.width > shown.suggestionsBounds.width)
    }

    private fun resolve(showClipboard: Boolean) = KeyboardGeometry.resolve(
        layout = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX),
        width = 1080f,
        height = 726f,
        spec = spec,
        showClipboard = showClipboard,
    )
}
