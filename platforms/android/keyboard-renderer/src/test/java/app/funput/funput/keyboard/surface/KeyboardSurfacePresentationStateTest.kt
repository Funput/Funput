package app.funput.funput.keyboard.surface

import app.funput.funput.keyboard.KeyboardClipboardHint
import app.funput.funput.keyboard.KeyboardClipboardOffer
import app.funput.funput.keyboard.suggestion.asSuggestionChips
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

        state.suggestions = listOf("xin", "chào", "bạn").asSuggestionChips()
        state.suggestions = listOf("xin", "chào", "bạn").asSuggestionChips()

        assertEquals(1, suggestionChanges)
        assertEquals(0, themeChanges)
        assertEquals(0, sizingChanges)
    }

    @Test
    fun `clipboard offer invalidates only when it changes`() {
        var changes = 0
        val state = KeyboardSurfacePresentationState(
            onThemeChanged = {}, onSizingChanged = {}, onBackgroundImageChanged = {},
            onSuggestionsChanged = {}, onClipboardOfferChanged = { changes++ },
            onEnterActionChanged = {},
        )
        state.clipboardOffer = KeyboardClipboardOffer(KeyboardClipboardHint.TEXT)
        state.clipboardOffer = KeyboardClipboardOffer(KeyboardClipboardHint.TEXT)
        state.clipboardOffer = KeyboardClipboardOffer(KeyboardClipboardHint.TEXT, preview = "xin chào")
        state.clipboardOffer = KeyboardClipboardOffer(KeyboardClipboardHint.TEXT, "xin chào", showsChip = false)
        state.clipboardOffer = KeyboardClipboardOffer(KeyboardClipboardHint.SENSITIVE)

        assertEquals(4, changes)
    }
}
