package app.funput.funput.ime.speech.integration.flow

import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.keyboard.ui.speech.SpeechPanelAction
import app.funput.funput.keyboard.ui.speech.SpeechPanelStage
import org.junit.Assert.*
import org.junit.Test

class SpeechFlowDeliveryTest {
    @Test fun openingAndPartialsDoNotDismissOrCommit() {
        val f = SpeechFlowFixture()
        f.start()
        val recording = f.backend.recordings.single()
        recording.emit(SpeechEvent.Ready)
        recording.emit(SpeechEvent.Partial("xin chao"))
        recording.emit(SpeechEvent.Partial("xin chào bạn"))
        assertTrue(f.ui.engaged)
        assertEquals(SpeechPanelStage.LISTENING, f.port.panels.last().stage)
        assertEquals("xin chào bạn", f.port.panels.last().preview)
        assertTrue(f.editor.commits.isEmpty())
        assertEquals(0, recording.cancels)
    }

    @Test fun finalReturnsToLettersAndCannotBeReplayed() {
        val f = SpeechFlowFixture()
        f.start()
        val recording = f.backend.recordings.single()
        recording.emit(SpeechEvent.Final("  xin chào  "))
        assertEquals(listOf("xin chào"), f.editor.commits)
        assertFalse(f.ui.engaged)
        val count = f.port.panels.size
        recording.emit(SpeechEvent.Final("duplicate"))
        recording.emit(SpeechEvent.Partial("late"))
        assertEquals(count, f.port.panels.size)
        assertEquals(1, recording.closes)
        assertEquals(0, recording.cancels)
        assertFalse(f.port.microphoneActive)
    }

    @Test fun stopWaitsForFinalAndBackDropsIt() {
        val f = SpeechFlowFixture()
        f.start()
        val recording = f.backend.recordings.single()
        recording.emit(SpeechEvent.Ready)
        f.flow.action(SpeechPanelAction.STOP)
        assertEquals(SpeechPanelStage.FINALIZING, f.port.panels.last().stage)
        assertEquals(1, recording.stops)
        assertTrue(f.flow.back())
        assertFalse(f.flow.back())
        recording.emit(SpeechEvent.Final("cancelled"))
        assertTrue(f.editor.commits.isEmpty())
        assertEquals(1, recording.cancels)
    }

    @Test fun rejectedCommitRemainsAnErrorWithoutRetryingWrite() {
        val f = SpeechFlowFixture()
        f.editor.accepts = false
        f.start()
        f.backend.recordings.single().emit(SpeechEvent.Final("hello"))
        assertEquals(1, f.editor.commitAttempts)
        assertEquals(SpeechPanelStage.ERROR, f.port.panels.last().stage)
        assertEquals("EDITOR", f.port.panels.last().message)
        assertTrue(f.port.panels.last().canRetry)
    }

    @Test fun finalUiCannotCommitAfterReentrantCancellation() {
        val f = SpeechFlowFixture()
        f.start()
        f.port.onPresent = {
            if (f.port.panels.last().stage == SpeechPanelStage.FINALIZING) f.flow.cancel(false)
        }
        f.backend.recordings.single().emit(SpeechEvent.Final("obsolete"))
        assertTrue(f.editor.commits.isEmpty())
        assertFalse(f.ui.engaged)
    }
    @Test fun cancellationInvalidatesBeforePresentationCanDeliverAFinal() {
        val f = SpeechFlowFixture()
        f.start()
        val recording = f.backend.recordings.single()
        f.port.onDismiss = { recording.emit(SpeechEvent.Final("reentrant")) }
        f.flow.cancel(false)
        assertTrue(f.editor.commits.isEmpty())
        assertEquals(1, recording.cancels)
        assertFalse(f.ui.engaged)
    }
}
