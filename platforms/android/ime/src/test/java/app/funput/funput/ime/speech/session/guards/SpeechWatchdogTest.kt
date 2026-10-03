package app.funput.funput.ime.speech.session

import app.funput.funput.ime.speech.session.watchdog.SpeechDeadline
import app.funput.funput.ime.speech.session.watchdog.SpeechWatchdog
import org.junit.Assert.*
import org.junit.Test

class SpeechWatchdogTest {
    @Test fun replacedAndCancelledSlotsCannotFireLateCallbacks() {
        val scheduler = SpikeScheduler()
        val watchdog = SpeechWatchdog(scheduler, scheduler)
        var calls = 0
        watchdog.arm(SpeechDeadline.READY) { calls += 100 }
        val old = scheduler.tasks.single()
        watchdog.arm(SpeechDeadline.READY) { calls++ }
        old.action()
        assertEquals(0, calls)
        scheduler.advance(5_000)
        assertEquals(1, calls)
        watchdog.close()
        scheduler.tasks.forEach { it.action() }
        assertEquals(1, calls)
        assertTrue(scheduler.tasks.all { it.cancellations == 1 })
    }

    @Test fun inlineTimeoutCannotLeaveAnOwnedTimerAfterClose() {
        var cancellations = 0
        val watchdog = SpeechWatchdog(SpeechScheduler { _, task ->
            task()
            SpeechCancellation { cancellations++ }
        }, SpeechClock { 0 })
        var calls = 0
        watchdog.arm(SpeechDeadline.ANCHOR) { calls++; watchdog.close() }
        watchdog.arm(SpeechDeadline.READY) { calls++ }
        assertEquals(1, calls)
        assertEquals(1, cancellations)
    }

    @Test fun inlineControllerAnchorDeadlineNeverPreparesEditorOrOpensBackend() {
        val editor = SpikeEditor()
        var preparations = 0
        editor.onPrepare = { preparations++ }
        val backend = SpikeBackend()
        val controller = SpeechSessionController(backend, editor,
            SpeechScheduler { _, task -> task(); SpeechCancellation {} }, SpeechClock { 0 }) {}
        controller.start(app.funput.funput.ime.speech.model.SpeechLocale.VI)
        assertEquals(SpeechError.ANCHOR_TIMEOUT, controller.state.error)
        assertEquals(0, preparations)
        assertTrue(backend.recordings.isEmpty())
    }
}
