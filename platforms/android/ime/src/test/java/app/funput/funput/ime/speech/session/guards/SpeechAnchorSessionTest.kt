package app.funput.funput.ime.speech.session

import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.model.SpeechLocale
import org.junit.Assert.*
import org.junit.Test

class SpeechAnchorSessionTest {
    @Test fun pendingAnchorNeverOpensRecordingAndTimesOutAtThreeSeconds() {
        val f = SpeechSpikeFixture()
        f.editor.automaticPreparation = false
        f.controller.start(SpeechLocale.VI)
        f.scheduler.advance(2_999)
        assertEquals(SpeechPhase.PREPARING, f.controller.state.phase)
        assertTrue(f.backend.recordings.isEmpty())
        f.scheduler.advance(1)
        assertEquals(SpeechError.ANCHOR_TIMEOUT, f.controller.state.error)
        assertEquals(1, f.editor.preparationCancels)
        f.editor.prepared!!(f.editor.anchor)
        assertTrue(f.backend.recordings.isEmpty())
    }

    @Test fun delayedAnchorStartsReadyBudgetOnlyWhenRecordingStarts() {
        val f = SpeechSpikeFixture()
        f.editor.automaticPreparation = false
        f.controller.start(SpeechLocale.VI)
        f.scheduler.advance(2_000)
        f.editor.prepared!!(f.editor.anchor)
        assertEquals(1, f.backend.recordings.single().starts)
        f.scheduler.advance(4_999)
        assertNull(f.controller.state.error)
        f.scheduler.advance(1)
        assertEquals(SpeechError.READY_TIMEOUT, f.controller.state.error)
    }

    @Test fun stopPendingAnchorCancelsWithoutOpeningMicrophone() {
        val f = SpeechSpikeFixture()
        f.editor.automaticPreparation = false
        f.controller.start(SpeechLocale.VI)
        f.controller.stop()
        f.editor.prepared!!(f.editor.anchor)
        assertTrue(f.backend.recordings.isEmpty())
        assertEquals(SpeechSessionState(), f.controller.state)
        assertEquals(1, f.editor.preparationCancels)
    }

    @Test fun oldAnchorCallbackCannotStartAReplacementSession() {
        val f = SpeechSpikeFixture()
        f.editor.automaticPreparation = false
        f.controller.start(SpeechLocale.VI)
        val old = f.editor.prepared!!
        f.controller.cancel()
        f.controller.start(SpeechLocale.EN)
        old(f.editor.anchor)
        assertTrue(f.backend.recordings.isEmpty())
        f.editor.prepared!!(f.editor.anchor)
        assertEquals(listOf(SpeechLocale.EN), f.backend.locales)
    }

    @Test fun duplicateAnchorResultStartsOneRecording() {
        val f = SpeechSpikeFixture()
        f.editor.automaticPreparation = false
        f.controller.start(SpeechLocale.VI)
        val callback = f.editor.prepared!!
        callback(f.editor.anchor)
        callback(f.editor.anchor)
        assertEquals(1, f.backend.recordings.size)
        f.backend.recordings.single().emit(SpeechEvent.Final("done"))
        assertEquals("", f.controller.state.preview)
        assertEquals(1, f.editor.preparationCancels)
    }
}
