package app.funput.funput.keyboard.ui.localtext

import app.funput.funput.keyboard.model.KeyboardInputMethod

/**
 * A [LocalTextComposing] field that keeps every key exactly as typed: the default for a
 * keyboard view the IME has not given an engine-backed field.
 */
class PlainLocalTextComposer : LocalTextComposing {
    override var text: String = ""
        private set
    override val inputMethod: KeyboardInputMethod? = null

    override fun apply(edit: LocalTextEdit) {
        text = when (edit) {
            is LocalTextEdit.Text -> text + edit.value
            LocalTextEdit.Space -> if (LocalTextSpacing.accepts(text)) "$text " else text
            LocalTextEdit.DeleteBackward -> text.dropLastCodePoint()
        }
    }

    override fun reset() {
        text = ""
    }
}

/** Drops the last code point, so a surrogate pair never leaves half of itself behind. */
fun String.dropLastCodePoint(): String =
    if (isEmpty()) this else substring(0, offsetByCodePoints(length, -1))
