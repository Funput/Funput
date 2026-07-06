package app.funput.funput.keyboard

import android.view.HapticFeedbackConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyboardHapticsTest {
    @Test
    fun `uses quiet repeated delete only when supported`() {
        assertNull(KeyboardHaptics.feedbackConstant(KeyboardHapticType.DELETE_REPEAT, sdkInt = 33))
        assertEquals(
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK,
            KeyboardHaptics.feedbackConstant(KeyboardHapticType.DELETE_REPEAT, sdkInt = 34),
        )
    }

    @Test
    fun `submit falls back on older Android versions`() {
        assertEquals(
            HapticFeedbackConstants.VIRTUAL_KEY,
            KeyboardHaptics.feedbackConstant(KeyboardHapticType.SUBMIT, sdkInt = 29),
        )
        assertEquals(
            HapticFeedbackConstants.CONFIRM,
            KeyboardHaptics.feedbackConstant(KeyboardHapticType.SUBMIT, sdkInt = 30),
        )
    }
}
