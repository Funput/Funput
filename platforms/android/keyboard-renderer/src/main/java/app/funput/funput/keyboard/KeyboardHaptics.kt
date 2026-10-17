package app.funput.funput.keyboard

import android.annotation.SuppressLint
import android.os.Build
import android.os.VibrationEffect
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Key feedback at the strength the user chose, falling back to the system-defined feedback where
 * Android cannot play a predefined effect (before Android 10, or with no vibrator).
 */
object KeyboardHaptics {
    /**
     * The chosen strength, or null for system-defined feedback. One setting of the one keyboard,
     * so it is kept here rather than handed to every panel that vibrates.
     */
    @Volatile
    var strength: KeyboardHapticStrength? = null

    fun perform(view: View, type: KeyboardHapticType): Boolean {
        val sdkInt = Build.VERSION.SDK_INT
        strength?.let { chosen ->
            val effect = effectId(type, chosen, sdkInt)
            if (effect != null && KeyboardVibrator.vibrate(view, effect)) return true
        }
        val constant = feedbackConstant(type, sdkInt) ?: return false
        return view.performHapticFeedback(constant)
    }

    /** One key press at [strength], for a settings screen letting the user feel each choice. */
    fun performSample(view: View, strength: KeyboardHapticStrength): Boolean {
        val effect = effectId(KeyboardHapticType.KEY_PRESS, strength, Build.VERSION.SDK_INT) ?: return false
        return KeyboardVibrator.vibrate(view, effect)
    }

    /**
     * The predefined effect for [type] at [strength]. Backspace repeat stays a tick at every
     * strength: many full clicks a second read as a buzz.
     */
    // The effect constants are inlined from API 29, which the sdkInt check above them guards.
    @SuppressLint("InlinedApi")
    internal fun effectId(type: KeyboardHapticType, strength: KeyboardHapticStrength, sdkInt: Int): Int? {
        if (sdkInt < Build.VERSION_CODES.Q) return null
        if (type == KeyboardHapticType.DELETE_REPEAT) return VibrationEffect.EFFECT_TICK
        return when (strength) {
            KeyboardHapticStrength.LIGHT -> VibrationEffect.EFFECT_TICK
            KeyboardHapticStrength.MEDIUM -> VibrationEffect.EFFECT_CLICK
            KeyboardHapticStrength.STRONG -> VibrationEffect.EFFECT_HEAVY_CLICK
        }
    }

    internal fun feedbackConstant(type: KeyboardHapticType, sdkInt: Int): Int? = when (type) {
        KeyboardHapticType.KEY_PRESS -> HapticFeedbackConstants.KEYBOARD_PRESS
        KeyboardHapticType.SPACE -> HapticFeedbackConstants.KEYBOARD_PRESS
        KeyboardHapticType.CONTROL -> HapticFeedbackConstants.VIRTUAL_KEY
        KeyboardHapticType.DELETE -> HapticFeedbackConstants.VIRTUAL_KEY
        KeyboardHapticType.DELETE_REPEAT -> if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            null
        }
        KeyboardHapticType.SUBMIT -> if (sdkInt >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
    }
}
