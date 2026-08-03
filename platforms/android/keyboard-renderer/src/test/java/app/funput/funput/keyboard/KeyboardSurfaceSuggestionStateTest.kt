package app.funput.funput.keyboard

import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.layout.ResolvedSuggestionBar
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardSurfaceSuggestionStateTest {
    @Test
    fun emptyToNonEmptyFlipsShowSettingsAndRebuildsGeometry() {
        var geometryRebuilds = 0
        var applied = emptyList<String>()
        var keyboard = keyboardWithSuggestionsWidth(500f)
        val state = KeyboardSurfaceSuggestionState(
            density = { 1f },
            keyboard = { keyboard },
            apply = { applied = it },
            onShowSettingsChanged = {
                geometryRebuilds += 1
                keyboard = keyboardWithSuggestionsWidth(700f)
                // Mimic KeyboardSurfaceView.resolveGeometry -> geometryChanged.
            },
        )

        assertTrue(state.showSettings)
        state.update(listOf("xin", "chào"))
        // Caller must invoke geometryChanged after rebuild, same as the surface view.
        state.geometryChanged()

        assertFalse(state.showSettings)
        assertEquals(1, geometryRebuilds)
        assertEquals(listOf("xin", "chào"), applied)
    }

    @Test
    fun sameVisibilityOnlyRefreshesCandidates() {
        var geometryRebuilds = 0
        var applied = emptyList<String>()
        val state = KeyboardSurfaceSuggestionState(
            density = { 1f },
            keyboard = { keyboardWithSuggestionsWidth(900f) },
            apply = { applied = it },
            onShowSettingsChanged = { geometryRebuilds += 1 },
        )

        state.update(listOf("a", "b"))
        state.geometryChanged()
        state.update(listOf("a", "b", "c"))

        assertEquals(1, geometryRebuilds)
        assertFalse(state.showSettings)
        assertEquals(listOf("a", "b", "c"), applied)
    }

    @Test
    fun clearingCandidatesShowsSettingsAgain() {
        var geometryRebuilds = 0
        val state = KeyboardSurfaceSuggestionState(
            density = { 3f },
            keyboard = { keyboardWithSuggestionsWidth(500f) },
            apply = {},
            onShowSettingsChanged = { geometryRebuilds += 1 },
        )

        state.update(listOf("xin"))
        state.update(emptyList())

        assertTrue(state.showSettings)
        assertEquals(2, geometryRebuilds)
    }

    private fun keyboardWithSuggestionsWidth(width: Float): ResolvedKeyboard {
        val emoji = ResolvedKey(
            KeySpec("emoji", "", KeyRole.EMOJI, accessibilityLabel = "Emoji"),
            KeyBounds(900f, 0f, 1000f, 100f),
        )
        return ResolvedKeyboard(
            width = 1080f,
            height = 700f,
            suggestionBar = ResolvedSuggestionBar(
                bounds = KeyBounds(0f, 0f, 1080f, 100f),
                logoBounds = KeyBounds(0f, 0f, 80f, 80f),
                suggestionsBounds = KeyBounds(90f, 0f, 90f + width, 100f),
                systemInputMethodKey = null,
                settingsKey = null,
                emojiKey = emoji,
                suggestionsEnabled = true,
            ),
            rows = listOf(
                listOf(
                    ResolvedKey(
                        KeySpec("character-a", "a", KeyRole.CHARACTER),
                        KeyBounds(0f, 120f, 100f, 220f),
                    ),
                ),
            ),
        )
    }
}
