package app.funput.funput.ime.suggestions.bar

import app.funput.funput.ime.editing.correction.preview.CorrectionPreview
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.suggestion.SuggestionChip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SuggestionBarStateTest {
    private val shown = mutableListOf<List<SuggestionChip>>()
    private val bar = SuggestionBarState { shown += it }

    @Test
    fun aSuggestionSlotMapsBackToItsSourceIndex() {
        bar.showPersonal(listOf("đường", "đi"))
        bar.showCorrection(CorrectionPreview.Offered("đườnh", listOf("đường")))
        // Slot 2 shows "đi", which is index 1 in the list the personal service owns.
        val selection = bar.personalSelection(SuggestionSelection(2, "đi"))
        assertEquals(SuggestionSelection(1, "đi"), selection)
        assertNull(bar.personalSelection(SuggestionSelection(0, "khác")))
    }

    @Test
    fun anUnchangedBarIsNotRedrawn() {
        bar.showPersonal(listOf("đi"))
        bar.showCorrection(null)
        bar.showPersonal(listOf("đi"))
        assertEquals(1, shown.size)
    }
}
