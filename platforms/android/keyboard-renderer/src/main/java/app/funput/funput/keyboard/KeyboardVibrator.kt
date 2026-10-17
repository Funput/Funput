package app.funput.funput.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.View

/**
 * Plays one key vibration the way [View.performHapticFeedback] would have: nothing when the view
 * has haptics off or the system has touch feedback off, and touch usage so the system's own touch
 * intensity still scales it.
 */
internal object KeyboardVibrator {
    @Volatile
    private var capabilities: KeyboardVibratorCapabilities? = null

    /**
     * Plays what [choose] picks for this device. Returns false when nothing could be played, so
     * the caller can fall back to system feedback.
     */
    fun vibrate(view: View, choose: (KeyboardVibratorCapabilities) -> KeyboardVibration?): Boolean {
        if (!view.isHapticFeedbackEnabled || !systemTouchFeedbackEnabled(view.context)) return true
        val vibrator = vibrator(view.context)?.takeIf { it.hasVibrator() } ?: return false
        val effect = choose(capabilitiesOf(vibrator))?.let(::effectOf) ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, TouchAudioAttributes)
            }
        }.isSuccess
    }

    private fun effectOf(vibration: KeyboardVibration): VibrationEffect? = when (vibration) {
        is KeyboardVibration.Primitive -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            VibrationEffect.startComposition().addPrimitive(vibration.primitiveId, vibration.scale).compose()
        } else {
            null
        }
        is KeyboardVibration.Pulse -> VibrationEffect.createOneShot(vibration.durationMs, vibration.amplitude)
        is KeyboardVibration.Predefined -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(vibration.effectId)
        } else {
            null
        }
    }

    /** Read once: the vibrator does not change while the process lives. */
    private fun capabilitiesOf(vibrator: Vibrator): KeyboardVibratorCapabilities = capabilities ?: run {
        val primitives = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            KeyPrimitives.filter { vibrator.areAllPrimitivesSupported(it) }.toSet()
        } else {
            emptySet()
        }
        KeyboardVibratorCapabilities(Build.VERSION.SDK_INT, primitives, vibrator.hasAmplitudeControl())
            .also { capabilities = it }
    }

    // Inlined from API 30; only read behind the sdkInt check in capabilitiesOf.
    @SuppressLint("InlinedApi")
    private val KeyPrimitives = listOf(
        VibrationEffect.Composition.PRIMITIVE_CLICK,
        VibrationEffect.Composition.PRIMITIVE_TICK,
    )

    private val TouchAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun systemTouchFeedbackEnabled(context: Context): Boolean =
        Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
}
