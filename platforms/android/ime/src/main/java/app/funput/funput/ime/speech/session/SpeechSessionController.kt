package app.funput.funput.ime.speech.session

import app.funput.funput.ime.speech.model.SpeechBackend
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.session.watchdog.SpeechDeadline

/** Owner serializes entry points on main. No platform classes or default dispatcher here. */
internal class SpeechSessionController(
    private val backend: SpeechBackend,
    private val editor: SpeechEditorGateway,
    private val scheduler: SpeechScheduler,
    private val clock: SpeechClock,
    private val onState: (SpeechSessionState) -> Unit,
) {
    var state = SpeechSessionState()
        private set
    private val delivery = SpeechFinalDelivery(editor)
    private var active: SpeechOwnedSession? = null
    private var sequence = 0L
    private var closed = false

    fun start(locale: SpeechLocale) {
        if (closed || active != null || state.phase == SpeechPhase.COMMITTING) return
        val session = SpeechOwnedSession(++sequence, locale, scheduler, clock)
        active = session
        publish(SpeechSessionState(phase = SpeechPhase.PREPARING, sessionId = session.id))
        if (!owns(session)) return
        try {
            session.watchdog.arm(SpeechDeadline.ANCHOR) { fail(session, SpeechError.ANCHOR_TIMEOUT) }
            if (!owns(session)) return
            val handle = editor.prepareAnchor { anchor ->
                if (owns(session) && session.anchor == null) {
                    if (session.watchdog.expired(SpeechDeadline.ANCHOR)) fail(session, SpeechError.ANCHOR_TIMEOUT)
                    else if (anchor == null) fail(session, SpeechError.EDITOR_UNAVAILABLE)
                    else {
                        session.anchored(anchor)
                        session.watchdog.disarm(SpeechDeadline.ANCHOR)
                        change(session, SpeechSessionEvent.Anchored)
                    }
                }
            }
            session.ownPreparation(handle)
        } catch (_: RuntimeException) {
            fail(session, SpeechError.EDITOR_UNAVAILABLE)
        }
    }

    fun stop() { active?.let { change(it, SpeechSessionEvent.Stop) } }

    fun cancel() {
        if (closed) return
        val request = ++sequence
        active?.let { take(it, cancel = true) }
        if (!closed && sequence == request) publish(SpeechSessionState())
    }

    fun close() {
        if (closed) return
        closed = true
        sequence++
        active?.let { take(it, cancel = true) }
        state = SpeechSessionState()
    }

    private fun change(session: SpeechOwnedSession, event: SpeechSessionEvent) {
        if (!owns(session)) return
        if (event is SpeechSessionEvent.Recognition || event == SpeechSessionEvent.Stop) {
            session.expiredError()?.let { return fail(session, it) }
        }
        if (event is SpeechSessionEvent.Recognition) {
            if (session.watchdog.expired(SpeechDeadline.SESSION)) change(session, SpeechSessionEvent.Stop)
            if (!owns(session)) return
        }
        val transition = SpeechSessionReducer.reduce(state, event)
        if (transition.state == state && transition.effects.isEmpty()) return
        val commit = transition.effects.filterIsInstance<SpeechEffect.Commit>().singleOrNull()
        if (commit != null) {
            finishCommit(session, transition.state, commit.text)
            return
        }
        if (SpeechEffect.Abort in transition.effects) take(session, cancel = true)
        if (closed || sequence != session.id) return
        state = transition.state
        session.updateDeadlines(transition.effects) { fail(session, SpeechError.FINAL_TIMEOUT) }
        if (closed || sequence != session.id || state != transition.state) return
        publish(transition.state)
        for (effect in transition.effects) {
            if (!owns(session)) return
            when (effect) {
                SpeechEffect.Record -> record(session)
                SpeechEffect.Stop -> command(session) { session.recording?.stop() }
                else -> Unit
            }
        }
    }

    private fun record(session: SpeechOwnedSession) = command(session) {
        val anchor = session.anchor
        if (anchor == null || !editor.isValid(anchor)) return@command fail(session, SpeechError.EDITOR_UNAVAILABLE)
        if (!owns(session)) return@command
        session.ownRecording(backend.open(session.locale) { event ->
            change(session, SpeechSessionEvent.Recognition(event))
        })
        if (!owns(session)) return@command
        session.watchdog.arm(SpeechDeadline.READY) { fail(session, SpeechError.READY_TIMEOUT) }
        if (!owns(session)) return@command
        session.watchdog.arm(SpeechDeadline.SESSION) { change(session, SpeechSessionEvent.Stop) }
        if (owns(session)) session.recording?.start()
    }

    private fun finishCommit(session: SpeechOwnedSession, committing: SpeechSessionState, text: String) {
        val anchor = session.anchor ?: return fail(session, SpeechError.COMMIT_REJECTED)
        // Invalidate callback ownership before close, presentation, or any editor write can re-enter.
        take(session, cancel = false)
        if (closed || sequence != session.id) return
        publish(committing)
        if (closed || sequence != session.id) return
        val outcome = delivery.commit(anchor, text) { !closed && sequence == session.id }
        if (!closed && sequence == session.id) publish(outcome)
    }

    private fun command(session: SpeechOwnedSession, action: () -> Unit) {
        try { action() } catch (_: RuntimeException) { fail(session, SpeechError.BACKEND) }
    }

    private fun fail(session: SpeechOwnedSession, reason: SpeechError) =
        change(session, SpeechSessionEvent.Failed(reason))

    private fun owns(session: SpeechOwnedSession) = !closed && active === session

    private fun take(session: SpeechOwnedSession, cancel: Boolean) {
        active = null
        session.release(cancel)
    }

    private fun publish(next: SpeechSessionState) {
        state = next
        onState(next)
    }
}
