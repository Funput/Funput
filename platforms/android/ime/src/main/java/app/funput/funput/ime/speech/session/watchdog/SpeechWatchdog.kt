package app.funput.funput.ime.speech.session.watchdog

import app.funput.funput.ime.speech.session.SpeechClock
import app.funput.funput.ime.speech.session.SpeechCancellation
import app.funput.funput.ime.speech.session.SpeechScheduler

internal enum class SpeechDeadline(val millis: Long) {
    ANCHOR(3_000), READY(5_000), SESSION(60_000), FINAL(5_000),
}

/** Slot identity also rejects cancelled timers delivered late or inline by a test scheduler. */
internal class SpeechWatchdog(private val scheduler: SpeechScheduler, private val clock: SpeechClock) {
    private class Slot(val expiresAt: Long, var handle: SpeechCancellation? = null)
    private val slots = mutableMapOf<SpeechDeadline, Slot>()
    private var closed = false

    fun arm(deadline: SpeechDeadline, action: () -> Unit) {
        if (closed) return
        disarm(deadline)
        val slot = Slot(clock.nowMillis() + deadline.millis)
        slots[deadline] = slot
        val handle = scheduler.schedule(deadline.millis) {
            if (!closed && slots[deadline] === slot) {
                disarm(deadline)
                action()
            }
        }
        if (slots[deadline] === slot) slot.handle = handle else runCatching { handle.cancel() }
    }

    fun expired(deadline: SpeechDeadline): Boolean = slots[deadline]?.let { clock.nowMillis() >= it.expiresAt } == true

    fun disarm(deadline: SpeechDeadline) {
        val owned = slots.remove(deadline)?.handle
        runCatching { owned?.cancel() }
    }

    fun close() {
        if (closed) return
        closed = true
        val handles = slots.values.mapNotNull { it.handle }
        slots.clear()
        handles.forEach { runCatching { it.cancel() } }
    }
}
