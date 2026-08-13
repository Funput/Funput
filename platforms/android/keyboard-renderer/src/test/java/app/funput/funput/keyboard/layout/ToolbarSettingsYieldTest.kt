package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolbarSettingsYieldTest {
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
    fun hidingSettingsWidensSuggestionRegionToEmoji() {
        val withSettings = resolve(showSettings = true)
        val withoutSettings = resolve(showSettings = false)
        val shown = requireNotNull(withSettings.suggestionBar)
        val yielded = requireNotNull(withoutSettings.suggestionBar)

        assertNotNull(shown.settingsKey)
        assertNull(yielded.settingsKey)
        assertTrue(yielded.suggestionsBounds.width > shown.suggestionsBounds.width)
        assertEquals(shown.emojiKey.bounds, yielded.emojiKey.bounds)
        assertTrue(yielded.suggestionsBounds.right < yielded.emojiKey.bounds.left)
    }

    @Test
    fun showingSettingsKeepsSettingsBeforeEmoji() {
        val bar = requireNotNull(resolve(showSettings = true, showClipboard = true).suggestionBar)
        val settings = requireNotNull(bar.settingsKey)
        val clipboard = requireNotNull(bar.clipboardKey)

        assertTrue(bar.suggestionsBounds.right < settings.bounds.left)
        assertTrue(settings.bounds.right < clipboard.bounds.left)
        assertTrue(clipboard.bounds.right < bar.emojiKey.bounds.left)
    }

    @Test
    fun clipboardDoesNotMoveEmojiAndYieldsWithSuggestions() {
        val hidden = requireNotNull(resolve(true, false).suggestionBar)
        val shown = requireNotNull(resolve(true, true).suggestionBar)
        assertNull(hidden.clipboardKey)
        assertNotNull(shown.clipboardKey)
        assertEquals(hidden.emojiKey.bounds, shown.emojiKey.bounds)
        assertTrue(hidden.suggestionsBounds.width > shown.suggestionsBounds.width)
    }

    private fun resolve(showSettings: Boolean, showClipboard: Boolean = false) = KeyboardGeometry.resolve(
        layout = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX),
        width = 1080f,
        height = 726f,
        spec = spec,
        showSettings = showSettings,
        showClipboard = showClipboard,
    )
}
