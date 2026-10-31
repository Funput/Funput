package app.funput.funput.ime.suggestions

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditKind
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditOutcome
import java.io.File
import java.util.concurrent.atomic.AtomicReference

internal class PersonalSuggestionWorker(
    private val storeDirectory: () -> File,
    private val attachLexicon: (PersonalSuggestionEngine) -> Unit = {},
    private val publish: (PersonalSuggestionRequest, List<String>) -> Unit,
) {
    private val thread = HandlerThread("FunputSuggestions", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }
    private val worker = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())
    private val requests = PersonalSuggestionRequestSlot()
    private var engine: PersonalSuggestionEngine? = null
    /** Whether edits last: false while the store is in memory, read from the main thread. */
    @Volatile var persistentAvailable = false
        private set
    private var learnedSinceFlush = 0
    private val idleFlush = Runnable(::flushNow)
    private val drain = Runnable(::drainQueries)
    /** Set on the main thread, taken on the worker; see [setSeedLanguage]. */
    private val pendingSeed = AtomicReference<SeedLanguage?>()
    private val applySeed = Runnable(::applyPendingSeed)

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

    /** Folds in what the settings app posted; it waits in the inbox until the store is open. */
    fun applyPendingEdits() = worker.post {
        if (persistentAvailable) engine?.applyPendingEdits()
    }

    /** A deliberate choice rather than a keystroke, so it is made durable at once. */
    fun edit(kind: DictionaryEditKind, word: String) = worker.post {
        if (persistentAvailable && engine?.applyEdit(kind, word) == DictionaryEditOutcome.APPLIED) flushNow()
    }

    fun flush() = worker.post(::flushNow)

    /**
     * Takes effect before any query submitted after it. A query can be drained in the same
     * pass as the one before it, ahead of anything posted in between, so each query applies
     * the latest setting itself rather than relying on a posted task's turn. The post covers
     * a change with no query behind it; [openEngine] always runs first, as `init` posts it.
     */
    fun setSeedLanguage(language: SeedLanguage) {
        pendingSeed.set(language)
        worker.post(applySeed)
    }

    fun clearQueries() = requests.clear()

    fun close(onClosed: () -> Unit = {}) {
        requests.clear()
        worker.removeCallbacks(idleFlush)
        worker.post {
            flushNow()
            engine?.close()
            engine = null
            thread.quitSafely()
            onClosed()
        }
    }

    private fun openEngine() = suggestionTrace("SuggestionOpen") {
        engine = runCatching { PersonalSuggestionEngine.open(storeDirectory()) }.getOrNull()
        persistentAvailable = engine != null
        if (engine == null) engine = PersonalSuggestionEngine.inMemory()
        engine?.let { runCatching { attachLexicon(it) } }
    }

    private fun drainQueries() {
        while (true) {
            val request = requests.takeLatest() ?: break
            applyPendingSeed()
            suggestionCounter("SuggestionGeneration", request.generation)
            val result = suggestionTrace("SuggestionJNIQuery") { engine?.query(request.prefix, request.context).orEmpty() }
            main.post { runCatching { publish(request, result) } }
        }
        if (requests.finishDrain()) worker.post(drain)
    }

    private fun applyPendingSeed() {
        pendingSeed.getAndSet(null)?.let { engine?.setSeedLanguage(it) }
    }

    private fun flushNow(): Unit = suggestionTrace("SuggestionFlush") {
        worker.removeCallbacks(idleFlush)
        if (engine?.flush() == true) learnedSinceFlush = 0
    }

    private companion object {
        const val FlushWordCount = 32
        const val IdleFlushMillis = 2_000L
    }
}
