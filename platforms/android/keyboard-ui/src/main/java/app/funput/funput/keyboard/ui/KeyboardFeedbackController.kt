package app.funput.funput.keyboard.ui

import app.funput.funput.keyboard.KeyboardSurfaceView

internal class KeyboardFeedbackController(
    private val keyboardSurface: KeyboardSurfaceView,
    private val emojiPanel: () -> EmojiPanelView?,
) {
    var hapticsEnabled: Boolean
        get() = keyboardSurface.isHapticFeedbackEnabled
        set(value) {
            keyboardSurface.isHapticFeedbackEnabled = value
            emojiPanel()?.hapticsEnabled = value
        }

    var soundsEnabled: Boolean
        get() = keyboardSurface.isSoundEffectsEnabled
        set(value) {
            keyboardSurface.isSoundEffectsEnabled = value
            emojiPanel()?.soundsEnabled = value
        }
}
