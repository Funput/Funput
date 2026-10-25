package app.funput.funput.ime.shortcuts.undo

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession

/**
 * Puts a gõ tắt expansion back when Backspace, or the `↩` chip, is still the next
 * action and the expansion is still what sits before the caret.
 */
internal class ShortcutUndo(
    private val composition: AndroidCompositionSession,
    private val connection: () -> InputConnection?,
) {
    private val engine get() = composition.engine.shortcutUndo

    /**
     * The chip label, when the document still ends with the expansion. A suffix that
     * is present and different means the caret moved: the beat is dropped. A host
     * that will not say what is before the caret leaves the beat armed and shows nothing.
     */
    fun chipText(): String? = engine.text()?.takeIf { confirmed() == true }

    /** True when the expansion was put back and Backspace must not also delete. */
    fun undo(): Boolean {
        val current = connection() ?: return false
        val suffix = engine.suffix() ?: return false
        if (!sitsBeforeCaret(current, suffix)) {
            engine.dismiss()
            return false
        }
        val edit = engine.undo() ?: return false
        return composition.replaceCommitted(current, suffix, edit.text)
    }

    private fun confirmed(): Boolean? {
        val suffix = engine.suffix() ?: return null
        val current = connection() ?: return null
        if (!sitsBeforeCaret(current, suffix)) {
            if (current.getTextBeforeCursor(suffix.length, 0) != null) engine.dismiss()
            return false
        }
        return true
    }

    private fun sitsBeforeCaret(current: InputConnection, suffix: String): Boolean {
        if (!current.getSelectedText(0).isNullOrEmpty()) return false
        return current.getTextBeforeCursor(suffix.length, 0)?.toString() == suffix
    }
}
