package app.funput.funput.keyboard.interaction.gestures.cursor

import app.funput.funput.keyboard.interaction.gestures.GestureControllerSubject
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The vertical half of the spacebar trackpad. The horizontal half, and everything the two share
 * about arming and claiming, lives in `SpaceTrackpadGestureTest`.
 */
class SpaceTrackpadVerticalTest {
    private val space = KeySpec(
        id = "space",
        label = "Tiếng Việt",
        role = KeyRole.SPACE,
        horizontalSwipeAction = KeySwipeAction.TOGGLE_LANGUAGE,
    )

    @Test
    fun holdingThenDraggingStraightUpMovesTheCaretALineAtATime() {
        val subject = GestureControllerSubject()
        subject.begin(space)
        subject.scheduler.fire(after = HoldMillis)
        subject.move(space, 120f, GestureControllerSubject.RestY - 2 * LineStep)

        assertEquals(listOf(KeyAction.MoveCursor(columns = 0, lines = -2)), subject.actions)
        assertTrue(subject.captured.contains(1))
    }

    @Test
    fun aDiagonalDragReportsBothAxesInOneStep() {
        val subject = GestureControllerSubject()
        subject.begin(space)
        subject.scheduler.fire(after = HoldMillis)
        subject.move(space, 150f, GestureControllerSubject.RestY + LineStep)

        assertEquals(listOf(KeyAction.MoveCursor(columns = 3, lines = 1)), subject.actions)
    }

    @Test
    fun reversingVerticallyCostsNothingAndDoesNotDrift() {
        val subject = GestureControllerSubject()
        subject.begin(space)
        subject.scheduler.fire(after = HoldMillis)
        subject.move(space, 120f, GestureControllerSubject.RestY + 2 * LineStep)
        subject.move(space, 120f, GestureControllerSubject.RestY)

        assertEquals(
            listOf(
                KeyAction.MoveCursor(columns = 0, lines = 2),
                KeyAction.MoveCursor(columns = 0, lines = -2),
            ),
            subject.actions,
        )
    }

    @Test
    fun aQuickUpwardFlickIsNotATrackpadDrag() {
        val subject = GestureControllerSubject()
        subject.begin(space)
        subject.move(space, 120f, GestureControllerSubject.RestY - 3 * LineStep)

        // Wandering before the hold fires cancels it outright. That is what keeps a flick from
        // becoming a caret pan a moment later, and it is why there is no timer left to fire.
        assertFalse(subject.scheduler.hasPending(after = HoldMillis))
        subject.move(space, 120f, GestureControllerSubject.RestY - 4 * LineStep)
        assertFalse(subject.actions.any { it is KeyAction.MoveCursor })
    }

    private companion object {
        const val HoldMillis = 350L

        /** One line per 24px at density 1, matching SpaceCursorPanTracker's step height. */
        const val LineStep = 24f
    }
}
