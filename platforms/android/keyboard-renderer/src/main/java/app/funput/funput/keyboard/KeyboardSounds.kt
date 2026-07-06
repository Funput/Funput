package app.funput.funput.keyboard

import android.media.AudioManager
import android.view.View

/** Plays the system key-click sound while respecting Funput and ringer settings. */
object KeyboardSounds {
    fun perform(view: View, type: KeyboardHapticType) {
        if (!view.isSoundEffectsEnabled) return
        val audioManager = view.context.getSystemService(AudioManager::class.java)
        if (!shouldPlay(audioManager.ringerMode)) return
        audioManager.playSoundEffect(soundEffect(type), KeyClickVolume)
    }

    internal fun shouldPlay(ringerMode: Int): Boolean =
        ringerMode == AudioManager.RINGER_MODE_NORMAL

    internal fun soundEffect(type: KeyboardHapticType): Int = when (type) {
        KeyboardHapticType.KEY_PRESS,
        KeyboardHapticType.CONTROL,
        -> AudioManager.FX_KEYPRESS_STANDARD
        KeyboardHapticType.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
        KeyboardHapticType.DELETE,
        KeyboardHapticType.DELETE_REPEAT,
        -> AudioManager.FX_KEYPRESS_DELETE
        KeyboardHapticType.SUBMIT -> AudioManager.FX_KEYPRESS_RETURN
    }

    private const val KeyClickVolume = 0.5f
}
