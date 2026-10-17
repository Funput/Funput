package app.funput.funput.keyboard

import android.annotation.SuppressLint
import android.os.Build
import android.os.VibrationEffect
import android.view.HapticFeedbackConstants
import android.view.View
import kotlin.math.roundToInt

/**
 * Key feedback at the strength the user chose, falling back to the system-defined feedback where
 * Android cannot play one (before Android 10, or with no vibrator).
 */
object KeyboardHaptics {
    /**
     * The chosen strength, or null for system-defined feedback. One setting of the one keyboard,
     * so it is kept here rather than handed to every panel that vibrates.
     */
    @Volatile
    var strength: KeyboardHapticStrength? = null

    fun perform(view: View, type: KeyboardHapticType): Boolean {
        strength?.let { chosen -> if (KeyboardVibrator.vibrate(view) { vibrationFor(type, chosen, it) }) return true }
        val constant = feedbackConstant(type, Build.VERSION.SDK_INT) ?: return false
        return view.performHapticFeedback(constant)
    }

    /** One key press at [strength], for a settings screen letting the user feel each choice. */
    fun performSample(view: View, strength: KeyboardHapticStrength): Boolean =
        KeyboardVibrator.vibrate(view) { vibrationFor(KeyboardHapticType.KEY_PRESS, strength, it) }

    /** How strongly each strength plays the same waveform: the intensities iOS uses. */
    internal fun scale(strength: KeyboardHapticStrength): Float = when (strength) {
        KeyboardHapticStrength.LIGHT -> 0.55f
        KeyboardHapticStrength.MEDIUM -> 0.75f
        KeyboardHapticStrength.STRONG -> 1f
    }

    /**
     * The vibration for [type] at [strength] on a device that can do [device]: one click primitive
     * scaled by strength, else a pulse scaled by strength, else a predefined effect. Backspace
     * repeat stays a soft tick at every strength: many full clicks a second read as a buzz.
     */
    // The primitive and effect constants are inlined from API 29 and 30; sdkInt guards both.
    @SuppressLint("InlinedApi")
    internal fun vibrationFor(
        type: KeyboardHapticType,
        strength: KeyboardHapticStrength,
        device: KeyboardVibratorCapabilities,
    ): KeyboardVibration? {
        val repeat = type == KeyboardHapticType.DELETE_REPEAT
        val scale = if (repeat) RepeatScale else scale(strength)
        val primitive = if (repeat) PrimitiveTick else PrimitiveClick
        return when {
            device.sdkInt >= Build.VERSION_CODES.R && primitive in device.supportedPrimitives ->
                KeyboardVibration.Primitive(primitive, scale)
            device.hasAmplitudeControl ->
                KeyboardVibration.Pulse(PulseMs, (MaxAmplitude * scale).roundToInt().coerceIn(1, MaxAmplitude))
            device.sdkInt >= Build.VERSION_CODES.Q -> KeyboardVibration.Predefined(predefinedEffect(repeat, strength))
            else -> null
        }
    }

    /**
     * Best effort for vibrators that can neither compose nor scale: the vendor's effects, whose
     * order of strength is not guaranteed.
     */
    @SuppressLint("InlinedApi")
    private fun predefinedEffect(repeat: Boolean, strength: KeyboardHapticStrength): Int = when {
        repeat -> VibrationEffect.EFFECT_TICK
        strength == KeyboardHapticStrength.LIGHT -> VibrationEffect.EFFECT_TICK
        strength == KeyboardHapticStrength.MEDIUM -> VibrationEffect.EFFECT_CLICK
        else -> VibrationEffect.EFFECT_HEAVY_CLICK
    }

    @SuppressLint("InlinedApi")
    private const val PrimitiveClick = VibrationEffect.Composition.PRIMITIVE_CLICK

    @SuppressLint("InlinedApi")
    private const val PrimitiveTick = VibrationEffect.Composition.PRIMITIVE_TICK

    private const val RepeatScale = 0.4f
    private const val PulseMs = 20L
    private const val MaxAmplitude = 255

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
