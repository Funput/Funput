package app.funput.funput.ime.suggestions

import app.funput.funput.ime.nativebridge.PersonalSuggestionNative
import app.funput.funput.ime.suggestions.dictionary.DictionaryCodes
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditKind
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditOutcome
import java.io.Closeable
import java.io.File

internal class PersonalSuggestionEngine private constructor(private var handle: Long) : Closeable {
    private val ownerThread = Thread.currentThread()

    fun learn(token: String, after: String? = null): Boolean = call(false) {
        PersonalSuggestionNative.nativeLearnAfter(it, after.orEmpty(), token)
    }
    fun query(prefix: String, after: String? = null): List<String> = call(emptyList()) { value ->
        PersonalSuggestionNative.nativeQueryWith(value, after.orEmpty(), prefix)
            ?.take(MaxCandidates)
            .orEmpty()
    }
    fun attachLexicon(file: File): Boolean = call(false) {
        PersonalSuggestionNative.nativeAttachLexicon(it, file.absolutePath)
    }
    fun setSeedLanguage(language: SeedLanguage): Boolean = call(false) {
        PersonalSuggestionNative.nativeSetSeedLanguage(it, language.wireCode, language.toneStyleValue)
    }
    fun flush(): Boolean = call(false, PersonalSuggestionNative::nativeFlush)
    fun compact(): Boolean = call(false, PersonalSuggestionNative::nativeCompact)
    fun reset(): Boolean = call(false, PersonalSuggestionNative::nativeReset)

    /** Durable on the next [flush], like anything learned. */
    fun applyEdit(kind: DictionaryEditKind, word: String): DictionaryEditOutcome =
        call(DictionaryEditOutcome.INVALID) {
            DictionaryCodes.outcome(PersonalSuggestionNative.nativeApplyEdit(it, DictionaryCodes.of(kind), word))
        }

    /** How many edits the app posted were applied, or null when the inbox could not be read. */
    fun applyPendingEdits(): Int? = call(null) { value ->
        PersonalSuggestionNative.nativeApplyPendingEdits(value).takeIf { it >= 0 }
    }
    fun stats(): PersonalSuggestionStats = call(PersonalSuggestionStats.Empty) { value ->
        PersonalSuggestionStats.decode(PersonalSuggestionNative.nativeStats(value))
    }

    override fun close() {
        if (!ownsThread() || handle == InvalidHandle) return
        runCatching { PersonalSuggestionNative.nativeDestroy(handle) }
        handle = InvalidHandle
    }

    private inline fun <T> call(default: T, operation: (Long) -> T): T {
        if (!ownsThread() || handle == InvalidHandle) return default
        return runCatching { operation(handle) }.getOrDefault(default)
    }

    private fun ownsThread() = Thread.currentThread() === ownerThread

    companion object {
        private const val InvalidHandle = 0L
        private const val MaxCandidates = 3

        fun inMemory(): PersonalSuggestionEngine? = create {
            PersonalSuggestionNative.nativeCreate()
        }

        fun open(directory: File): PersonalSuggestionEngine? = create {
            PersonalSuggestionNative.nativeOpen(directory.absolutePath)
        }

        private inline fun create(operation: () -> Long): PersonalSuggestionEngine? {
            val handle = runCatching(operation).getOrDefault(InvalidHandle)
            return handle.takeIf { it != InvalidHandle }?.let(::PersonalSuggestionEngine)
        }
    }
}
