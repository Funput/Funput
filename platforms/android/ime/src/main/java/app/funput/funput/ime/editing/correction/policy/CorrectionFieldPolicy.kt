package app.funput.funput.ime.editing.correction.policy

import android.text.InputType
import app.funput.funput.keyboard.model.KeyboardEditorMode

/**
 * Whether a field accepts having its words corrected, read from the same flags a field uses
 * to turn off the system keyboard's own autocorrect.
 */
internal object CorrectionFieldPolicy {
    fun allows(inputType: Int, editorMode: KeyboardEditorMode): Boolean {
        if (editorMode.isPassword) return false
        if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        if (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0) return false
        return inputType and InputType.TYPE_MASK_VARIATION !in Refused
    }

    /** Secrets and addresses: what is typed there is exact, never a misspelt word. */
    private val Refused = intArrayOf(
        InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_URI,
    )
}
