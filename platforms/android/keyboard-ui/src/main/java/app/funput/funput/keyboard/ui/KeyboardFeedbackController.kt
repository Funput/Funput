package app.funput.funput.keyboard.ui

import app.funput.funput.keyboard.KeyboardHapticStrength
import app.funput.funput.keyboard.KeyboardHaptics
import app.funput.funput.keyboard.KeyboardSounds
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.sound.KeyboardSoundChoice
import app.funput.funput.keyboard.ui.clipboard.ClipboardPanelView

internal class KeyboardFeedbackController(
    private val keyboardSurface: KeyboardSurfaceView,
    private val emojiPanel: () -> EmojiPanelView?,
    private val clipboardPanel: () -> ClipboardPanelView?,
    private val feedbackChanged: () -> Unit = {},
) {
    var hapticsEnabled: Boolean
        get() = keyboardSurface.isHapticFeedbackEnabled
        set(value) {
            keyboardSurface.isHapticFeedbackEnabled = value
            emojiPanel()?.hapticsEnabled = value
            clipboardPanel()?.hapticsEnabled = value
            feedbackChanged()
        }

    /** The strength key presses vibrate at; null leaves feedback to the system. */
    var hapticStrength: KeyboardHapticStrength?
        get() = KeyboardHaptics.strength
        set(value) {
            KeyboardHaptics.strength = value
        }

    /** Whether pressed keys show their enlarged preview. Only the letters surface has them. */
    var keyPreviewsEnabled: Boolean
        get() = keyboardSurface.keyPreviewsEnabled
        set(value) {
            keyboardSurface.keyPreviewsEnabled = value
        }

    /** The sound key presses make, or null for silent keys. */
    var sound: KeyboardSoundChoice?
        get() = KeyboardSounds.choice.takeIf { keyboardSurface.isSoundEffectsEnabled }
        set(value) {
            val enabled = value != null
            keyboardSurface.isSoundEffectsEnabled = enabled
            emojiPanel()?.soundsEnabled = enabled
            clipboardPanel()?.soundsEnabled = enabled
            KeyboardSounds.select(keyboardSurface.context, value)
            feedbackChanged()
        }
}
