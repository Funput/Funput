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

    private val composing = KeyboardVibratorCapabilities(
        sdkInt = 35,
        supportedPrimitives = setOf(
            VibrationEffect.Composition.PRIMITIVE_CLICK,
            VibrationEffect.Composition.PRIMITIVE_TICK,
        ),
        hasAmplitudeControl = true,
    )

    private fun press(strength: KeyboardHapticStrength, device: KeyboardVibratorCapabilities) =
        KeyboardHaptics.vibrationFor(KeyboardHapticType.KEY_PRESS, strength, device)

    @Test
    fun `strengths scale one click, so a stronger choice always vibrates harder`() {
        val clicks = KeyboardHapticStrength.entries.map { press(it, composing) as KeyboardVibration.Primitive }

        assertEquals(setOf(VibrationEffect.Composition.PRIMITIVE_CLICK), clicks.map { it.primitiveId }.toSet())
        assertEquals(listOf(0.55f, 0.75f, 1f), clicks.map { it.scale })
    }

    @Test
    fun `without primitives the pulse amplitude rises with strength`() {
        val scaling = KeyboardVibratorCapabilities(sdkInt = 29, hasAmplitudeControl = true)
        val pulses = KeyboardHapticStrength.entries.map { press(it, scaling) as KeyboardVibration.Pulse }

        assertEquals(listOf(140, 191, 255), pulses.map { it.amplitude })
    }

    @Test
    fun `vendor effects are the last resort, and nothing plays before Android 10 without amplitude control`() {
        val plain = KeyboardVibratorCapabilities(sdkInt = 29)

        assertEquals(
            KeyboardVibration.Predefined(VibrationEffect.EFFECT_HEAVY_CLICK),
            press(KeyboardHapticStrength.STRONG, plain),
        )
        assertNull(press(KeyboardHapticStrength.STRONG, KeyboardVibratorCapabilities(sdkInt = 28)))
    }

    @Test
    fun `backspace repeat stays a soft tick at every strength`() {
        KeyboardHapticStrength.entries.forEach { strength ->
            assertEquals(
                KeyboardVibration.Primitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f),
                KeyboardHaptics.vibrationFor(KeyboardHapticType.DELETE_REPEAT, strength, composing),
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
