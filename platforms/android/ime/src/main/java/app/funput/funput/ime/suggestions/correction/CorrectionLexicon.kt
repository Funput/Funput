package app.funput.funput.ime.suggestions.correction

import app.funput.funput.ime.nativebridge.PersonalSuggestionNative
import java.io.File

/** The shipped word lists: the English lexicon and the keep list (`ko`, `dc`, brand names). */
internal interface CorrectionLexicon : AutoCloseable {
    /** Whether the lists know [word], or null while they are still loading. */
    fun isKnown(word: String): Boolean?

    override fun close() = Unit
}

/**
 * A read-only, in-memory suggestion store with the English lexicon attached, built once on
 * a thread of its own. It is not the personal store: that one belongs to the suggestion
 * worker's thread, and asking it from the main thread would wait behind a flush.
 */
internal class NativeCorrectionLexicon(attach: ((File) -> Boolean) -> Boolean) : CorrectionLexicon {
    private val lock = Any()
    @Volatile private var handle = 0L
    private var closed = false

    init {
        Thread({
            val created = PersonalSuggestionNative.nativeCreate()
            if (created != 0L) attach { file -> PersonalSuggestionNative.nativeAttachLexicon(created, file.path) }
            synchronized(lock) {
                if (closed) PersonalSuggestionNative.nativeDestroy(created) else handle = created
            }
        }, "FunputCorrectionLexicon").start()
    }

    override fun isKnown(word: String): Boolean? {
        val value = handle.takeIf { it != 0L } ?: return null
        return PersonalSuggestionNative.nativeIsKnownWord(value, word)
    }

    override fun close() = synchronized(lock) {
        closed = true
        if (handle != 0L) PersonalSuggestionNative.nativeDestroy(handle)
        handle = 0L
    }
}
