package app.funput.funput.ime.editing

import android.text.InputType
import android.view.inputmethod.EditorInfo
import app.funput.funput.keyboard.model.KeyboardEditorMode

/** Resolves Android editor bit flags into renderer behavior without leaking platform flags. */
internal object EditorInfoKeyboardModeResolver {
    fun resolve(info: EditorInfo): KeyboardEditorMode = resolve(info.inputType)

    fun resolve(inputType: Int): KeyboardEditorMode {
        return when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> resolveText(inputType)
            InputType.TYPE_CLASS_NUMBER -> resolveNumber(inputType)
            InputType.TYPE_CLASS_PHONE -> KeyboardEditorMode.PHONE
            else -> KeyboardEditorMode.TEXT
        }
    }

    private fun resolveText(inputType: Int): KeyboardEditorMode =
        when (inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            -> KeyboardEditorMode.EMAIL

            InputType.TYPE_TEXT_VARIATION_URI -> KeyboardEditorMode.URL
            else -> KeyboardEditorMode.TEXT
        }

    private fun resolveNumber(inputType: Int): KeyboardEditorMode {
        val decimal = inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0
        val signed = inputType and InputType.TYPE_NUMBER_FLAG_SIGNED != 0
        return when {
            decimal && signed -> KeyboardEditorMode.NUMBER_SIGNED_DECIMAL
            decimal -> KeyboardEditorMode.NUMBER_DECIMAL
            signed -> KeyboardEditorMode.NUMBER_SIGNED
            else -> KeyboardEditorMode.NUMBER
        }
    }
}
