package app.funput.funput.keyboard

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
 * Plays one predefined vibration for a key, the way [View.performHapticFeedback] would have:
 * nothing when the view has haptics off or the system has touch feedback off, and touch usage so
 * the system's own touch intensity still scales it.
 */
internal object KeyboardVibrator {
    /** Returns false when it could not vibrate, so the caller can fall back to system feedback. */
    fun vibrate(view: View, effectId: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        if (!view.isHapticFeedbackEnabled || !systemTouchFeedbackEnabled(view.context)) return true
        val vibrator = vibrator(view.context)?.takeIf { it.hasVibrator() } ?: return false
        val effect = VibrationEffect.createPredefined(effectId)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, TouchAudioAttributes)
            }
        }.isSuccess
    }

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
