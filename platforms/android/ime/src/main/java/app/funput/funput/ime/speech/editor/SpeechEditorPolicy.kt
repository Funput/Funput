package app.funput.funput.ime.speech.editor

import android.text.InputType
import app.funput.funput.ime.editing.CompositionRenderMode

internal object SpeechEditorPolicy {
    fun allows(inputType: Int, renderMode: CompositionRenderMode): Boolean {
        if (renderMode == CompositionRenderMode.KEY_EVENT) return false
        if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        return when (inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> false
            else -> true
        }
    }
}
