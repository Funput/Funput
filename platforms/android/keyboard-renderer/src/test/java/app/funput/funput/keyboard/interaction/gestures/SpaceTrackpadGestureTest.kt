package app.funput.funput.keyboard.interaction.gestures

import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceTrackpadGestureTest {
    private val space = KeySpec(
        id = "space",
        label = "Tiếng Việt",
        role = KeyRole.SPACE,
        horizontalSwipeAction = KeySwipeAction.TOGGLE_LANGUAGE,
    )

    @Test
    fun aQuickSwipeStillTogglesTheLanguage() {
        val subject = subject()
        subject.begin(space)
        subject.move(space, 180f)
        subject.release(space, 180f)

        assertTrue(subject.actions.any { it is KeyAction.ToggleLanguage })
        assertFalse(subject.actions.any { it is KeyAction.MoveCursor })
    }

    @Test
    fun holdingThenDraggingMovesTheCaretInstead() {
        val subject = subject()
        subject.begin(space)
        subject.scheduler.fire(after = 350L)
        subject.move(space, 145f)

        assertEquals(listOf(KeyAction.MoveCursor(2)), subject.actions)
        assertTrue(subject.captured.contains(1))
        assertEquals(listOf(KeyboardHapticType.SPACE, KeyboardHapticType.CONTROL, KeyboardHapticType.DELETE_REPEAT), subject.haptics)
    }

    @Test
    fun theCaretFollowsTheFingerBackAndForth() {
        val subject = subject()
        subject.begin(space)
        subject.scheduler.fire(after = 350L)
        subject.move(space, 150f)
        subject.move(space, 130f)

        assertEquals(listOf(KeyAction.MoveCursor(3), KeyAction.MoveCursor(-2)), subject.actions)
    }

    @Test
    fun withSmartGesturesOffTheSpacebarSwipesAsBefore() {
        val subject = subject()
        subject.controller.areSmartGesturesEnabled = false
        subject.begin(space)
        subject.move(space, 145f)
        subject.release(space, 145f)

        assertTrue(subject.actions.any { it is KeyAction.ToggleLanguage })
        assertFalse(subject.actions.any { it is KeyAction.MoveCursor })
    }

    @Test
    fun releasingATrackpadDragCommitsNoSpace() {
        val subject = subject()
        subject.begin(space)
        subject.scheduler.fire(after = 350L)
        subject.move(space, 150f)
        subject.release(space, 150f)

        assertFalse(subject.actions.any { it == KeyAction.Space })
        assertTrue(subject.actions.any { it is KeyAction.MoveCursor })
    }
}

private fun subject() = GestureControllerSubject()
