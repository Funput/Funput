package app.funput.funput.ime.speech.session

import app.funput.funput.ime.speech.model.SpeechEvent

internal sealed interface SpeechSessionEvent {
    data object Anchored : SpeechSessionEvent
    data object Stop : SpeechSessionEvent
    data class Recognition(val event: SpeechEvent) : SpeechSessionEvent
    data class Failed(val reason: SpeechError, val code: Int? = null) : SpeechSessionEvent
}

internal sealed interface SpeechEffect {
    data object Record : SpeechEffect
    data object Stop : SpeechEffect
    data object AwaitFinal : SpeechEffect
    data object Ready : SpeechEffect
    data object Abort : SpeechEffect
    data class Commit(val text: String) : SpeechEffect
}

internal data class SpeechTransition(
    val state: SpeechSessionState,
    val effects: List<SpeechEffect> = emptyList(),
)

/** Pure transition rules. Ownership, timers, framework commands and writes belong to adapters. */
internal object SpeechSessionReducer {
    fun reduce(state: SpeechSessionState, event: SpeechSessionEvent): SpeechTransition {
        if (state.phase !in listOf(SpeechPhase.PREPARING, SpeechPhase.LISTENING, SpeechPhase.FINALIZING)) {
            return SpeechTransition(state)
        }
        return when (event) {
            SpeechSessionEvent.Anchored -> if (state.anchored) SpeechTransition(state)
                else SpeechTransition(state.copy(anchored = true), listOf(SpeechEffect.Record))
            SpeechSessionEvent.Stop -> when {
                !state.anchored -> SpeechTransition(SpeechSessionState(), listOf(SpeechEffect.Abort))
                state.phase == SpeechPhase.FINALIZING -> SpeechTransition(state)
                else -> finalize(state, requestStop = true)
            }
            is SpeechSessionEvent.Failed -> fail(event.reason, event.code)
            is SpeechSessionEvent.Recognition -> if (!state.anchored) SpeechTransition(state)
                else recognition(state, event.event)
        }
    }

    private fun recognition(state: SpeechSessionState, event: SpeechEvent): SpeechTransition = when (event) {
        SpeechEvent.Ready -> if (state.ready) SpeechTransition(state) else {
            val shouldStop = state.stopRequested && !state.stopIssued
            SpeechTransition(state.copy(ready = true, stopIssued = shouldStop,
                phase = if (state.phase == SpeechPhase.FINALIZING) state.phase else SpeechPhase.LISTENING),
                listOf(SpeechEffect.Ready) + if (shouldStop) listOf(SpeechEffect.Stop) else emptyList())
        }
        SpeechEvent.Ended -> if (state.phase == SpeechPhase.FINALIZING) SpeechTransition(state)
            else finalize(state, requestStop = false)
        is SpeechEvent.Partial -> if (event.text.length > SpeechFinalDelivery.MAX_TEXT_UNITS) {
            fail(SpeechError.TEXT_TOO_LONG)
        } else SpeechTransition(state.copy(preview = event.text))
        is SpeechEvent.Final -> when {
            event.text.length > SpeechFinalDelivery.MAX_TEXT_UNITS -> fail(SpeechError.TEXT_TOO_LONG)
            event.text.isBlank() -> fail(SpeechError.EMPTY_FINAL)
            else -> SpeechTransition(state.copy(phase = SpeechPhase.COMMITTING, preview = ""),
                listOf(SpeechEffect.Commit(event.text.trim())))
        }
        is SpeechEvent.Failure -> fail(SpeechError.BACKEND, event.code)
    }

    private fun finalize(state: SpeechSessionState, requestStop: Boolean): SpeechTransition {
        val shouldStop = requestStop && state.ready && !state.stopIssued
        return SpeechTransition(state.copy(phase = SpeechPhase.FINALIZING, stopRequested = requestStop,
            stopIssued = shouldStop), listOf(SpeechEffect.AwaitFinal) +
            if (shouldStop) listOf(SpeechEffect.Stop) else emptyList())
    }

    private fun fail(reason: SpeechError, code: Int?) = SpeechTransition(
        SpeechSessionState(phase = SpeechPhase.ERROR, error = reason, backendErrorCode = code),
        listOf(SpeechEffect.Abort),
    )

    private fun fail(reason: SpeechError) = fail(reason, null)
}
