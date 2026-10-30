package app.funput.funput.keyboard.interaction.gestures

import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardEnterAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnterLineBreakHoldTest {
    private val enter = KeySpec(id = "enter", label = "", role = KeyRole.ENTER, accessibilityLabel = "Enter")

    @Test
    fun holdingASendKeyBreaksTheLineAndDoesNotSend() {
        val subject = subject(KeyboardEnterAction.Standard.SEND)
        subject.begin(enter)
        subject.scheduler.fire(after = 350L)
        subject.release(enter, 120f)

        assertEquals(listOf<KeyAction>(KeyAction.NewLine), subject.actions)
        assertTrue(subject.captured.contains(1))
        assertEquals(KeyboardHapticType.CONTROL, subject.haptics.last())
    }

    @Test
    fun aTapOnASendKeyStillSends() {
        val subject = subject(KeyboardEnterAction.Standard.SEND)
        subject.begin(enter)
        subject.release(enter, 120f)

        assertEquals(listOf<KeyAction>(KeyAction.Enter), subject.actions)
        assertFalse(subject.scheduler.hasPending(after = 350L))
    }

    @Test
    fun aNewLineKeyHasNothingToHoldFor() {
        val subject = subject(KeyboardEnterAction.Standard.NEW_LINE)
        subject.begin(enter)

        assertFalse(subject.scheduler.hasPending(after = 350L))
        subject.release(enter, 120f)
        assertEquals(listOf<KeyAction>(KeyAction.Enter), subject.actions)
    }

    @Test
    fun slidingOffBeforeTheHoldFiresIsNotAHold() {
        val subject = subject(KeyboardEnterAction.Standard.SEND)
        subject.begin(enter)
        subject.move(enter, 160f)

        assertFalse(subject.scheduler.hasPending(after = 350L))
    }

    @Test
    fun theHoldDoesNotNeedSmartGestures() {
        val subject = subject(KeyboardEnterAction.Standard.SEND)
        subject.controller.areSmartGesturesEnabled = false
        subject.begin(enter)
        subject.scheduler.fire(after = 350L)
        subject.release(enter, 120f)

        assertEquals(listOf<KeyAction>(KeyAction.NewLine), subject.actions)
    }

    private fun subject(action: KeyboardEnterAction) =
        GestureControllerSubject().also { it.controller.enterAction = action }
}
