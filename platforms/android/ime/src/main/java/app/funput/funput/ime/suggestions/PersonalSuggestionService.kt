package app.funput.funput.ime.suggestions

import android.content.Context
import app.funput.funput.ime.editing.AuthoredSuggestionUpdate
import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.ImeSuggestionSource
import app.funput.funput.ime.settings.PersonalSuggestionPreferences
import app.funput.funput.ime.settings.pendingReset
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.ui.KeyboardPanel

internal class PersonalSuggestionService(
    context: Context,
    private val show: (List<String>) -> Unit,
    private val acknowledgeReset: (String) -> Unit,
) {
    private val worker = runCatching {
        PersonalSuggestionWorker(
            storeDirectory = { FileStore.directory(context) },
            publish = ::publish,
        )
    }.getOrNull()
    private var preferences = PersonalSuggestionPreferences.Default
    private var policy = EditorInfoPolicy.Default
    private var language = KeyboardLanguage.VIETNAMESE
    private var panel = KeyboardPanel.LETTERS
    private var session = 0L
    private var generation = 0L
    private var prefix = ""
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

    fun updateLanguage(value: KeyboardLanguage) {
        language = value
        if (value != KeyboardLanguage.VIETNAMESE) clear()
    }

    fun updatePanel(value: KeyboardPanel) {
        panel = value
        if (value != KeyboardPanel.LETTERS) clear()
    }

    fun consume(update: AuthoredSuggestionUpdate) {
        update.completedToken?.takeIf { eligible() && policy.allowsPersonalizedLearning }?.let { worker?.learn(it) }
        if (update.completedToken == null && update.prefix == prefix) return
        prefix = update.prefix
        if (!eligible() || prefix.codePointCount(0, prefix.length) < MinimumPrefix) return clear()
        generation += 1
        clearCandidates()
        worker?.query(PersonalSuggestionRequest(prefix, generation, session))
    }

    fun select(selection: SuggestionSelection, handler: ImeKeyActionHandler): Boolean {
        val candidate = candidates.getOrNull(selection.index) ?: return reject()
        if (candidate != selection.text || !eligible() || prefix.isEmpty()) return reject()
        if (!handler.acceptSuggestion(candidate, prefix)) return reject()
        consume(handler.takeSuggestionUpdate())
        return true
    }

    fun flush() = worker?.flush()
    fun close() = worker?.close()

    private fun publish(request: PersonalSuggestionRequest, values: List<String>) {
        if (request.generation != generation || request.session != session || request.prefix != prefix) return
        if (!eligible()) return
        candidates = values.map { PersonalSuggestionCasing.apply(it, prefix) }
        runCatching { show(candidates) }
        suggestionCounter("SuggestionQueryToToolbarUs", (System.nanoTime() - request.startedNanos) / 1_000)
    }

    private fun eligible() = preferences.enabled && policy.allowsPersonalSuggestions &&
        policy.suggestionSource == ImeSuggestionSource.FUNPUT &&
        language == KeyboardLanguage.VIETNAMESE && panel == KeyboardPanel.LETTERS

    private fun reject(): Boolean = false.also { clear() }

    private fun clear() {
        generation += 1
        prefix = ""
        worker?.clearQueries()
        clearCandidates()
    }

    private fun clearCandidates() {
        if (candidates.isEmpty()) return
        candidates = emptyList()
        runCatching { show(emptyList()) }
    }

    private object FileStore {
        fun directory(context: Context) = context.noBackupFilesDir.resolve("PersonalSuggestions")
    }

    private companion object {
        const val MinimumPrefix = 2
    }
}
