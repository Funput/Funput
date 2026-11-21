package app.funput.funput.ime.suggestions

import android.content.Context
import app.funput.funput.ime.editing.AuthoredSuggestionUpdate
import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.ImeSuggestionSource
import app.funput.funput.ime.settings.PersonalSuggestionPreferences
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditKind
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.ui.KeyboardPanel

internal class PersonalSuggestionService(
    context: Context,
    show: (List<String>) -> Unit,
    shift: () -> ShiftState = { ShiftState.OFF },
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
    /** The tracker's last word: what the next one follows, and the words the bar asks after. */
    private var latest = AuthoredSuggestionUpdate.Empty
    /** Whether the bar is currently answering a context rather than a prefix. */
    private var predicting = false
    private val candidates = PersonalSuggestionCandidates(show, shift)
    private val resets = PersonalSuggestionResetGate(worker) { reset ->
        clear()
        acknowledgeReset(reset)
    }

    fun configure(value: PersonalSuggestionPreferences) {
        preferences = value
        resets.apply(value)
        if (!value.enabled) clear()
    }

    /** Called by [SeedLanguageReportingEngine] whenever the composer's state changes it. */
    fun setSeedLanguage(language: SeedLanguage) {
        worker?.setSeedLanguage(language)
    }

    /** A field gets focus: also the moment to fold in what the settings app posted. */
    fun start(value: EditorInfoPolicy) {
        session += 1
        policy = value
        panel = KeyboardPanel.LETTERS
        clear()
        worker?.applyPendingEdits()
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
            ?.let { worker?.learn(it, latest.learnContext) }
        // The caret can move to other words with no key typed: then the same empty prefix asks anew.
        val unchanged = update.prefix == prefix && update.queryContext == latest.queryContext
        latest = update
        // A prediction answers a context with no prefix at all. One character is
        // neither a prefix worth completing nor a word boundary, and stays out.
        val predicts = eligible() && update.prefix.isEmpty() && latest.context != null
        if (update.completedToken == null && unchanged && predicts == predicting) {
            // Shift arrives here: same words, new shape, nothing to ask the engine.
            if (eligible()) candidates.draw(prefix)
            return
        }
        prefix = update.prefix
        predicting = predicts
        if (!eligible() || (!predicts && prefix.codePointCount(0, prefix.length) < MinimumPrefix)) {
            return clear()
        }
        requery()
    }

    /** Whether a long press may block or forget [selection]: a word this service drew, from a store that lasts. */
    fun isEditable(selection: SuggestionSelection): Boolean =
        eligible() && worker?.persistentAvailable == true && candidates.isShown(selection.index, selection.text)

    /** Applies a long-press edit to the stored spelling behind [selection], then asks again so the bar drops it. */
    fun edit(selection: SuggestionSelection, kind: DictionaryEditKind) {
        if (!isEditable(selection)) return
        val stored = candidates.stored(selection.index, selection.text) ?: return
        worker?.edit(kind, stored)
        candidates.clear()
        requery()
    }

    fun select(selection: SuggestionSelection, handler: ImeKeyActionHandler): Boolean {
        // A held word answered an older prefix. The tap is dropped; the answer on its way still lands.
        if (candidates.held) return false
        // An empty prefix is the ordinary shape of accepting a prediction: it
        // replaces nothing and inserts a word.
        if (!candidates.isShown(selection.index, selection.text) || !eligible()) return reject()
        if (!handler.acceptSuggestion(selection.text, prefix)) return reject()
        consume(handler.takeSuggestionUpdate())
        return true
    }

    fun flush() = worker?.flush()
    fun close() = worker?.close()

    private fun publish(request: PersonalSuggestionRequest, values: List<String>) {
        if (request.generation != generation || request.session != session || request.prefix != prefix) return
        candidates.receive(values, prefix)
        suggestionCounter("SuggestionQueryToToolbarUs", (System.nanoTime() - request.startedNanos) / 1_000)
    }

    /** Asks again for what the bar is answering. What it showed stays up, untappable, until the answer lands. */
    private fun requery() {
        generation += 1
        candidates.hold()
        // No worker means no answer on its way, and nothing to hold the bar for.
        worker?.query(PersonalSuggestionRequest(prefix, generation, session, latest.queryContext)) ?: candidates.clear()
    }

    private fun eligible() = preferences.enabled && policy.allowsPersonalSuggestions &&
        policy.suggestionSource == ImeSuggestionSource.FUNPUT &&
        panel == KeyboardPanel.LETTERS

    private fun reject(): Boolean = false.also { clear() }

    private fun clear() {
        generation += 1
        prefix = ""
        latest = AuthoredSuggestionUpdate.Empty
        predicting = false
        worker?.clearQueries()
        candidates.clear()
    }
}

/** The shortest prefix worth completing. A single character is asked about only as a prediction's context. */
private const val MinimumPrefix = 2
