package app.funput.funput.keyboard.suggestion

import app.funput.funput.keyboard.interaction.SuggestionTargetIds
import app.funput.funput.keyboard.interaction.selectionForTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionChipTest {
    private val typed = SuggestionChip("đườnh", SuggestionChipKind.CORRECTION_TYPED)
    private val candidate = SuggestionChip("đường", SuggestionChipKind.CORRECTION_CANDIDATE)
    private val undo = SuggestionChip("đườnh", SuggestionChipKind.CORRECTION_UNDO)
    private val shortcut = SuggestionChip("vn", SuggestionChipKind.SHORTCUT_UNDO)

    @Test
    fun titlesQuoteTheTypedWordAndMarkTheUndo() {
        assertEquals("“đườnh”", typed.title)
        assertEquals("đường", candidate.title)
        assertEquals("↩ đườnh", undo.title)
        assertEquals("↩ vn", shortcut.title)
        assertEquals("xin", SuggestionChip("xin").title)
    }

    @Test
    fun labelsNameWhatATapDoes() {
        assertEquals("Giữ nguyên, đườnh", typed.accessibilityLabel)
        assertEquals("Sửa thành, đường", candidate.accessibilityLabel)
        assertEquals("Hoàn tác, đườnh", undo.accessibilityLabel)
        assertEquals("Hoàn tác gõ tắt, vn", shortcut.accessibilityLabel)
        assertEquals("Gợi ý, xin", SuggestionChip("xin").accessibilityLabel)
    }

    @Test
    fun theRepairIsAccentedNeverTheTypedWord() {
        assertEquals(1, listOf(typed, candidate, SuggestionChip("xin")).accentIndex())
        assertEquals(1, listOf(undo, SuggestionChip("xin")).accentIndex())
        assertEquals(1, listOf(shortcut, SuggestionChip("xin")).accentIndex())
        assertEquals(-1, listOf(shortcut).accentIndex())
        assertEquals(0, listOf("xin", "chào").asSuggestionChips().accentIndex())
        assertEquals(-1, listOf(undo).accentIndex())
    }

    @Test
    fun aSelectionCarriesTheChipKind() {
        val selection = listOf(typed, candidate).selectionForTarget(SuggestionTargetIds.id(1))!!
        assertEquals(SuggestionChipKind.CORRECTION_CANDIDATE, selection.kind)
        assertEquals("đường", selection.text)
    }
}
