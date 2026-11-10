package app.funput.funput.keyboard

import android.content.Context
import android.media.AudioManager
import android.view.View
import app.funput.funput.keyboard.sound.KeyboardSoundChoice
import app.funput.funput.keyboard.sound.KeyboardSoundKind
import app.funput.funput.keyboard.sound.KeyboardSoundPool
import app.funput.funput.keyboard.sound.KeyboardSoundStyle

/**
 * Key sounds in the style and at the volume the user chose: the system keyboard click, or one of
 * the sounds Funput bundles. Whether a view sounds at all is its own [View.isSoundEffectsEnabled].
 */
object KeyboardSounds {
    /**
     * The chosen sound, or null for the system click at medium volume. One setting of the one
     * keyboard, so it is kept here rather than handed to every panel that sounds.
     */
    @Volatile
    var choice: KeyboardSoundChoice? = null
        private set

    private val pool by lazy { KeyboardSoundPool() }

    /** Makes [choice] the sound of every press, loading a bundled style ahead of the first one. */
    fun select(context: Context, choice: KeyboardSoundChoice?) {
        this.choice = choice
        if (choice != null && choice.style != KeyboardSoundStyle.SYSTEM) pool.load(context, choice.style)
    }

    fun perform(view: View, type: KeyboardHapticType) {
        if (!view.isSoundEffectsEnabled) return
        play(view, choice ?: KeyboardSoundChoice.Default, type)
    }

    /** One key press in [choice], for a settings screen letting the user hear each style. */
    fun performSample(view: View, choice: KeyboardSoundChoice) {
        play(view, choice, KeyboardHapticType.KEY_PRESS)
    }

    private fun play(view: View, choice: KeyboardSoundChoice, type: KeyboardHapticType) {
        val audioManager = view.context.getSystemService(AudioManager::class.java)
        if (!shouldPlay(audioManager.ringerMode)) return
        val gain = choice.volume.gain
        if (choice.style == KeyboardSoundStyle.SYSTEM) {
            audioManager.playSoundEffect(soundEffect(type), gain)
        } else {
            pool.play(view.context, choice.style, KeyboardSoundKind.of(type), gain)
        }
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
}
