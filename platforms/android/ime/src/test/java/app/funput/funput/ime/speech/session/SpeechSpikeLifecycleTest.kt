package app.funput.funput.ime.speech.session

import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.ime.speech.model.SpeechLocale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechSpikeLifecycleTest {
    @Test
    fun `missing editor does not open recording`() {
        val f = SpeechSpikeFixture()
        f.editor.anchor = null
        f.controller.start(SpeechLocale.VI)
        assertTrue(f.backend.recordings.isEmpty())
        assertEquals(SpeechError.EDITOR_UNAVAILABLE, f.controller.state.error)
    }

    @Test
    fun `partials replace preview without editing`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Ready)
        recording.emit(SpeechEvent.Partial("xin chao"))
        recording.emit(SpeechEvent.Partial("Xin chào Hà Nội"))
        assertEquals(SpeechPhase.LISTENING, f.controller.state.phase)
        assertEquals("Xin chào Hà Nội", f.controller.state.preview)
        assertEquals(0, f.editor.commitAttempts)
    }

    @Test
    fun `synchronous final during start owns handle before callback`() {
        val f = SpeechSpikeFixture()
        f.backend.onStart = { it.emit(SpeechEvent.Final("đã xong")) }
        val recording = f.start()
        assertEquals(listOf("đã xong"), f.editor.commits)
        assertEquals(1, recording.closes)
        assertEquals(SpeechPhase.IDLE, f.controller.state.phase)
    }

    @Test
    fun `stop before ready waits and stops only once`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.controller.stop()
        f.controller.stop()
        assertEquals(0, recording.stops)
        assertEquals(SpeechPhase.FINALIZING, f.controller.state.phase)
        recording.emit(SpeechEvent.Ready)
        recording.emit(SpeechEvent.Ready)
        assertEquals(1, recording.stops)
        assertEquals(SpeechPhase.FINALIZING, f.controller.state.phase)
    }

    @Test
    fun `cancel during finalizing drops final and releases once`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Ready)
        f.controller.stop()
        f.controller.cancel()
        f.controller.cancel()
        recording.emit(SpeechEvent.Final("không chèn"))
        assertEquals(0, f.editor.commitAttempts)
        assertEquals(1, recording.cancels)
        assertEquals(1, recording.closes)
        assertEquals(SpeechSessionState(), f.controller.state)
    }

    @Test
    fun `close prevents recording and ignores late callbacks`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.controller.close()
        f.controller.close()
        f.controller.start(SpeechLocale.EN)
        recording.emit(SpeechEvent.Ready)
        recording.emit(SpeechEvent.Final("discard"))
        assertEquals(1, f.backend.recordings.size)
        assertEquals(1, recording.closes)
        assertEquals(0, f.editor.commitAttempts)
    }

    @Test
    fun `backend error carries code and has no retry`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Failure(9))
        recording.emit(SpeechEvent.Failure(5))
        assertEquals(9, f.controller.state.backendErrorCode)
        assertEquals(SpeechError.BACKEND, f.controller.state.error)
        assertEquals(1, f.backend.recordings.size)
        assertEquals(1, recording.closes)
    }

    @Test
    fun `new start is ignored until the active session is cancelled`() {
        val f = SpeechSpikeFixture()
        val old = f.start()
        f.controller.start(SpeechLocale.EN)
        assertEquals(1, f.backend.recordings.size)
        f.controller.cancel()
        f.controller.start(SpeechLocale.EN)
        old.emit(SpeechEvent.Final("cũ"))
        val current = f.backend.recordings.last()
        current.emit(SpeechEvent.Final("new"))
        assertEquals(listOf("new"), f.editor.commits)
        assertEquals(listOf(SpeechLocale.VI, SpeechLocale.EN), f.backend.locales)
        assertEquals(1, old.cancels)
        assertEquals(1, old.closes)
    }
}
