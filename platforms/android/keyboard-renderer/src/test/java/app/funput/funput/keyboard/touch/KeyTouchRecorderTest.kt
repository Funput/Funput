package app.funput.funput.keyboard.touch

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.touch.KeyTouchTestKeyboard.PitchX
import app.funput.funput.keyboard.touch.KeyTouchTestKeyboard.centre
import app.funput.funput.keyboard.touch.KeyTouchTestKeyboard.keyboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyTouchRecorderTest {
    private val recorder = KeyTouchRecorder { keyboard() }

    private fun lift(pointerId: Int, keyId: String, action: KeyAction): KeyAction {
        var attached: KeyAction = action
        recorder.releasing(pointerId, keyId) { attached = recorder.attach(action) }
        return attached
    }

    @Test
    fun measuredAtTouchDown() {
        // The finger came down off-centre and rolled onto the middle before lifting: the
        // evidence is where it landed.
        val (x, y) = centre(1)
        recorder.down(0, x + PitchX * 0.4f, y)
        val touch = (lift(0, "key-w", KeyAction.Input("key-w", "w")) as KeyAction.Input).touch!!
        assertEquals(0.4f, touch.typedDistance, 1e-3f)
    }

    @Test
    fun shiftedLetterStillCarriesTheTouch() {
        val (x, y) = centre(1)
        recorder.down(0, x, y)
        val action = lift(0, "key-w", KeyAction.Input("key-w", "W")) as KeyAction.Input
        assertEquals('w'.code, action.touch!!.typed)
    }

    @Test
    fun alternateTextHasNoTouch() {
        // A popup alternate is typed by the same lift, but it is not the key's own letter.
        val (x, y) = centre(2)
        recorder.down(0, x, y)
        val action = lift(0, "key-e", KeyAction.Input("key-e", "ê")) as KeyAction.Input
        assertNull(action.touch)
    }

    @Test
    fun accessibilityClickHasNoTouch() {
        val action = recorder.attach(KeyAction.Input("key-w", "w")) as KeyAction.Input
        assertNull(action.touch)
    }

    @Test
    fun rolloverKeepsPointersApart() {
        val (wx, wy) = centre(1)
        val (ex, ey) = centre(2)
        recorder.down(0, wx, wy)
        recorder.down(1, ex + PitchX * 0.3f, ey)
        val first = lift(0, "key-w", KeyAction.Input("key-w", "w")) as KeyAction.Input
        val second = lift(1, "key-e", KeyAction.Input("key-e", "e")) as KeyAction.Input
        assertEquals(0f, first.touch!!.typedDistance, 1e-4f)
        assertEquals(0.3f, second.touch!!.typedDistance, 1e-3f)
    }

    @Test
    fun aLiftIsSpentOnce() {
        val (x, y) = centre(1)
        recorder.down(0, x, y)
        lift(0, "key-w", KeyAction.Input("key-w", "w"))
        val again = lift(0, "key-w", KeyAction.Input("key-w", "w")) as KeyAction.Input
        assertNull(again.touch)
    }
}
