package app.funput.funput.keyboard

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** Performs system-defined haptics while respecting device and user settings. */
object KeyboardHaptics {
    fun perform(view: View, type: KeyboardHapticType): Boolean {
        val constant = feedbackConstant(type, Build.VERSION.SDK_INT) ?: return false
        return view.performHapticFeedback(constant)
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
