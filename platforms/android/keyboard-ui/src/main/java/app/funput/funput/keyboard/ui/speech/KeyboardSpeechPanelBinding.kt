package app.funput.funput.keyboard.ui.speech

import android.content.Context
import android.view.View
import app.funput.funput.keyboard.ui.FunputKeyboardView
import app.funput.funput.keyboard.ui.KeyboardPanel
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.ui.FunputKeyboardCallbacks

/** Stores presentation before lazy creation, and drops transcript when the panel closes. */
internal class KeyboardSpeechPanelBinding(
    private val context: Context,
    private val surface: KeyboardSurfaceView,
    private val callbacks: FunputKeyboardCallbacks,
    private val showLetters: () -> Unit,
) {
    private var panel: SpeechPanelView? = null
    var state = SpeechPanelState()
        set(value) { field = value; panel?.submit(value) }

    fun create(): SpeechPanelView = SpeechPanelView(context).also {
        panel = it
        it.submit(state)
        it.updateTheme(surface.keyboardTheme)
        it.onAction = { action ->
            if (it.visibility == View.VISIBLE) {
                if (action == SpeechPanelAction.CANCEL) showLetters()
                else callbacks.dispatchSpeechAction(action)
            }
        }
        updateFeedback()
    }

    fun clear() {
        state = SpeechPanelState()
        panel?.clear()
    }

    fun updateFeedback() {
        panel?.isHapticFeedbackEnabled = surface.isHapticFeedbackEnabled
        panel?.isSoundEffectsEnabled = surface.isSoundEffectsEnabled
    }
}

/** Hosts route Back here before their normal navigation; no recognition starts here. */
fun FunputKeyboardView.cancelSpeechPanel(): Boolean {
    if (activePanel != KeyboardPanel.SPEECH) return false
    showLettersPanel()
    return true
}
