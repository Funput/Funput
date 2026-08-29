package app.funput.funput.ime.suggestions

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import java.io.File

internal class PersonalSuggestionWorker(
    private val storeDirectory: () -> File,
    private val publish: (PersonalSuggestionRequest, List<String>) -> Unit,
) {
    private val thread = HandlerThread("FunputSuggestions", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }
    private val worker = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())
    private val requests = PersonalSuggestionRequestSlot()
    private var engine: PersonalSuggestionEngine? = null
    private var persistentAvailable = false
    private var learnedSinceFlush = 0
    private val idleFlush = Runnable(::flushNow)
    private val drain = Runnable(::drainQueries)

    init {
        worker.post(::openEngine)
    }

    fun query(request: PersonalSuggestionRequest) {
        if (requests.submit(request)) worker.post(drain)
    }

    fun learn(token: String, after: String?) = worker.post {
        if (engine?.learn(token, after) != true) return@post
        learnedSinceFlush += 1
        worker.removeCallbacks(idleFlush)
        if (learnedSinceFlush >= FlushWordCount) flushNow() else worker.postDelayed(idleFlush, IdleFlushMillis)
    }

    fun reset(onApplied: () -> Unit) = worker.post {
        if (!persistentAvailable || engine?.reset() != true) return@post
        learnedSinceFlush = 0
        main.post { runCatching(onApplied) }
    }

    fun flush() = worker.post(::flushNow)

    fun clearQueries() = requests.clear()

    fun close() {
        requests.clear()
        worker.removeCallbacks(idleFlush)
        worker.post {
            flushNow()
            engine?.close()
            engine = null
            thread.quitSafely()
        }
    }

    private fun openEngine() = suggestionTrace("SuggestionOpen") {
        engine = runCatching { PersonalSuggestionEngine.open(storeDirectory()) }.getOrNull()
        persistentAvailable = engine != null
        if (engine == null) engine = PersonalSuggestionEngine.inMemory()
    }

    private fun drainQueries() {
        while (true) {
            val request = requests.takeLatest() ?: break
            suggestionCounter("SuggestionGeneration", request.generation)
            val result = suggestionTrace("SuggestionJNIQuery") { engine?.query(request.prefix, request.context).orEmpty() }
            main.post { runCatching { publish(request, result) } }
        }
        if (requests.finishDrain()) worker.post(drain)
    }

    private fun flushNow() = suggestionTrace("SuggestionFlush") {
        worker.removeCallbacks(idleFlush)
        if (engine?.flush() == true) learnedSinceFlush = 0
    }

    private companion object {
        const val FlushWordCount = 32
        const val IdleFlushMillis = 2_000L
    }
}
