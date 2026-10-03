package app.funput.funput.ime.speech.session

import app.funput.funput.ime.speech.model.SpeechEvent
import org.junit.Assert.*
import org.junit.Test

class SpeechReducerTest {
    private val listening = SpeechSessionState(SpeechPhase.LISTENING,
        preview = "partial", sessionId = 1, anchored = true, ready = true)

    @Test fun directFinalIsConsumedWithoutAnEndpointAndClearsPreview() {
        val next = recognition(listening, SpeechEvent.Final("  Hello,  Bạn🙂!  "))
        assertEquals(SpeechPhase.COMMITTING, next.state.phase)
        assertEquals("", next.state.preview)
        assertEquals(listOf(SpeechEffect.Commit("Hello,  Bạn🙂!")), next.effects)
        assertTrue(recognition(next.state, SpeechEvent.Final("duplicate")).effects.isEmpty())
    }

    @Test fun stopIsIssuedAtMostOnceAndLateReadyDoesNotResumeListening() {
        val preparing = listening.copy(phase = SpeechPhase.PREPARING, ready = false)
        val stopped = SpeechSessionReducer.reduce(preparing, SpeechSessionEvent.Stop)
        assertFalse(SpeechEffect.Stop in stopped.effects)
        val ready = recognition(stopped.state, SpeechEvent.Ready)
        assertEquals(SpeechPhase.FINALIZING, ready.state.phase)
        assertEquals(1, ready.effects.count { it == SpeechEffect.Stop })
        assertTrue(recognition(ready.state, SpeechEvent.Ready).effects.isEmpty())
        assertTrue(SpeechSessionReducer.reduce(ready.state, SpeechSessionEvent.Stop).effects.isEmpty())
    }

    @Test fun endpointWaitsForFinalWithoutCallingStop() {
        val ended = recognition(listening, SpeechEvent.Ended)
        assertEquals(listOf(SpeechEffect.AwaitFinal), ended.effects)
        assertTrue(SpeechSessionReducer.reduce(ended.state, SpeechSessionEvent.Stop).effects.isEmpty())
    }

    @Test fun noRecognitionEventIsAcceptedBeforeAnchor() {
        val preparing = SpeechSessionState(SpeechPhase.PREPARING, sessionId = 1)
        assertEquals(preparing, recognition(preparing, SpeechEvent.Final("early")).state)
        assertEquals(listOf(SpeechEffect.Abort), SpeechSessionReducer.reduce(preparing, SpeechSessionEvent.Stop).effects)
    }

    @Test fun oversizedPartialAbortsWithoutKeepingText() {
        val result = recognition(listening, SpeechEvent.Partial("🙂".repeat(2_049)))
        assertEquals(SpeechError.TEXT_TOO_LONG, result.state.error)
        assertEquals("", result.state.preview)
        assertEquals(listOf(SpeechEffect.Abort), result.effects)
    }

    private fun recognition(state: SpeechSessionState, event: SpeechEvent) =
        SpeechSessionReducer.reduce(state, SpeechSessionEvent.Recognition(event))
}
