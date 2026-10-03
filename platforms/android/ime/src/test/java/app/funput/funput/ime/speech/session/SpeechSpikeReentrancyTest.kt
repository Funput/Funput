package app.funput.funput.ime.speech.session

import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.model.SpeechLocale
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechSpikeReentrancyTest {
    @Test
    fun `cancellation from preparing presentation opens no capture`() {
        val f = SpeechSpikeFixture()
        f.onState = {
            if (it.phase == SpeechPhase.PREPARING) f.controller.cancel()
        }
        f.controller.start(SpeechLocale.VI)
        assertEquals(0, f.backend.recordings.size)
        assertEquals(SpeechSessionState(), f.controller.state)
    }

    @Test
    fun `invalidation during editor preparation prevents capture`() {
        val f = SpeechSpikeFixture()
        f.editor.onPrepare = { f.controller.cancel() }
        f.controller.start(SpeechLocale.VI)
        assertEquals(0, f.backend.recordings.size)
        assertEquals(SpeechSessionState(), f.controller.state)
    }

    @Test
    fun `editor preparation failure opens no microphone`() {
        val f = SpeechSpikeFixture()
        f.editor.onPrepare = { throw IllegalStateException("editor unavailable") }
        f.controller.start(SpeechLocale.VI)
        assertEquals(0, f.backend.recordings.size)
        assertEquals(SpeechError.EDITOR_UNAVAILABLE, f.controller.state.error)
    }

    @Test
    fun `stop while anchor is not prepared opens no microphone`() {
        val f = SpeechSpikeFixture()
        f.onState = {
            if (it.phase == SpeechPhase.PREPARING) f.controller.stop()
        }
        f.backend.onStart = { it.emit(SpeechEvent.Ready) }
        f.controller.start(SpeechLocale.VI)
        assertEquals(0, f.backend.recordings.size)
        assertEquals(SpeechPhase.IDLE, f.controller.state.phase)
    }

    @Test
    fun `cancel from finalizing presentation commands no stop`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Ready)
        f.onState = {
            if (it.phase == SpeechPhase.FINALIZING) f.controller.cancel()
        }
        f.controller.stop()
        assertEquals(0, recording.stops)
        assertEquals(1, recording.cancels)
        assertEquals(SpeechSessionState(), f.controller.state)
    }

    @Test
    fun `start exception releases recording and reports backend failure`() {
        val f = SpeechSpikeFixture()
        f.backend.onStart = { throw IllegalStateException("test failure") }
        val recording = f.start()
        assertEquals(SpeechError.BACKEND, f.controller.state.error)
        assertEquals(1, recording.cancels)
        assertEquals(1, recording.closes)
    }

    @Test
    fun `final reentered from stop is consumed safely`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Ready)
        recording.onStop = { recording.emit(SpeechEvent.Final("final")) }
        f.controller.stop()
        assertEquals(listOf("final"), f.editor.commits)
        assertEquals(SpeechPhase.IDLE, f.controller.state.phase)
        assertEquals(1, recording.closes)
    }

    @Test
    fun `start during committing presentation cannot create another session`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.editor.onCommit = { f.controller.start(SpeechLocale.EN) }
        recording.emit(SpeechEvent.Final("first"))
        assertEquals(listOf("first"), f.editor.commits)
        assertEquals(SpeechPhase.IDLE, f.controller.state.phase)
        assertEquals(1, f.backend.recordings.size)
    }

    @Test
    fun `cancel during commit cannot restore old final presentation`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.editor.onCommit = { f.controller.cancel() }
        recording.emit(SpeechEvent.Final("first"))
        assertEquals(SpeechSessionState(), f.controller.state)
        assertEquals(1, f.editor.commitAttempts)
    }
}
