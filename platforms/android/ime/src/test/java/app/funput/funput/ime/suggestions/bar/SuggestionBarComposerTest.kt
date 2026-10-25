package app.funput.funput.ime.suggestions.bar

import app.funput.funput.ime.editing.correction.preview.CorrectionPreview
import app.funput.funput.keyboard.suggestion.SuggestionChip
import app.funput.funput.keyboard.suggestion.SuggestionChipKind.CORRECTION_CANDIDATE
import app.funput.funput.keyboard.suggestion.SuggestionChipKind.CORRECTION_TYPED
import app.funput.funput.keyboard.suggestion.SuggestionChipKind.CORRECTION_UNDO
import app.funput.funput.keyboard.suggestion.SuggestionChipKind.SHORTCUT_UNDO
import app.funput.funput.keyboard.suggestion.asSuggestionChips
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionBarComposerTest {
    private val personal = listOf("đường", "đi", "đây")

    @Test
    fun personalOnly() {
        assertEquals(personal.asSuggestionChips(), SuggestionBarComposer.compose(null, personal))
    }

    @Test
    fun offered() {
        val chips = SuggestionBarComposer.compose(CorrectionPreview.Offered("đườnh", listOf("đường")), personal)
        assertEquals(
            listOf(SuggestionChip("đườnh", CORRECTION_TYPED), SuggestionChip("đường", CORRECTION_CANDIDATE), SuggestionChip("đi")),
            chips,
        )
    }

    @Test
    fun ambiguous() {
        val chips = SuggestionBarComposer.compose(CorrectionPreview.Offered("nhad", listOf("nhà", "nhá")), personal)
        assertEquals(listOf(CORRECTION_TYPED, CORRECTION_CANDIDATE, CORRECTION_CANDIDATE), chips.map { it.kind })
    }

    @Test
    fun parked() {
        val chips = SuggestionBarComposer.compose(CorrectionPreview.Parked("đườnh", listOf("đường")), emptyList())
        assertEquals(listOf(SuggestionChip("đườnh", CORRECTION_TYPED), SuggestionChip("đường", CORRECTION_CANDIDATE)), chips)
    }

    @Test
    fun applied() {
        val chips = SuggestionBarComposer.compose(CorrectionPreview.Applied("đườnh"), personal)
        assertEquals(SuggestionChip("đườnh", CORRECTION_UNDO), chips.first())
        assertEquals(3, chips.size)
    }

    @Test
    fun deduplicated() {
        val chips = SuggestionBarComposer.compose(CorrectionPreview.Offered("đườnh", listOf("đường")), personal)
        assertEquals("a repair is not offered twice", 1, chips.count { it.text == "đường" })
    }

    @Test
    fun shortcutUndoLeadsWhenNothingIsBeingCorrected() {
        val chips = SuggestionBarComposer.compose(null, personal, "vn")
        assertEquals(SuggestionChip("vn", SHORTCUT_UNDO), chips.first())
        assertEquals(listOf("vn", "đường", "đi"), chips.map { it.text })
    }

    @Test
    fun aCorrectionKeepsTheShortcutUndoOffTheBar() {
        val chips = SuggestionBarComposer.compose(CorrectionPreview.Applied("đườnh"), personal, "vn")
        assertEquals(CORRECTION_UNDO, chips.first().kind)
    }

    @Test
    fun withoutPersonal() {
        val chips = SuggestionBarComposer.compose(CorrectionPreview.Offered("đườnh", listOf("đường")), emptyList())
        assertEquals(2, chips.size)
    }
}
