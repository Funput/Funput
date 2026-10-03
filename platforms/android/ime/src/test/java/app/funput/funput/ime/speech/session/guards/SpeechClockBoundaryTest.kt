package app.funput.funput.ime.speech.session

import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.model.SpeechLocale
import org.junit.Assert.*
import org.junit.Test

class SpeechClockBoundaryTest {
    @Test fun expiredAnchorCannotStartRecordingBeforeQueuedTimerRuns() {
        val f = SpeechSpikeFixture()
        f.editor.automaticPreparation = false
        f.controller.start(SpeechLocale.VI)
        f.scheduler.now = 3_000
        f.editor.prepared!!(f.editor.anchor)
        assertEquals(SpeechError.ANCHOR_TIMEOUT, f.controller.state.error)
        assertTrue(f.backend.recordings.isEmpty())
    }

    @Test fun lateReadyIsRejectedEvenIfItsTimeoutHasNotRun() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.scheduler.now = 5_000
        recording.emit(SpeechEvent.Ready)
        assertEquals(SpeechError.READY_TIMEOUT, f.controller.state.error)
        assertEquals(1, recording.cancels)
    }

    @Test fun lateFinalCannotBeatAnExpiredFinalDeadline() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Ready)
        f.controller.stop()
        f.scheduler.now = 5_000
        recording.emit(SpeechEvent.Final("late"))
        assertEquals(SpeechError.FINAL_TIMEOUT, f.controller.state.error)
        assertEquals(0, f.editor.commitAttempts)
    }

    @Test fun eventPastSessionLimitCommandsStopBeforeHandlingResult() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Ready)
        f.scheduler.now = 60_000
        recording.emit(SpeechEvent.Final("final"))
        assertEquals(1, recording.stops)
        assertEquals(listOf("final"), f.editor.commits)
        assertEquals(SpeechPhase.IDLE, f.controller.state.phase)
    }
    @Test fun readyAcknowledgementDisarmsDeadlineBeforePresentationCanReenter() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        var reentered = false
        f.onState = {
            if (it.phase == SpeechPhase.LISTENING && !reentered) {
                reentered = true
                f.scheduler.now = 5_000
                recording.emit(SpeechEvent.Partial("current"))
            }
        }
        recording.emit(SpeechEvent.Ready)
        assertEquals(SpeechPhase.LISTENING, f.controller.state.phase)
        assertEquals("current", f.controller.state.preview)
        assertEquals(0, recording.closes)
    }

    @Test fun finalDeadlineExistsBeforeFinalizingPresentationCanReenter() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Ready)
        f.onState = {
            if (it.phase == SpeechPhase.FINALIZING) {
                f.scheduler.now = 5_000
                recording.emit(SpeechEvent.Final("expired"))
            }
        }
        f.controller.stop()
        assertEquals(SpeechError.FINAL_TIMEOUT, f.controller.state.error)
        assertEquals(0, f.editor.commitAttempts)
        assertEquals(1, recording.closes)
    }

    @Test fun stopCannotExtendAnAlreadyExpiredReadyBudget() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.scheduler.now = 5_000
        f.controller.stop()
        assertEquals(SpeechError.READY_TIMEOUT, f.controller.state.error)
        assertEquals(0, recording.stops)
        assertEquals(1, recording.cancels)
    }

}
