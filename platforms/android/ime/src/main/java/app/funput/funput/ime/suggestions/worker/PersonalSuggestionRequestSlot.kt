package app.funput.funput.ime.suggestions

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal data class PersonalSuggestionRequest(
    val prefix: String,
    val generation: Long,
    val session: Long,
    /** The word [prefix] is being typed after, when one can be vouched for. */
    val context: String? = null,
    val startedNanos: Long = System.nanoTime(),
)

internal class PersonalSuggestionRequestSlot {
    private val latest = AtomicReference<PersonalSuggestionRequest?>()
    private val drainScheduled = AtomicBoolean(false)

    fun submit(request: PersonalSuggestionRequest): Boolean {
        latest.set(request)
        return drainScheduled.compareAndSet(false, true)
    }

    fun takeLatest(): PersonalSuggestionRequest? = latest.getAndSet(null)

    fun finishDrain(): Boolean {
        drainScheduled.set(false)
        return latest.get() != null && drainScheduled.compareAndSet(false, true)
    }

    fun clear() {
        latest.set(null)
    }
}
