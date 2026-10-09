package app.funput.funput.ime.speech.session

import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.model.SpeechLocale
import org.junit.Assert.*
import org.junit.Test

class SpeechCommitOwnershipTest {
    @Test fun cancellationFromCommittingPresentationPreventsAnyWrite() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.onState = { if (it.phase == SpeechPhase.COMMITTING) f.controller.cancel() }
        recording.emit(SpeechEvent.Final("discard"))
        assertEquals(0, f.editor.commitAttempts)
        assertEquals(SpeechSessionState(), f.controller.state)
        assertEquals(1, recording.closes)
    }

    @Test fun invalidationReenteredDuringEditorValidationPreventsWrite() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.editor.onValidate = { f.controller.cancel() }
        recording.emit(SpeechEvent.Final("discard"))
        assertEquals(0, f.editor.commitAttempts)
        assertEquals(SpeechSessionState(), f.controller.state)
    }

    @Test fun lateCallbacksDuringCloseCannotInsertOrRestorePreview() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.onClose = {
            recording.emit(SpeechEvent.Final("reentered"))
            recording.emit(SpeechEvent.Partial("old preview"))
        }
        recording.emit(SpeechEvent.Final("first"))
        assertEquals(listOf("first"), f.editor.commits)
        assertEquals("", f.controller.state.preview)
        assertEquals(1, recording.closes)
    }

    @Test fun replacementStartedDuringErrorCleanupKeepsItsState() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.onClose = { f.controller.start(SpeechLocale.EN) }
        recording.emit(SpeechEvent.Failure(5))
        assertEquals(SpeechPhase.PREPARING, f.controller.state.phase)
        assertNull(f.controller.state.error)
        assertEquals(2, f.backend.recordings.size)
    }

    @Test fun replacementStartedDuringCancelCleanupKeepsItsState() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.onClose = { f.controller.start(SpeechLocale.EN) }
        f.controller.cancel()
        assertEquals(SpeechPhase.PREPARING, f.controller.state.phase)
        assertEquals(2, f.backend.recordings.size)
    }

    @Test fun failedCommitHasNoTranscriptInTerminalState() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.editor.accepts = false
        recording.emit(SpeechEvent.Partial("private preview"))
        recording.emit(SpeechEvent.Final("private final"))
        assertEquals(SpeechError.COMMIT_REJECTED, f.controller.state.error)
        assertEquals("", f.controller.state.preview)
        assertEquals(1, f.editor.commitAttempts)
    }
}
