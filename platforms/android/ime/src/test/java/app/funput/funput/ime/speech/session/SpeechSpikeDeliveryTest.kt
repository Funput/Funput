package app.funput.funput.ime.speech.session

import app.funput.funput.ime.speech.model.SpeechEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechSpikeDeliveryTest {
    @Test
    fun `final needs no ready or end event and trims only outer whitespace`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Final(" \nXin CHÀO,  bạn🙂!\nđến đây\t "))
        assertEquals(listOf("Xin CHÀO,  bạn🙂!\nđến đây"), f.editor.commits)
        assertEquals(1, recording.closes)
        assertEquals(0, recording.cancels)
    }

    @Test
    fun `duplicate and reentrant final commit at most once`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.editor.onCommit = { recording.emit(SpeechEvent.Final("reentrant")) }
        recording.emit(SpeechEvent.Final("first"))
        recording.emit(SpeechEvent.Final("duplicate"))
        assertEquals(listOf("first"), f.editor.commits)
        assertEquals(1, f.editor.commitAttempts)
        assertEquals(1, recording.closes)
    }

    @Test
    fun `rejected commit is consumed without retry or partial fallback`() {
        val f = SpeechSpikeFixture()
        f.editor.accepts = false
        val recording = f.start()
        recording.emit(SpeechEvent.Partial("partial"))
        recording.emit(SpeechEvent.Final("final"))
        f.editor.accepts = true
        recording.emit(SpeechEvent.Final("retry"))
        assertEquals(1, f.editor.commitAttempts)
        assertEquals(emptyList<String>(), f.editor.commits)
        assertEquals(SpeechError.COMMIT_REJECTED, f.controller.state.error)
    }

    @Test
    fun `empty final never commits`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Final(" \t\n"))
        assertEquals(0, f.editor.commitAttempts)
        assertEquals(SpeechError.EMPTY_FINAL, f.controller.state.error)
    }

    @Test
    fun `limit counts UTF16 units without truncating emoji`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Final("🙂".repeat(2_049)))
        assertEquals(0, f.editor.commitAttempts)
        assertEquals(SpeechError.TEXT_TOO_LONG, f.controller.state.error)
    }

    @Test
    fun `exact maximum Unicode text is preserved`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        val text = "🙂".repeat(2_048)
        recording.emit(SpeechEvent.Final(text))
        assertEquals(listOf(text), f.editor.commits)
    }

    @Test
    fun `oversized partial stops capture without retaining it`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Partial("x".repeat(4_097)))
        assertEquals(SpeechError.TEXT_TOO_LONG, f.controller.state.error)
        assertEquals("", f.controller.state.preview)
        assertEquals(0, f.editor.commitAttempts)
        assertEquals(1, recording.cancels)
    }

    @Test
    fun `changed generation rejects final even at the same caret`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.editor.anchor = SpeechEditorAnchor(2, 0, 0)
        recording.emit(SpeechEvent.Final("another editor"))
        assertEquals(emptyList<String>(), f.editor.commits)
        assertEquals(SpeechError.COMMIT_REJECTED, f.controller.state.error)
    }

    @Test
    fun `caret returning to original position keeps revision invalid`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.editor.anchor = SpeechEditorAnchor(1, 1, 5)
        f.editor.anchor = SpeechEditorAnchor(1, 2, 0)
        recording.emit(SpeechEvent.Final("stale anchor"))
        assertEquals(emptyList<String>(), f.editor.commits)
        assertEquals(SpeechError.COMMIT_REJECTED, f.controller.state.error)
    }
}
