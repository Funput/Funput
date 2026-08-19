package app.funput.funput.ime.editing.gestures

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.keyboard.model.KeyAction

/**
 * Applies the three smart-gesture document mutations: smart space, caret pan, and word delete.
 *
 * Returns whether the action was fully handled so the ordinary key path stays untouched.
 */
internal class ImeGestureEditor(
    private val composition: AndroidCompositionSession,
    private val editor: InputConnectionEditor,
    private val connection: () -> InputConnection?,
    private val fallbackBackspace: () -> Unit,
    private val onSuggestionsCleared: () -> Unit,
) {
    var enabled = true
        set(value) {
            field = value
            if (!value) tracker.reset()
        }
    private val tracker = SpaceTapTracker()

    fun consume(action: KeyAction): Boolean = when (action) {
        is KeyAction.MoveCursor -> moveCursor(action.offset)
        KeyAction.DeleteWord -> deleteWord()
        KeyAction.Space -> applySmartSpace()
        is KeyAction.Input,
        KeyAction.Backspace,
        KeyAction.Enter,
        is KeyAction.ToggleLanguage,
        -> {
            tracker.reset()
            false
        }
        else -> false
    }

    fun reset() = tracker.reset()

    private fun applySmartSpace(): Boolean {
        if (!enabled) return false
        if (!tracker.registerSpace()) return false
        val current = connection() ?: return false
        if (!current.getSelectedText(0).isNullOrEmpty()) return false
        val context = current.getTextBeforeCursor(ContextLookback, 0)?.toString()
        if (!SentencePunctuationRule.appliesTo(context)) return false
        composition.finish(current)
        editor.execute(current, ImeEditCommand.DeleteSurrounding(1))
        editor.execute(current, ImeEditCommand.CommitText(". "))
        onSuggestionsCleared()
        return true
    }

    private fun moveCursor(offset: Int): Boolean {
        if (offset == 0) return true
        val current = connection() ?: return true
        composition.finish(current)
        editor.execute(current, ImeEditCommand.MoveCursor(offset))
        onSuggestionsCleared()
        return true
    }

    private fun deleteWord(): Boolean {
        val current = connection()
        if (current == null || !current.getSelectedText(0).isNullOrEmpty()) {
            fallbackBackspace()
            return true
        }
        val context = current.getTextBeforeCursor(ContextLookback, 0)?.toString()
        val count = KeyboardWordDeletion.spanBeforeCursor(context)
        if (count == null) {
            fallbackBackspace()
            return true
        }
        composition.finish(current)
        editor.execute(current, ImeEditCommand.DeleteSurrounding(count))
        onSuggestionsCleared()
        return true
    }

    private companion object {
        const val ContextLookback = 256
    }
}
