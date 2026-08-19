package app.funput.funput.keyboard.interaction.gestures

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackspaceWordSwipeTest {
    private val backspace = KeySpec(id = "backspace", label = "Xóa", role = KeyRole.BACKSPACE)

    @Test
    fun rubbingLeftOneStepDeletesOneWord() {
        val subject = GestureControllerSubject()
        subject.begin(backspace)
        subject.move(backspace, 75f)

        assertEquals(listOf(KeyAction.DeleteWord), subject.actions)
        assertTrue(subject.captured.contains(1))
    }

    @Test
    fun rubbingFurtherDeletesFurtherWords() {
        val subject = GestureControllerSubject()
        subject.begin(backspace)
        subject.move(backspace, 75f)
        subject.move(backspace, 35f)
        subject.move(backspace, 20f)

        assertEquals(listOf(KeyAction.DeleteWord, KeyAction.DeleteWord), subject.actions)
    }

    @Test
    fun rubbingRightDoesNothing() {
        val subject = GestureControllerSubject()
        subject.begin(backspace)
        subject.move(backspace, 220f)

        assertTrue(subject.actions.isEmpty())
        assertTrue(subject.captured.isEmpty())
    }

    @Test
    fun aClaimedRubTooShortToDeleteAWordStillDeletesOneCharacter() {
        val subject = GestureControllerSubject()
        subject.begin(backspace)
        subject.move(backspace, 100f)
        subject.release(backspace, 100f)

        assertEquals(listOf(KeyAction.Backspace), subject.actions)
    }

    @Test
    fun aClaimedRubThatDeletedWordsCommitsNoExtraBackspace() {
        val subject = GestureControllerSubject()
        subject.begin(backspace)
        subject.move(backspace, 75f)
        subject.release(backspace, 75f)

        assertEquals(listOf(KeyAction.DeleteWord), subject.actions)
    }

    @Test
    fun onceBackspaceHasRepeatedRubbingLeftKeepsRepeatingCharacters() {
        val subject = GestureControllerSubject()
        subject.begin(backspace)
        subject.scheduler.fire(after = 400L)
        subject.move(backspace, 75f)

        assertTrue(subject.actions.all { it == KeyAction.Backspace })
        assertFalse(subject.actions.any { it == KeyAction.DeleteWord })
    }

    @Test
    fun withSmartGesturesOffALeftwardDragIsNotAWordDelete() {
        val subject = GestureControllerSubject()
        subject.controller.areSmartGesturesEnabled = false
        subject.begin(backspace)
        subject.move(backspace, 75f)
        subject.release(backspace, 75f)

        assertFalse(subject.actions.any { it == KeyAction.DeleteWord })
        assertEquals(listOf(KeyAction.Backspace), subject.actions)
    }
}
