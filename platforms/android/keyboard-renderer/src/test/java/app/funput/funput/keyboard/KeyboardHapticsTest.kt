package app.funput.funput.keyboard

import android.os.VibrationEffect
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

    @Test
    fun `each strength plays its own predefined effect from Android 10`() {
        fun effect(strength: KeyboardHapticStrength, sdkInt: Int = 29) =
            KeyboardHaptics.effectId(KeyboardHapticType.KEY_PRESS, strength, sdkInt)

        assertEquals(VibrationEffect.EFFECT_TICK, effect(KeyboardHapticStrength.LIGHT))
        assertEquals(VibrationEffect.EFFECT_CLICK, effect(KeyboardHapticStrength.MEDIUM))
        assertEquals(VibrationEffect.EFFECT_HEAVY_CLICK, effect(KeyboardHapticStrength.STRONG))
        // Older versions have no predefined effects and keep the system feedback.
        assertNull(effect(KeyboardHapticStrength.STRONG, sdkInt = 28))
    }

    @Test
    fun `backspace repeat stays a tick at every strength`() {
        KeyboardHapticStrength.entries.forEach { strength ->
            assertEquals(
                VibrationEffect.EFFECT_TICK,
                KeyboardHaptics.effectId(KeyboardHapticType.DELETE_REPEAT, strength, sdkInt = 34),
            )
        }
    }

    @Test
    fun `stored strengths read back, and unknown ones fall back to medium`() {
        KeyboardHapticStrength.entries.forEach { strength ->
            assertEquals(strength, KeyboardHapticStrength.fromStorage(strength.storageId))
        }
        assertEquals(KeyboardHapticStrength.MEDIUM, KeyboardHapticStrength.fromStorage(null))
        assertEquals(KeyboardHapticStrength.MEDIUM, KeyboardHapticStrength.fromStorage("loud"))
    }
}
