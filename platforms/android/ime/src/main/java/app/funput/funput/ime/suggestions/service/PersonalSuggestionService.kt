package app.funput.funput.ime.suggestions

import android.content.Context
import app.funput.funput.ime.editing.AuthoredSuggestionUpdate
import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.ImeSuggestionSource
import app.funput.funput.ime.settings.PersonalSuggestionPreferences
import app.funput.funput.ime.settings.pendingReset
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.ui.KeyboardPanel

internal class PersonalSuggestionService(
    context: Context,
    private val show: (List<String>) -> Unit,
    private val shift: () -> ShiftState = { ShiftState.OFF },
    private val acknowledgeReset: (String) -> Unit,
    createWorker: (Context, (PersonalSuggestionRequest, List<String>) -> Unit) -> PersonalSuggestionWorker = ::suggestionWorker,
) {
    private val worker = runCatching { createWorker(context, ::publish) }.getOrNull()
    private var preferences = PersonalSuggestionPreferences.Default
    private var policy = EditorInfoPolicy.Default
    private var panel = KeyboardPanel.LETTERS
    private var session = 0L
    private var generation = 0L
    private var prefix = ""
    /** The word the next one follows, when the tracker can vouch for one. */
    private var previousWord: String? = null
    /** Whether the bar is currently answering a context rather than a prefix. */
    private var predicting = false
    /** As the engine returned them; [candidates] is the same list in the user's case. */
    private var received = emptyList<String>()
    private var candidates = emptyList<String>()
    private var pendingReset: String? = null

    fun configure(value: PersonalSuggestionPreferences) {
        preferences = value
        val reset = value.pendingReset(pendingReset)
        if (reset != null && worker != null) {
            pendingReset = reset
            worker.reset {
                pendingReset = null
                clear()
                acknowledgeReset(reset)
            }
        }
        if (!value.enabled) clear()
    }

    fun start(value: EditorInfoPolicy) {
        session += 1
        policy = value
        panel = KeyboardPanel.LETTERS
        clear()
    }

    fun finish() {
        session += 1
        clear()
        worker?.flush()
    }

    fun updatePanel(value: KeyboardPanel) {
        panel = value
        if (value != KeyboardPanel.LETTERS) clear()
    }

    fun consume(update: AuthoredSuggestionUpdate) {
        update.completedToken
            ?.takeIf { eligible() && policy.allowsPersonalizedLearning }
            ?.let { worker?.learn(it, previousWord) }
        previousWord = update.context
        // A prediction answers a context with no prefix at all. One character is
        // neither a prefix worth completing nor a word boundary, and stays out.
        val predicts = eligible() && update.prefix.isEmpty() && previousWord != null
        if (update.completedToken == null && update.prefix == prefix && predicts == predicting) {
            // Shift arrives here: same words, new shape, nothing to ask the engine.
            return draw()
        }
        prefix = update.prefix
        predicting = predicts
        if (!eligible() || (!predicts && prefix.codePointCount(0, prefix.length) < MinimumPrefix)) {
            return clear()
        }
        generation += 1
        clearCandidates()
        worker?.query(PersonalSuggestionRequest(prefix, generation, session, previousWord))
    }

    fun select(selection: SuggestionSelection, handler: ImeKeyActionHandler): Boolean {
        val candidate = candidates.getOrNull(selection.index) ?: return reject()
        // An empty prefix is the ordinary shape of accepting a prediction: it
        // replaces nothing and inserts a word.
        if (candidate != selection.text || !eligible()) return reject()
        if (!handler.acceptSuggestion(candidate, prefix)) return reject()
        consume(handler.takeSuggestionUpdate())
        return true
    }

    fun flush() = worker?.flush()
    fun close() = worker?.close()

    private fun publish(request: PersonalSuggestionRequest, values: List<String>) {
        if (request.generation != generation || request.session != session || request.prefix != prefix) return
        received = values
        draw()
        suggestionCounter("SuggestionQueryToToolbarUs", (System.nanoTime() - request.startedNanos) / 1_000)
    }

    /** Cases what the engine gave us and shows it, keeping both lists in step: the
     *  accept path matches the tapped text against [candidates], so a bar drawn from a
     *  list nobody stored is a bar whose every tap is silently refused. */
    private fun draw() {
        if (!eligible()) return
        val next = received.map { PersonalSuggestionCasing.apply(it, prefix, shift()) }
        if (next == candidates) return
        candidates = next
        runCatching { show(candidates) }
    }

    private fun eligible() = preferences.enabled && policy.allowsPersonalSuggestions &&
        policy.suggestionSource == ImeSuggestionSource.FUNPUT &&
        panel == KeyboardPanel.LETTERS

    private fun reject(): Boolean = false.also { clear() }

    private fun clear() {
        generation += 1
        prefix = ""
        previousWord = null
        predicting = false
        worker?.clearQueries()
        clearCandidates()
    }

    private fun clearCandidates() {
        // The raw list goes with them, or a later Shift would redraw what was dropped.
        received = emptyList()
        if (candidates.isEmpty()) return
        candidates = emptyList()
        runCatching { show(emptyList()) }
    }

    private companion object {
        const val MinimumPrefix = 2
    }
}
