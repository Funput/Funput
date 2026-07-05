package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardHitTesterTest {
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
    private val keyboard = resolveLayout(KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX))
    private val keyboardWithSuggestions = resolveLayout(
        qwertyLayout(
            id = "test-suggestions",
            inputMethod = KeyboardInputMethod.TELEX,
            actionKeys = KeyboardLayouts.telex.rows.last().keys,
            showSuggestionBar = true,
        ).copy(
            suggestionBar = requireNotNull(KeyboardLayouts.telex.suggestionBar).copy(
                suggestionsEnabled = true,
            ),
        ),
    )

    private fun resolveLayout(layout: app.funput.funput.keyboard.model.KeyboardLayout) = KeyboardGeometry.resolve(
        layout = layout,
        width = 1080f,
        height = 726f,
        spec = spec,
    )

    @Test
    fun horizontalGapIsSplitBetweenAdjacentKeys() {
        val q = keyboard.rows.first()[0]
        val w = keyboard.rows.first()[1]
        val midpoint = (q.bounds.right + w.bounds.left) / 2f

        assertEquals(q, keyboard.keyAt(midpoint - TestOffset, q.bounds.centerY))
        assertEquals(w, keyboard.keyAt(midpoint + TestOffset, w.bounds.centerY))
    }

    @Test
    fun verticalGapIsSplitBetweenAdjacentRows() {
        val topRow = keyboard.rows[0]
        val homeRow = keyboard.rows[1]
        val midpoint = (topRow.first().bounds.bottom + homeRow.first().bounds.top) / 2f
        val x = topRow[4].bounds.centerX

        assertTrue(keyboard.keyAt(x, midpoint - TestOffset) in topRow)
        assertTrue(keyboard.keyAt(x, midpoint + TestOffset) in homeRow)
    }

    @Test
    fun outerKeysReachSurfaceEdges() {
        val topRow = keyboard.rows.first()
        val y = topRow.first().bounds.centerY

        assertEquals(topRow.first(), keyboard.keyAt(0f, y))
        assertEquals(topRow.last(), keyboard.keyAt(keyboard.width, y))
    }

    @Test
    fun suggestionTextAreaDoesNotResolveToAKey() {
        val bounds = requireNotNull(keyboardWithSuggestions.suggestionBar).suggestionsBounds

        assertNull(keyboardWithSuggestions.keyAt(bounds.centerX, bounds.centerY))
    }

    @Test
    fun settingsTargetAbsorbsGapBetweenSuggestionsAndEmoji() {
        val bar = requireNotNull(keyboardWithSuggestions.suggestionBar)
        val midpoint = (bar.suggestionsBounds.right + bar.settingsKey.bounds.left) / 2f

        assertNull(keyboardWithSuggestions.keyAt(midpoint - TestOffset, bar.bounds.centerY))
        assertEquals(
            bar.settingsKey,
            keyboardWithSuggestions.keyAt(midpoint + TestOffset, bar.bounds.centerY),
        )
    }

    @Test
    fun emojiTargetAbsorbsItsAdjacentGap() {
        val bar = requireNotNull(keyboardWithSuggestions.suggestionBar)
        val midpoint = (bar.settingsKey.bounds.right + bar.emojiKey.bounds.left) / 2f

        assertEquals(
            bar.settingsKey,
            keyboardWithSuggestions.keyAt(midpoint - TestOffset, bar.bounds.centerY),
        )
        assertEquals(
            bar.emojiKey,
            keyboardWithSuggestions.keyAt(midpoint + TestOffset, bar.bounds.centerY),
        )
    }

    @Test
    fun coordinatesOutsideSurfaceDoNotResolveToAKey() {
        assertNull(keyboard.keyAt(-TestOffset, keyboard.height / 2f))
        assertNull(keyboard.keyAt(keyboard.width + TestOffset, keyboard.height / 2f))
        assertNull(keyboard.keyAt(keyboard.width / 2f, keyboard.height + TestOffset))
    }

    private companion object {
        const val TestOffset = 0.1f
    }
}
