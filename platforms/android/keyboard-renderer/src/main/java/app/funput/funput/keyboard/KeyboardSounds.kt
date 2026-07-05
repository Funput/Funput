package app.funput.funput.keyboard

import android.view.SoundEffectConstants
import android.view.View

/** Plays Android's system key click while respecting the view and system sound settings. */
object KeyboardSounds {
    fun perform(view: View) {
        if (!view.isSoundEffectsEnabled) return
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }
}
