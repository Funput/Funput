package app.funput.funput.ime.speech.editor

import app.funput.funput.ime.speech.session.SpeechEditorAnchor

internal data class SpeechEditorStamp(val generation: Long, val invalidation: Long)
internal data class SpeechSelection(val start: Int, val end: Int)

/** Seed from EditorInfo. Revisions never roll back when a caret moves away and returns. */
internal class SpeechEditorTracker {
    private var generation = 0L
    private var revision = 0L
    private var invalidation = 0L
    private var selection = SpeechSelection(-1, -1)
    private var allowed = false
    var selectionPending = false
        private set

    fun startInput(eligible: Boolean, selectionStart: Int, selectionEnd: Int) {
        generation++
        invalidate()
        allowed = eligible
        selection = SpeechSelection(selectionStart, selectionEnd)
        selectionPending = false
    }

    fun selectionChanged(selectionStart: Int, selectionEnd: Int): Boolean {
        selectionPending = false
        val next = SpeechSelection(selectionStart, selectionEnd)
        if (selection == next) return false
        selection = next
        revision++
        return true
    }

    fun invalidate(expectSelection: Boolean = false) {
        revision++
        invalidation++
        if (expectSelection) selectionPending = true
    }

    fun stamp() = SpeechEditorStamp(generation, invalidation)
    fun matches(stamp: SpeechEditorStamp) = stamp() == stamp

    fun anchor(): SpeechEditorAnchor? = if (allowed && selection.start >= 0 && selection.start == selection.end) {
        SpeechEditorAnchor(generation, revision, selection.start)
    } else null

    fun matches(anchor: SpeechEditorAnchor): Boolean = !selectionPending && anchor() == anchor
}
