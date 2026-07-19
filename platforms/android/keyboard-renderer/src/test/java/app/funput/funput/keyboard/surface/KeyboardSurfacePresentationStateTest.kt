package app.funput.funput.keyboard.surface

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardSurfacePresentationStateTest {
    @Test
    fun `candidate update invalidates without theme or geometry rebuild`() {
        var themeChanges = 0
        var sizingChanges = 0
        var suggestionChanges = 0
        val state = KeyboardSurfacePresentationState(
            onThemeChanged = { themeChanges++ },
            onSizingChanged = { sizingChanges++ },
            onBackgroundImageChanged = {},
            onSuggestionsChanged = { suggestionChanges++ },
            onEnterActionChanged = {},
        )

        state.suggestions = listOf("xin", "chào", "bạn")
        state.suggestions = listOf("xin", "chào", "bạn")

        assertEquals(1, suggestionChanges)
        assertEquals(0, themeChanges)
        assertEquals(0, sizingChanges)
    }
}
