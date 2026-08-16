package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeySwipeGestureTrackerTest {
    private val tracker = KeySwipeGestureTracker(thresholdPx = 32f)
    private val space = KeySpec(
        id = "space",
        label = "Tiếng Việt",
        role = KeyRole.SPACE,
        horizontalSwipeAction = KeySwipeAction.TOGGLE_LANGUAGE,
    )

    @Test
    fun rightSwipeIsRecognized() {
        tracker.start(pointerId = 3, key = space, x = 100f, y = 50f)

        assertEquals(
            KeySwipeAction.TOGGLE_LANGUAGE,
            tracker.finish(pointerId = 3, x = 140f, y = 52f),
        )
    }

    @Test
    fun leftSwipeIsRecognized() {
        tracker.start(pointerId = 3, key = space, x = 100f, y = 50f)

        assertEquals(
            KeySwipeAction.TOGGLE_LANGUAGE,
            tracker.finish(pointerId = 3, x = 60f, y = 48f),
        )
    }

    @Test
    fun movementBelowThresholdIsATap() {
        tracker.start(pointerId = 3, key = space, x = 100f, y = 50f)

        assertNull(tracker.finish(pointerId = 3, x = 120f, y = 50f))
    }

    @Test
    fun verticalMovementIsNotAHorizontalSwipe() {
        tracker.start(pointerId = 3, key = space, x = 100f, y = 50f)

        assertNull(tracker.finish(pointerId = 3, x = 135f, y = 90f))
    }

    @Test
    fun releaseOverAnotherKeyStillTriggersSwipeStartedFromSpace() {
        tracker.start(pointerId = 3, key = space, x = 100f, y = 50f)

        assertEquals(
            KeySwipeAction.TOGGLE_LANGUAGE,
            tracker.finish(pointerId = 3, x = 140f, y = 50f),
        )
    }

    @Test
    fun cancelClearsEveryPointer() {
        tracker.start(pointerId = 3, key = space, x = 100f, y = 50f)
        tracker.start(pointerId = 11, key = space, x = 200f, y = 50f)

        tracker.cancel()

        assertNull(tracker.finish(pointerId = 3, x = 140f, y = 50f))
        assertNull(tracker.finish(pointerId = 11, x = 160f, y = 50f))
    }
}
