package app.funput.funput.ime.speech.editor

import app.funput.funput.ime.speech.session.SpeechCancellation
import app.funput.funput.ime.speech.session.SpeechEditorAnchor

internal interface SpeechAnchorEnvironment {
    fun available(): Boolean
    fun finishComposition()
    fun selection(): SpeechSelection?
}

/** Accept finish-owned selection updates; independent edits/restarts still invalidate the stamp. */
internal class SpeechAnchorResolver(
    private val tracker: SpeechEditorTracker,
    private val environment: SpeechAnchorEnvironment,
) {
    private class Pending(val stamp: SpeechEditorStamp, val listener: (SpeechEditorAnchor?) -> Unit)
    private var pending: Pending? = null
    private var finishing = false
    private var reconciling = false

    fun prepare(listener: (SpeechEditorAnchor?) -> Unit): SpeechCancellation {
        cancel()
        val request = Pending(tracker.stamp(), listener)
        pending = request
        try {
            if (!environment.available() || tracker.anchor() == null) complete(request, null)
            else {
                finishing = true
                try { environment.finishComposition() } finally { finishing = false }
                reconcile(request)
            }
        } catch (_: RuntimeException) {
            complete(request, null)
        }
        return SpeechCancellation { if (pending === request) cancel() }
    }

    fun selectionUpdated(): Boolean {
        val request = pending ?: return false
        if (!finishing && !reconciling) {
            try { reconcile(request) } catch (_: RuntimeException) { complete(request, null) }
        }
        return true
    }

    fun cancel() { pending = null }

    private fun reconcile(request: Pending) {
        if (pending !== request) return
        if (!tracker.matches(request.stamp) || !environment.available()) return complete(request, null)
        reconciling = true
        val observed = try { environment.selection() } finally { reconciling = false }
        if (pending !== request) return
        if (!tracker.matches(request.stamp) || !environment.available()) return complete(request, null)
        if (observed != null) tracker.selectionChanged(observed.start, observed.end)
        // No fresh callback is required when the seeded/last caret is already confirmed and unchanged.
        if (tracker.selectionPending) return
        complete(request, tracker.anchor())
    }

    private fun complete(request: Pending, anchor: SpeechEditorAnchor?) {
        if (pending !== request) return
        pending = null
        request.listener(anchor)
    }
}
