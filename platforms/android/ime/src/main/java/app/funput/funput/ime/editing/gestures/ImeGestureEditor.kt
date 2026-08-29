package app.funput.funput.ime.editing.gestures

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.caret.CaretLineKeys
import app.funput.funput.ime.editing.caret.CaretPanResolver
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
    private val pan = CaretPanResolver()

    fun consume(action: KeyAction): Boolean = when (action) {
        is KeyAction.MoveCursor -> moveCaret(action.columns, action.lines)
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

    /** The caret pan's only absolute reference in editors that expose no extracted text. */
    fun onSelectionChanged(position: Int) = pan.onSelectionChanged(position)

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

    /**
     * Moves the caret [columns] characters sideways and [lines] logical lines vertically.
     *
     * Composition cannot survive the caret leaving the buffer it mirrors, so every step ends it —
     * but a step asking for no movement touches nothing, composition included.
     *
     * The two axes travel by different means: sideways is an absolute `setSelection` we compute,
     * while vertical is arrow keys the editor answers itself, because only the editor knows where
     * its text wraps. Lines go first so the sideways nudge of a diagonal drag lands on the row the
     * finger ended on.
     *
     * Always returns true: the pan owns this action whether or not the caret had room to move.
     */
    private fun moveCaret(columns: Int, lines: Int): Boolean {
        if (columns == 0 && lines == 0) return true
        val current = connection() ?: return true
        composition.finish(current)
        var moved = CaretLineKeys.move(current, lines)
        if (pan.apply(current, columns)) moved = true
        if (moved) onSuggestionsCleared()
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
