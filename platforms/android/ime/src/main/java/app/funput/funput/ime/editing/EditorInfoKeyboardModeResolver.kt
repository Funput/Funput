package app.funput.funput.ime.editing

import android.text.InputType
import android.view.inputmethod.EditorInfo
import app.funput.funput.keyboard.model.KeyboardEditorMode

/** Resolves Android editor bit flags into renderer behavior without leaking platform flags. */
internal object EditorInfoKeyboardModeResolver {
    fun resolve(info: EditorInfo): KeyboardEditorMode = resolve(info.inputType)

    fun resolve(inputType: Int): KeyboardEditorMode {
        if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) {
            return KeyboardEditorMode.TEXT
        }
        return when (inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            -> KeyboardEditorMode.EMAIL

            InputType.TYPE_TEXT_VARIATION_URI -> KeyboardEditorMode.URL
            else -> KeyboardEditorMode.TEXT
        }
    }
}
