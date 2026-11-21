package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection

/** Tracks Funput-authored tokens and applies candidates for composed and direct editors. */
internal class ImeSuggestionSession(
    private val composition: AndroidCompositionSession,
    private val connection: () -> InputConnection?,
) {
    private val tracker = AuthoredTokenTracker()

    fun updateComposition() {
        val completed = composition.takeCompleted()
        tracker.update(composition.composingText, completed?.first, completed?.second == true)
    }

    fun inputDirect(text: String) = tracker.input(text)

    fun backspaceDirect() = tracker.backspace()

    fun takeUpdate(): AuthoredSuggestionUpdate = tracker.consume()

    /**
     * The caret moved without a key. A direct prefix the document no longer ends with is
     * dropped; then, with nothing selected, the words before the caret are read back.
     */
    fun reconcileSelection(direct: Boolean, collapsed: Boolean) {
        if (direct) reconcileDirectPrefix()
        if (collapsed) readContext()
    }

    /**
     * Reads the words before the caret from the document, so the bar can ask after text
     * Funput did not type. Only between words: a word in progress has its context already.
     */
    fun readContext() {
        if (tracker.currentPrefix().isNotEmpty()) return
        // An editor that reports nothing cannot be told from an empty one: the context stays as it is.
        val before = connection()?.getTextBeforeCursor(ContextLookback, 0)?.takeIf { it.isNotEmpty() }
        before?.let(tracker::reconcileContext)
    }

    private fun reconcileDirectPrefix() {
        val prefix = tracker.currentPrefix()
        if (prefix.isEmpty()) return
        val current = connection()
        val before = current?.getTextBeforeCursor(prefix.length, 0)?.toString()
        if (current == null || !current.getSelectedText(0).isNullOrEmpty() || before?.endsWith(prefix) != true) {
            tracker.reset()
        }
    }

    fun reset() = tracker.reset()

    fun accept(candidate: String, prefix: String, usesComposition: Boolean): Boolean =
        if (usesComposition) acceptComposed(candidate, prefix) else acceptDirect(candidate, prefix)

    private fun acceptComposed(candidate: String, prefix: String): Boolean {
        if (composition.composingText != prefix) return false
        val current = validConnection(prefix) ?: return false
        current.beginBatchEdit()
        val accepted = try {
            composition.acceptSuggestion(current, prefix, candidate)
        } finally {
            current.endBatchEdit()
        }
        if (accepted) tracker.accepted(candidate)
        return accepted
    }

    private fun acceptDirect(candidate: String, prefix: String): Boolean {
        val current = validConnection(prefix) ?: return false
        current.beginBatchEdit()
        val accepted = try {
            when {
                !current.deleteSurroundingText(prefix.length, 0) -> false
                current.commitText("$candidate ", CursorAfterText) -> true
                else -> {
                    current.commitText(prefix, CursorAfterText)
                    false
                }
            }
        } finally {
            current.endBatchEdit()
        }
        if (accepted) tracker.accepted(candidate)
        return accepted
    }

    private fun validConnection(prefix: String): InputConnection? {
        val current = connection() ?: return null
        if (!current.getSelectedText(0).isNullOrEmpty()) return null
        val before = current.getTextBeforeCursor(prefix.length, 0)?.toString() ?: return null
        return current.takeIf { before.endsWith(prefix) }
    }

    private companion object {
        const val CursorAfterText = 1
    }
}
