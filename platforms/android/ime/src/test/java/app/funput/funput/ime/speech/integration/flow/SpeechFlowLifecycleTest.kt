package app.funput.funput.ime.speech.integration.flow

import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechCapability
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.ui.speech.SpeechPanelAction
import org.junit.Assert.*
import org.junit.Test

class SpeechFlowLifecycleTest {
    @Test fun ordinaryTypingRefreshNeverOpensASpeechClient() {
        val f = SpeechFlowFixture()
        repeat(100) { f.flow.refresh(); f.flow.cancel() }
        assertTrue(f.preparation.operations.isEmpty())
        assertTrue(f.backend.recordings.isEmpty())
    }

    @Test fun permissionSetupNeverAutomaticallyRecordsAfterGrant() {
        val f = SpeechFlowFixture()
        f.permitted = false
        f.flow.start()
        assertTrue(f.port.panels.last().canOpenSetup)
        assertFalse(f.port.panels.last().canRetry)
        f.flow.action(SpeechPanelAction.OPEN_SETUP)
        assertEquals(listOf(SpeechLocale.VI), f.setups)
        f.permitted = true
        f.flow.refresh()
        assertFalse(f.ui.engaged)
        assertTrue(f.backend.recordings.isEmpty())
        f.start()
        assertEquals(1, f.backend.recordings.size)
    }

    @Test fun hiddenEditorAndLatePreflightCannotReopenThePanel() {
        val f = SpeechFlowFixture()
        f.flow.start()
        val operation = f.preparation.operations.single()
        f.eligible = false
        f.flow.cancel(false)
        val presentations = f.port.panels.size
        operation.deliver(SpeechCapability.READY)
        assertEquals(presentations, f.port.panels.size)
        assertTrue(f.backend.recordings.isEmpty())
        assertFalse(f.port.microphoneVisible)
        assertEquals(1, operation.closes)
    }

    @Test fun permissionLossCancelsRecordingBeforeLateFinal() {
        val f = SpeechFlowFixture()
        f.start()
        val recording = f.backend.recordings.single()
        f.permitted = false
        f.scheduler.advance(250)
        recording.emit(SpeechEvent.Final("obsolete"))
        assertFalse(f.ui.engaged)
        assertTrue(f.editor.commits.isEmpty())
        assertEquals(1, recording.cancels)
        assertEquals(1, recording.closes)
    }

    @Test fun disabledOrLockedEnvironmentCancelsPreflight() {
        val f = SpeechFlowFixture()
        f.flow.start()
        f.eligible = false
        f.scheduler.advance(250)
        assertEquals(1, f.preparation.operations.single().closes)
        assertFalse(f.ui.engaged)
        assertFalse(f.port.microphoneVisible)
    }

    @Test fun closeIsIdempotentAndIgnoresOldResultsAndTimers() {
        val f = SpeechFlowFixture()
        f.start()
        val recording = f.backend.recordings.single()
        f.flow.close()
        f.flow.close()
        f.scheduler.tasks.forEach { it.action() }
        recording.emit(SpeechEvent.Final("closed"))
        f.flow.start()
        assertEquals(1, recording.closes)
        assertEquals(1, recording.cancels)
        assertTrue(f.editor.commits.isEmpty())
        assertEquals(1, f.backend.recordings.size)
    }

    @Test fun retryUsesCurrentLocaleAndAnIndependentRecording() {
        val f = SpeechFlowFixture()
        f.start()
        f.backend.recordings.single().emit(SpeechEvent.Failure(12))
        assertTrue(f.port.panels.last().canOpenSetup)
        assertEquals(1, f.preparation.invalidations)
        f.language = KeyboardLanguage.ENGLISH
        f.flow.action(SpeechPanelAction.RETRY)
        f.preparation.operations.last().deliver(SpeechCapability.UNKNOWN)
        assertEquals(listOf(SpeechLocale.VI, SpeechLocale.EN), f.backend.locales)
        assertEquals(KeyboardLanguage.ENGLISH, f.port.panels.last().language)
    }
    @Test fun cancelledSafetyTimerCannotOrphanANewerLease() {
        val f = SpeechFlowFixture()
        f.flow.start()
        val oldSafety = f.scheduler.tasks.first()
        f.flow.cancel(false)
        val nextIndex = f.scheduler.tasks.size
        f.flow.start()
        val newSafety = f.scheduler.tasks[nextIndex]
        oldSafety.action()
        f.flow.cancel(false)
        assertEquals(1, newSafety.cancellations)
        assertEquals(1, f.preparation.operations.last().closes)
    }

    @Test fun cancellingDuringOpenNeverCreatesACapabilityOrRecordingClient() {
        val f = SpeechFlowFixture()
        f.port.onPresent = { f.flow.cancel(false) }
        f.flow.start()
        assertTrue(f.preparation.operations.isEmpty())
        assertTrue(f.backend.recordings.isEmpty())
        assertFalse(f.ui.engaged)
    }
}
