package app.funput.funput.ime.editing

import android.text.TextUtils
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoCapitalizationControllerTest {
    private var cursorCapsMode = 0
    private var shiftState = ShiftState.OFF
    private val controller = AutoCapitalizationController(
        cursorCapsMode = { modes -> if (modes == 0) 0 else cursorCapsMode },
        currentShiftState = { shiftState },
        updateShiftState = { value -> shiftState = value },
    )

    @Test
    fun `cursor caps mode enables and disables one shot shift`() {
        controller.configure(capitalizationModes = TextUtils.CAP_MODE_SENTENCES)
        cursorCapsMode = TextUtils.CAP_MODE_SENTENCES
        controller.update()
        assertEquals(ShiftState.ON, shiftState)

        cursorCapsMode = 0
        controller.update()
        assertEquals(ShiftState.OFF, shiftState)
    }

    @Test
    fun `disabling the preference silences sentence and word modes`() {
        controller.configure(capitalizationModes = TextUtils.CAP_MODE_SENTENCES)
        cursorCapsMode = TextUtils.CAP_MODE_SENTENCES
        controller.setEnabled(false)

        controller.update()

        assertEquals(ShiftState.OFF, shiftState)
    }

    @Test
    fun `a field demanding all caps still shifts when the preference is off`() {
        controller.configure(capitalizationModes = TextUtils.CAP_MODE_CHARACTERS)
        cursorCapsMode = TextUtils.CAP_MODE_CHARACTERS
        controller.setEnabled(false)

        controller.update()

        assertEquals(ShiftState.ON, shiftState)
    }

    @Test
    fun `selection updates preserve manual caps lock`() {
        shiftState = ShiftState.CAPS_LOCK
        controller.configure(capitalizationModes = TextUtils.CAP_MODE_SENTENCES)

        controller.update()

        assertEquals(ShiftState.CAPS_LOCK, shiftState)
    }

    @Test
    fun `new editor resets caps lock to its requested state`() {
        shiftState = ShiftState.CAPS_LOCK
        controller.configure(capitalizationModes = 0)

        controller.update(preserveCapsLock = false)

        assertEquals(ShiftState.OFF, shiftState)
    }
}
