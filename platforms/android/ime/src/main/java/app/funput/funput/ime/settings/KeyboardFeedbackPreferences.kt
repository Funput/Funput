package app.funput.funput.ime.settings

import app.funput.funput.keyboard.KeyboardHapticStrength
import app.funput.funput.keyboard.sound.KeyboardSoundChoice
import app.funput.funput.keyboard.sound.KeyboardSoundStyle
import app.funput.funput.keyboard.sound.KeyboardSoundVolume

data class KeyboardFeedbackPreferences(
    val hapticsEnabled: Boolean,
    val soundsEnabled: Boolean,
    /** Kept while vibration is off, so turning it back on returns to the same strength. */
    val hapticStrength: KeyboardHapticStrength = KeyboardHapticStrength.Default,
    /** The enlarged character over a pressed key; on unless the user turns it off. */
    val keyPreviewsEnabled: Boolean = true,
    /** Kept while sound is off, so turning it back on returns to the same style. */
    val soundStyle: KeyboardSoundStyle = KeyboardSoundStyle.Default,
    /** How loud key presses sound, whatever the style. */
    val soundVolume: KeyboardSoundVolume = KeyboardSoundVolume.Default,
) {
    /** The strength in use, or null when vibration is off. */
    val activeHapticStrength: KeyboardHapticStrength?
        get() = hapticStrength.takeIf { hapticsEnabled }

    /** The sound in use, or null when sound is off. */
    val activeSound: KeyboardSoundChoice?
        get() = KeyboardSoundChoice(soundStyle, soundVolume).takeIf { soundsEnabled }

    companion object {
        val Default = KeyboardFeedbackPreferences(
            hapticsEnabled = true,
            soundsEnabled = false,
        )
    }
}
