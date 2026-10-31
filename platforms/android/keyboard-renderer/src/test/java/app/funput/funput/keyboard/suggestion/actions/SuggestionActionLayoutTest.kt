package app.funput.funput.keyboard.suggestion.actions

import app.funput.funput.keyboard.accessibility.KeyboardAccessibilitySnapshot
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.suggestion.SuggestionBand
import app.funput.funput.keyboard.suggestion.SuggestionBandSegment.Tone
import app.funput.funput.keyboard.suggestion.asSuggestionChips
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionActionLayoutTest {
    private val bounds = KeyBounds(left = 10f, top = 0f, right = 410f, bottom = 40f)
    private val selection = SuggestionSelection(0, "xin")

    @Test
    fun theStripFillsTheBandWordFirstAndCloseLast() {
        val segments = SuggestionActionLayout.segments(selection, bounds)

        assertEquals(
            listOf("“xin”", "Không gợi ý", "Quên", "✕"),
            segments.map { it.title },
        )
        assertEquals(bounds.left, segments.first().left, 0.001f)
        assertEquals(bounds.right, segments.last().right, 0.001f)
        segments.zipWithNext { left, right -> assertEquals(left.right, right.left, 0.001f) }
        assertFalse(segments.first().pressable)
    }

    @Test
    fun targetsRoundTripToTheirActions() {
        SuggestionAction.entries.forEach { action ->
            val id = SuggestionActionLayout.targetId(action)
            assertEquals(action, SuggestionActionLayout.action(id))
            assertTrue(SuggestionActionLayout.owns(id))
        }
        assertTrue(SuggestionActionLayout.owns(SuggestionActionLayout.CloseTargetId))
        assertFalse(SuggestionActionLayout.owns("suggestion-0"))
        assertNull(SuggestionActionLayout.action(null))
    }

    @Test
    fun theOuterSlotsTakeTheSlackBesideTheBand() {
        val segments = SuggestionActionLayout.segments(selection, bounds)

        assertEquals(SuggestionActionLayout.WordTargetId, SuggestionBand.targetAt(segments, bounds.left - 5f))
        assertEquals(SuggestionActionLayout.CloseTargetId, SuggestionBand.targetAt(segments, bounds.right + 5f))
    }

    @Test
    fun theCandidatesShareTheBandEquallyAndAccentTheTopPick() {
        val chips = SuggestionBand.chips(listOf("xin", "chào").asSuggestionChips(), bounds)

        assertEquals(listOf(200f, 200f), chips.map { it.width })
        assertEquals(listOf("suggestion-0", "suggestion-1"), chips.map { it.targetId })
        assertEquals(listOf(Tone.ACCENT, Tone.PLAIN), chips.map { it.tone })
    }

    @Test
    fun talkBackOffersTheActionsOnlyOnWordsTheHostCanEdit() {
        val snapshot = KeyboardAccessibilitySnapshot(
            keyboard = SuggestionBarFixture.keyboard,
            shiftState = ShiftState.OFF,
            suggestions = listOf("xin", "chào").asSuggestionChips(),
            suggestionEditable = { it.text == "chào" },
        )
        val nodes = snapshot.nodes.filter { it.keyId.startsWith("suggestion-") }

        assertTrue(nodes[0].customActions.isEmpty())
        assertEquals(listOf("Không gợi ý từ này", "Quên từ này"), nodes[1].customActions.map { it.label })
        val ids = nodes[1].customActions.map { it.actionId }
        assertEquals(SuggestionAction.entries, ids.map { SuggestionAction.forAccessibilityAction(it) })
    }
}
