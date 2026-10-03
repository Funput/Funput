package app.funput.funput.ime.speech.session

import app.funput.funput.ime.speech.model.SpeechEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechSpikeTimeoutTest {
    @Test
    fun `cancelled ready timer cannot end a listening session`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        val readyTimer = f.scheduler.tasks.first { it.at == 5_000L }
        recording.emit(SpeechEvent.Ready)
        readyTimer.action()
        assertEquals(SpeechPhase.LISTENING, f.controller.state.phase)
        assertEquals(0, recording.closes)
    }

    @Test
    fun `ready timeout cancels capture after five seconds`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.scheduler.advance(4_999)
        assertEquals(SpeechPhase.PREPARING, f.controller.state.phase)
        f.scheduler.advance(1)
        assertEquals(SpeechError.READY_TIMEOUT, f.controller.state.error)
        assertEquals(1, recording.cancels)
        assertEquals(1, recording.closes)
    }

    @Test
    fun `total session limit stops once at sixty seconds and awaits final`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        f.scheduler.advance(4_000)
        recording.emit(SpeechEvent.Ready)
        f.scheduler.advance(55_999)
        assertEquals(SpeechPhase.LISTENING, f.controller.state.phase)
        f.scheduler.advance(1)
        assertEquals(SpeechPhase.FINALIZING, f.controller.state.phase)
        assertEquals(1, recording.stops)
        recording.emit(SpeechEvent.Final("bounded final"))
        assertEquals(listOf("bounded final"), f.editor.commits)
    }

    @Test
    fun `stop waits five seconds for final then drops late final`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Ready)
        f.controller.stop()
        f.scheduler.advance(4_999)
        assertEquals(SpeechPhase.FINALIZING, f.controller.state.phase)
        f.scheduler.advance(1)
        recording.emit(SpeechEvent.Final("too late"))
        assertEquals(SpeechError.FINAL_TIMEOUT, f.controller.state.error)
        assertEquals(0, f.editor.commitAttempts)
        assertEquals(1, recording.closes)
    }

    @Test
    fun `natural endpoint waits exactly five seconds without commanding stop`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Ended)
        assertEquals(SpeechPhase.FINALIZING, f.controller.state.phase)
        f.scheduler.advance(4_000)
        recording.emit(SpeechEvent.Ended)
        f.controller.stop()
        f.scheduler.advance(999)
        assertEquals(SpeechPhase.FINALIZING, f.controller.state.phase)
        f.scheduler.advance(1)
        assertEquals(SpeechError.FINAL_TIMEOUT, f.controller.state.error)
        assertEquals(0, recording.stops)
        assertEquals(1, recording.closes)
    }

    @Test
    fun `natural endpoint accepts final and ignores later ready`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Ended)
        recording.emit(SpeechEvent.Ready)
        assertEquals(SpeechPhase.FINALIZING, f.controller.state.phase)
        recording.emit(SpeechEvent.Final("endpoint final"))
        assertEquals(listOf("endpoint final"), f.editor.commits)
        assertEquals(0, recording.stops)
    }

    @Test
    fun `cancelled timer delivered by scheduler cannot affect next session`() {
        val f = SpeechSpikeFixture()
        f.start()
        val oldTimers = f.scheduler.tasks.toList()
        f.controller.cancel()
        val current = f.start()
        current.emit(SpeechEvent.Ready)
        oldTimers.forEach { it.action() }
        assertEquals(SpeechPhase.LISTENING, f.controller.state.phase)
        assertEquals(0, current.closes)
        current.emit(SpeechEvent.Final("current"))
        assertEquals(listOf("current"), f.editor.commits)
    }

    @Test
    fun `terminal result cancels each pending timer exactly once`() {
        val f = SpeechSpikeFixture()
        val recording = f.start()
        recording.emit(SpeechEvent.Ready)
        f.controller.stop()
        recording.emit(SpeechEvent.Final("done"))
        f.controller.close()
        assertEquals(listOf(1, 1, 1, 1), f.scheduler.tasks.map { it.cancellations })
        f.scheduler.tasks.forEach { it.action() }
        assertEquals(listOf("done"), f.editor.commits)
        assertEquals(1, recording.closes)
    }
}
