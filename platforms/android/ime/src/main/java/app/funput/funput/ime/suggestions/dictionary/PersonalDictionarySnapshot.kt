package app.funput.funput.ime.suggestions.dictionary

import app.funput.funput.ime.nativebridge.PersonalSuggestionNative
import java.io.Closeable
import java.io.File

/**
 * A read-only view of the store the keyboard owns, as the settings app sees it.
 *
 * Opening writes nothing and includes the edits still waiting in the inbox, so a listing
 * shows what the store is about to be. [preview] asks the engine whether an edit would
 * apply, under the rules the keyboard will apply it by, and changes only this view;
 * [post] is what makes an edit real. Not thread-safe: one owner at a time.
 */
internal class PersonalDictionarySnapshot private constructor(
    private val directory: File,
    private var handle: Long,
) : Closeable {
    fun words(query: DictionaryQuery): List<DictionaryWord> =
        if (query.filter == DictionaryFilter.BLOCKED) blocked(query.search) else listed(query)

    fun count(filter: DictionaryFilter): Int = words(DictionaryQuery(filter = filter)).size

    fun preview(kind: DictionaryEditKind, word: String): DictionaryEditOutcome {
        if (handle == InvalidHandle) return DictionaryEditOutcome.INVALID
        val code = runCatching { PersonalSuggestionNative.nativeApplyEdit(handle, DictionaryCodes.of(kind), word) }
        return code.map(DictionaryCodes::outcome).getOrDefault(DictionaryEditOutcome.INVALID)
    }

    /** Hands the edit to the keyboard, which applies it the next time it starts on a field. */
    fun post(kind: DictionaryEditKind, word: String): Boolean = runCatching {
        PersonalSuggestionNative.nativePostEdit(directory.absolutePath, DictionaryCodes.of(kind), word)
    }.getOrDefault(false)

    override fun close() {
        if (handle == InvalidHandle) return
        runCatching { PersonalSuggestionNative.nativeDestroy(handle) }
        handle = InvalidHandle
    }

    private fun listed(query: DictionaryQuery): List<DictionaryWord> {
        if (handle == InvalidHandle) return emptyList()
        val filter = DictionaryCodes.of(query.filter)
        val sort = DictionaryCodes.of(query.sort)
        val search = query.search
        val words = runCatching { PersonalSuggestionNative.nativeDictionaryWords(handle, filter, sort, search) }
        val details = runCatching { PersonalSuggestionNative.nativeDictionaryDetails(handle, filter, sort, search) }
        return DictionaryCodes.words(words.getOrNull(), details.getOrNull())
    }

    private fun blocked(search: String): List<DictionaryWord> {
        if (handle == InvalidHandle) return emptyList()
        val words = runCatching { PersonalSuggestionNative.nativeBlockedWords(handle) }.getOrNull().orEmpty()
        val needle = DictionarySearchKey(search)
        return words
            .filter { needle.isEmpty || DictionarySearchKey(it).contains(needle) }
            .map { DictionaryWord(it, uses = 0, lastUsed = 0, origin = DictionaryOrigin.BLOCKED) }
    }

    companion object {
        private const val InvalidHandle = 0L

        /** Null when the store cannot be read at all; a store not created yet reads as empty. */
        fun open(directory: File): PersonalDictionarySnapshot? {
            val handle = runCatching { PersonalSuggestionNative.nativeOpenSnapshot(directory.absolutePath) }
                .getOrDefault(InvalidHandle)
            return handle.takeIf { it != InvalidHandle }?.let { PersonalDictionarySnapshot(directory, it) }
        }
    }
}
