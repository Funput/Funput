package app.funput.funput.keyboard.suggestion.actions

import app.funput.funput.keyboard.interaction.SuggestionTargetIds
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.suggestion.SuggestionChip
import app.funput.funput.keyboard.suggestion.SuggestionChipKind
import app.funput.funput.keyboard.suggestion.asSuggestionChips
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionActionsTest {
    private val words = listOf("xin", "chào", "bạn").asSuggestionChips()
    private val first = SuggestionTargetIds.id(0)
    private val delay = SuggestionHold.HoldDelayMillis

    @Test
    fun holdingALearnedWordOpensItsActionsAndKeepsTheReleaseFromPickingIt() {
        val bar = SuggestionBarFixture(words)
        bar.hold()

        assertEquals(listOf(1), bar.captured)
        assertTrue(bar.subject.isCaptured(1))
        assertEquals(1, bar.feedback)
        assertEquals("“xin”", bar.subject.segments(SuggestionBarFixture.keyboard).first().title)
        assertNull(bar.subject.released(1, null))
        assertFalse(bar.subject.isCaptured(1))
        assertTrue(bar.subject.segments(SuggestionBarFixture.keyboard).isNotEmpty())
    }

    @Test
    fun aTapIsStillATap() {
        val bar = SuggestionBarFixture(words)
        bar.subject.started(1, first, 10f, 10f)

        assertEquals(first, bar.subject.released(1, first))
        assertFalse(bar.scheduler.hasPending(after = delay))
        assertTrue(bar.subject.segments(SuggestionBarFixture.keyboard).isEmpty())
    }

    @Test
    fun slidingOffOrASecondFingerIsTypingNotHolding() {
        val bar = SuggestionBarFixture(words)
        bar.subject.started(1, first, 10f, 10f)
        bar.subject.moved(1, 10f + SuggestionBarFixture.TouchSlop + 1f, 10f)
        assertFalse(bar.scheduler.hasPending(after = delay))

        bar.subject.started(2, first, 10f, 10f)
        bar.subject.started(3, "character-a", 50f, 50f)
        assertFalse(bar.scheduler.hasPending(after = delay))
    }

    @Test
    fun aWordTheHostCannotEditStaysAnOrdinaryTap() {
        val bar = SuggestionBarFixture(words, editable = { false })
        bar.hold()

        assertTrue(bar.captured.isEmpty())
        assertEquals(first, bar.subject.released(1, first))
    }

    @Test
    fun repairsAndUndosAreNeverHeld() {
        val chips = listOf(SuggestionChip("xin", SuggestionChipKind.CORRECTION_CANDIDATE))
        val bar = SuggestionBarFixture(chips)
        bar.subject.started(1, first, 10f, 10f)

        assertFalse(bar.scheduler.hasPending(after = delay))
    }

    @Test
    fun eachSlotOfTheStripActsOnTheWordOnce() {
        val bar = SuggestionBarFixture(words)
        bar.hold()
        bar.subject.released(1, null)
        val block = SuggestionActionLayout.targetId(SuggestionAction.BLOCK)

        assertEquals(block, bar.targetAtCentreOf(block))
        assertNull(bar.subject.released(2, block))
        assertEquals(listOf(SuggestionSelection(0, "xin") to SuggestionAction.BLOCK), bar.actions)
        assertTrue(bar.subject.segments(SuggestionBarFixture.keyboard).isEmpty())
    }

    @Test
    fun theWordLabelDoesNothingAndCloseActsOnNothing() {
        val bar = SuggestionBarFixture(words)
        bar.hold()
        bar.subject.released(1, null)

        assertNull(bar.subject.released(2, SuggestionActionLayout.WordTargetId))
        assertTrue(bar.subject.segments(SuggestionBarFixture.keyboard).isNotEmpty())
        assertNull(bar.subject.released(3, SuggestionActionLayout.CloseTargetId))
        assertTrue(bar.subject.segments(SuggestionBarFixture.keyboard).isEmpty())
        assertTrue(bar.actions.isEmpty())
    }

    @Test
    fun typingAKeyClosesTheStripAndStillTypes() {
        val bar = SuggestionBarFixture(words)
        bar.hold()
        bar.subject.released(1, null)

        assertEquals("character-a", bar.subject.released(2, "character-a"))
        assertTrue(bar.subject.segments(SuggestionBarFixture.keyboard).isEmpty())
    }

    @Test
    fun theStripGivesWayWhenTheCandidatesChange() {
        val bar = SuggestionBarFixture(words)
        bar.hold()
        bar.suggestions = listOf("chào").asSuggestionChips()

        assertTrue(bar.subject.segments(SuggestionBarFixture.keyboard).isEmpty())
        assertNull(bar.subject.targetAt(bar.band.centerX, bar.band.centerY, SuggestionBarFixture.keyboard))
    }

    @Test
    fun talkBackActsWithoutOpeningTheStrip() {
        val bar = SuggestionBarFixture(words)
        bar.subject.perform(SuggestionTargetIds.id(1), SuggestionAction.FORGET)

        assertEquals(listOf(SuggestionSelection(1, "chào") to SuggestionAction.FORGET), bar.actions)
        assertEquals(0, bar.changes)
    }
}
