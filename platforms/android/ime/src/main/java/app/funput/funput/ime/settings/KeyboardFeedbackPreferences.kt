package app.funput.funput.ime.settings

import app.funput.funput.keyboard.KeyboardHapticStrength

data class KeyboardFeedbackPreferences(
    val hapticsEnabled: Boolean,
    val soundsEnabled: Boolean,
    /** Kept while vibration is off, so turning it back on returns to the same strength. */
    val hapticStrength: KeyboardHapticStrength = KeyboardHapticStrength.Default,
) {
    /** The strength in use, or null when vibration is off. */
    val activeHapticStrength: KeyboardHapticStrength?
        get() = hapticStrength.takeIf { hapticsEnabled }

    companion object {
        val Default = KeyboardFeedbackPreferences(
            hapticsEnabled = true,
            soundsEnabled = false,
        )
    }
}
