package app.funput.funput.ime.speech.preparation.operations

import app.funput.funput.ime.speech.platform.SpeechMainQueue
import app.funput.funput.ime.speech.preparation.SpeechPreparationOperation
import app.funput.funput.ime.speech.preparation.platform.PreparationCancellation
import app.funput.funput.ime.speech.preparation.platform.PreparationClient
import app.funput.funput.ime.speech.preparation.platform.PreparationClientFactory
import app.funput.funput.ime.speech.preparation.platform.PreparationTime

/** One temporary client and absolute deadline; cleanup precedes terminal delivery. */
internal abstract class PreparationLease<T>(
    protected val factory: PreparationClientFactory,
    private val main: SpeechMainQueue,
    private val time: PreparationTime,
    private val listener: (T) -> Unit,
) : SpeechPreparationOperation {
    private var started = false
    private var closed = false
    private var deadline: PreparationCancellation? = null
    private var client: PreparationClient? = null

    final override fun start() = main.dispatch {
        if (started || closed) return@dispatch
        started = true
        startOnMain()
    }

    protected abstract fun startOnMain()

    protected fun acquire(timeoutMillis: Long, failure: () -> T, command: (PreparationClient) -> Unit) {
        try {
            val owned = factory.create()
            client = owned
            val timer = time.schedule(timeoutMillis) { receive { deliver(failure()) } }
            if (closed) timer.cancel() else deadline = timer
            if (!closed) command(owned)
        } catch (_: Exception) {
            deliver(failure())
        }
    }

    protected fun receive(action: () -> Unit) = main.dispatch {
        if (started && !closed) action()
    }

    protected fun deliver(value: T, terminal: Boolean = true) {
        if (closed || !started) return
        if (terminal) release()
        listener(value)
    }

    final override fun close() = main.dispatch { release() }

    private fun release() {
        if (closed) return
        closed = true
        deadline?.cancel()
        deadline = null
        val owned = client
        client = null
        runCatching { owned?.close() }
    }
}
